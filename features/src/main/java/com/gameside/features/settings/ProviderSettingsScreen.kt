package com.gameside.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProviderSettingsRoute(modifier: Modifier = Modifier, viewModel: ProviderSettingsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("AI provider", style = MaterialTheme.typography.headlineMedium)
        Text("DeepSeek receives only the active game's context, recent conversation, and the question you explicitly send. Requests may consume paid API credit.")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (state.hasStoredKey) "API key stored securely" else "No API key stored", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.keyDraft,
                    onValueChange = viewModel::setKeyDraft,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (state.hasStoredKey) "Replace API key" else "DeepSeek API key") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    enabled = !state.isWorking,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::saveKey, enabled = state.keyDraft.isNotBlank() && !state.isWorking) { Text("Encrypt & save") }
                    if (state.hasStoredKey) OutlinedButton(onClick = viewModel::removeKey, enabled = !state.isWorking) { Text("Remove") }
                }
            }
        }
        Text("Model", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.model == "deepseek-v4-flash",
                onClick = { viewModel.setModel("deepseek-v4-flash") },
                label = { Text("V4 Flash · faster") },
            )
            FilterChip(
                selected = state.model == "deepseek-v4-pro",
                onClick = { viewModel.setModel("deepseek-v4-pro") },
                label = { Text("V4 Pro · stronger") },
            )
        }
        Text("Answer length & cost", style = MaterialTheme.typography.titleMedium)
        Text("A lower output limit usually responds faster and uses less paid API credit. The exact charge is determined by your DeepSeek account.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(512, 900, 1500).forEach { tokens ->
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = state.maxAnswerTokens == tokens,
                    onClick = { viewModel.setMaxAnswerTokens(tokens) },
                    label = { Text(tokens.toString()) },
                )
            }
        }
        Text("512 short · 900 balanced · 1500 detailed", style = MaterialTheme.typography.labelMedium)
        Text("Only the 14 most recent messages are sent as conversation context. Retrieved Wiki evidence is separately size-limited by spoiler mode.", style = MaterialTheme.typography.bodySmall)
        Button(onClick = viewModel::testConnection, enabled = state.hasStoredKey && !state.isWorking) {
            if (state.isWorking) CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
            else Text("Test DeepSeek connection")
        }
        state.statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
        Spacer(Modifier.height(24.dp))
        Text("Privacy", style = MaterialTheme.typography.titleMedium)
        Text("The key is encrypted with Android Keystore and excluded from backup. Game profiles and chat history stay in the local Room database. The app has no analytics, advertising, microphone, or screen-capture permission.")
    }
}
