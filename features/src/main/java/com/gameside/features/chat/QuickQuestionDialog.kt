package com.gameside.features.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gameside.domain.controller.QuestionCategory
import com.gameside.domain.controller.QuestionTemplate
import com.gameside.domain.controller.QuickQuestionComposer
import com.gameside.domain.controller.QuickQuestionFavorite
import com.gameside.domain.game.SpoilerLevel

@Composable
internal fun QuickQuestionDialog(
    spoilerLevel: SpoilerLevel,
    favorites: List<QuickQuestionFavorite>,
    startWithKeyword: Boolean,
    onSend: (String, QuestionCategory?) -> Unit,
    onSaveFavorite: (String, String, QuestionCategory) -> Unit,
    onDeleteFavorite: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var category by remember(startWithKeyword) { mutableStateOf<QuestionCategory?>(if (startWithKeyword) QuestionCategory.ITEM else null) }
    var template by remember { mutableStateOf<QuestionTemplate?>(null) }
    var keyword by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick question · controller mode") },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (favorites.isNotEmpty() && category == null) {
                    item { Text("Favorites") }
                    items(favorites, key = QuickQuestionFavorite::id) { favorite ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onSend(favorite.question, favorite.category); onDismiss() }, modifier = Modifier.weight(1f)) { Text(favorite.label) }
                                TextButton(onClick = { onDeleteFavorite(favorite.id) }) { Text("Remove") }
                            }
                        }
                    }
                }
                item {
                    Text("Category")
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        QuestionCategory.entries.chunked(2).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { value ->
                                    FilterChip(
                                        selected = category == value,
                                        onClick = { category = value; template = null; preview = null },
                                        label = { Text(value.label) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
                category?.let { selectedCategory ->
                    item { Text("Action") }
                    items(QuickQuestionComposer.templates.filter { it.category == selectedCategory }, key = QuestionTemplate::id) { value ->
                        OutlinedButton(
                            onClick = { template = value; preview = null },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (template == value) "✓ ${value.label}" else value.label) }
                    }
                }
                template?.takeIf { it.needsKeyword }?.let {
                    item {
                        OutlinedTextField(
                            value = keyword,
                            onValueChange = { keyword = it.take(120); preview = null },
                            label = { Text("Short keyword, item, boss or NPC") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                template?.let { selectedTemplate ->
                    val valid = !selectedTemplate.needsKeyword || keyword.isNotBlank()
                    if (valid) item {
                        val question = QuickQuestionComposer.compose(selectedTemplate, keyword, spoilerLevel)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(preview ?: question)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onSend(question, selectedTemplate.category); onDismiss() }) { Text("Send") }
                                OutlinedButton(onClick = {
                                    onSaveFavorite(selectedTemplate.label, question, selectedTemplate.category)
                                    preview = "Saved as favorite: ${selectedTemplate.label}"
                                }) { Text("Favorite") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close (B)") } },
    )
}
