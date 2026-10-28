package com.procut.engine

import com.procut.history.UndoRedoStack
import com.procut.model.*
import com.procut.repository.FileDraftRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.UUID

class TimelineEngineTest {

    private lateinit var engine: TimelineEngine
    private lateinit var sampleAsset1: MediaAsset
    private lateinit var sampleAsset2: MediaAsset
    private lateinit var sampleAsset3: MediaAsset
    private lateinit var initialProject: EditorProject

    @Before
    fun setUp() {
        engine = TimelineEngine()

        sampleAsset1 = MediaAsset(
            id = "asset-1",
            filePath = "/sdcard/Movies/clip1.mp4",
            durationUs = 10_000_000L, // 10 seconds
            width = 1920,
            height = 1080
        )

        sampleAsset2 = MediaAsset(
            id = "asset-2",
            filePath = "/sdcard/Movies/clip2.mp4",
            durationUs = 8_000_000L, // 8 seconds
            width = 1920,
            height = 1080
        )

        sampleAsset3 = MediaAsset(
            id = "asset-3",
            filePath = "/sdcard/Movies/clip3.mp4",
            durationUs = 5_000_000L, // 5 seconds
            width = 1920,
            height = 1080
        )

        val mainTrack = Track(
            id = "track-main",
            type = TrackType.MAIN_VIDEO,
            name = "Main Video"
        )

        val pipTrack = Track(
            id = "track-pip",
            type = TrackType.PIP_OVERLAY,
            name = "Overlay 1"
        )

        initialProject = EditorProject(
            id = "proj-test",
            name = "Unit Test Project",
            tracks = listOf(mainTrack, pipTrack),
            assets = listOf(sampleAsset1, sampleAsset2, sampleAsset3)
        )
    }

    @Test
    fun testMagneticMainTrackContinuousPlacement() {
        // Add clip 1 (10s)
        var p = engine.addClip(initialProject, "track-main", sampleAsset1)
        var mainTrack = p.findTrackById("track-main")!!
        assertEquals(1, mainTrack.clips.size)
        assertEquals(0L, mainTrack.clips[0].targetTimeRangeUs.startUs)
        assertEquals(10_000_000L, mainTrack.clips[0].targetTimeRangeUs.durationUs)
        assertEquals(10_000_000L, p.totalDurationUs)

        // Add clip 2 (8s)
        p = engine.addClip(p, "track-main", sampleAsset2)
        mainTrack = p.findTrackById("track-main")!!
        assertEquals(2, mainTrack.clips.size)
        assertEquals(0L, mainTrack.clips[0].targetTimeRangeUs.startUs)
        assertEquals(10_000_000L, mainTrack.clips[1].targetTimeRangeUs.startUs)
        assertEquals(8_000_000L, mainTrack.clips[1].targetTimeRangeUs.durationUs)
        assertEquals(18_000_000L, p.totalDurationUs)

        // Add clip 3 (5s)
        p = engine.addClip(p, "track-main", sampleAsset3)
        mainTrack = p.findTrackById("track-main")!!
        assertEquals(3, mainTrack.clips.size)
        assertEquals(18_000_000L, mainTrack.clips[2].targetTimeRangeUs.startUs)
        assertEquals(23_000_000L, p.totalDurationUs)
    }

    @Test
    fun testMagneticDeleteAndRippleShift() {
        // Setup 3 clips: [0..10s], [10..18s], [18..23s]
        var p = engine.addClip(initialProject, "track-main", sampleAsset1)
        p = engine.addClip(p, "track-main", sampleAsset2)
        p = engine.addClip(p, "track-main", sampleAsset3)

        val clip2Id = p.findTrackById("track-main")!!.clips[1].id

        // Delete middle clip (clip 2)
        p = engine.deleteClip(p, "track-main", clip2Id)
        val mainTrack = p.findTrackById("track-main")!!

        assertEquals(2, mainTrack.clips.size)
        // First clip untouched
        assertEquals(0L, mainTrack.clips[0].targetTimeRangeUs.startUs)
        assertEquals(10_000_000L, mainTrack.clips[0].targetTimeRangeUs.durationUs)
        // Downstream clip (originally at 18s) must ripple-shift left to 10s
        assertEquals(10_000_000L, mainTrack.clips[1].targetTimeRangeUs.startUs)
        assertEquals(5_000_000L, mainTrack.clips[1].targetTimeRangeUs.durationUs)
        assertEquals(15_000_000L, p.totalDurationUs)
    }

