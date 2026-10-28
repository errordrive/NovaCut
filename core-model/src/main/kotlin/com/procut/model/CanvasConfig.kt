package com.procut.model

import kotlinx.serialization.Serializable

@Serializable
enum class AspectRatio(val widthRatio: Int, val heightRatio: Int, val label: String) {
    RATIO_9_16(9, 16, "9:16 (TikTok/Reels)"),
    RATIO_16_9(16, 9, "16:9 (YouTube)"),
    RATIO_1_1(1, 1, "1:1 (Square)"),
    RATIO_4_5(4, 5, "4:5 (Instagram)"),
    RATIO_2_35_1(235, 100, "2.35:1 (Cinematic)"),
    CUSTOM(0, 0, "Custom");

    val floatRatio: Float
        get() = if (heightRatio != 0) widthRatio.toFloat() / heightRatio.toFloat() else 1.0f
}

@Serializable
sealed class CanvasBackground {
    @Serializable
    data class SolidColor(val colorHex: String = "#000000") : CanvasBackground()

    @Serializable
    data class Blur(val radius: Float = 25f, val darken: Float = 0.2f) : CanvasBackground()

    @Serializable
    data class Gradient(
        val startColorHex: String = "#1A1A1A",
        val endColorHex: String = "#000000",
        val angleDegrees: Float = 90f
    ) : CanvasBackground()
}

@Serializable
data class CanvasConfig(
    val aspectRatio: AspectRatio = AspectRatio.RATIO_9_16,
    val width: Int = 1080,
    val height: Int = 1920,
    val background: CanvasBackground = CanvasBackground.SolidColor("#000000")
) {
    fun calculateDimensions(targetLongSide: Int = 1920): Pair<Int, Int> {
        return when (aspectRatio) {
            AspectRatio.RATIO_9_16 -> Pair(1080, 1920)
            AspectRatio.RATIO_16_9 -> Pair(1920, 1080)
            AspectRatio.RATIO_1_1 -> Pair(1080, 1080)
            AspectRatio.RATIO_4_5 -> Pair(1080, 1350)
            AspectRatio.RATIO_2_35_1 -> Pair(1920, 817)
            AspectRatio.CUSTOM -> Pair(width, height)
        }
    }
}
