package com.procut.text

import com.procut.model.TextAnimationConfig
import com.procut.model.TextAnimationType
import com.procut.model.TextConfig
import org.junit.Assert.*
import org.junit.Test

class TextAnimationEvaluatorTest {

    @Test
    fun testTypewriterCharacterProgression() {
        val sampleText = "HELLO ANTIGRAVITY"
        val config = TextConfig(
            text = sampleText,
            animationIn = TextAnimationConfig(
                type = TextAnimationType.TYPEWRITER,
                durationUs = 1_000_000L
            )
        )
        val clipDurationUs = 5_000_000L

        // At t = 0s: 0 characters visible
        val state0 = TextAnimationEvaluator.evaluate(config, 0L, clipDurationUs)
        assertEquals(0, state0.visibleCharacterCount)

        // At t = 0.5s: half the characters visible
        val stateHalf = TextAnimationEvaluator.evaluate(config, 500_000L, clipDurationUs)
        assertEquals(sampleText.length / 2, stateHalf.visibleCharacterCount)

        // At t = 1.0s: all characters revealed
        val stateFull = TextAnimationEvaluator.evaluate(config, 1_000_000L, clipDurationUs)
        assertEquals(sampleText.length, stateFull.visibleCharacterCount)

        // At t = 2.0s: remains fully revealed
        val stateMid = TextAnimationEvaluator.evaluate(config, 2_000_000L, clipDurationUs)
        assertEquals(sampleText.length, stateMid.visibleCharacterCount)
    }

    @Test
    fun testFadeUpEntranceTransform() {
        val config = TextConfig(
            text = "Fade Title",
            animationIn = TextAnimationConfig(
                type = TextAnimationType.FADE_UP,
                durationUs = 500_000L
            )
        )
        val clipDurationUs = 3_000_000L

        // At t = 0s: alpha is 0.0, translationY > 0
        val state0 = TextAnimationEvaluator.evaluate(config, 0L, clipDurationUs)
        assertEquals(0.0f, state0.textAlpha, 1e-4f)
        assertTrue("translationY should be positive at start", state0.transform.translationY > 0f)

        // At t = 500ms (end of entrance): alpha is 1.0, translationY is 0.0
        val stateEnd = TextAnimationEvaluator.evaluate(config, 500_000L, clipDurationUs)
        assertEquals(1.0f, stateEnd.textAlpha, 1e-4f)
        assertEquals(0.0f, stateEnd.transform.translationY, 1e-4f)
    }

    @Test
    fun testLoopPulseModulation() {
        val config = TextConfig(
            text = "Pulsing Glow",
            animationLoop = TextAnimationConfig(
                type = TextAnimationType.PULSE,
                durationUs = 1_000_000L // 1 second period
            )
        )
        val clipDurationUs = 4_000_000L

        // Phase = 0: sin(0) = 0 -> scale = 1.0
        val state0 = TextAnimationEvaluator.evaluate(config, 0L, clipDurationUs)
        assertEquals(1.0f, state0.transform.scaleX, 1e-3f)

        // Phase = pi/2 (250ms): sin(pi/2) = 1 -> scale = 1.08
        val statePeak = TextAnimationEvaluator.evaluate(config, 250_000L, clipDurationUs)
        assertEquals(1.08f, statePeak.transform.scaleX, 1e-3f)

        // Phase = 3pi/2 (750ms): sin(3pi/2) = -1 -> scale = 0.92
        val stateTrough = TextAnimationEvaluator.evaluate(config, 750_000L, clipDurationUs)
        assertEquals(0.92f, stateTrough.transform.scaleX, 1e-3f)
    }
}
