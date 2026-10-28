package com.procut.engine

import com.procut.model.*
import java.util.UUID
import kotlin.math.abs

/**
 * Keyframe & Easing Engine evaluating dynamic parameter curves at arbitrary timeline timestamps.
 * Implements linear and parametric cubic-bezier (x1, y1, x2, y2) interpolation with Newton-Raphson root solving.
 */
class KeyframeEvaluator {

    /**
     * Evaluates a single scalar property channel at clip [offsetUs] relative to clip start.
     */
    fun evaluatePropertyAt(
        clip: ClipSegment,
        property: KeyframeProperty,
        offsetUs: Long,
        defaultValue: Float
    ): Float {
        val propertyKeyframes = clip.keyframes
            .filter { it.property == property }
            .sortedBy { it.timeOffsetUs }

        if (propertyKeyframes.isEmpty()) return defaultValue
        if (offsetUs <= propertyKeyframes.first().timeOffsetUs) return propertyKeyframes.first().value
        if (offsetUs >= propertyKeyframes.last().timeOffsetUs) return propertyKeyframes.last().value

        for (i in 0 until propertyKeyframes.size - 1) {
            val k0 = propertyKeyframes[i]
            val k1 = propertyKeyframes[i + 1]
            if (offsetUs in k0.timeOffsetUs..k1.timeOffsetUs) {
                return Keyframe.interpolate(k0, k1, offsetUs)
            }
        }
        return propertyKeyframes.last().value
    }

    /**
     * Evaluates 2D affine transform matrix parameters at absolute timeline [playheadUs].
     */
    fun evaluateTransformAt(clip: ClipSegment, playheadUs: Long): Transform2D {
        val offsetUs = (playheadUs - clip.targetTimeRangeUs.startUs).coerceIn(0L, clip.targetTimeRangeUs.durationUs)
        val base = clip.transform

        return Transform2D(
            translationX = evaluatePropertyAt(clip, KeyframeProperty.POSITION_X, offsetUs, base.translationX),
            translationY = evaluatePropertyAt(clip, KeyframeProperty.POSITION_Y, offsetUs, base.translationY),
            scaleX = evaluatePropertyAt(clip, KeyframeProperty.SCALE_X, offsetUs, base.scaleX),
            scaleY = evaluatePropertyAt(clip, KeyframeProperty.SCALE_Y, offsetUs, base.scaleY),
            rotationDeg = evaluatePropertyAt(clip, KeyframeProperty.ROTATION_DEG, offsetUs, base.rotationDeg),
            opacity = evaluatePropertyAt(clip, KeyframeProperty.OPACITY, offsetUs, base.opacity).coerceIn(0f, 1f)
        )
    }

    /**
     * Evaluates audio volume multiplier at absolute timeline [playheadUs].
     */
    fun evaluateVolumeAt(clip: ClipSegment, playheadUs: Long): Float {
        if (clip.isMuted) return 0f
        val offsetUs = (playheadUs - clip.targetTimeRangeUs.startUs).coerceIn(0L, clip.targetTimeRangeUs.durationUs)
        return evaluatePropertyAt(clip, KeyframeProperty.VOLUME, offsetUs, clip.volume).coerceAtLeast(0f)
    }

    /**
     * Evaluates all 12 color grading & adjustment parameters at absolute timeline [playheadUs].
     */
    fun evaluateColorAdjustmentAt(clip: ClipSegment, playheadUs: Long): ColorAdjustment {
        val offsetUs = (playheadUs - clip.targetTimeRangeUs.startUs).coerceIn(0L, clip.targetTimeRangeUs.durationUs)
        val base = clip.colorAdjustment

        return ColorAdjustment(
            brightness = evaluatePropertyAt(clip, KeyframeProperty.BRIGHTNESS, offsetUs, base.brightness).coerceIn(-1f, 1f),
            contrast = evaluatePropertyAt(clip, KeyframeProperty.CONTRAST, offsetUs, base.contrast).coerceIn(-1f, 1f),
            saturation = evaluatePropertyAt(clip, KeyframeProperty.SATURATION, offsetUs, base.saturation).coerceIn(-1f, 1f),
            exposure = evaluatePropertyAt(clip, KeyframeProperty.EXPOSURE, offsetUs, base.exposure).coerceIn(-1f, 1f),
            sharpen = evaluatePropertyAt(clip, KeyframeProperty.SHARPEN, offsetUs, base.sharpen).coerceIn(0f, 1f),
            highlights = evaluatePropertyAt(clip, KeyframeProperty.HIGHLIGHTS, offsetUs, base.highlights).coerceIn(-1f, 1f),
            shadows = evaluatePropertyAt(clip, KeyframeProperty.SHADOWS, offsetUs, base.shadows).coerceIn(-1f, 1f),
            temperature = evaluatePropertyAt(clip, KeyframeProperty.TEMPERATURE, offsetUs, base.temperature).coerceIn(-1f, 1f),
            tint = evaluatePropertyAt(clip, KeyframeProperty.TINT, offsetUs, base.tint).coerceIn(-1f, 1f),
            vignette = evaluatePropertyAt(clip, KeyframeProperty.VIGNETTE, offsetUs, base.vignette).coerceIn(0f, 1f),
            fade = evaluatePropertyAt(clip, KeyframeProperty.FADE, offsetUs, base.fade).coerceIn(0f, 1f),
            grain = evaluatePropertyAt(clip, KeyframeProperty.GRAIN, offsetUs, base.grain).coerceIn(0f, 1f)
        )
    }

