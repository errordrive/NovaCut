package com.procut.audio

import com.procut.model.AudioFadeConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFadeMathTest {

    @Test
    fun testFadeInGainCurve() {
        val fadeConfig = AudioFadeConfig(
            fadeInDurationUs = 1_000_000L, // 1 second
            fadeOutDurationUs = 0L
        )
        val clipDurationUs = 5_000_000L

        // At t = 0, gain must be exactly 0.0
        val gain0 = fadeConfig.computeGain(0L, clipDurationUs)
        assertEquals(0.0f, gain0, 1e-4f)

        // At t = 0.5s (halfway through fade in), sin(pi/4) approx 0.7071
        val gainHalf = fadeConfig.computeGain(500_000L, clipDurationUs)
        assertEquals(0.7071f, gainHalf, 1e-3f)

        // At t = 1.0s (end of fade in), gain must reach 1.0
        val gainEnd = fadeConfig.computeGain(1_000_000L, clipDurationUs)
        assertEquals(1.0f, gainEnd, 1e-4f)

        // Beyond fade in, gain remains 1.0
        val gainMid = fadeConfig.computeGain(2_500_000L, clipDurationUs)
        assertEquals(1.0f, gainMid, 1e-4f)
    }

    @Test
    fun testFadeOutGainCurve() {
        val fadeConfig = AudioFadeConfig(
            fadeInDurationUs = 0L,
            fadeOutDurationUs = 2_000_000L // 2 seconds
        )
        val clipDurationUs = 10_000_000L // 10 seconds total

        // Before fade out begins (e.g. at 7s), gain is 1.0
        val gainBefore = fadeConfig.computeGain(7_000_000L, clipDurationUs)
        assertEquals(1.0f, gainBefore, 1e-4f)

        // Fade out begins at 8s (10s - 2s): cos(0) = 1.0
        val gainStart = fadeConfig.computeGain(8_000_000L, clipDurationUs)
        assertEquals(1.0f, gainStart, 1e-3f)

        // Halfway through fade out (at 9s): cos(pi/4) approx 0.7071
        val gainHalf = fadeConfig.computeGain(9_000_000L, clipDurationUs)
        assertEquals(0.7071f, gainHalf, 1e-3f)

        // At the end of clip (10s): cos(pi/2) = 0.0
        val gainFinal = fadeConfig.computeGain(10_000_000L, clipDurationUs)
        assertEquals(0.0f, gainFinal, 1e-4f)
    }

    @Test
    fun testCombinedFadeAndClamping() {
        val fadeConfig = AudioFadeConfig(
            fadeInDurationUs = 1_000_000L,
            fadeOutDurationUs = 1_000_000L
        )
        val clipDurationUs = 4_000_000L

        // Negative time clamp
        val gainNeg = fadeConfig.computeGain(-100_000L, clipDurationUs)
        assertEquals(0.0f, gainNeg, 1e-4f)

        // Overflow time clamp
        val gainOver = fadeConfig.computeGain(5_000_000L, clipDurationUs)
        assertEquals(0.0f, gainOver, 1e-4f)

        // Middle plateau
        val gainMid = fadeConfig.computeGain(2_000_000L, clipDurationUs)
        assertEquals(1.0f, gainMid, 1e-4f)
    }
}
