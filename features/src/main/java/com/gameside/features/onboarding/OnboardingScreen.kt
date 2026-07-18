package com.gameside.features.onboarding

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 104.dp),
        ) {
            item {
                Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(18.dp))
                Text("Your private gaming sidekick", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(12.dp))
                Text(
                    "GameSide AI is designed for the lower screen while your game remains on the primary display.",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(24.dp))
            }
            item { OnboardingPoint("Game context", "Select a game once; future chat and wiki results stay scoped to it.") }
            item { OnboardingPoint("Spoiler control", "Every profile has an explicit spoiler level and player-progress context.") }
            item { OnboardingPoint("Local first", "Profiles, notes and saved content remain on this device by default.") }
            item { OnboardingPoint("Optional providers", "No text leaves the device until you configure a provider and send a request.") }
            item { OnboardingPoint("Honest answers", "AI can be wrong. Sources and uncertainty remain visible when chat is added.") }
        }
        Button(
            onClick = onComplete,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
        ) { Text("Continue to game library") }
    }
}

@Composable
private fun OnboardingPoint(title: String, body: String) {
    Text(title, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.titleMedium)
    Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(14.dp))
}
