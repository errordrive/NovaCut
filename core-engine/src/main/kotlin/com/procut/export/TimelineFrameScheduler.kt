package com.procut.export

import kotlin.math.max
import kotlin.math.roundToLong

data class ExportProgress(
    val currentFrame: Int,
    val totalFrames: Int,
    val progress: Float, // 0.0 to 1.0
    val elapsedMs: Long,
    val estimatedRemainingMs: Long,
    val currentPlayheadUs: Long
)

class TimelineFrameScheduler(
    val durationUs: Long,
    val frameRate: Int
) {
    init {
        require(frameRate > 0) { "Frame rate must be greater than 0, got $frameRate" }
    }

    val frameDurationUs: Long = (1_000_000.0 / frameRate).roundToLong()

    val totalFrames: Int = if (durationUs <= 0L) {
        0
    } else {
        ((durationUs + frameDurationUs - 1) / frameDurationUs).toInt()
    }

    /**
     * Returns the exact timeline presentation timestamp in microseconds for [frameIndex].
     */
    fun getFrameTimeUs(frameIndex: Int): Long {
        require(frameIndex in 0 until totalFrames) {
            "Frame index $frameIndex out of range [0, $totalFrames)"
        }
        val tUs = (frameIndex.toDouble() * 1_000_000.0 / frameRate).roundToLong()
        return minOf(tUs, durationUs)
    }

    /**
     * Calculates the export progress and smoothed estimated time remaining (ETA).
     */
    fun computeProgress(
        renderedFrameCount: Int,
        elapsedRealtimeMs: Long
    ): ExportProgress {
        if (totalFrames == 0) {
            return ExportProgress(
                currentFrame = 0,
                totalFrames = 0,
                progress = 1.0f,
                elapsedMs = elapsedRealtimeMs,
                estimatedRemainingMs = 0L,
                currentPlayheadUs = 0L
            )
        }

        val clampedFrame = renderedFrameCount.coerceIn(0, totalFrames)
        val progress = (clampedFrame.toFloat() / totalFrames.toFloat()).coerceIn(0f, 1f)
        val currentPlayheadUs = if (clampedFrame > 0) getFrameTimeUs(clampedFrame - 1) else 0L

        val estimatedRemainingMs = if (clampedFrame <= 1 || elapsedRealtimeMs <= 0) {
            0L
        } else {
            val msPerFrame = elapsedRealtimeMs.toDouble() / clampedFrame.toDouble()
            val framesLeft = totalFrames - clampedFrame
            (framesLeft * msPerFrame).roundToLong()
        }

        return ExportProgress(
            currentFrame = clampedFrame,
            totalFrames = totalFrames,
            progress = progress,
            elapsedMs = elapsedRealtimeMs,
            estimatedRemainingMs = max(0L, estimatedRemainingMs),
            currentPlayheadUs = currentPlayheadUs
        )
    }
}
