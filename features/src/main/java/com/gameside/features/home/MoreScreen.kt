package com.gameside.features.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gameside.device.CompanionLaunchResult
import com.gameside.features.display.DisplayDashboardRoute
import com.gameside.features.settings.ProviderSettingsRoute
import com.gameside.features.privacy.PrivacyRoute

@Composable
fun MoreRoute(
    onLaunchCompanion: (Int) -> CompanionLaunchResult,
    onOpenSingleScreen: () -> CompanionLaunchResult,
    modifier: Modifier = Modifier,
) {
    var section by remember { mutableIntStateOf(0) }
    Surface(modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Column {
            Row(Modifier.fillMaxWidth().padding(12.dp)) {
                if (section == 0) Button(onClick = {}, Modifier.weight(1f)) { Text("Displays") }
                else OutlinedButton(onClick = { section = 0 }, Modifier.weight(1f)) { Text("Displays") }
                if (section == 1) Button(onClick = {}, Modifier.weight(1f)) { Text("AI") }
                else OutlinedButton(onClick = { section = 1 }, Modifier.weight(1f)) { Text("AI") }
                if (section == 2) Button(onClick = {}, Modifier.weight(1f)) { Text("Privacy") }
                else OutlinedButton(onClick = { section = 2 }, Modifier.weight(1f)) { Text("Privacy") }
            }
            if (section == 0) DisplayDashboardRoute(onLaunchCompanion, onOpenSingleScreen, Modifier.weight(1f))
            else if (section == 1) ProviderSettingsRoute(Modifier.weight(1f))
            else PrivacyRoute(Modifier.weight(1f))
        }
    }
}
