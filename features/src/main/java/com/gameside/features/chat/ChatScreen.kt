package com.gameside.features.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gameside.domain.chat.ChatMessage
import com.gameside.domain.chat.ChatRole
import com.gameside.domain.chat.ChatSession
import com.gameside.domain.knowledge.SourceCitation

@Composable
fun ChatRoute(modifier: Modifier = Modifier, viewModel: ChatViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        onDraftChange = viewModel::setDraft,
        onSend = viewModel::send,
        onStop = viewModel::stop,
        onNewConversation = viewModel::newConversation,
        onSelectConversation = viewModel::selectConversation,
        onRenameConversation = viewModel::renameConversation,
        onDeleteConversation = viewModel::deleteConversation,
        onSaveAnswer = viewModel::saveAnswer,
        onRetryAnswer = viewModel::retryAnswer,
        onChecklist = viewModel::convertToChecklist,
        onDismissMessage = viewModel::dismissError,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(
    state: GameChatState,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onNewConversation: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onSaveAnswer: (ChatMessage) -> Unit,
    onRetryAnswer: (ChatMessage) -> Unit,
    onChecklist: (ChatMessage) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showHistory by remember { mutableStateOf(false) }
    var renameSession by remember { mutableStateOf<ChatSession?>(null) }
    var renameDraft by remember { mutableStateOf("") }
    var deleteSession by remember { mutableStateOf<ChatSession?>(null) }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it); onDismissMessage() }
    }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.game?.title ?: "Ask")
                        state.game?.let {
                            Text(
                                "${state.thread?.session?.title ?: "New conversation"} · ${it.spoilerLevel.name.lowercase()} spoilers · ${state.maxAnswerTokens} max tokens",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNewConversation, enabled = !state.isGenerating && state.game != null) {
                        Icon(Icons.Rounded.AddComment, contentDescription = "New conversation")
                    }
                    IconButton(onClick = { showHistory = true }, enabled = !state.isGenerating && state.sessions.isNotEmpty()) {
                        Icon(Icons.Rounded.History, contentDescription = "Conversation history")
                    }
                },
            )
        },
    ) { padding ->
        if (state.game == null) NoGameSelected(Modifier.padding(padding))
        else ChatContent(
            state = state,
            onDraftChange = onDraftChange,
            onSend = onSend,
            onStop = onStop,
            onSaveAnswer = onSaveAnswer,
            onRetryAnswer = onRetryAnswer,
            onChecklist = onChecklist,
            onCopy = { message ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("GameSide AI answer", message.content))
            },
            modifier = Modifier.padding(padding),
        )
    }

    if (showHistory) ConversationHistoryDialog(
        sessions = state.sessions,
        selectedId = state.thread?.session?.id,
        onSelect = { showHistory = false; onSelectConversation(it.id) },
        onRename = { showHistory = false; renameSession = it; renameDraft = it.title },
        onDelete = { showHistory = false; deleteSession = it },
        onDismiss = { showHistory = false },
    )
    renameSession?.let { session ->
        AlertDialog(
            onDismissRequest = { renameSession = null },
            title = { Text("Rename conversation") },
            text = { OutlinedTextField(renameDraft, { renameDraft = it.take(80) }, label = { Text("Title") }, singleLine = true) },
            confirmButton = { TextButton(enabled = renameDraft.isNotBlank(), onClick = { onRenameConversation(session.id, renameDraft); renameSession = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { renameSession = null }) { Text("Cancel") } },
        )
    }
    deleteSession?.let { session ->
        AlertDialog(
            onDismissRequest = { deleteSession = null },
            title = { Text("Delete this conversation?") },
            text = { Text("${session.title} and its source links will be permanently removed. Saved answers stay available.") },
            confirmButton = { TextButton(onClick = { onDeleteConversation(session.id); deleteSession = null; showHistory = false }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteSession = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun NoGameSelected(modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Select a game first", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Questions are always scoped to the active game. Add or select one in Games.")
    }
}

@Composable
private fun ChatContent(
    state: GameChatState,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onSaveAnswer: (ChatMessage) -> Unit,
    onRetryAnswer: (ChatMessage) -> Unit,
    onChecklist: (ChatMessage) -> Unit,
    onCopy: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val messages = state.thread?.messages.orEmpty()
    val listState = rememberLazyListState()
    val displayedCount = messages.size + if (state.streamingAnswer.isNotEmpty()) 1 else 0
    LaunchedEffect(displayedCount, state.streamingAnswer.length) {
        if (displayedCount > 0) listState.scrollToItem(displayedCount - 1)
    }
    Column(modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (messages.isEmpty() && state.streamingAnswer.isEmpty()) item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Ask about ${state.game?.title}", fontWeight = FontWeight.Bold)
                        Text("Try locations, builds, mechanics, quests, bosses, puzzles, or settings. Spoiler protection is applied to sources and the AI prompt.")
                    }
                }
            }
            items(messages, key = ChatMessage::id) { message ->
                MessageCard(message, onSaveAnswer, onRetryAnswer, onChecklist) { onCopy(message) }
            }
            if (state.isSearchingSources) item("sources-loading") {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                    Text("Searching game-wiki sources…")
                }
            }
            if (state.streamingAnswer.isNotEmpty()) item("streaming") {
                AssistantCard(state.streamingAnswer, state.streamingCitations, true, null, null, null, null)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                label = { Text("Ask about ${state.game?.title}") },
                minLines = 1,
                maxLines = 4,
                enabled = !state.isGenerating,
                supportingText = { Text("${state.draft.length}/2000 · recent context is limited to 14 messages") },
            )
            if (state.isGenerating) Button(onClick = onStop) { Icon(Icons.Rounded.Stop, contentDescription = "Stop answer") }
            else Button(onClick = onSend, enabled = state.draft.isNotBlank()) { Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send question") }
        }
    }
}

