package com.procut.render

import com.procut.engine.KeyframeEvaluator
import com.procut.model.*
import kotlin.math.abs

sealed class RenderPass {
    data class Background(
        val canvasConfig: CanvasConfig,
        val mainVideoTextureId: Int? = null
    ) : RenderPass()

    data class VideoLayer(
        val clipId: String,
        val textureId: Int,
        val isOesTexture: Boolean,
        val mvpMatrix: Matrix4f,
        val opacity: Float,
        val colorAdjustment: ColorAdjustment,
        val maskConfig: MaskConfig,
        val chromaKeyConfig: ChromaKeyConfig
    ) : RenderPass()

    data class TransitionPass(
        val clipAId: String,
        val textureAId: Int,
        val clipBId: String,
        val textureBId: Int,
        val transitionType: TransitionType,
        val progress: Float // 0.0 to 1.0
    ) : RenderPass()
}

data class CompositionFrame(
    val playheadUs: Long,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val passes: List<RenderPass>
)

class CompositorPipeline(
    private val keyframeEvaluator: KeyframeEvaluator = KeyframeEvaluator()
) {

    /**
     * Builds the deterministic multi-pass render graph for the current playhead timestamp.
     */
    fun buildCompositionFrame(
        project: EditorProject,
        playheadUs: Long,
        resolvedTextures: Map<String, Int> // clipId -> OpenGL Texture ID
    ): CompositionFrame {
        val (canvasW, canvasH) = project.canvasConfig.calculateDimensions()
        val passes = mutableListOf<RenderPass>()

        val mainTrack = project.mainTrack
        val activeMainClip = mainTrack?.findClipAt(playheadUs)

        // 1. Canvas Background Pass
        val mainTexId = activeMainClip?.let { resolvedTextures[it.id] }
        passes.add(RenderPass.Background(project.canvasConfig, mainTexId))

        // 2. Main Track (or Transition)
        if (mainTrack != null) {
            val transitionPass = checkActiveTransition(mainTrack, playheadUs, resolvedTextures)
            if (transitionPass != null) {
                passes.add(transitionPass)
            } else if (activeMainClip != null) {
                val texId = resolvedTextures[activeMainClip.id] ?: 0
                val asset = project.findAssetById(activeMainClip.assetId)
                val videoW = asset?.width ?: canvasW
                val videoH = asset?.height ?: canvasH

                val transform = keyframeEvaluator.evaluateTransformAt(activeMainClip, playheadUs)
                val mvp = Matrix4f.calculateLayerMVP(
                    canvasWidth = canvasW,
                    canvasHeight = canvasH,
                    videoWidth = videoW,
                    videoHeight = videoH,
                    translationX = transform.translationX,
                    translationY = transform.translationY,
                    scaleX = transform.scaleX,
                    scaleY = transform.scaleY,
                    rotationDeg = transform.rotationDeg
                )
                val colorAdj = keyframeEvaluator.evaluateColorAdjustmentAt(activeMainClip, playheadUs)

                passes.add(
                    RenderPass.VideoLayer(
                        clipId = activeMainClip.id,
                        textureId = texId,
                        isOesTexture = false,
                        mvpMatrix = mvp,
                        opacity = transform.opacity,
                        colorAdjustment = colorAdj,
                        maskConfig = activeMainClip.maskConfig,
                        chromaKeyConfig = activeMainClip.chromaKeyConfig
                    )
                )
            }
        }

        // 3. PIP Overlay Tracks (Sorted bottom-to-top)
        for (overlayTrack in project.overlayTracks) {
            if (overlayTrack.isHidden) continue
            val activeOverlayClip = overlayTrack.findClipAt(playheadUs) ?: continue
            val texId = resolvedTextures[activeOverlayClip.id] ?: 0
            val asset = project.findAssetById(activeOverlayClip.assetId)
            val videoW = asset?.width ?: canvasW
            val videoH = asset?.height ?: canvasH

            val transform = keyframeEvaluator.evaluateTransformAt(activeOverlayClip, playheadUs)
            val mvp = Matrix4f.calculateLayerMVP(
                canvasWidth = canvasW,
                canvasHeight = canvasH,
                videoWidth = videoW,
                videoHeight = videoH,
                translationX = transform.translationX,
                translationY = transform.translationY,
                scaleX = transform.scaleX,
                scaleY = transform.scaleY,
                rotationDeg = transform.rotationDeg
            )
            val colorAdj = keyframeEvaluator.evaluateColorAdjustmentAt(activeOverlayClip, playheadUs)

            passes.add(
                RenderPass.VideoLayer(
                    clipId = activeOverlayClip.id,
                    textureId = texId,
                    isOesTexture = false,
                    mvpMatrix = mvp,
                    opacity = transform.opacity,
                    colorAdjustment = colorAdj,
                    maskConfig = activeOverlayClip.maskConfig,
                    chromaKeyConfig = activeOverlayClip.chromaKeyConfig
                )
            )
        }

        return CompositionFrame(
            playheadUs = playheadUs,
            canvasWidth = canvasW,
            canvasHeight = canvasH,
            passes = passes
        )
    }

    private fun checkActiveTransition(
        mainTrack: Track,
        playheadUs: Long,
        resolvedTextures: Map<String, Int>
    ): RenderPass.TransitionPass? {
        val clips = mainTrack.clips
        for (i in 0 until clips.size - 1) {
            val clipA = clips[i]
            val clipB = clips[i + 1]
            val transition = clipB.transitionIn ?: continue

            val boundaryUs = clipA.targetTimeRangeUs.endUs
            val halfDuration = transition.durationUs / 2
            val transitionStartUs = boundaryUs - halfDuration
            val transitionEndUs = boundaryUs + halfDuration

            if (playheadUs in transitionStartUs..transitionEndUs) {
                val totalSpan = (transitionEndUs - transitionStartUs).toFloat()
                val progress = if (totalSpan > 0) ((playheadUs - transitionStartUs).toFloat() / totalSpan).coerceIn(0f, 1f) else 1f

                val texA = resolvedTextures[clipA.id] ?: 0
                val texB = resolvedTextures[clipB.id] ?: 0

                return RenderPass.TransitionPass(
                    clipAId = clipA.id,
                    textureAId = texA,
                    clipBId = clipB.id,
                    textureBId = texB,
                    transitionType = transition.type,
                    progress = progress
                )
            }
        }
        return null
    }
}
