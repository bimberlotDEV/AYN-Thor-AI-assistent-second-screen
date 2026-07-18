package com.gameside.data.ai

import com.gameside.domain.ai.AiAnswerRequest
import com.gameside.domain.ai.AiGenerationEvent
import com.gameside.domain.ai.AiCredentialIds
import com.gameside.domain.ai.AiProviderException
import com.gameside.domain.ai.TextAiProvider
import com.gameside.domain.security.CredentialStore
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject

class DeepSeekTextAiProvider @Inject constructor(
    private val credentialStore: CredentialStore,
) : TextAiProvider {
    override fun generateAnswer(request: AiAnswerRequest): Flow<AiGenerationEvent> = flow {
        val apiKey = credentialStore.get(AiCredentialIds.DEEPSEEK_API_KEY) ?: throw AiProviderException.MissingCredential()
        emit(AiGenerationEvent.Started)
        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = READ_TIMEOUT_MILLIS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "text/event-stream")
                setRequestProperty("Authorization", "Bearer $apiKey")
        }
        try {
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(request.toJson().toString()) }
                val status = connection.responseCode
                if (status !in 200..299) throw mapHttpError(status)
                var promptTokens: Int? = null
                var completionTokens: Int? = null
                connection.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                    for (line in lines) {
                        currentCoroutineContext().ensureActive()
                        if (!line.startsWith("data:")) continue
                        val payload = line.removePrefix("data:").trim()
                        if (payload == "[DONE]") break
                        val chunk = runCatching { JSONObject(payload) }.getOrNull() ?: continue
                        val usage = chunk.optJSONObject("usage")
                        if (usage != null) {
                            promptTokens = usage.optInt("prompt_tokens").takeIf { it > 0 }
                            completionTokens = usage.optInt("completion_tokens").takeIf { it > 0 }
                        }
                        val choices = chunk.optJSONArray("choices") ?: continue
                        if (choices.length() == 0) continue
                        val delta = choices.optJSONObject(0)?.optJSONObject("delta") ?: continue
                        val text = delta.optString("content")
                        if (text.isNotEmpty()) emit(AiGenerationEvent.TextDelta(text))
                    }
                }
                emit(AiGenerationEvent.Finished(promptTokens, completionTokens))
        } catch (exception: AiProviderException) {
            throw exception
        } catch (_: SocketTimeoutException) {
            throw AiProviderException.NetworkUnavailable()
        } catch (_: IOException) {
            throw AiProviderException.NetworkUnavailable()
        } finally {
            connection.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    private fun AiAnswerRequest.toJson(): JSONObject = JSONObject().apply {
        put("model", model)
        put("stream", true)
        put("stream_options", JSONObject().put("include_usage", true))
        put("max_tokens", maxTokens)
        put("thinking", JSONObject().put("type", "disabled"))
        put("messages", JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemPrompt))
            messages.forEach { put(JSONObject().put("role", it.role).put("content", it.content)) }
        })
    }

    private fun mapHttpError(status: Int): AiProviderException = when (status) {
        401, 403 -> AiProviderException.InvalidCredential()
        429 -> AiProviderException.RateLimited()
        else -> AiProviderException.ProviderFailure("DeepSeek request failed (HTTP $status). Try again later.")
    }

    companion object {
        private const val ENDPOINT = "https://api.deepseek.com/chat/completions"
        private const val CONNECT_TIMEOUT_MILLIS = 15_000
        private const val READ_TIMEOUT_MILLIS = 90_000
    }
}
