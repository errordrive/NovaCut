package com.procut.render

import com.procut.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CompositorPipelineTest {

    private lateinit var pipeline: CompositorPipeline
    private lateinit var project: EditorProject

    @Before
    fun setUp() {
        pipeline = CompositorPipeline()

        val asset1 = MediaAsset(id = "asset-1", filePath = "/path/1.mp4", durationUs = 10_000_000L)
        val asset2 = MediaAsset(id = "asset-2", filePath = "/path/2.mp4", durationUs = 10_000_000L)

        val clip1 = ClipSegment(
            id = "clip-1",
            assetId = "asset-1",
            trackId = "track-main",
            sourceTimeRangeUs = TimeRangeUs(0L, 5_000_000L),
            targetTimeRangeUs = TimeRangeUs(0L, 5_000_000L)
        )

        val clip2 = ClipSegment(
            id = "clip-2",
            assetId = "asset-2",
            trackId = "track-main",
            sourceTimeRangeUs = TimeRangeUs(0L, 5_000_000L),
            targetTimeRangeUs = TimeRangeUs(5_000_000L, 5_000_000L),
            transitionIn = Transition(type = TransitionType.ZOOM, durationUs = 1_000_000L) // 1s transition
        )

        val mainTrack = Track(
            id = "track-main",
            type = TrackType.MAIN_VIDEO,
            name = "Main",
            clips = listOf(clip1, clip2)
        )

        val pipClip = ClipSegment(
            id = "clip-pip",
            assetId = "asset-2",
            trackId = "track-pip",
            sourceTimeRangeUs = TimeRangeUs(0L, 3_000_000L),
            targetTimeRangeUs = TimeRangeUs(2_000_000L, 3_000_000L)
        )

        val pipTrack = Track(
            id = "track-pip",
            type = TrackType.PIP_OVERLAY,
            name = "Overlay",
            clips = listOf(pipClip)
        )

        project = EditorProject(
            id = "proj-1",
            name = "Pipeline Test",
            tracks = listOf(mainTrack, pipTrack),
            assets = listOf(asset1, asset2)
        )
    }

    @Test
    fun testCompositionFramePassHierarchy() {
        // At t = 3.0s, we have: Background -> Main Clip 1 -> PIP Clip
        val frame = pipeline.buildCompositionFrame(
            project = project,
            playheadUs = 3_000_000L,
            resolvedTextures = mapOf("clip-1" to 101, "clip-pip" to 202)
        )

        assertEquals(3, frame.passes.size)
        assertTrue(frame.passes[0] is RenderPass.Background)
        assertTrue(frame.passes[1] is RenderPass.VideoLayer)
        assertEquals("clip-1", (frame.passes[1] as RenderPass.VideoLayer).clipId)
        assertTrue(frame.passes[2] is RenderPass.VideoLayer)
        assertEquals("clip-pip", (frame.passes[2] as RenderPass.VideoLayer).clipId)
    }

    @Test
    fun testActiveTransitionWindowDetection() {
        // Cut point between clip1 and clip2 is at 5.0s (5_000_000 us).
        // Transition duration is 1.0s, so window is [4.5s .. 5.5s].
        // At t = 4.8s, transition should be active with progress = (4.8 - 4.5) / 1.0 = 0.3
        val frame = pipeline.buildCompositionFrame(
            project = project,
            playheadUs = 4_800_000L,
            resolvedTextures = mapOf("clip-1" to 101, "clip-2" to 102)
        )

        val transitionPass = frame.passes.filterIsInstance<RenderPass.TransitionPass>().firstOrNull()
        assertNotNull(transitionPass)
        assertEquals("clip-1", transitionPass!!.clipAId)
        assertEquals("clip-2", transitionPass.clipBId)
        assertEquals(TransitionType.ZOOM, transitionPass.transitionType)
        assertEquals(0.3f, transitionPass.progress, 0.05f)
    }
}
