package com.procut.text

import com.procut.model.*
import kotlin.math.sin

data class TextRenderState(
    val transform: Transform2D,
    val visibleCharacterCount: Int,
    val textAlpha: Float,
    val shadowAlpha: Float
)

/**
 * Text Animation Evaluator executing parametric entrance, loop, and exit typography transforms.
 */
object TextAnimationEvaluator {

    fun evaluate(
        config: TextConfig,
        offsetUs: Long,
        totalClipDurationUs: Long
    ): TextRenderState {
        val totalChars = config.text.length
        var currentTransform = Transform2D.IDENTITY
        var visibleChars = totalChars
        var currentAlpha = config.textAlpha
        var shadowAlpha = if (config.shadow.hasShadow) 1.0f else 0.0f

        val inAnim = config.animationIn
        val outAnim = config.animationOut
        val loopAnim = config.animationLoop

        val inDurationUs = inAnim?.durationUs ?: 0L
        val outDurationUs = outAnim?.durationUs ?: 0L

        val isEntrance = inAnim != null && inAnim.type != TextAnimationType.NONE && offsetUs < inDurationUs
        val isExit = outAnim != null && outAnim.type != TextAnimationType.NONE && offsetUs > (totalClipDurationUs - outDurationUs)

        if (isEntrance && inDurationUs > 0L) {
            val progress = (offsetUs.toFloat() / inDurationUs.toFloat()).coerceIn(0f, 1f)
            when (inAnim.type) {
                TextAnimationType.TYPEWRITER -> {
                    visibleChars = (totalChars * progress).toInt().coerceIn(0, totalChars)
                }
                TextAnimationType.FADE_UP -> {
                    currentAlpha *= progress
                    val transY = (1.0f - progress) * 0.25f
                    currentTransform = currentTransform.copy(translationY = transY)
                }
                TextAnimationType.BOUNCE -> {
                    // Elastic overshoot bounce
                    val bounceScale = if (progress >= 1f) 1f else {
                        1f - kotlin.math.cos(progress * Math.PI.toFloat() * 2.5f) * (1f - progress) * 0.4f
                    }
                    currentTransform = currentTransform.copy(scaleX = bounceScale, scaleY = bounceScale)
                    currentAlpha *= progress
                }
                TextAnimationType.POP -> {
                    val popScale = if (progress < 0.7f) {
                        (progress / 0.7f) * 1.15f
                    } else {
                        1.15f - ((progress - 0.7f) / 0.3f) * 0.15f
                    }
                    currentTransform = currentTransform.copy(scaleX = popScale, scaleY = popScale)
                    currentAlpha *= progress
                }
                TextAnimationType.ZOOM -> {
                    val zoomScale = 2.0f - (progress * 1.0f)
                    currentTransform = currentTransform.copy(scaleX = zoomScale, scaleY = zoomScale)
                    currentAlpha *= progress
                }
                TextAnimationType.SLIDE_UP -> {
                    val transY = (1.0f - progress) * 0.6f
                    currentTransform = currentTransform.copy(translationY = transY)
                    currentAlpha *= progress
                }
                TextAnimationType.SLIDE_RIGHT -> {
                    val transX = (progress - 1.0f) * 0.6f
                    currentTransform = currentTransform.copy(translationX = transX)
                    currentAlpha *= progress
                }
                TextAnimationType.GLITCH -> {
                    val jitterX = sin(progress * 48f) * 0.035f * (1f - progress)
                    val jitterY = sin(progress * 64f) * 0.02f * (1f - progress)
                    currentTransform = currentTransform.copy(translationX = jitterX, translationY = jitterY)
                    currentAlpha *= if ((progress * 10).toInt() % 2 == 0) 0.5f else 1.0f
                }
                else -> Unit
            }
        } else if (isExit && outDurationUs > 0L) {
            val exitOffsetUs = offsetUs - (totalClipDurationUs - outDurationUs)
            val progress = (exitOffsetUs.toFloat() / outDurationUs.toFloat()).coerceIn(0f, 1f)
            when (outAnim.type) {
                TextAnimationType.FADE_UP -> {
                    currentAlpha *= (1.0f - progress)
                    val transY = -progress * 0.25f
                    currentTransform = currentTransform.copy(translationY = transY)
                }
                TextAnimationType.SLIDE_RIGHT -> {
                    val transX = progress * 0.6f
                    currentTransform = currentTransform.copy(translationX = transX)
                    currentAlpha *= (1.0f - progress)
                }
                TextAnimationType.POP -> {
                    val popScale = (1.0f - progress).coerceAtLeast(0f)
                    currentTransform = currentTransform.copy(scaleX = popScale, scaleY = popScale)
                    currentAlpha *= (1.0f - progress)
                }
                else -> {
                    currentAlpha *= (1.0f - progress)
                }
            }
        } else if (loopAnim != null && loopAnim.type != TextAnimationType.NONE) {
            // Loop Phase: Periodic modulation
            val loopDurationUs = if (loopAnim.durationUs > 0L) loopAnim.durationUs else 1_000_000L
            val phase = ((offsetUs % loopDurationUs).toFloat() / loopDurationUs.toFloat()) * (2f * Math.PI.toFloat())

            when (loopAnim.type) {
                TextAnimationType.PULSE -> {
                    val pulseScale = 1.0f + 0.08f * sin(phase)
                    currentTransform = currentTransform.copy(scaleX = pulseScale, scaleY = pulseScale)
                }
                TextAnimationType.WOBBLE -> {
                    val wobbleAngle = 4.0f * sin(phase)
                    currentTransform = currentTransform.copy(rotationDeg = wobbleAngle)
                }
                else -> Unit
            }
        }

        return TextRenderState(
            transform = currentTransform,
            visibleCharacterCount = visibleChars,
            textAlpha = currentAlpha.coerceIn(0f, 1f),
            shadowAlpha = (shadowAlpha * currentAlpha).coerceIn(0f, 1f)
        )
    }
}
