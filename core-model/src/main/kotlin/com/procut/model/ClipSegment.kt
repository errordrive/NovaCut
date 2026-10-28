package com.procut.model

import kotlinx.serialization.Serializable

@Serializable
data class ClipSegment(
    val id: String,
    val assetId: String,
    val trackId: String = "",
    val name: String = "",
    val sourceTimeRangeUs: TimeRangeUs,
    val targetTimeRangeUs: TimeRangeUs,
    val speedConfig: SpeedConfig = SpeedConfig.NORMAL,
    val transform: Transform2D = Transform2D.IDENTITY,
    val volume: Float = 1.0f,
    val keyframes: List<Keyframe> = emptyList(),
    val colorAdjustment: ColorAdjustment = ColorAdjustment.DEFAULT,
    val maskConfig: MaskConfig = MaskConfig.NONE,
    val chromaKeyConfig: ChromaKeyConfig = ChromaKeyConfig.DISABLED,
    val transitionIn: Transition? = null,
    val isReversed: Boolean = false,
    val isMuted: Boolean = false,
    val audioConfig: AudioConfig = AudioConfig(),
    val textConfig: TextConfig? = null
) {
    init {
        require(volume >= 0.0f) { "Clip volume must be non-negative: $volume" }
    }

    /**
     * Checks whether the clip is active and visible at the given absolute timeline timestamp.
     */
    fun isLayerVisibleAt(timelineTimeUs: Long): Boolean {
        return timelineTimeUs >= targetTimeRangeUs.startUs &&
                timelineTimeUs <= (targetTimeRangeUs.startUs + targetTimeRangeUs.durationUs)
    }

    /**
     * Interpolates active transform at a target timeline offset relative to clip start.
     */
    fun evaluateTransformAt(offsetUs: Long): Transform2D {
        val posXKeyframes = keyframes.filter { it.property == KeyframeProperty.POSITION_X }.sortedBy { it.timeOffsetUs }
        val posYKeyframes = keyframes.filter { it.property == KeyframeProperty.POSITION_Y }.sortedBy { it.timeOffsetUs }
        val scaleXKeyframes = keyframes.filter { it.property == KeyframeProperty.SCALE_X }.sortedBy { it.timeOffsetUs }
        val scaleYKeyframes = keyframes.filter { it.property == KeyframeProperty.SCALE_Y }.sortedBy { it.timeOffsetUs }
        val rotKeyframes = keyframes.filter { it.property == KeyframeProperty.ROTATION_DEG }.sortedBy { it.timeOffsetUs }
        val opKeyframes = keyframes.filter { it.property == KeyframeProperty.OPACITY }.sortedBy { it.timeOffsetUs }

        return Transform2D(
            translationX = interpolateProperty(posXKeyframes, offsetUs, transform.translationX),
            translationY = interpolateProperty(posYKeyframes, offsetUs, transform.translationY),
            scaleX = interpolateProperty(scaleXKeyframes, offsetUs, transform.scaleX),
            scaleY = interpolateProperty(scaleYKeyframes, offsetUs, transform.scaleY),
            rotationDeg = interpolateProperty(rotKeyframes, offsetUs, transform.rotationDeg),
            opacity = interpolateProperty(opKeyframes, offsetUs, transform.opacity).coerceIn(0f, 1f)
        )
    }

    fun evaluateVolumeAt(offsetUs: Long): Float {
        if (isMuted) return 0f
        val baseVol = volume * audioConfig.volume
        val fadeGain = audioConfig.fade.computeGain(offsetUs, targetTimeRangeUs.durationUs)
        val volKeyframes = keyframes.filter { it.property == KeyframeProperty.VOLUME }.sortedBy { it.timeOffsetUs }
        val kfVol = interpolateProperty(volKeyframes, offsetUs, baseVol).coerceAtLeast(0f)
        return (kfVol * fadeGain).coerceIn(0f, 2.0f)
    }

    private fun interpolateProperty(keyframes: List<Keyframe>, offsetUs: Long, defaultValue: Float): Float {
        if (keyframes.isEmpty()) return defaultValue
        if (offsetUs <= keyframes.first().timeOffsetUs) return keyframes.first().value
        if (offsetUs >= keyframes.last().timeOffsetUs) return keyframes.last().value

        for (i in 0 until keyframes.size - 1) {
            val k0 = keyframes[i]
            val k1 = keyframes[i + 1]
            if (offsetUs in k0.timeOffsetUs..k1.timeOffsetUs) {
                return Keyframe.interpolate(k0, k1, offsetUs)
            }
        }
        return keyframes.last().value
    }
}
