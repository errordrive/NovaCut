package com.procut.model

import kotlinx.serialization.Serializable
import kotlin.math.roundToLong

@Serializable
enum class SpeedMode {
    CONSTANT,
    CURVE
}

@Serializable
data class CurvePoint(
    val x: Float, // Normalized timeline position [0.0, 1.0]
    val y: Float  // Speed multiplier at this point (e.g., 0.2f to 10.0f)
) {
    init {
        require(x in 0f..1f) { "CurvePoint x must be in range [0, 1]: $x" }
        require(y > 0f) { "CurvePoint speed y must be positive: $y" }
    }
}

@Serializable
data class SpeedConfig(
    val mode: SpeedMode = SpeedMode.CONSTANT,
    val speedMultiplier: Double = 1.0,
    val curvePoints: List<CurvePoint> = listOf(
        CurvePoint(0.0f, 1.0f),
        CurvePoint(1.0f, 1.0f)
    ),
    val isPitchPreserved: Boolean = true
) {
    init {
        require(speedMultiplier > 0.0) { "Speed multiplier must be positive: $speedMultiplier" }
    }

    /**
     * Calculates the target duration on the timeline for a given source duration.
     * For constant speed: targetDuration = sourceDuration / speedMultiplier.
     * For curve speed: numerical integration over curve segments.
     */
    fun computeTargetDurationUs(sourceDurationUs: Long): Long {
        if (sourceDurationUs <= 0L) return 0L
        return when (mode) {
            SpeedMode.CONSTANT -> {
                (sourceDurationUs / speedMultiplier).roundToLong()
            }
            SpeedMode.CURVE -> {
                if (curvePoints.size < 2) {
                    return (sourceDurationUs / speedMultiplier).roundToLong()
                }
                // Integral of dt_target = dt_source / v(t)
                // We use trapezoidal numerical integration over 100 samples
                val samples = 100
                val dtSource = sourceDurationUs.toDouble() / samples
                var totalTargetDuration = 0.0
                for (i in 0 until samples) {
                    val normSource = (i + 0.5) / samples.toDouble()
                    val speed = sampleSpeedAtNormalizedSource(normSource.toFloat())
                    totalTargetDuration += dtSource / speed
                }
                totalTargetDuration.roundToLong().coerceAtLeast(1000L)
            }
        }
    }

    /**
     * Maps an elapsed time on the timeline (targetOffsetUs) to the corresponding source media timestamp.
     */
    fun mapTargetTimeToSourceTime(
        targetOffsetUs: Long,
        targetDurationUs: Long,
        sourceDurationUs: Long
    ): Long {
        if (targetOffsetUs <= 0L) return 0L
        if (targetOffsetUs >= targetDurationUs) return sourceDurationUs
        if (targetDurationUs <= 0L || sourceDurationUs <= 0L) return 0L

        return when (mode) {
            SpeedMode.CONSTANT -> {
                (targetOffsetUs * speedMultiplier).roundToLong().coerceIn(0L, sourceDurationUs)
            }
            SpeedMode.CURVE -> {
                // Numerical integration to find source time where target integral matches
                val targetFraction = targetOffsetUs.toDouble() / targetDurationUs.toDouble()
                val samples = 200
                var accumulatedTargetFrac = 0.0
                var prevTargetFrac = 0.0
                val dtNorm = 1.0 / samples

                // Compute total target integral first
                var totalIntegral = 0.0
                for (i in 0 until samples) {
                    val s = sampleSpeedAtNormalizedSource((i + 0.5f) / samples)
                    totalIntegral += dtNorm / s
                }

                var currentIntegral = 0.0
                val desiredIntegral = targetFraction * totalIntegral
                for (i in 0 until samples) {
                    val s = sampleSpeedAtNormalizedSource((i + 0.5f) / samples)
                    val dInt = dtNorm / s
                    if (currentIntegral + dInt >= desiredIntegral) {
                        val remaining = desiredIntegral - currentIntegral
                        val partialFrac = remaining / dInt
                        val normSource = (i + partialFrac) / samples
                        return (normSource * sourceDurationUs).roundToLong().coerceIn(0L, sourceDurationUs)
                    }
                    currentIntegral += dInt
                }
                sourceDurationUs
            }
        }
    }

    /**
     * Samples curve speed at a given normalized position using linear interpolation between control points.
     */
    fun sampleSpeedAtNormalizedSource(norm: Float): Float {
        val clamped = norm.coerceIn(0f, 1f)
        if (curvePoints.isEmpty()) return 1.0f
        if (clamped <= curvePoints.first().x) return curvePoints.first().y
        if (clamped >= curvePoints.last().x) return curvePoints.last().y

        for (i in 0 until curvePoints.size - 1) {
            val p0 = curvePoints[i]
            val p1 = curvePoints[i + 1]
            if (clamped in p0.x..p1.x) {
                val span = p1.x - p0.x
                if (span <= 1e-6f) return p0.y
                val factor = (clamped - p0.x) / span
                return p0.y + factor * (p1.y - p0.y)
            }
        }
        return curvePoints.last().y
    }

    companion object {
        val NORMAL = SpeedConfig(mode = SpeedMode.CONSTANT, speedMultiplier = 1.0)

        // Preset curve templates matching CapCut curve speed presets
        val MONTAGE_HERO = SpeedConfig(
            mode = SpeedMode.CURVE,
            curvePoints = listOf(
                CurvePoint(0.0f, 1.0f),
                CurvePoint(0.3f, 3.5f),
                CurvePoint(0.5f, 0.4f),
                CurvePoint(0.7f, 3.5f),
                CurvePoint(1.0f, 1.0f)
            )
        )

        val BULLET_CURVE = SpeedConfig(
            mode = SpeedMode.CURVE,
            curvePoints = listOf(
                CurvePoint(0.0f, 1.0f),
                CurvePoint(0.25f, 5.0f),
                CurvePoint(0.5f, 0.2f),
                CurvePoint(0.75f, 5.0f),
                CurvePoint(1.0f, 1.0f)
            )
        )

        val JUMP_CUT = SpeedConfig(
            mode = SpeedMode.CURVE,
            curvePoints = listOf(
                CurvePoint(0.0f, 2.5f),
                CurvePoint(0.5f, 0.5f),
                CurvePoint(1.0f, 2.5f)
            )
        )
    }
}
