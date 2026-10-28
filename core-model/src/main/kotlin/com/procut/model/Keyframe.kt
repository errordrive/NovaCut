package com.procut.model

import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
enum class KeyframeProperty {
    POSITION_X,
    POSITION_Y,
    SCALE_X,
    SCALE_Y,
    ROTATION_DEG,
    OPACITY,
    VOLUME,
    // Color grading properties
    BRIGHTNESS,
    CONTRAST,
    SATURATION,
    EXPOSURE,
    SHARPEN,
    HIGHLIGHTS,
    SHADOWS,
    TEMPERATURE,
    TINT,
    VIGNETTE,
    FADE,
    GRAIN
}

@Serializable
enum class KeyframeCurve {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    CUBIC_BEZIER
}

@Serializable
data class Keyframe(
    val id: String,
    val timeOffsetUs: Long, // Offset relative to clip start timestamp
    val property: KeyframeProperty,
    val value: Float,
    val curve: KeyframeCurve = KeyframeCurve.LINEAR,
    val controlPoint1X: Float = 0.42f,
    val controlPoint1Y: Float = 0.0f,
    val controlPoint2X: Float = 0.58f,
    val controlPoint2Y: Float = 1.0f
) {
    init {
        require(timeOffsetUs >= 0) { "Keyframe timeOffsetUs must be non-negative: $timeOffsetUs" }
    }

    companion object {
        /**
         * Evaluates cubic bezier parameter t for a given x using Newton-Raphson iteration.
         */
        fun evaluateCubicBezier(
            p1x: Float, p1y: Float,
            p2x: Float, p2y: Float,
            normalizedTime: Float
        ): Float {
            val clampedX = normalizedTime.coerceIn(0f, 1f)
            if (clampedX <= 0f) return 0f
            if (clampedX >= 1f) return 1f

            var t = clampedX
            // 5 iterations of Newton-Raphson
            for (i in 0 until 5) {
                val currentX = sampleCurveX(p1x, p2x, t) - clampedX
                if (abs(currentX) < 1e-5f) break
                val dX = sampleCurveDerivativeX(p1x, p2x, t)
                if (abs(dX) < 1e-5f) break
                t -= currentX / dX
            }
            return sampleCurveY(p1y, p2y, t.coerceIn(0f, 1f))
        }

        private fun sampleCurveX(p1x: Float, p2x: Float, t: Float): Float {
            val invT = 1f - t
            return 3f * invT * invT * t * p1x + 3f * invT * t * t * p2x + t * t * t
        }

        private fun sampleCurveY(p1y: Float, p2y: Float, t: Float): Float {
            val invT = 1f - t
            return 3f * invT * invT * t * p1y + 3f * invT * t * t * p2y + t * t * t
        }

        private fun sampleCurveDerivativeX(p1x: Float, p2x: Float, t: Float): Float {
            val invT = 1f - t
            return 3f * invT * invT * p1x + 6f * invT * t * (p2x - p1x) + 3f * t * t * (1f - p2x)
        }

        /**
         * Interpolates value between two keyframes at target offset.
         */
        fun interpolate(k0: Keyframe, k1: Keyframe, targetOffsetUs: Long): Float {
            require(k0.property == k1.property) { "Cannot interpolate across different properties" }
            if (targetOffsetUs <= k0.timeOffsetUs) return k0.value
            if (targetOffsetUs >= k1.timeOffsetUs) return k1.value

            val totalSpan = (k1.timeOffsetUs - k0.timeOffsetUs).toFloat()
            if (totalSpan <= 0f) return k0.value

            val rawProgress = (targetOffsetUs - k0.timeOffsetUs).toFloat() / totalSpan
            val easedProgress = when (k0.curve) {
                KeyframeCurve.LINEAR -> rawProgress
                KeyframeCurve.EASE_IN -> evaluateCubicBezier(0.42f, 0.0f, 1.0f, 1.0f, rawProgress)
                KeyframeCurve.EASE_OUT -> evaluateCubicBezier(0.0f, 0.0f, 0.58f, 1.0f, rawProgress)
                KeyframeCurve.EASE_IN_OUT -> evaluateCubicBezier(0.42f, 0.0f, 0.58f, 1.0f, rawProgress)
                KeyframeCurve.CUBIC_BEZIER -> evaluateCubicBezier(
                    k0.controlPoint1X, k0.controlPoint1Y,
                    k0.controlPoint2X, k0.controlPoint2Y,
                    rawProgress
                )
            }
            return k0.value + easedProgress * (k1.value - k0.value)
        }
    }
}