@Composable
private fun MessageCard(
    message: ChatMessage,
    onSave: (ChatMessage) -> Unit,
    onRetry: (ChatMessage) -> Unit,
    onChecklist: (ChatMessage) -> Unit,
    onCopy: () -> Unit,
) {
    if (message.role == ChatRole.ASSISTANT) AssistantCard(
        message.content, message.citations, false,
        onSave = { onSave(message) }, onCopy = onCopy, onRetry = { onRetry(message) }, onChecklist = { onChecklist(message) },
    ) else Card(
        modifier = Modifier.fillMaxWidth().padding(start = 42.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) { Text(message.content, Modifier.padding(14.dp)) }
}

@Composable
private fun AssistantCard(
    text: String,
    citations: List<SourceCitation>,
    isStreaming: Boolean,
    onSave: (() -> Unit)?,
    onCopy: (() -> Unit)?,
    onRetry: (() -> Unit)?,
    onChecklist: (() -> Unit)?,
) {
    val uriHandler = LocalUriHandler.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(end = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("GameSide AI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.weight(1f))
                if (isStreaming) CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
            }
            if (onCopy != null || onRetry != null || onChecklist != null || onSave != null) Row(
                Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
            ) {
                onCopy?.let { IconButton(onClick = it) { Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy answer") } }
                onRetry?.let { IconButton(onClick = it) { Icon(Icons.Rounded.Replay, contentDescription = "Retry answer") } }
                onChecklist?.let { IconButton(onClick = it) { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = "Convert answer to checklist") } }
                onSave?.let { IconButton(onClick = it) { Icon(Icons.Rounded.BookmarkAdd, contentDescription = "Save answer") } }
            }
            Spacer(Modifier.height(6.dp))
            Text(text)
            if (citations.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Sources", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                citations.forEachIndexed { index, citation ->
                    Button(
                        onClick = { runCatching { uriHandler.openUri(citation.url) } },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    ) {
                        Text("[${index + 1}] ${citation.title}", modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open source")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationHistoryDialog(
    sessions: List<ChatSession>,
    selectedId: String?,
    onSelect: (ChatSession) -> Unit,
    onRename: (ChatSession) -> Unit,
    onDelete: (ChatSession) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conversations") },
        text = {
            LazyColumn(modifier = Modifier.height(360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sessions, key = ChatSession::id) { session ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onSelect(session) }, modifier = Modifier.weight(1f)) {
                            Text(if (session.id == selectedId) "• ${session.title}" else session.title)
                        }
                        IconButton(onClick = { onRename(session) }) { Icon(Icons.Rounded.Edit, contentDescription = "Rename ${session.title}") }
                        IconButton(onClick = { onDelete(session) }) { Icon(Icons.Rounded.Delete, contentDescription = "Delete ${session.title}") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
