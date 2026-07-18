package com.gameside.features.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gameside.domain.game.GamePlatform
import com.gameside.domain.game.GameProfile
import com.gameside.domain.game.SpoilerLevel

@Composable
fun GameLibraryRoute(
    modifier: Modifier = Modifier,
    viewModel: GameLibraryViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GameLibraryScreen(
        state = state,
        modifier = modifier,
        onSearch = viewModel::setSearchQuery,
        onSave = viewModel::save,
        onSetActive = viewModel::setActive,
        onTogglePin = viewModel::togglePin,
        onDelete = viewModel::delete,
    )
}

@Composable
private fun GameLibraryScreen(
    state: GameLibraryState,
    modifier: Modifier,
    onSearch: (String) -> Unit,
    onSave: (GameProfileDraft) -> Unit,
    onSetActive: (String) -> Unit,
    onTogglePin: (GameProfile) -> Unit,
    onDelete: (GameProfile) -> Unit,
) {
    var editing by remember { mutableStateOf<GameProfileDraft?>(null) }
    var deleting by remember { mutableStateOf<GameProfile?>(null) }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = GameProfileDraft() }) {
                Icon(Icons.Rounded.Add, contentDescription = "Add game")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Game library", style = MaterialTheme.typography.headlineMedium)
                Text("Profiles keep chat, sources and spoiler context separated.", color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearch,
                    label = { Text("Search games") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.games.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(18.dp)) {
                            Text("No games yet", style = MaterialTheme.typography.titleLarge)
                            Text("Add an Android or emulated game. The first profile becomes active automatically.")
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { editing = GameProfileDraft() }) { Text("Add first game") }
                        }
                    }
                }
            }
            items(state.games, key = { it.id }) { game ->
                GameCard(
                    game = game,
                    active = game.id == state.activeGameId,
                    onSetActive = { onSetActive(game.id) },
                    onEdit = {
                        editing = GameProfileDraft(
                            id = game.id,
                            title = game.title,
                            packageName = game.packageNames.firstOrNull().orEmpty(),
                            preferredWikiSource = game.preferredWikiSources.firstOrNull().orEmpty(),
                            platform = game.platform,
                            spoilerLevel = game.spoilerLevel,
                            isPinned = game.isPinned,
                            createdAt = game.createdAt,
                        )
                    },
                    onPin = { onTogglePin(game) },
                    onDelete = { deleting = game },
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    editing?.let { draft ->
        GameEditorDialog(
            initial = draft,
            onDismiss = { editing = null },
            onSave = { onSave(it); editing = null },
        )
    }
    deleting?.let { game ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${game.title}?") },
            text = { Text("This removes the profile and all data linked to it. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onDelete(game); deleting = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun GameCard(
    game: GameProfile,
    active: Boolean,
    onSetActive: () -> Unit,
    onEdit: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSetActive),
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(game.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    listOf(game.platform.name.replace('_', ' '), "Spoilers: ${game.spoilerLevel.name.lowercase()}")
                        .joinToString(" · "),
                    color = MaterialTheme.colorScheme.secondary,
                )
                game.packageNames.firstOrNull()?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text(
                    game.preferredWikiSources.firstOrNull()?.let { "Game wiki: $it" } ?: "Game wiki: automatic",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (active) Text("ACTIVE GAME", style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onPin) { Icon(Icons.Rounded.PushPin, contentDescription = "Pin game") }
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Edit game") }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Delete game") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameEditorDialog(
    initial: GameProfileDraft,
    onDismiss: () -> Unit,
    onSave: (GameProfileDraft) -> Unit,
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == null) "Add game" else "Edit game") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { draft = draft.copy(title = it) },
                    label = { Text("Game title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.packageName,
                    onValueChange = { draft = draft.copy(packageName = it) },
                    label = { Text("Android package (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.preferredWikiSource,
                    onValueChange = { draft = draft.copy(preferredWikiSource = it) },
                    label = { Text("Game wiki URL (optional)") },
                    supportingText = { Text("Leave empty for automatic wiki.gg/Fandom detection") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Platform", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(GamePlatform.ANDROID, GamePlatform.EMULATED, GamePlatform.OTHER).forEachIndexed { index, platform ->
                        SegmentedButton(
                            selected = draft.platform == platform,
                            onClick = { draft = draft.copy(platform = platform) },
                            shape = SegmentedButtonDefaults.itemShape(index, 3),
                            label = { Text(platform.name.lowercase().replaceFirstChar(Char::uppercase)) },
                        )
                    }
                }
                Text("Spoiler level", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SpoilerLevel.entries.forEachIndexed { index, level ->
                        SegmentedButton(
                            selected = draft.spoilerLevel == level,
                            onClick = { draft = draft.copy(spoilerLevel = level) },
                            shape = SegmentedButtonDefaults.itemShape(index, SpoilerLevel.entries.size),
                            label = { Text(level.name.take(3)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }, enabled = draft.title.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
