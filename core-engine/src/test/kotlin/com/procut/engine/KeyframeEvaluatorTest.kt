package com.procut.engine

import com.procut.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class KeyframeEvaluatorTest {

    private lateinit var evaluator: KeyframeEvaluator
    private lateinit var baseClip: ClipSegment

    @Before
    fun setUp() {
        evaluator = KeyframeEvaluator()
        baseClip = ClipSegment(
            id = "clip-1",
            assetId = "asset-1",
            trackId = "track-main",
            sourceTimeRangeUs = TimeRangeUs(0L, 10_000_000L),
            targetTimeRangeUs = TimeRangeUs(0L, 10_000_000L),
            transform = Transform2D(
                translationX = 0f,
                translationY = 0f,
                scaleX = 1f,
                scaleY = 1f,
                rotationDeg = 0f,
                opacity = 1f
            ),
            volume = 1.0f,
            colorAdjustment = ColorAdjustment.DEFAULT
        )
    }

    @Test
    fun testLinearInterpolation() {
        val kf0 = Keyframe(id = "kf-0", timeOffsetUs = 0L, property = KeyframeProperty.POSITION_X, value = 0f, curve = KeyframeCurve.LINEAR)
        val kf1 = Keyframe(id = "kf-1", timeOffsetUs = 4_000_000L, property = KeyframeProperty.POSITION_X, value = 100f, curve = KeyframeCurve.LINEAR)

        val clip = baseClip.copy(keyframes = listOf(kf0, kf1))

        // t = 0
        assertEquals(0f, evaluator.evaluatePropertyAt(clip, KeyframeProperty.POSITION_X, 0L, 0f), 0.001f)
        // t = 2s (midpoint)
        assertEquals(50f, evaluator.evaluatePropertyAt(clip, KeyframeProperty.POSITION_X, 2_000_000L, 0f), 0.001f)
        // t = 4s
        assertEquals(100f, evaluator.evaluatePropertyAt(clip, KeyframeProperty.POSITION_X, 4_000_000L, 0f), 0.001f)
        // t > 4s (clamped to boundary value)
        assertEquals(100f, evaluator.evaluatePropertyAt(clip, KeyframeProperty.POSITION_X, 6_000_000L, 0f), 0.001f)
    }

    @Test
    fun testCubicBezierNewtonRaphsonEasing() {
        // Custom Cubic Bezier curve (0.42, 0.0, 0.58, 1.0)
        val kf0 = Keyframe(
            id = "kf-0",
            timeOffsetUs = 0L,
            property = KeyframeProperty.OPACITY,
            value = 0f,
            curve = KeyframeCurve.CUBIC_BEZIER,
            controlPoint1X = 0.42f,
            controlPoint1Y = 0.0f,
            controlPoint2X = 0.58f,
            controlPoint2Y = 1.0f
        )
        val kf1 = Keyframe(
            id = "kf-1",
            timeOffsetUs = 1_000_000L, // 1s
            property = KeyframeProperty.OPACITY,
            value = 1f,
            curve = KeyframeCurve.LINEAR
        )

        val clip = baseClip.copy(keyframes = listOf(kf0, kf1))

        val v0 = evaluator.evaluatePropertyAt(clip, KeyframeProperty.OPACITY, 0L, 0f)
        val vMid = evaluator.evaluatePropertyAt(clip, KeyframeProperty.OPACITY, 500_000L, 0f)
        val v1 = evaluator.evaluatePropertyAt(clip, KeyframeProperty.OPACITY, 1_000_000L, 0f)

        assertEquals(0f, v0, 0.001f)
        assertEquals(1f, v1, 0.001f)
        // S-curve symmetry at midpoint
        assertEquals(0.5f, vMid, 0.05f)
    }

    @Test
    fun testMultiChannelTransformEvaluation() {
        val posX0 = Keyframe(id = "1", timeOffsetUs = 0L, property = KeyframeProperty.POSITION_X, value = -0.5f)
        val posX1 = Keyframe(id = "2", timeOffsetUs = 2_000_000L, property = KeyframeProperty.POSITION_X, value = 0.5f)

        val rot0 = Keyframe(id = "3", timeOffsetUs = 0L, property = KeyframeProperty.ROTATION_DEG, value = 0f)
        val rot1 = Keyframe(id = "4", timeOffsetUs = 2_000_000L, property = KeyframeProperty.ROTATION_DEG, value = 180f)

        val scale0 = Keyframe(id = "5", timeOffsetUs = 0L, property = KeyframeProperty.SCALE_X, value = 1.0f)
        val scale1 = Keyframe(id = "6", timeOffsetUs = 2_000_000L, property = KeyframeProperty.SCALE_X, value = 2.0f)

        val clip = baseClip.copy(keyframes = listOf(posX0, posX1, rot0, rot1, scale0, scale1))

        val transformMid = evaluator.evaluateTransformAt(clip, playheadUs = 1_000_000L)

        assertEquals(0.0f, transformMid.translationX, 0.001f)
        assertEquals(90.0f, transformMid.rotationDeg, 0.001f)
        assertEquals(1.5f, transformMid.scaleX, 0.001f)
        assertEquals(1.0f, transformMid.scaleY, 0.001f)
        assertEquals(1.0f, transformMid.opacity, 0.001f)
    }

    @Test
    fun testColorAdjustment12ParametersEvaluation() {
        val brightKf = Keyframe(id = "b0", timeOffsetUs = 0L, property = KeyframeProperty.BRIGHTNESS, value = 0.0f)
        val brightKf1 = Keyframe(id = "b1", timeOffsetUs = 2_000_000L, property = KeyframeProperty.BRIGHTNESS, value = 0.5f)

        val tempKf = Keyframe(id = "t0", timeOffsetUs = 0L, property = KeyframeProperty.TEMPERATURE, value = -0.2f)
        val tempKf1 = Keyframe(id = "t1", timeOffsetUs = 2_000_000L, property = KeyframeProperty.TEMPERATURE, value = 0.4f)

        val grainKf = Keyframe(id = "g0", timeOffsetUs = 0L, property = KeyframeProperty.GRAIN, value = 0.0f)
        val grainKf1 = Keyframe(id = "g1", timeOffsetUs = 2_000_000L, property = KeyframeProperty.GRAIN, value = 0.8f)

        val clip = baseClip.copy(keyframes = listOf(brightKf, brightKf1, tempKf, tempKf1, grainKf, grainKf1))

        val adjMid = evaluator.evaluateColorAdjustmentAt(clip, playheadUs = 1_000_000L)

        assertEquals(0.25f, adjMid.brightness, 0.001f)
        assertEquals(0.10f, adjMid.temperature, 0.001f)
        assertEquals(0.40f, adjMid.grain, 0.001f)
        assertEquals(0.0f, adjMid.contrast, 0.001f)
    }

    @Test
    fun testDiamondIndicatorAndKeyframeCRUD() {
        assertFalse(evaluator.hasKeyframeAt(baseClip, playheadUs = 2_000_000L))

        // 1. Add keyframe at 2.0s
        var clip = evaluator.addOrUpdateKeyframe(
            clip = baseClip,
            playheadUs = 2_000_000L,
            property = KeyframeProperty.ROTATION_DEG,
            value = 45f
        )
        assertTrue(evaluator.hasKeyframeAt(clip, playheadUs = 2_000_000L))
        // Tolerance test (1.995s is within 10ms tolerance)
        assertTrue(evaluator.hasKeyframeAt(clip, playheadUs = 1_995_000L))
        // Outside tolerance
        assertFalse(evaluator.hasKeyframeAt(clip, playheadUs = 1_950_000L))

        // 2. Update existing keyframe at 2.0s
        clip = evaluator.addOrUpdateKeyframe(
            clip = clip,
            playheadUs = 2_000_000L,
            property = KeyframeProperty.ROTATION_DEG,
            value = 90f
        )
        val kf = evaluator.getKeyframeAt(clip, playheadUs = 2_000_000L, property = KeyframeProperty.ROTATION_DEG)
        assertNotNull(kf)
        assertEquals(90f, kf!!.value, 0.001f)
        assertEquals(1, clip.keyframes.size)

        // 3. Toggle keyframe (already exists -> removes it)
        clip = evaluator.toggleKeyframe(clip, playheadUs = 2_000_000L, property = KeyframeProperty.ROTATION_DEG, currentValue = 90f)
        assertFalse(evaluator.hasKeyframeAt(clip, playheadUs = 2_000_000L))
        assertEquals(0, clip.keyframes.size)

        // 4. Toggle keyframe again (does not exist -> adds it)
        clip = evaluator.toggleKeyframe(clip, playheadUs = 2_000_000L, property = KeyframeProperty.ROTATION_DEG, currentValue = 120f)
        assertTrue(evaluator.hasKeyframeAt(clip, playheadUs = 2_000_000L))
        assertEquals(1, clip.keyframes.size)
        assertEquals(120f, clip.keyframes[0].value, 0.001f)
    }
}
