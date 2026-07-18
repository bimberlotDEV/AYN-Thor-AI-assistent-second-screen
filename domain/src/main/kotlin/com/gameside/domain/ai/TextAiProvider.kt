package com.gameside.domain.ai

import kotlinx.coroutines.flow.Flow

data class AiChatMessage(val role: String, val content: String)

data class AiAnswerRequest(
    val systemPrompt: String,
    val messages: List<AiChatMessage>,
    val model: String,
    val maxTokens: Int,
)

sealed interface AiGenerationEvent {
    data object Started : AiGenerationEvent
    data class TextDelta(val text: String) : AiGenerationEvent
    data class Finished(val promptTokens: Int?, val completionTokens: Int?) : AiGenerationEvent
}

interface TextAiProvider {
    fun generateAnswer(request: AiAnswerRequest): Flow<AiGenerationEvent>
}

object AiCredentialIds {
    const val DEEPSEEK_API_KEY = "deepseek.api_key"
}

sealed class AiProviderException(message: String) : Exception(message) {
    class MissingCredential : AiProviderException("Add your DeepSeek API key in Settings first.")
    class InvalidCredential : AiProviderException("DeepSeek rejected the API key. Check or replace it in Settings.")
    class RateLimited : AiProviderException("DeepSeek is rate-limiting requests. Wait briefly and try again.")
    class NetworkUnavailable : AiProviderException("No working internet connection is available.")
    class ProviderFailure(message: String) : AiProviderException(message)
}
