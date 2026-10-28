package com.procut.render

import com.procut.model.ColorAdjustment
import kotlinx.serialization.Serializable

@Serializable
enum class LutPreset(val displayName: String) {
    NONE("Original"),
    CINEMATIC("Cinematic (Teal & Orange)"),
    WARM("Golden Warm"),
    COOL("Crisp Cool"),
    VINTAGE("Vintage Film"),
    BW("Monochrome B&W"),
    CYBERPUNK("Cyberpunk Neon"),
    VIVID("Vivid Pop"),
    MATTE("Matte Pastel");

    /**
     * Returns the baseline procedural color grading parameters for this preset.
     */
    fun toColorAdjustment(): ColorAdjustment {
        return when (this) {
            NONE -> ColorAdjustment.DEFAULT
            CINEMATIC -> ColorAdjustment(
                contrast = 0.25f,
                saturation = 0.15f,
                exposure = 0.05f,
                temperature = 0.20f,
                tint = -0.15f,
                shadows = -0.20f,
                highlights = 0.10f,
                vignette = 0.25f
            )
            WARM -> ColorAdjustment(
                contrast = 0.10f,
                saturation = 0.20f,
                temperature = 0.35f,
                tint = 0.05f,
                highlights = 0.15f,
                fade = 0.05f
            )
            COOL -> ColorAdjustment(
                contrast = 0.15f,
                saturation = -0.10f,
                temperature = -0.40f,
                tint = 0.10f,
                shadows = 0.10f,
                vignette = 0.15f
            )
            VINTAGE -> ColorAdjustment(
                contrast = -0.15f,
                saturation = -0.25f,
                temperature = 0.25f,
                fade = 0.30f,
                grain = 0.35f,
                vignette = 0.30f
            )
            BW -> ColorAdjustment(
                saturation = -1.0f, // Complete desaturation
                contrast = 0.30f,
                exposure = 0.05f,
                sharpen = 0.20f,
                grain = 0.15f
            )
            CYBERPUNK -> ColorAdjustment(
                contrast = 0.40f,
                saturation = 0.50f,
                temperature = -0.30f,
                tint = 0.40f,
                shadows = -0.30f,
                highlights = 0.20f,
                vignette = 0.35f
            )
            VIVID -> ColorAdjustment(
                contrast = 0.20f,
                saturation = 0.35f,
                brightness = 0.05f,
                sharpen = 0.25f
            )
            MATTE -> ColorAdjustment(
                contrast = -0.20f,
                saturation = -0.10f,
                fade = 0.40f,
                shadows = 0.25f,
                highlights = -0.15f
            )
        }
    }
}
