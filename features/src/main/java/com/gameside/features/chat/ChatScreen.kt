package com.gameside.features.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Stop
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gameside.domain.chat.ChatMessage
import com.gameside.domain.chat.ChatRole

@Composable
fun ChatRoute(modifier: Modifier = Modifier, viewModel: ChatViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        onDraftChange = viewModel::setDraft,
        onSend = viewModel::send,
        onStop = viewModel::stop,
        onClear = viewModel::clearHistory,
        onDismissError = viewModel::dismissError,
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
    onClear: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it); onDismissError() }
    }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.game?.title ?: "Ask")
                        state.game?.let { Text("${it.platform.name} · ${it.spoilerLevel.name.lowercase()} spoilers", style = MaterialTheme.typography.labelSmall) }
                    }
                },
                actions = {
                    if (!state.thread?.messages.isNullOrEmpty()) {
                        IconButton(onClick = onClear, enabled = !state.isGenerating) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear chat history")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.game == null) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Select a game first", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text("Questions are always scoped to the active game. Add or select one in Games.")
            }
        } else {
            val messages = state.thread?.messages.orEmpty()
            val listState = rememberLazyListState()
            val displayedCount = messages.size + if (state.streamingAnswer.isNotEmpty()) 1 else 0
            LaunchedEffect(displayedCount, state.streamingAnswer.length) {
                if (displayedCount > 0) listState.scrollToItem(displayedCount - 1)
            }
            Column(Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (messages.isEmpty() && state.streamingAnswer.isEmpty()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(Modifier.padding(18.dp)) {
                                    Text("Ask about ${state.game.title}", fontWeight = FontWeight.Bold)
                                    Text("Try locations, builds, mechanics, quests, bosses, puzzles, or settings. The selected spoiler level is enforced in the prompt.")
                                }
                            }
                        }
                    }
                    items(messages, key = ChatMessage::id) { MessageCard(it) }
                    if (state.streamingAnswer.isNotEmpty()) {
                        item("streaming") { AssistantCard(state.streamingAnswer, isStreaming = true) }
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
                        label = { Text("Ask about ${state.game.title}") },
                        minLines = 1,
                        maxLines = 4,
                        enabled = !state.isGenerating,
                    )
                    if (state.isGenerating) {
                        Button(onClick = onStop) { Icon(Icons.Rounded.Stop, contentDescription = "Stop answer") }
                    } else {
                        Button(onClick = onSend, enabled = state.draft.isNotBlank()) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send question")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageCard(message: ChatMessage) {
    if (message.role == ChatRole.ASSISTANT) AssistantCard(message.content, false)
    else Card(
        modifier = Modifier.fillMaxWidth().padding(start = 42.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) { Text(message.content, Modifier.padding(14.dp)) }
}

@Composable
private fun AssistantCard(text: String, isStreaming: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(end = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("GameSide AI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                if (isStreaming) {
                    Spacer(Modifier.weight(1f))
                    CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(text)
        }
    }
}
