package com.procut.audio

import org.junit.Assert.*
import org.junit.Test

class BeatDetectorTest {

    @Test
    fun testDetectBeatsFromSyntheticRhythm() {
        // Create 4 seconds of synthetic amplitude points at 25 points/sec (100 points total)
        // Insert sharp transient peaks every 25 points (every 1 second = 60 BPM beat)
        val samplePoints = FloatArray(100) { 0.05f }
        samplePoints[25] = 0.95f
        samplePoints[50] = 0.90f
        samplePoints[75] = 0.92f

        val beats = BeatDetector.detectBeats(
            amplitudes = samplePoints,
            pointsPerSecond = 25,
            thresholdMultiplier = 1.2f,
            minIntervalMs = 300L
        )

        // Expect at least 3 detected beat markers around 1.0s, 2.0s, 3.0s
        assertTrue("Expected detected beats, got ${beats.size}", beats.size >= 3)

        // Verify beat timestamps
        val t0Us = beats[0].timeUs
        val t1Us = beats[1].timeUs
        val t2Us = beats[2].timeUs

        assertEquals(1_000_000L, t0Us)
        assertEquals(2_000_000L, t1Us)
        assertEquals(3_000_000L, t2Us)

        // Verify intensities are positive
        beats.forEach { beat ->
            assertTrue("Beat intensity must be >= 0.5", beat.intensity >= 0.5f)
        }
    }

    @Test
    fun testRefractoryPeriodSuppressesDoubleHits() {
        val samplePoints = FloatArray(50) { 0.05f }
        // Two consecutive spikes within 80ms (points 10 and 11)
        samplePoints[10] = 0.90f
        samplePoints[11] = 0.88f

        val beats = BeatDetector.detectBeats(
            amplitudes = samplePoints,
            pointsPerSecond = 25,
            thresholdMultiplier = 1.2f,
            minIntervalMs = 250L // 250ms refractory window (~6.25 points)
        )

        // Must only detect 1 beat due to refractory suppression
        assertEquals(1, beats.size)
        assertEquals(10 * 40_000L, beats[0].timeUs)
    }
}
