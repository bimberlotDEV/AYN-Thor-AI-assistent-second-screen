package com.gameside.features.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PersonalToolsRoute(modifier: Modifier = Modifier, viewModel: PersonalToolsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dialog by remember { mutableIntStateOf(0) }
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Saved & personal", style = MaterialTheme.typography.headlineMedium)
            Text("Everything here works offline and stays with the active game.")
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { dialog = 1 }, enabled = state.gameProfileId != null, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Add, null); Text("Note")
                }
                Button(onClick = { dialog = 2 }, enabled = state.gameProfileId != null, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Add, null); Text("Checklist")
                }
            }
        }
        if (state.gameProfileId == null) item { Text("Select a game first.") }
        if (state.tools.savedAnswers.isNotEmpty()) item { SectionTitle("Saved answers") }
        state.tools.savedAnswers.forEach { saved ->
            item(saved.id) {
                ToolCard(onDelete = { viewModel.deleteSaved(saved.id) }) {
                    Text(saved.question, fontWeight = FontWeight.Bold)
                    Text(saved.answer)
                    if (saved.citations.isNotEmpty()) Text("${saved.citations.size} source(s)", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        if (state.tools.notes.isNotEmpty()) item { SectionTitle("Notes") }
        state.tools.notes.forEach { note ->
            item(note.id) { ToolCard(onDelete = { viewModel.deleteNote(note.id) }) { Text(note.title, fontWeight = FontWeight.Bold); Text(note.content) } }
        }
        if (state.tools.checklists.isNotEmpty()) item { SectionTitle("Checklists") }
        state.tools.checklists.forEach { checklist ->
            item(checklist.id) {
                ToolCard(onDelete = { viewModel.deleteChecklist(checklist.id) }) {
                    Text(checklist.title, fontWeight = FontWeight.Bold)
                    checklist.items.forEach { item ->
                        Row(Modifier.fillMaxWidth()) {
                            Checkbox(checked = item.isChecked, onCheckedChange = { viewModel.toggleItem(item) })
                            Text(item.text, Modifier.padding(top = 12.dp))
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
    if (dialog != 0) CreateToolDialog(
        checklist = dialog == 2,
        onDismiss = { dialog = 0 },
        onSave = { title, body ->
            if (dialog == 1) viewModel.addNote(title, body)
            else viewModel.addChecklist(title, body.lines().filter(String::isNotBlank))
            dialog = 0
        },
    )
}

@Composable private fun SectionTitle(text: String) = Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))

@Composable
private fun ToolCard(onDelete: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Delete") }
        }
    }
}

@Composable
private fun CreateToolDialog(checklist: Boolean, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (checklist) "New checklist" else "New note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    body, { body = it }, label = { Text(if (checklist) "One item per line" else "Note") },
                    minLines = 4, modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title, body) }, enabled = title.isNotBlank() && body.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
