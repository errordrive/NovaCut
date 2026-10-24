package com.novacut.tests;

import com.novacut.engine.*;
import com.novacut.history.*;
import com.novacut.model.*;
import com.novacut.ui.EditorViewModel;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class VideoEditorTestSuite {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   NOVACUT INDEPENDENT VIDEO EDITOR TEST SUITE   ");
        System.out.println("=================================================\n");

        runTest("TC-01: Timebase & Microsecond Frame Mathematics", VideoEditorTestSuite::testTimebase);
        runTest("TC-02: Playhead Split Operation", VideoEditorTestSuite::testSplitClip);
        runTest("TC-03: Head & Tail Trimming with Magnetic Ripple", VideoEditorTestSuite::testTrimClip);
        runTest("TC-04: Bidirectional Undo & Redo History", VideoEditorTestSuite::testUndoRedo);
        runTest("TC-05: Project JSON Serialization & Atomic Autosave", VideoEditorTestSuite::testPersistence);
        runTest("TC-06: Audio Envelope & Fade Calculations", VideoEditorTestSuite::testAudioEnvelope);
        runTest("TC-07: Keyframe Evaluation & Curve Interpolation", VideoEditorTestSuite::testKeyframeInterpolation);
        runTest("TC-08: Magnetic Snapping to Clip Boundaries", VideoEditorTestSuite::testSnapping);
        runTest("TC-09: Multitrack Synchronization & Active Audio Sources", VideoEditorTestSuite::testMultitrackSync);
        runTest("TC-10: Export Frame Sequencer & Progress Pipeline", VideoEditorTestSuite::testExportPipeline);

        System.out.println("\n-------------------------------------------------");
        System.out.println(String.format("TEST RESULTS: %d PASSED, %d FAILED (TOTAL %d)", passed, failed, passed + failed));
        System.out.println("-------------------------------------------------");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void runTest(String testName, TestCase test) {
        try {
            test.run();
            System.out.println("[PASS] " + testName);
            passed++;
        } catch (Throwable t) {
            System.out.println("[FAIL] " + testName + " -> " + t.getMessage());
            t.printStackTrace(System.out);
            failed++;
        }
    }

    @FunctionalInterface
    interface TestCase {
        void run() throws Exception;
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError("Assertion failed: " + message);
    }

    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionError(String.format("%s (Expected: %d, Actual: %d)", message, expected, actual));
        }
    }

    private static void assertEquals(String expected, String actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(String.format("%s (Expected: %s, Actual: %s)", message, expected, actual));
        }
    }

    private static void assertEquals(double expected, double actual, double epsilon, String message) {
        if (Math.abs(expected - actual) > epsilon) {
            throw new AssertionError(String.format("%s (Expected: %f, Actual: %f)", message, expected, actual));
        }
    }

    // --- TEST IMPLEMENTATIONS ---

    private static void testTimebase() {
        assertEquals(1_000_000L, Timebase.secondsToUs(1.0), "1 second should equal 1,000,000 us");
        assertEquals(1.5, Timebase.usToSeconds(1_500_000L), 0.0001, "1,500,000 us should equal 1.5 s");
        assertEquals(33_333L, Timebase.framesToUs(1, 30), "1 frame at 30fps is ~33,333 us");
        assertEquals("01:30:15", Timebase.formatTimecode(90_500_000L, 30), "Formatted timecode at 90.5s @ 30fps");
    }

    private static void testSplitClip() {
        ProjectDocument project = new ProjectDocument("p1", "TestProject");
        Track mainTrack = new Track("t_main", TrackType.VIDEO, true, 0);
        project.getTracks().add(mainTrack);

        ClipSegment clip = new ClipSegment("c1", "m1", TrackType.VIDEO, 0L, 0L, 4_000_000L); // 4 seconds
        TimelineEngine.appendClip(project, "t_main", clip);

        assertEquals(4_000_000L, project.getProjectDurationUs(), "Initial project duration is 4s");

        // Split at 1.5s
        ClipSegment secondClip = TimelineEngine.splitClip(project, "t_main", "c1", 1_500_000L);
        assertTrue(secondClip != null, "Split should return new second clip");
        assertEquals(2, mainTrack.getClips().size(), "Track should have 2 clips after split");

        ClipSegment firstClip = mainTrack.getClips().get(0);
        assertEquals(0L, firstClip.getTimelineStartUs(), "First clip start at 0s");
        assertEquals(1_500_000L, firstClip.getTimelineDurationUs(), "First clip duration 1.5s");
        assertEquals(0L, firstClip.getSourceInUs(), "First clip source in");
        assertEquals(1_500_000L, firstClip.getSourceOutUs(), "First clip source out");

        assertEquals(1_500_000L, secondClip.getTimelineStartUs(), "Second clip start at 1.5s");
        assertEquals(2_500_000L, secondClip.getTimelineDurationUs(), "Second clip duration 2.5s");
        assertEquals(1_500_000L, secondClip.getSourceInUs(), "Second clip source in");
        assertEquals(4_000_000L, secondClip.getSourceOutUs(), "Second clip source out");

        assertEquals(4_000_000L, project.getProjectDurationUs(), "Total duration remains exactly 4s");
    }

    private static void testTrimClip() {
        ProjectDocument project = new ProjectDocument("p1", "TrimTest");
        Track mainTrack = new Track("t_main", TrackType.VIDEO, true, 0);
        project.getTracks().add(mainTrack);

        ClipSegment c1 = new ClipSegment("c1", "m1", TrackType.VIDEO, 0L, 0L, 3_000_000L);
        ClipSegment c2 = new ClipSegment("c2", "m2", TrackType.VIDEO, 3_000_000L, 0L, 2_000_000L);
        TimelineEngine.appendClip(project, "t_main", c1);
        TimelineEngine.appendClip(project, "t_main", c2);

        assertEquals(5_000_000L, project.getProjectDurationUs(), "Duration before trim: 5s");

        // Trim head of c1 by 500ms
        boolean trimmed = TimelineEngine.trimClip(project, "t_main", "c1", true, 500_000L);
        assertTrue(trimmed, "Trim head should succeed");
        assertEquals(500_000L, c1.getSourceInUs(), "c1 source in shifted by 500ms");
        assertEquals(2_500_000L, c1.getTimelineDurationUs(), "c1 duration reduced to 2.5s");

        // Verify c2 rippled automatically to prevent gap
        assertEquals(2_500_000L, c2.getTimelineStartUs(), "c2 must ripple to 2.5s");
        assertEquals(4_500_000L, project.getProjectDurationUs(), "Project duration updated to 4.5s");
    }

    private static void testUndoRedo() {
        ProjectDocument project = new ProjectDocument("p1", "UndoRedoTest");
        Track mainTrack = new Track("t_main", TrackType.VIDEO, true, 0);
        project.getTracks().add(mainTrack);

        ClipSegment clip = new ClipSegment("c1", "m1", TrackType.VIDEO, 0L, 0L, 4_000_000L);
        TimelineEngine.appendClip(project, "t_main", clip);

        EditorViewModel vm = new EditorViewModel(project);
        vm.selectClip("t_main", "c1");
        vm.seekTo(2_000_000L);

        // Perform split
        vm.splitSelectedClip();
        assertEquals(2, mainTrack.getClips().size(), "After split, 2 clips exist");

        // Undo
        assertTrue(vm.canUndo(), "Should be able to undo");
        vm.undo();
        assertEquals(1, mainTrack.getClips().size(), "After undo, back to 1 clip");
        assertEquals(4_000_000L, mainTrack.getClips().get(0).getSourceOutUs(), "Original out point restored");

        // Redo
        assertTrue(vm.canRedo(), "Should be able to redo");
        vm.redo();
        assertEquals(2, mainTrack.getClips().size(), "After redo, 2 clips restored");
    }

    private static void testPersistence() throws IOException {
        ProjectDocument project = new ProjectDocument("proj-save-test", "PersistenceDemo");
        Track mainTrack = new Track("track_0", TrackType.VIDEO, true, 0);
        project.getTracks().add(mainTrack);

        ClipSegment c1 = new ClipSegment("c1", "m1", TrackType.VIDEO, 0L, 0L, 3_000_000L);
        c1.getTransform().setScale(1.2f);
        c1.getColorAdjustments().setBrightness(0.15f);
        c1.getAudioEnvelope().setFadeInUs(500_000L);
        mainTrack.getClips().add(c1);

        File tempDraft = File.createTempFile("draft_content_", ".json");
        tempDraft.deleteOnExit();

        PersistenceEngine.saveProject(project, tempDraft);
        assertTrue(tempDraft.exists() && tempDraft.length() > 0, "Draft file must exist and have content");

        String json = PersistenceEngine.readProjectJson(tempDraft);
        assertTrue(json.contains("\"schema_version\": \"1.0.0\""), "Contains schema version");
        assertTrue(json.contains("\"proj-save-test\""), "Contains project ID");
        assertTrue(json.contains("\"scale\": 1.2"), "Contains serialized scale");
        assertTrue(json.contains("\"brightness\": 0.15"), "Contains serialized brightness");
        assertTrue(json.contains("\"fade_in_us\": 500000"), "Contains audio envelope");
    }

    private static void testAudioEnvelope() {
        AudioEnvelope env = new AudioEnvelope(1_000_000L, 1_000_000L); // 1s fade-in, 1s fade-out
        long clipDuration = 5_000_000L; // 5s total

        assertEquals(0.0, env.computeGain(0L, clipDuration), 0.001, "Gain at start is 0.0");
        assertEquals(0.5, env.computeGain(500_000L, clipDuration), 0.001, "Gain at 0.5s is 0.5");
        assertEquals(1.0, env.computeGain(1_000_000L, clipDuration), 0.001, "Gain at 1s is 1.0");
        assertEquals(1.0, env.computeGain(3_000_000L, clipDuration), 0.001, "Gain in middle is 1.0");
        assertEquals(0.5, env.computeGain(4_500_000L, clipDuration), 0.001, "Gain at 4.5s is 0.5");
        assertEquals(0.0, env.computeGain(5_000_000L, clipDuration), 0.001, "Gain at end is 0.0");
    }

    private static void testKeyframeInterpolation() {
        ClipSegment clip = new ClipSegment("c1", "m1", TrackType.VIDEO, 0L, 0L, 4_000_000L);
        clip.getKeyframes().add(new Keyframe(0L, Keyframe.Property.OPACITY, 0.0f, Keyframe.Interpolation.LINEAR));
        clip.getKeyframes().add(new Keyframe(2_000_000L, Keyframe.Property.OPACITY, 1.0f, Keyframe.Interpolation.LINEAR));

        // Before first keyframe
        assertEquals(0.0f, EffectsEngine.evaluateKeyframeProperty(clip, Keyframe.Property.OPACITY, -100L, 1.0f), 0.001, "Clamped before start");
        // Midway
        assertEquals(0.5f, EffectsEngine.evaluateKeyframeProperty(clip, Keyframe.Property.OPACITY, 1_000_000L, 1.0f), 0.001, "Linear at 50%");
        // At second keyframe
        assertEquals(1.0f, EffectsEngine.evaluateKeyframeProperty(clip, Keyframe.Property.OPACITY, 2_000_000L, 1.0f), 0.001, "At keyframe 2");
        // After second keyframe
        assertEquals(1.0f, EffectsEngine.evaluateKeyframeProperty(clip, Keyframe.Property.OPACITY, 3_000_000L, 1.0f), 0.001, "Clamped after end");
    }

    private static void testSnapping() {
        ProjectDocument project = new ProjectDocument("p1", "SnapTest");
        Track track = new Track("t1", TrackType.VIDEO, true, 0);
        project.getTracks().add(track);

        // Clip 1: [0, 2s]
        ClipSegment c1 = new ClipSegment("c1", "m1", TrackType.VIDEO, 0L, 0L, 2_000_000L);
        track.getClips().add(c1);

        // Playhead at 1.98s (20ms before 2.0s boundary)
        long targetUs = 1_980_000L;
        long snapped = SnappingEngine.calculateSnappedPosition(project, targetUs, SnappingEngine.DEFAULT_SNAP_THRESHOLD_US);
        assertEquals(2_000_000L, snapped, "Should snap to 2.0s boundary");

        // Playhead at 1.80s (200ms away, outside 50ms threshold)
        long unsnapped = SnappingEngine.calculateSnappedPosition(project, 1_800_000L, SnappingEngine.DEFAULT_SNAP_THRESHOLD_US);
        assertEquals(1_800_000L, unsnapped, "Should not snap when outside threshold");
    }

    private static void testMultitrackSync() {
        ProjectDocument project = new ProjectDocument("p1", "MultiSync");
        Track videoTrack = new Track("t_v", TrackType.VIDEO, true, 0);
        Track audioTrack = new Track("t_a", TrackType.AUDIO, false, 1);
        project.getTracks().add(videoTrack);
        project.getTracks().add(audioTrack);

        ClipSegment vClip = new ClipSegment("v1", "mv1", TrackType.VIDEO, 0L, 0L, 5_000_000L);
        videoTrack.getClips().add(vClip);

        ClipSegment aClip = new ClipSegment("a1", "ma1", TrackType.AUDIO, 1_000_000L, 0L, 3_000_000L); // starts at 1s, duration 3s (ends at 4s)
        audioTrack.getClips().add(aClip);

        // At 0.5s: video is active, audio is not yet active
        List<AudioEngine.ActiveAudioSource> at0_5 = AudioEngine.getActiveAudioSources(project, 500_000L);
        assertEquals(1, at0_5.size(), "At 0.5s, only video audio is active");

        // At 2.0s: both video and audio track are active
        List<AudioEngine.ActiveAudioSource> at2_0 = AudioEngine.getActiveAudioSources(project, 2_000_000L);
        assertEquals(2, at2_0.size(), "At 2.0s, both tracks are active");
    }

    private static void testExportPipeline() throws IOException {
        ProjectDocument project = new ProjectDocument("export_proj", "ExportTest");
        Track track = new Track("t1", TrackType.VIDEO, true, 0);
        project.getTracks().add(track);
        ClipSegment clip = new ClipSegment("c1", "m1", TrackType.VIDEO, 0L, 0L, 2_000_000L); // 2 seconds
        track.getClips().add(clip);

        File outFile = File.createTempFile("output_rendered_", ".mp4");
        outFile.deleteOnExit();

        ExportEngine.ExportConfig config = new ExportEngine.ExportConfig(
                1080, 1920, 30, 8_000_000, outFile.getAbsolutePath()
        );

        AtomicBoolean completed = new AtomicBoolean(false);
        ExportEngine.renderProject(project, config, new ExportEngine.ExportProgressListener() {
            @Override
            public void onProgress(float progressFraction, long currentFrame, long totalFrames) {
                assertTrue(progressFraction >= 0.0f && progressFraction <= 1.0f, "Progress fraction bounded");
            }

            @Override
            public void onCompleted(String outputFilePath) {
                completed.set(true);
            }

            @Override
            public void onError(Exception exception) {
                throw new RuntimeException("Export failed", exception);
            }
        });

        assertTrue(completed.get(), "Export callback onCompleted called");
        assertTrue(outFile.exists() && outFile.length() > 32, "Rendered output file exists with MP4 box headers");
    }
}
