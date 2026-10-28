package com.procut.model

import kotlinx.serialization.Serializable

@Serializable
data class ChromaKeyConfig(
    val isEnabled: Boolean = false,
    val keyColorHex: String = "#00FF00", // Default green
    val intensity: Float = 0.0f,          // 0.0f to 1.0f color distance tolerance
    val shadowFeather: Float = 0.0f      // Edge feathering / spill suppression
) {
    companion object {
        val DISABLED = ChromaKeyConfig()
    }
}
