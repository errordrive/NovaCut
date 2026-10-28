package com.procut.model

import kotlinx.serialization.Serializable

@Serializable
enum class TextAlignment {
    LEFT,
    CENTER,
    RIGHT
}

@Serializable
enum class TextAnimationType {
    NONE,
    TYPEWRITER,
    FADE_UP,
    BOUNCE,
    POP,
    GLITCH,
    ZOOM,
    SLIDE_UP,
    SLIDE_RIGHT,
    PULSE,
    WOBBLE
}

@Serializable
data class TextAnimationConfig(
    val type: TextAnimationType = TextAnimationType.NONE,
    val durationUs: Long = 500_000L
)

@Serializable
data class TextShadowConfig(
    val hasShadow: Boolean = false,
    val colorHex: String = "#80000000",
    val blurRadius: Float = 4.0f,
    val angleDeg: Float = -45.0f,
    val distance: Float = 4.0f
)

@Serializable
data class TextBackgroundConfig(
    val hasBackground: Boolean = false,
    val colorHex: String = "#B3000000",
    val cornerRadius: Float = 8.0f,
    val paddingHorizontal: Float = 12.0f,
    val paddingVertical: Float = 6.0f
)

@Serializable
data class TextStrokeConfig(
    val hasStroke: Boolean = false,
    val colorHex: String = "#FF000000",
    val width: Float = 2.0f
)

@Serializable
data class TextConfig(
    val text: String = "Pro Studio Text",
    val fontFamily: String = "Default",
    val fontSizeSp: Float = 24.0f,
    val textColorHex: String = "#FFFFFFFF",
    val textAlpha: Float = 1.0f,
    val alignment: TextAlignment = TextAlignment.CENTER,
    val letterSpacing: Float = 0.0f,
    val lineSpacing: Float = 0.0f,
    val stroke: TextStrokeConfig = TextStrokeConfig(),
    val shadow: TextShadowConfig = TextShadowConfig(),
    val background: TextBackgroundConfig = TextBackgroundConfig(),
    val animationIn: TextAnimationConfig? = null,
    val animationOut: TextAnimationConfig? = null,
    val animationLoop: TextAnimationConfig? = null
)