    @Test
    fun testSplitClipAtTimestamp() {
        // Add 10s clip
        var p = engine.addClip(initialProject, "track-main", sampleAsset1)
        val clipId = p.findTrackById("track-main")!!.clips[0].id

        // Attach keyframes to verify partitioning
        val kf1 = Keyframe(id = "kf-1", timeOffsetUs = 2_000_000L, property = KeyframeProperty.OPACITY, value = 0.5f)
        val kf2 = Keyframe(id = "kf-2", timeOffsetUs = 7_000_000L, property = KeyframeProperty.OPACITY, value = 0.9f)
        val trackWithKf = p.findTrackById("track-main")!!.let { track ->
            val updatedClip = track.clips[0].copy(keyframes = listOf(kf1, kf2))
            track.copy(clips = listOf(updatedClip))
        }
        p = p.copy(tracks = listOf(trackWithKf, p.findTrackById("track-pip")!!))

        // Split at 4.0s (4_000_000 us)
        p = engine.splitClip(p, "track-main", clipId, 4_000_000L)
        val mainTrack = p.findTrackById("track-main")!!

        assertEquals(2, mainTrack.clips.size)
        val leftClip = mainTrack.clips[0]
        val rightClip = mainTrack.clips[1]

        // Check target ranges
        assertEquals(0L, leftClip.targetTimeRangeUs.startUs)
        assertEquals(4_000_000L, leftClip.targetTimeRangeUs.durationUs)
        assertEquals(4_000_000L, rightClip.targetTimeRangeUs.startUs)
        assertEquals(6_000_000L, rightClip.targetTimeRangeUs.durationUs)

        // Check source ranges
        assertEquals(0L, leftClip.sourceTimeRangeUs.startUs)
        assertEquals(4_000_000L, leftClip.sourceTimeRangeUs.durationUs)
        assertEquals(4_000_000L, rightClip.sourceTimeRangeUs.startUs)
        assertEquals(6_000_000L, rightClip.sourceTimeRangeUs.durationUs)

        // Check keyframe partitioning
        assertEquals(1, leftClip.keyframes.size)
        assertEquals(2_000_000L, leftClip.keyframes[0].timeOffsetUs)

        assertEquals(1, rightClip.keyframes.size)
        // 7s in original - 4s split = 3s offset in right clip
        assertEquals(3_000_000L, rightClip.keyframes[0].timeOffsetUs)
    }

    @Test
    fun testTrimClipStartWithMagneticRipple() {
        // Setup 2 clips: [0..10s], [10..18s]
        var p = engine.addClip(initialProject, "track-main", sampleAsset1)
        p = engine.addClip(p, "track-main", sampleAsset2)

        val clip1Id = p.findTrackById("track-main")!!.clips[0].id

        // Trim 2 seconds off the start of clip 1
        p = engine.trimClipStart(p, "track-main", clip1Id, 2_000_000L)
        val mainTrack = p.findTrackById("track-main")!!

        val trimmedClip = mainTrack.clips[0]
        val downstreamClip = mainTrack.clips[1]

        // Clip 1 is trimmed to 8s duration, starting at 0 on magnetic track
        assertEquals(0L, trimmedClip.targetTimeRangeUs.startUs)
        assertEquals(8_000_000L, trimmedClip.targetTimeRangeUs.durationUs)
        assertEquals(2_000_000L, trimmedClip.sourceTimeRangeUs.startUs)
        assertEquals(8_000_000L, trimmedClip.sourceTimeRangeUs.durationUs)

        // Downstream clip must ripple left from 10s to 8s
        assertEquals(8_000_000L, downstreamClip.targetTimeRangeUs.startUs)
        assertEquals(8_000_000L, downstreamClip.targetTimeRangeUs.durationUs)
        assertEquals(16_000_000L, p.totalDurationUs)
    }

    @Test
    fun testTrimClipEndWithMagneticRipple() {
        // Setup 2 clips: [0..10s], [10..18s]
        var p = engine.addClip(initialProject, "track-main", sampleAsset1)
        p = engine.addClip(p, "track-main", sampleAsset2)

        val clip1Id = p.findTrackById("track-main")!!.clips[0].id

        // Trim end of clip 1 to 6s
        p = engine.trimClipEnd(p, "track-main", clip1Id, 6_000_000L)
        val mainTrack = p.findTrackById("track-main")!!

        val trimmedClip = mainTrack.clips[0]
        val downstreamClip = mainTrack.clips[1]

        assertEquals(0L, trimmedClip.targetTimeRangeUs.startUs)
        assertEquals(6_000_000L, trimmedClip.targetTimeRangeUs.durationUs)

        // Downstream clip ripples left to 6s
        assertEquals(6_000_000L, downstreamClip.targetTimeRangeUs.startUs)
        assertEquals(14_000_000L, p.totalDurationUs)
    }