    // --- Diamond Indicator & Keyframe Editing Operations ---

    /**
     * Determines whether a keyframe exists at or very near [playheadUs] (within [toleranceUs]).
     * Lights up the diamond icon in the UI.
     */
    fun hasKeyframeAt(
        clip: ClipSegment,
        playheadUs: Long,
        property: KeyframeProperty? = null,
        toleranceUs: Long = 10_000L // 10ms tolerance
    ): Boolean {
        val offsetUs = playheadUs - clip.targetTimeRangeUs.startUs
        return clip.keyframes.any { kf ->
            (property == null || kf.property == property) &&
            abs(kf.timeOffsetUs - offsetUs) <= toleranceUs
        }
    }

    /**
     * Retrieves keyframe at [playheadUs] if one exists within [toleranceUs].
     */
    fun getKeyframeAt(
        clip: ClipSegment,
        playheadUs: Long,
        property: KeyframeProperty,
        toleranceUs: Long = 10_000L
    ): Keyframe? {
        val offsetUs = playheadUs - clip.targetTimeRangeUs.startUs
        return clip.keyframes.firstOrNull { kf ->
            kf.property == property && abs(kf.timeOffsetUs - offsetUs) <= toleranceUs
        }
    }

    /**
     * Adds or updates a keyframe at [playheadUs].
     */
    fun addOrUpdateKeyframe(
        clip: ClipSegment,
        playheadUs: Long,
        property: KeyframeProperty,
        value: Float,
        curve: KeyframeCurve = KeyframeCurve.LINEAR,
        c1x: Float = 0.42f,
        c1y: Float = 0.0f,
        c2x: Float = 0.58f,
        c2y: Float = 1.0f,
        toleranceUs: Long = 10_000L
    ): ClipSegment {
        val offsetUs = (playheadUs - clip.targetTimeRangeUs.startUs).coerceIn(0L, clip.targetTimeRangeUs.durationUs)
        val existing = getKeyframeAt(clip, playheadUs, property, toleranceUs)

        val newKeyframes = if (existing != null) {
            // Update existing keyframe in place
            clip.keyframes.map {
                if (it.id == existing.id) {
                    it.copy(
                        value = value,
                        curve = curve,
                        controlPoint1X = c1x,
                        controlPoint1Y = c1y,
                        controlPoint2X = c2x,
                        controlPoint2Y = c2y
                    )
                } else it
            }
        } else {
            // Insert new keyframe
            val newKf = Keyframe(
                id = UUID.randomUUID().toString(),
                timeOffsetUs = offsetUs,
                property = property,
                value = value,
                curve = curve,
                controlPoint1X = c1x,
                controlPoint1Y = c1y,
                controlPoint2X = c2x,
                controlPoint2Y = c2y
            )
            (clip.keyframes + newKf).sortedBy { it.timeOffsetUs }
        }

        return clip.copy(keyframes = newKeyframes)
    }

    /**
     * Removes keyframe(s) at [playheadUs] within [toleranceUs].
     */
    fun removeKeyframeAt(
        clip: ClipSegment,
        playheadUs: Long,
        property: KeyframeProperty? = null,
        toleranceUs: Long = 10_000L
    ): ClipSegment {
        val offsetUs = playheadUs - clip.targetTimeRangeUs.startUs
        val filtered = clip.keyframes.filterNot { kf ->
            (property == null || kf.property == property) &&
            abs(kf.timeOffsetUs - offsetUs) <= toleranceUs
        }
        return clip.copy(keyframes = filtered)
    }

    /**
     * Toggles keyframe at [playheadUs]: if diamond is active, removes it; if inactive, creates it.
     */
    fun toggleKeyframe(
        clip: ClipSegment,
        playheadUs: Long,
        property: KeyframeProperty,
        currentValue: Float,
        curve: KeyframeCurve = KeyframeCurve.LINEAR,
        toleranceUs: Long = 10_000L
    ): ClipSegment {
        return if (hasKeyframeAt(clip, playheadUs, property, toleranceUs)) {
            removeKeyframeAt(clip, playheadUs, property, toleranceUs)
        } else {
            addOrUpdateKeyframe(clip, playheadUs, property, currentValue, curve, toleranceUs = toleranceUs)
        }
    }
}
