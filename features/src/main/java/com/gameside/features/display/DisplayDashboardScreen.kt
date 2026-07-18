package com.gameside.features.display

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
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gameside.device.CompanionLaunchResult
import com.gameside.domain.display.DeviceDisplayInfo

@Composable
fun DisplayDashboardRoute(
    onLaunchCompanion: (Int) -> CompanionLaunchResult,
    onOpenSingleScreen: () -> CompanionLaunchResult,
    viewModel: DisplayDashboardViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DisplayDashboardScreen(state, onLaunchCompanion, onOpenSingleScreen)
}

@Composable
private fun DisplayDashboardScreen(
    state: DisplayDashboardState,
    onLaunchCompanion: (Int) -> CompanionLaunchResult,
    onOpenSingleScreen: () -> CompanionLaunchResult,
) {
    var launchMessage by remember { mutableStateOf<String?>(null) }
    fun describe(result: CompanionLaunchResult): String = when (result) {
        is CompanionLaunchResult.Success -> "Companion launch requested on display ${result.displayId}"
        is CompanionLaunchResult.Failure -> result.reason
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(12.dp))
                Text("GameSide AI", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Dual-display readiness check",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Choose a display for the companion. If Android exposes no eligible second display, open the same touch interface here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                launchMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.secondary)
                }
            }
            if (state.displays.isEmpty()) {
                item { Text("Detecting displays…") }
            }
            items(state.displays, key = { it.id }) { display ->
                DisplayCard(
                    display = display,
                    preferred = display.id == state.preferredDisplayId,
                    onLaunch = { launchMessage = describe(onLaunchCompanion(display.id)) },
                )
            }
            item {
                OutlinedButton(onClick = { launchMessage = describe(onOpenSingleScreen()) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                    Text("  Open single-screen companion")
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun DisplayCard(display: DeviceDisplayInfo, preferred: Boolean, onLaunch: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(display.name, style = MaterialTheme.typography.titleMedium)
                if (preferred) Text("RECOMMENDED", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall)
            }
            Text(
                "Display ${display.id} · ${display.widthPixels}×${display.heightPixels} · ${display.densityDpi} dpi · ${"%.1f".format(display.refreshRateHz)} Hz",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                listOfNotNull(
                    if (display.isDefault) "Primary" else "Secondary",
                    if (display.hasTouch) "Touch" else "No touch reported",
                    if (display.isPresentation) "Presentation" else null,
                    if (display.isPrivate) "Private" else null,
                    if (display.canHostActivities) "Activity allowed" else "Activity blocked",
                ).joinToString(" · "),
                color = if (display.canHostActivities) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            if (!display.isDefault) {
                Button(onClick = onLaunch, enabled = display.canHostActivities, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.DeveloperBoard, contentDescription = null)
                    Text("  Launch companion here")
                }
            }
        }
    }
}
