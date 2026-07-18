package com.gameside.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GameSideColors = darkColorScheme(
    primary = Color(0xFF9DB1FF),
    onPrimary = Color(0xFF10245E),
    primaryContainer = Color(0xFF263A73),
    secondary = Color(0xFF78DBC3),
    background = Color(0xFF050609),
    surface = Color(0xFF0D1016),
    surfaceVariant = Color(0xFF1A1E28),
    onBackground = Color(0xFFE7E9F2),
    onSurface = Color(0xFFE7E9F2),
    error = Color(0xFFFFB4AB),
)

@Composable
fun GameSideTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GameSideColors, content = content)
}
