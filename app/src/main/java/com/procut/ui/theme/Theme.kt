package com.procut.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val StudioDarkBackground = Color(0xFF121212)
val StudioDarkSurface = Color(0xFF1E1E1E)
val StudioDarkSurfaceVariant = Color(0xFF2A2A2A)
val StudioCyanAccent = Color(0xFF00E5FF)
val StudioWhite = Color(0xFFEEEEEE)
val StudioGrey = Color(0xFF888888)

private val DarkColorScheme = darkColorScheme(
    primary = StudioCyanAccent,
    background = StudioDarkBackground,
    surface = StudioDarkSurface,
    surfaceVariant = StudioDarkSurfaceVariant,
    onPrimary = Color.Black,
    onBackground = StudioWhite,
    onSurface = StudioWhite
)

@Composable
fun ProCutTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
