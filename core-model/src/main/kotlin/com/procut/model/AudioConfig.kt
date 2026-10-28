package com.procut.model

import kotlinx.serialization.Serializable

@Serializable
data class BeatMarker(
    val timeUs: Long,
    val intensity: Float = 1.0f
)

@Serializable
data class AudioFadeConfig(
    val fadeInDurationUs: Long = 0L,
    val fadeOutDurationUs: Long = 0L
) {
    init {
        require(fadeInDurationUs >= 0L) { "fadeInDurationUs must be non-negative: $fadeInDurationUs" }
        require(fadeOutDurationUs >= 0L) { "fadeOutDurationUs must be non-negative: $fadeOutDurationUs" }
    }

    /**
     * Computes the logarithmic/sine gain multiplier at a given offset [tUs] within a clip of [totalDurationUs].
     */
    fun computeGain(tUs: Long, totalDurationUs: Long): Float {
        if (totalDurationUs <= 0L) return 1.0f
        val clampedT = tUs.coerceIn(0L, totalDurationUs)

        // Fade In phase
        if (fadeInDurationUs > 0L && clampedT < fadeInDurationUs) {
            val progress = clampedT.toFloat() / fadeInDurationUs.toFloat()
            // Sine quarter-wave response: sin(pi/2 * progress)
            return kotlin.math.sin(Math.PI.toFloat() * 0.5f * progress).coerceIn(0f, 1f)
        }

        // Fade Out phase
        val fadeOutStartUs = totalDurationUs - fadeOutDurationUs
        if (fadeOutDurationUs > 0L && clampedT > fadeOutStartUs) {
            val progress = (clampedT - fadeOutStartUs).toFloat() / fadeOutDurationUs.toFloat()
            // Cosine quarter-wave response: cos(pi/2 * progress)
            return kotlin.math.cos(Math.PI.toFloat() * 0.5f * progress).coerceIn(0f, 1f)
        }

        return 1.0f
    }
}

@Serializable
data class AudioConfig(
    val volume: Float = 1.0f,
    val fade: AudioFadeConfig = AudioFadeConfig(),
    val pitchShiftSemitones: Float = 0.0f,
    val keepPitchOnSpeedChange: Boolean = true,
    val beatMarkers: List<BeatMarker> = emptyList(),
    val voiceEffect: String = "None"
) {
    init {
        require(volume >= 0.0f) { "Volume must be non-negative: $volume" }
    }
}
