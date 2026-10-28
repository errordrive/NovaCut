package com.procut.model

import kotlinx.serialization.Serializable

@Serializable
data class ColorAdjustment(
    val brightness: Float = 0.0f,
    val contrast: Float = 0.0f,
    val saturation: Float = 0.0f,
    val exposure: Float = 0.0f,
    val sharpen: Float = 0.0f,
    val highlights: Float = 0.0f,
    val shadows: Float = 0.0f,
    val temperature: Float = 0.0f,
    val tint: Float = 0.0f,
    val vignette: Float = 0.0f,
    val fade: Float = 0.0f,
    val grain: Float = 0.0f
) {
    val isDefault: Boolean
        get() = brightness == 0f &&
                contrast == 0f &&
                saturation == 0f &&
                exposure == 0f &&
                sharpen == 0f &&
                highlights == 0f &&
                shadows == 0f &&
                temperature == 0f &&
                tint == 0f &&
                vignette == 0f &&
                fade == 0f &&
                grain == 0f

    companion object {
        val DEFAULT = ColorAdjustment()
    }
}
