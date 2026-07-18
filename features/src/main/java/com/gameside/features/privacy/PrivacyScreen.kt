package com.gameside.features.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private data class ConfirmAction(val title: String, val detail: String, val action: () -> Unit)

@Composable
fun PrivacyRoute(modifier: Modifier = Modifier, viewModel: PrivacyViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PrivacyScreen(
        state = state,
        clearConversations = viewModel::clearConversations,
        clearPersonalTools = viewModel::clearPersonalTools,
        clearWikiCache = viewModel::clearWikiCache,
        removeCredential = viewModel::removeCredential,
        resetAll = viewModel::resetAllData,
        dismissMessage = viewModel::dismissMessage,
        modifier = modifier,
    )
}

@Composable
private fun PrivacyScreen(
    state: PrivacyUiState,
    clearConversations: () -> Unit,
    clearPersonalTools: () -> Unit,
    clearWikiCache: () -> Unit,
    removeCredential: () -> Unit,
    resetAll: () -> Unit,
    dismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmation by remember { mutableStateOf<ConfirmAction?>(null) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("Privacy & local data", style = MaterialTheme.typography.headlineSmall); Text("Nothing is synced to a GameSide account.") }
            if (state.isWorking) CircularProgressIndicator()
        }
        Text("On this device", style = MaterialTheme.typography.titleMedium)
        DataLine("Game profiles", state.summary.gameProfiles)
        DataLine("Conversations", state.summary.conversations)
        DataLine("Saved answers", state.summary.savedAnswers)
        DataLine("Notes", state.summary.notes)
        DataLine("Checklists", state.summary.checklists)
        DataLine("Downloaded wiki pages", state.summary.cachedWikiPages)
        DataLine("Encrypted DeepSeek key", if (state.summary.hasProviderCredential) "Stored" else "Not stored")
        HorizontalDivider()
        Text("Remove selected data", style = MaterialTheme.typography.titleMedium)
        PrivacyAction("Clear conversations", "Deletes all local chat sessions and their citations.", state.summary.conversations > 0, state.isWorking) {
            confirmation = ConfirmAction("Clear all conversations?", "Saved answers, notes, games, and the API key stay on this device.", clearConversations)
        }
        PrivacyAction("Clear personal tools", "Deletes saved answers, notes, and checklists.", state.summary.savedAnswers + state.summary.notes + state.summary.checklists > 0, state.isWorking) {
            confirmation = ConfirmAction("Clear all personal tools?", "This cannot be undone.", clearPersonalTools)
        }
        PrivacyAction("Clear wiki cache", "Deletes downloaded pages; they can be retrieved again online.", state.summary.cachedWikiPages > 0, state.isWorking) {
            confirmation = ConfirmAction("Clear downloaded wiki pages?", "Chat citations remain, but offline page text is removed.", clearWikiCache)
        }
        PrivacyAction("Remove API key", "Deletes the encrypted credential and its local reference.", state.summary.hasProviderCredential, state.isWorking) {
            confirmation = ConfirmAction("Remove the DeepSeek API key?", "AI chat will stop working until a key is entered again.", removeCredential)
        }
        HorizontalDivider()
        Text("Factory reset", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
        Text("Deletes every profile, conversation, saved item, cached page, setting, and encryption key. Onboarding starts again.")
        Button(enabled = !state.isWorking, onClick = {
            confirmation = ConfirmAction("Reset GameSide AI?", "All local app data will be permanently deleted. This action cannot be undone.", resetAll)
        }) { Text("Delete all local data") }
    }

    confirmation?.let { pending ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text(pending.title) },
            text = { Text(pending.detail) },
            confirmButton = { TextButton(onClick = { confirmation = null; pending.action() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancel") } },
        )
    }
    state.message?.let { text ->
        AlertDialog(onDismissRequest = dismissMessage, text = { Text(text) }, confirmButton = { TextButton(onClick = dismissMessage) { Text("OK") } })
    }
}

@Composable
private fun DataLine(label: String, value: Int) = DataLine(label, value.toString())

@Composable
private fun DataLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun PrivacyAction(title: String, detail: String, enabled: Boolean, busy: Boolean, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedButton(onClick = onClick, enabled = enabled && !busy) { Text(title) }
        Text(detail, style = MaterialTheme.typography.bodySmall)
    }
}
