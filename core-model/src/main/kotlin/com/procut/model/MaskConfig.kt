package com.procut.model

import kotlinx.serialization.Serializable

@Serializable
enum class MaskType {
    NONE,
    LINEAR,
    MIRROR,
    CIRCLE,
    RECTANGLE,
    HEART,
    STAR
}

@Serializable
data class MaskConfig(
    val type: MaskType = MaskType.NONE,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val width: Float = 0.5f,
    val height: Float = 0.5f,
    val rotationDeg: Float = 0.0f,
    val feather: Float = 0.0f, // 0.0f to 1.0f edge softness
    val isInverted: Boolean = false
) {
    val isEnabled: Boolean
        get() = type != MaskType.NONE

    companion object {
        val NONE = MaskConfig(type = MaskType.NONE)
    }
}
