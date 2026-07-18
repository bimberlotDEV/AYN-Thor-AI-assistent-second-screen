package com.gameside.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameside.domain.ai.AiAnswerRequest
import com.gameside.domain.ai.AiChatMessage
import com.gameside.domain.ai.AiGenerationEvent
import com.gameside.domain.ai.GamePromptBuilder
import com.gameside.domain.ai.TextAiProvider
import com.gameside.domain.chat.ChatMessage
import com.gameside.domain.chat.ChatRepository
import com.gameside.domain.chat.ChatRole
import com.gameside.domain.chat.ChatThread
import com.gameside.domain.game.GameProfile
import com.gameside.domain.game.GameProfileRepository
import com.gameside.domain.knowledge.KnowledgeRetriever
import com.gameside.domain.knowledge.SourceCitation
import com.gameside.domain.settings.AppSettings
import com.gameside.domain.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GameChatState(
    val game: GameProfile? = null,
    val thread: ChatThread? = null,
    val draft: String = "",
    val streamingAnswer: String = "",
    val isGenerating: Boolean = false,
    val isSearchingSources: Boolean = false,
    val streamingCitations: List<SourceCitation> = emptyList(),
    val errorMessage: String? = null,
    val model: String = "deepseek-v4-flash",
    val maxAnswerTokens: Int = 900,
)

private data class ChatContext(val settings: AppSettings, val game: GameProfile?, val thread: ChatThread?)
private data class ChatInputs(
    val draft: String,
    val streaming: String,
    val generating: Boolean,
    val searchingSources: Boolean,
    val citations: List<SourceCitation>,
    val error: String?,
)

private data class ChatInputContent(
    val draft: String,
    val streaming: String,
    val generating: Boolean,
    val searchingSources: Boolean,
    val citations: List<SourceCitation>,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val games: GameProfileRepository,
    private val chats: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val provider: TextAiProvider,
    private val knowledgeRetriever: KnowledgeRetriever,
) : ViewModel() {
    private val draft = MutableStateFlow("")
    private val streaming = MutableStateFlow("")
    private val generating = MutableStateFlow(false)
    private val searchingSources = MutableStateFlow(false)
    private val streamingCitations = MutableStateFlow<List<SourceCitation>>(emptyList())
    private val error = MutableStateFlow<String?>(null)
    private var generationJob: Job? = null

    private val context = settingsRepository.settings.flatMapLatest { settings ->
        val gameId = settings.activeGameId ?: return@flatMapLatest flowOf(ChatContext(settings, null, null))
        games.observeById(gameId).flatMapLatest { game ->
            if (game == null) flowOf(ChatContext(settings, null, null))
            else chats.observeLatestThread(gameId).map { ChatContext(settings, game, it) }
        }
    }

    private val inputContent = combine(draft, streaming, generating, searchingSources, streamingCitations) {
            draft, streaming, generating, searchingSources, citations ->
        ChatInputContent(draft, streaming, generating, searchingSources, citations)
    }

    private val inputs = combine(inputContent, error) { content, error ->
        ChatInputs(
            content.draft, content.streaming, content.generating, content.searchingSources, content.citations, error,
        )
    }

    val state: StateFlow<GameChatState> = combine(context, inputs) { activeContext, inputs ->
        GameChatState(
            game = activeContext.game,
            thread = activeContext.thread,
            draft = inputs.draft,
            streamingAnswer = inputs.streaming,
            isGenerating = inputs.generating,
            isSearchingSources = inputs.searchingSources,
            streamingCitations = inputs.citations,
            errorMessage = inputs.error,
            model = activeContext.settings.aiModel,
            maxAnswerTokens = activeContext.settings.maxAnswerTokens,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameChatState())

    fun setDraft(value: String) { draft.value = value.take(MAX_QUESTION_CHARS) }

    fun dismissError() { error.value = null }

    fun send() {
        if (generationJob?.isActive == true) return
        val snapshot = state.value
        val game = snapshot.game ?: return
        val question = snapshot.draft.trim()
        if (question.isEmpty() || snapshot.isGenerating) return
        draft.value = ""
        error.value = null
        streaming.value = ""
        streamingCitations.value = emptyList()
        generationJob = viewModelScope.launch {
            generating.value = true
            try {
                val session = chats.getOrCreateSession(game)
                chats.addMessage(ChatMessage(UUID.randomUUID().toString(), session.id, ChatRole.USER, question, Instant.now()))
                val history = chats.recentMessages(session.id, HISTORY_MESSAGE_LIMIT)
                searchingSources.value = true
                val knowledge = runCatching { knowledgeRetriever.retrieve(game, question) }
                    .getOrElse { com.gameside.domain.knowledge.RetrievedKnowledge.Empty }
                searchingSources.value = false
                streamingCitations.value = knowledge.citations
                val request = AiAnswerRequest(
                    systemPrompt = GamePromptBuilder().build(game, knowledge),
                    messages = history.map { AiChatMessage(it.role.name.lowercase(), it.content) },
                    model = snapshot.model,
                    maxTokens = snapshot.maxAnswerTokens,
                )
                provider.generateAnswer(request).collect { event ->
                    when (event) {
                        AiGenerationEvent.Started -> Unit
                        is AiGenerationEvent.TextDelta -> streaming.value += event.text
                        is AiGenerationEvent.Finished -> Unit
                    }
                }
                val answer = streaming.value.trim()
                if (answer.isEmpty()) error.value = "DeepSeek returned an empty answer. Try again."
                else chats.addMessage(
                    ChatMessage(
                        UUID.randomUUID().toString(), session.id, ChatRole.ASSISTANT, answer, Instant.now(), knowledge.citations,
                    ),
                )
                streaming.value = ""
                streamingCitations.value = emptyList()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                error.value = throwable.message ?: "The answer could not be generated."
            } finally {
                searchingSources.value = false
                generating.value = false
            }
        }
    }

    fun stop() {
        generationJob?.cancel()
        generationJob = null
        streaming.value = ""
        streamingCitations.value = emptyList()
        searchingSources.value = false
        generating.value = false
    }

    fun clearHistory() {
        val gameId = state.value.game?.id ?: return
        stop()
        viewModelScope.launch { chats.clearGameHistory(gameId) }
    }

    private companion object {
        const val MAX_QUESTION_CHARS = 2_000
        const val HISTORY_MESSAGE_LIMIT = 14
    }
}
