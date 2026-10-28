package com.procut.model

import kotlinx.serialization.Serializable

@Serializable
enum class TransitionType(val displayName: String) {
    DISSOLVE("Dissolve"),
    ZOOM("Zoom"),
    SLIDE_LEFT("Slide Left"),
    SLIDE_RIGHT("Slide Right"),
    GLITCH("Glitch"),
    BLUR("Blur"),
    WIPE_RIGHT("Wipe Right")
}

@Serializable
data class Transition(
    val type: TransitionType = TransitionType.DISSOLVE,
    val durationUs: Long = 500_000L // 0.5 seconds default
) {
    init {
        require(durationUs > 0) { "Transition duration must be strictly positive: $durationUs" }
    }
}
