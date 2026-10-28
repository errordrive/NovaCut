package com.procut.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineFrameSchedulerTest {

    @Test
    fun testTotalFramesCalculation() {
        // 1 second (1_000_000 us) at 30 fps -> 30 frames
        val scheduler30fps = TimelineFrameScheduler(1_000_000L, 30)
        assertEquals(30, scheduler30fps.totalFrames)
        assertEquals(33_333L, scheduler30fps.frameDurationUs)

        // 60 fps
        val scheduler60fps = TimelineFrameScheduler(1_000_000L, 60)
        assertEquals(60, scheduler60fps.totalFrames)
        assertEquals(16_667L, scheduler60fps.frameDurationUs)

        // 0 duration
        val schedulerZero = TimelineFrameScheduler(0L, 30)
        assertEquals(0, schedulerZero.totalFrames)
    }

    @Test
    fun testFrameTimestamps() {
        val scheduler = TimelineFrameScheduler(1_000_000L, 30)
        // Frame 0 should be 0 us
        assertEquals(0L, scheduler.getFrameTimeUs(0))
        // Frame 15 (half second) should be 500_000 us
        assertEquals(500_000L, scheduler.getFrameTimeUs(15))
        // Frame 29 should be near end
        assertTrue(scheduler.getFrameTimeUs(29) <= 1_000_000L)
    }

    @Test
    fun testProgressAndEtaCalculation() {
        val scheduler = TimelineFrameScheduler(10_000_000L, 30) // 300 frames total
        assertEquals(300, scheduler.totalFrames)

        // Rendered 150 frames in 5000 ms -> ~33.3 ms per frame -> 150 frames left -> 5000 ms ETA
        val progress = scheduler.computeProgress(renderedFrameCount = 150, elapsedRealtimeMs = 5000L)
        assertEquals(150, progress.currentFrame)
        assertEquals(300, progress.totalFrames)
        assertEquals(0.5f, progress.progress, 0.001f)
        assertEquals(5000L, progress.elapsedMs)
        assertEquals(5000L, progress.estimatedRemainingMs)

        // Completion
        val doneProgress = scheduler.computeProgress(renderedFrameCount = 300, elapsedRealtimeMs = 10000L)
        assertEquals(1.0f, doneProgress.progress, 0.001f)
        assertEquals(0L, doneProgress.estimatedRemainingMs)
    }
}