    @Test
    fun testSlipClipContent() {
        var p = engine.addClip(initialProject, "track-main", sampleAsset1)
        val clipId = p.findTrackById("track-main")!!.clips[0].id

        // Trim end first so there is headroom to slip
        p = engine.trimClipEnd(p, "track-main", clipId, 5_000_000L)

        // Slip source by +2.0 seconds
        p = engine.slipClipContent(p, "track-main", clipId, 2_000_000L)
        val slippedClip = p.findTrackById("track-main")!!.clips[0]

        // Target bounds remain identical [0..5s]
        assertEquals(0L, slippedClip.targetTimeRangeUs.startUs)
        assertEquals(5_000_000L, slippedClip.targetTimeRangeUs.durationUs)

        // Source start has slipped to 2s
        assertEquals(2_000_000L, slippedClip.sourceTimeRangeUs.startUs)
        assertEquals(5_000_000L, slippedClip.sourceTimeRangeUs.durationUs)
    }

    @Test
    fun testFreePlacementOnOverlayTrack() {
        // Add overlay clip at 3.0s
        var p = engine.addClip(initialProject, "track-pip", sampleAsset3, startUs = 3_000_000L)
        var pipTrack = p.findTrackById("track-pip")!!

        assertEquals(1, pipTrack.clips.size)
        assertEquals(3_000_000L, pipTrack.clips[0].targetTimeRangeUs.startUs)
        assertEquals(5_000_000L, pipTrack.clips[0].targetTimeRangeUs.durationUs)

        // Add second overlay clip at 12.0s with gap
        p = engine.addClip(p, "track-pip", sampleAsset3, startUs = 12_000_000L)
        pipTrack = p.findTrackById("track-pip")!!

        assertEquals(2, pipTrack.clips.size)
        assertEquals(3_000_000L, pipTrack.clips[0].targetTimeRangeUs.startUs)
        assertEquals(12_000_000L, pipTrack.clips[1].targetTimeRangeUs.startUs)

        // Delete first clip, verify second clip does NOT ripple
        val firstClipId = pipTrack.clips[0].id
        p = engine.deleteClip(p, "track-pip", firstClipId)
        pipTrack = p.findTrackById("track-pip")!!

        assertEquals(1, pipTrack.clips.size)
        // Position remains locked at 12s
        assertEquals(12_000_000L, pipTrack.clips[0].targetTimeRangeUs.startUs)
    }

    @Test
    fun testFreezeFrameAtPlayhead() {
        var p = engine.addClip(initialProject, "track-main", sampleAsset1) // 10s
        val clipId = p.findTrackById("track-main")!!.clips[0].id

        // Freeze at 4.0s for 3.0s duration
        p = engine.freezeFrameAtPlayhead(p, "track-main", clipId, 4_000_000L, 3_000_000L)
        val mainTrack = p.findTrackById("track-main")!!

        assertEquals(3, mainTrack.clips.size)
        val leftClip = mainTrack.clips[0]
        val freezeClip = mainTrack.clips[1]
        val rightClip = mainTrack.clips[2]

        assertEquals(0L, leftClip.targetTimeRangeUs.startUs)
        assertEquals(4_000_000L, leftClip.targetTimeRangeUs.durationUs)

        assertEquals(4_000_000L, freezeClip.targetTimeRangeUs.startUs)
        assertEquals(3_000_000L, freezeClip.targetTimeRangeUs.durationUs)

        assertEquals(7_000_000L, rightClip.targetTimeRangeUs.startUs)
        assertEquals(6_000_000L, rightClip.targetTimeRangeUs.durationUs)

        assertEquals(13_000_000L, p.totalDurationUs)
    }

    @Test
    fun testExtractAudioToTrack() {
        var p = engine.addClip(initialProject, "track-main", sampleAsset1)
        val clipId = p.findTrackById("track-main")!!.clips[0].id

        p = engine.extractAudioToTrack(p, clipId)
        val mainClip = p.findTrackById("track-main")!!.clips[0]
        val audioTrack = p.audioTracks.first()

        // Source video clip is muted
        assertTrue(mainClip.isMuted)

        // Audio clip created with matching time range
        assertEquals(1, audioTrack.clips.size)
        val audioClip = audioTrack.clips[0]
        assertEquals(mainClip.targetTimeRangeUs, audioClip.targetTimeRangeUs)
        assertEquals(mainClip.sourceTimeRangeUs, audioClip.sourceTimeRangeUs)
    }

    @Test
    fun testConstantSpeedMultiplier() {
        var p = engine.addClip(initialProject, "track-main", sampleAsset1) // 10s
        val clipId = p.findTrackById("track-main")!!.clips[0].id

        // 2x Speed
        p = engine.applyConstantSpeed(p, "track-main", clipId, 2.0)
        var clip = p.findTrackById("track-main")!!.clips[0]
        assertEquals(5_000_000L, clip.targetTimeRangeUs.durationUs)

        // 0.5x Speed
        p = engine.applyConstantSpeed(p, "track-main", clipId, 0.5)
        clip = p.findTrackById("track-main")!!.clips[0]
        assertEquals(20_000_000L, clip.targetTimeRangeUs.durationUs)
    }

