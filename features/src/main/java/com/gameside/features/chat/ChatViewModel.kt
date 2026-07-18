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
import com.gameside.domain.chat.ChatSession
import com.gameside.domain.game.GameProfile
import com.gameside.domain.game.GameProfileRepository
import com.gameside.domain.knowledge.KnowledgeRetriever
import com.gameside.domain.knowledge.SourceCitation
import com.gameside.domain.personal.PersonalToolsRepository
import com.gameside.domain.personal.SavedAnswer
import com.gameside.domain.personal.GameChecklist
import com.gameside.domain.personal.ChecklistItem
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
    val sessions: List<ChatSession> = emptyList(),
    val draft: String = "",
    val streamingAnswer: String = "",
    val isGenerating: Boolean = false,
    val isSearchingSources: Boolean = false,
    val streamingCitations: List<SourceCitation> = emptyList(),
    val errorMessage: String? = null,
    val model: String = "deepseek-v4-flash",
    val maxAnswerTokens: Int = 900,
)

private data class ChatContext(val settings: AppSettings, val game: GameProfile?, val sessions: List<ChatSession>, val thread: ChatThread?)
private data class SessionSelection(val settings: AppSettings, val game: GameProfile, val sessions: List<ChatSession>, val sessionId: String?)
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
    private val personalTools: PersonalToolsRepository,
) : ViewModel() {
    private val draft = MutableStateFlow("")
    private val streaming = MutableStateFlow("")
    private val generating = MutableStateFlow(false)
    private val searchingSources = MutableStateFlow(false)
    private val streamingCitations = MutableStateFlow<List<SourceCitation>>(emptyList())
    private val error = MutableStateFlow<String?>(null)
    private val selectedSessionId = MutableStateFlow<String?>(null)
    private val startFresh = MutableStateFlow(false)
    private var generationJob: Job? = null

    private val context = settingsRepository.settings.flatMapLatest { settings ->
        val gameId = settings.activeGameId ?: return@flatMapLatest flowOf(ChatContext(settings, null, emptyList(), null))
        games.observeById(gameId).flatMapLatest { game ->
            if (game == null) flowOf(ChatContext(settings, null, emptyList(), null))
            else combine(chats.observeSessions(gameId), selectedSessionId, startFresh) { sessions, selected, fresh ->
                val sessionId = if (fresh) null else sessions.firstOrNull { it.id == selected }?.id ?: sessions.firstOrNull()?.id
                SessionSelection(settings, game, sessions, sessionId)
            }.flatMapLatest { selection ->
                selection.sessionId?.let { id ->
                    chats.observeThread(id).map { ChatContext(selection.settings, selection.game, selection.sessions, it) }
                } ?: flowOf(ChatContext(selection.settings, selection.game, selection.sessions, null))
            }
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
            sessions = activeContext.sessions,
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
        if (snapshot.game == null) return
        val question = snapshot.draft.trim()
        if (question.isEmpty() || snapshot.isGenerating) return
        sendQuestion(question)
    }

    private fun sendQuestion(question: String) {
        val snapshot = state.value
        val game = snapshot.game ?: return
        if (question.isBlank() || snapshot.isGenerating || generationJob?.isActive == true) return
        draft.value = ""
        error.value = null
        streaming.value = ""
        streamingCitations.value = emptyList()
        generationJob = viewModelScope.launch {
            generating.value = true
            try {
                val session = snapshot.thread?.session ?: chats.createSession(game).also {
                    selectedSessionId.value = it.id
                    startFresh.value = false
                }
                chats.addMessage(ChatMessage(UUID.randomUUID().toString(), session.id, ChatRole.USER, question, Instant.now()))
                if (session.title == "New conversation") chats.renameSession(session.id, question.replace('\n', ' ').take(48))
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

    fun newConversation() {
        stop()
        selectedSessionId.value = null
        startFresh.value = true
        draft.value = ""
    }

    fun selectConversation(id: String) {
        stop()
        selectedSessionId.value = id
        startFresh.value = false
    }

    fun renameConversation(id: String, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { chats.renameSession(id, title) }
    }

    fun deleteConversation(id: String) {
        stop()
        viewModelScope.launch {
            chats.deleteSession(id)
            if (selectedSessionId.value == id || state.value.thread?.session?.id == id) selectedSessionId.value = null
            startFresh.value = false
        }
    }

    fun retryAnswer(message: ChatMessage) {
        val messages = state.value.thread?.messages.orEmpty()
        val index = messages.indexOfFirst { it.id == message.id }
        val question = messages.take(index.coerceAtLeast(0)).lastOrNull { it.role == ChatRole.USER }?.content ?: return
        sendQuestion(question)
    }

    fun convertToChecklist(message: ChatMessage) {
        if (message.role != ChatRole.ASSISTANT) return
        val snapshot = state.value
        val gameId = snapshot.game?.id ?: return
        val messages = snapshot.thread?.messages.orEmpty()
        val index = messages.indexOfFirst { it.id == message.id }
        val question = messages.take(index.coerceAtLeast(0)).lastOrNull { it.role == ChatRole.USER }?.content ?: "AI answer"
        val lines = message.content.lineSequence().map { it.trim().replace(Regex("^[-*•\\d.)\\s]+"), "") }
            .filter { it.length in 3..180 }.take(25).toList().ifEmpty { listOf(message.content.take(180)) }
        val now = Instant.now()
        val checklistId = UUID.randomUUID().toString()
        viewModelScope.launch {
            runCatching { personalTools.saveChecklist(
                GameChecklist(
                    checklistId, gameId, question.replace('\n', ' ').take(60),
                    lines.mapIndexed { position, text -> ChecklistItem(UUID.randomUUID().toString(), text, false, position) },
                    now, now,
                ),
            ) }.onSuccess { error.value = "Checklist created in Saved." }
                .onFailure { error.value = "Checklist could not be created." }
        }
    }

    fun saveAnswer(message: ChatMessage) {
        if (message.role != ChatRole.ASSISTANT) return
        val snapshot = state.value
        val gameId = snapshot.game?.id ?: return
        val messages = snapshot.thread?.messages.orEmpty()
        val index = messages.indexOfFirst { it.id == message.id }
        val question = messages.take(index.coerceAtLeast(0)).lastOrNull { it.role == ChatRole.USER }?.content ?: "Saved answer"
        viewModelScope.launch {
            runCatching { personalTools.saveAnswer(
                SavedAnswer(
                    id = UUID.randomUUID().toString(), gameProfileId = gameId, sourceMessageId = message.id,
                    question = question, answer = message.content, citations = message.citations, createdAt = Instant.now(),
                ),
            ) }.onSuccess { error.value = "Answer saved." }
                .onFailure { error.value = "This answer is already saved." }
        }
    }

    private companion object {
        const val MAX_QUESTION_CHARS = 2_000
        const val HISTORY_MESSAGE_LIMIT = 14
    }
}
