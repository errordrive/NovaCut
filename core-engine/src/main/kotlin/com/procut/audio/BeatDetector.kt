package com.procut.audio

import com.procut.model.BeatMarker
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Audio Beat Marker & Onset Detection Engine.
 * Detects rhythmic transients and musical onsets using energy flux derivation and adaptive local thresholding.
 */
object BeatDetector {

    /**
     * Detects beat marker timestamps from downsampled amplitude or RMS energy points.
     *
     * @param amplitudes Normalized amplitude array (points sampled at [pointsPerSecond])
     * @param pointsPerSecond Sampling rate of the amplitude array (e.g. 25 points/sec = 40ms per point)
     * @param thresholdMultiplier Dynamic standard deviation threshold multiplier (typically 1.2 to 1.8)
     * @param minIntervalMs Minimum time interval between adjacent beats in milliseconds (refractory period)
     */
    fun detectBeats(
        amplitudes: FloatArray,
        pointsPerSecond: Int = AudioWaveformExtractor.DEFAULT_POINTS_PER_SECOND,
        thresholdMultiplier: Float = 1.35f,
        minIntervalMs: Long = 200L
    ): List<BeatMarker> {
        if (amplitudes.size < 3) return emptyList()

        val dtUs = (1_000_000L / pointsPerSecond)
        val minIntervalPoints = max(1, ((minIntervalMs * 1000L) / dtUs).toInt())

        // 1. Half-wave rectified energy difference (Spectral / Energy Flux)
        val flux = FloatArray(amplitudes.size)
        for (i in 1 until amplitudes.size) {
            val diff = amplitudes[i] - amplitudes[i - 1]
            flux[i] = if (diff > 0f) diff else 0f
        }

        // 2. Adaptive local thresholding over a sliding window (+/- 8 points ~ 0.64s)
        val windowRadius = 8
        val beatMarkers = mutableListOf<BeatMarker>()
        var lastBeatIndex = -minIntervalPoints

        for (i in 1 until flux.size - 1) {
            // Local window bounds
            val wStart = max(0, i - windowRadius)
            val wEnd = min(flux.size - 1, i + windowRadius)
            val wCount = wEnd - wStart + 1

            var sum = 0.0
            for (j in wStart..wEnd) {
                sum += flux[j]
            }
            val mean = (sum / wCount).toFloat()

            var varianceSum = 0.0
            for (j in wStart..wEnd) {
                val d = flux[j] - mean
                varianceSum += (d * d)
            }
            val stdDev = sqrt(varianceSum / wCount).toFloat()
            val threshold = mean + (thresholdMultiplier * stdDev)

            // Local maximum check & threshold check & refractory check
            val currentFlux = flux[i]
            if (currentFlux > threshold &&
                currentFlux >= flux[i - 1] &&
                currentFlux >= flux[i + 1] &&
                (i - lastBeatIndex) >= minIntervalPoints
            ) {
                val timeUs = i * dtUs
                val intensity = (currentFlux / (threshold + 1e-5f)).coerceIn(0.5f, 2.0f)
                beatMarkers.add(BeatMarker(timeUs = timeUs, intensity = intensity))
                lastBeatIndex = i
            }
        }

        return beatMarkers
    }
}