    @Test
    fun testCurveSpeedRampingPacing() {
        val speedConfig = SpeedConfig.MONTAGE_HERO
        val sourceDurationUs = 10_000_000L // 10s
        val targetDurationUs = speedConfig.computeTargetDurationUs(sourceDurationUs)

        assertTrue(targetDurationUs > 0)

        // Test monotonic time mapping
        val t0 = speedConfig.mapTargetTimeToSourceTime(0L, targetDurationUs, sourceDurationUs)
        val tMid = speedConfig.mapTargetTimeToSourceTime(targetDurationUs / 2, targetDurationUs, sourceDurationUs)
        val tEnd = speedConfig.mapTargetTimeToSourceTime(targetDurationUs, targetDurationUs, sourceDurationUs)

        assertEquals(0L, t0)
        assertTrue(tMid in 1 until sourceDurationUs)
        assertEquals(sourceDurationUs, tEnd)
    }

    @Test
    fun testMagneticSnappingWithinThreshold() {
        var p = engine.addClip(initialProject, "track-main", sampleAsset1) // 10s clip [0..10s]
        p = p.copy(playheadPositionUs = 5_000_000L) // Playhead at 5s

        // 4.95s is within 100ms (100,000us) of playhead at 5.0s -> should snap to 5.0s
        val snap1 = engine.computeMagneticSnap(p, candidateTimestampUs = 4_950_000L)
        assertEquals(5_000_000L, snap1)

        // 10.05s is within 100ms of clip end at 10.0s -> should snap to 10.0s
        val snap2 = engine.computeMagneticSnap(p, candidateTimestampUs = 10_050_000L)
        assertEquals(10_000_000L, snap2)

        // 7.5s is far away from all targets -> no snap, returns original timestamp
        val snap3 = engine.computeMagneticSnap(p, candidateTimestampUs = 7_500_000L)
        assertEquals(7_500_000L, snap3)
    }

    @Test
    fun testUndoRedoStack() {
        val stack = UndoRedoStack(maxCapacity = 10)
        assertFalse(stack.canUndo)
        assertFalse(stack.canRedo)

        var p = initialProject
        stack.push(p, "Initial State")

        // Action 1: Add Clip
        p = engine.addClip(p, "track-main", sampleAsset1)
        stack.push(p, "Add Clip 1")

        // Action 2: Split Clip
        val clipId = p.findTrackById("track-main")!!.clips[0].id
        p = engine.splitClip(p, "track-main", clipId, 4_000_000L)

        assertTrue(stack.canUndo)
        assertFalse(stack.canRedo)

        // Undo Split
        val (undoneState1, label1) = stack.undo(p)!!
        assertEquals("Add Clip 1", label1)
        assertEquals(1, undoneState1.findTrackById("track-main")!!.clips.size)
        assertTrue(stack.canRedo)

        // Redo Split
        val (redoneState1, redoLabel1) = stack.redo(undoneState1)!!
        assertEquals("Add Clip 1", redoLabel1)
        assertEquals(2, redoneState1.findTrackById("track-main")!!.clips.size)
    }

    @Test
    fun testDraftPersistenceAndDuplication() {
        val tempDir = Files.createTempDirectory("procut-test-drafts").toFile()
        try {
            val repository = FileDraftRepository(tempDir)

            var p = engine.addClip(initialProject, "track-main", sampleAsset1)
            p = engine.addClip(p, "track-pip", sampleAsset3, startUs = 2_000_000L)

            // Save draft
            repository.saveDraft(p)

            // Load draft and verify fidelity
            val loaded = repository.loadDraft(p.id)
            assertNotNull(loaded)
            assertEquals(p.id, loaded!!.id)
            assertEquals(p.name, loaded.name)
            assertEquals(p.tracks.size, loaded.tracks.size)
            assertEquals(p.totalDurationUs, loaded.totalDurationUs)

            // Duplicate draft
            val duplicated = repository.duplicateDraft(p.id, "Duplicated Copy")
            assertNotNull(duplicated)
            assertNotEquals(p.id, duplicated!!.id)
            assertEquals("Duplicated Copy", duplicated.name)

            // List drafts
            val drafts = repository.listDrafts()
            assertEquals(2, drafts.size)

            // Delete original
            val deleted = repository.deleteDraft(p.id)
            assertTrue(deleted)
            assertEquals(1, repository.listDrafts().size)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
