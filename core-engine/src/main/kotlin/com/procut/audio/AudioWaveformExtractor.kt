package com.procut.audio

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * High-performance audio waveform extractor and peak/RMS downsampling pipeline.
 * Implements windowed peak and root-mean-square amplitude extraction conforming to CapCut VEAudio specs.
 */
object AudioWaveformExtractor {

    const val DEFAULT_POINTS_PER_SECOND = 25
    const val MAX_16BIT_PCM = 32768.0f

    /**
     * Downsamples raw 16-bit PCM byte array into normalized [0.0, 1.0] amplitude points.
     */
    fun downsamplePcm16(
        pcmBytes: ByteArray,
        sampleRate: Int = 44100,
        channels: Int = 2,
        pointsPerSecond: Int = DEFAULT_POINTS_PER_SECOND
    ): FloatArray {
        if (pcmBytes.isEmpty()) return FloatArray(0)

        // 16-bit = 2 bytes per sample
        val bytesPerFrame = channels * 2
        val totalFrames = pcmBytes.size / bytesPerFrame
        if (totalFrames == 0) return FloatArray(0)

        val windowSizeFrames = max(1, sampleRate / pointsPerSecond)
        val totalWindows = (totalFrames + windowSizeFrames - 1) / windowSizeFrames
        val output = FloatArray(totalWindows)

        var byteOffset = 0
        for (w in 0 until totalWindows) {
            var peakAbs = 0f
            val framesInThisWindow = min(windowSizeFrames, totalFrames - (w * windowSizeFrames))

            for (f in 0 until framesInThisWindow) {
                // Read 16-bit little-endian sample from first channel
                if (byteOffset + 1 < pcmBytes.size) {
                    val low = pcmBytes[byteOffset].toInt() and 0xFF
                    val high = pcmBytes[byteOffset + 1].toInt()
                    val sample = (high shl 8) or low
                    val absVal = abs(sample.toFloat())
                    if (absVal > peakAbs) {
                        peakAbs = absVal
                    }
                }
                byteOffset += bytesPerFrame
            }

            output[w] = (peakAbs / MAX_16BIT_PCM).coerceIn(0.0f, 1.0f)
        }

        return output
    }

    /**
     * Downsamples normalized 32-bit float audio buffer into normalized [0.0, 1.0] amplitude points.
     */
    fun downsampleFloatBuffer(
        samples: FloatArray,
        sampleRate: Int = 44100,
        pointsPerSecond: Int = DEFAULT_POINTS_PER_SECOND
    ): FloatArray {
        if (samples.isEmpty()) return FloatArray(0)

        val windowSize = max(1, sampleRate / pointsPerSecond)
        val totalWindows = (samples.size + windowSize - 1) / windowSize
        val output = FloatArray(totalWindows)

        for (w in 0 until totalWindows) {
            val start = w * windowSize
            val end = min(start + windowSize, samples.size)
            var peak = 0.0f

            for (i in start until end) {
                val a = abs(samples[i])
                if (a > peak) peak = a
            }
            output[w] = peak.coerceIn(0.0f, 1.0f)
        }

        return output
    }

    /**
     * Computes RMS (Root-Mean-Square) energy envelope for float audio buffers.
     */
    fun computeRmsEnvelope(
        samples: FloatArray,
        sampleRate: Int = 44100,
        pointsPerSecond: Int = DEFAULT_POINTS_PER_SECOND
    ): FloatArray {
        if (samples.isEmpty()) return FloatArray(0)

        val windowSize = max(1, sampleRate / pointsPerSecond)
        val totalWindows = (samples.size + windowSize - 1) / windowSize
        val output = FloatArray(totalWindows)

        for (w in 0 until totalWindows) {
            val start = w * windowSize
            val end = min(start + windowSize, samples.size)
            var sumSquare = 0.0

            for (i in start until end) {
                val s = samples[i]
                sumSquare += (s * s)
            }
            val count = max(1, end - start)
            output[w] = sqrt(sumSquare / count).toFloat().coerceIn(0.0f, 1.0f)
        }

        return output
    }
}
