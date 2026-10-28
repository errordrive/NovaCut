package com.procut.model

import kotlinx.serialization.Serializable

@Serializable
data class Transform2D(
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotationDeg: Float = 0f,
    val opacity: Float = 1f
) {
    init {
        require(opacity in 0f..1f) { "Opacity must be within [0.0, 1.0]: $opacity" }
    }

    companion object {
        val IDENTITY = Transform2D()
    }
}
