package com.procut.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val StudioDarkBackground = Color(0xFF0F0F11)
val StudioDarkSurface = Color(0xFF18181B)
val StudioDarkSurfaceVariant = Color(0xFF27272A)
val StudioCyanAccent = Color(0xFF00E5FF)
val StudioWhite = Color(0xFFF4F4F5)
val StudioGrey = Color(0xFFA1A1AA)
val StudioDarkGrey = Color(0xFF3F3F46)

// Timeline & Track colors
val StudioTrackBackground = Color(0xFF141416)
val StudioClipMain = Color(0xFF2E3842)
val StudioClipOverlay = Color(0xFF1E3A4B)
val StudioClipAudio = Color(0xFF1B4332)
val StudioClipText = Color(0xFF4A2820)
val StudioClipEffect = Color(0xFF3D234A)

// Selection & Indicators
val StudioSelectedBorder = Color(0xFF00E5FF)
val StudioTrimHandle = Color(0xFFFFFFFF)
val StudioPlayhead = Color(0xFFFFFFFF)
val StudioGuideLine = Color(0xFF2979FF)
val StudioKeyframeDiamond = Color(0xFFFFAB00)
val StudioTransitionBadge = Color(0xFF3B82F6)

private val DarkColorScheme = darkColorScheme(
    primary = StudioCyanAccent,
    background = StudioDarkBackground,
    surface = StudioDarkSurface,
    surfaceVariant = StudioDarkSurfaceVariant,
    onPrimary = Color.Black,
    onBackground = StudioWhite,
    onSurface = StudioWhite,
    secondary = StudioKeyframeDiamond
)

@Composable
fun ProCutTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
