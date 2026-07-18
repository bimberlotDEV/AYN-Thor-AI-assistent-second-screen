package com.gameside.features.companion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gameside.device.GameLaunchResult

@Composable
fun CompanionScreen(
    displayId: Int,
    onLaunchGame: (String) -> GameLaunchResult,
    onClose: () -> Unit,
) {
    var tapCount by rememberSaveable { mutableIntStateOf(0) }
    var packageName by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf("Ready for touch validation") }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("GameSide AI", style = MaterialTheme.typography.headlineSmall)
                    Text("Companion on display $displayId", color = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close companion") }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Touch proof", style = MaterialTheme.typography.titleMedium)
                    Text("Use this control on the physical lower screen. Its counter survives rotation and recreation.")
                    Button(
                        onClick = { tapCount++; status = "Touch received on display $displayId" },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.TouchApp, contentDescription = null)
                        Text("  Touch received: $tapCount")
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Launch a mapped game", style = MaterialTheme.typography.titleMedium)
                    Text("Enter its Android package name. GameSide requests launch on the primary display without scanning unrelated apps.")
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text("Package name") },
                        placeholder = { Text("com.example.game") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            status = when (val result = onLaunchGame(packageName)) {
                                GameLaunchResult.Success -> "Game launch requested on the primary display"
                                is GameLaunchResult.Failure -> result.reason
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Gamepad, contentDescription = null)
                        Text("  Launch game on primary")
                    }
                }
            }

            Text(status, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium)
            Text(
                "AI chat remains intentionally disabled until this display foundation passes device validation.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
