package com.gameside.features.wiki

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gameside.domain.game.SpoilerLevel

@Composable
fun WikiRoute(modifier: Modifier = Modifier, viewModel: WikiViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Game wiki", style = MaterialTheme.typography.headlineMedium)
                    Text(state.game?.title ?: "Select a game first")
                }
                Text(if (state.isOnline) "ONLINE" else "OFFLINE", color = if (state.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                if (state.documents.isNotEmpty()) IconButton(onClick = viewModel::clearCache) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear wiki cache")
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    state.query, viewModel::setQuery, label = { Text("Search this game's wiki") },
                    enabled = state.game != null && !state.isLoading, modifier = Modifier.weight(1f), singleLine = true,
                )
                Button(onClick = viewModel::search, enabled = state.query.isNotBlank() && !state.isLoading) {
                    if (state.isLoading) CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Search wiki")
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            if (state.documents.isNotEmpty()) Text("Downloaded pages · available offline", modifier = Modifier.padding(top = 10.dp), fontWeight = FontWeight.Bold)
        }
        if (state.game != null && state.documents.isEmpty() && !state.isLoading) {
            item { Text("Search for an item, boss, quest, location, mechanic, or build. Opened results are cached locally.") }
        }
        items(state.documents, key = { "${it.result.sourceApiUrl}:${it.result.id}" }) { document ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(document.result.title, style = MaterialTheme.typography.titleLarge)
                    Text(document.result.sourceName, color = MaterialTheme.colorScheme.secondary)
                    val strictSpoilers = state.game?.spoilerLevel in setOf(SpoilerLevel.NONE, SpoilerLevel.MINIMAL)
                    Text(
                        if (strictSpoilers) "Preview hidden because this game's spoiler protection is strict. Open the source intentionally to read it."
                        else document.plainText.replace(Regex("\\s+"), " ").take(320),
                    )
                    Button(onClick = { runCatching { uriHandler.openUri(document.result.url) } }, modifier = Modifier.fillMaxWidth()) {
                        Text("Open source", Modifier.weight(1f)); Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}
