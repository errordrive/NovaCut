package com.novacut;

import static org.junit.Assert.*;

import com.novacut.engine.*;
import com.novacut.history.*;
import com.novacut.model.*;
import com.novacut.ui.EditorViewModel;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class NovaCutCoreUnitTest {

    @Test
    public void testTimebaseMathematics() {
        assertEquals(1_000_000L, Timebase.secondsToUs(1.0));
        assertEquals(1.5, Timebase.usToSeconds(1_500_000L), 0.0001);
        assertEquals(33_333L, Timebase.framesToUs(1, 30));
        assertEquals("01:30:15", Timebase.formatTimecode(90_500_000L, 30));
    }

    @Test
    public void testPlayheadSplitOperation() {
        ProjectDocument project = new ProjectDocument("p1", "TestProject");
        Track mainTrack = new Track("t_main", TrackType.VIDEO, true, 0);
        project.getTracks().add(mainTrack);

        ClipSegment clip = new ClipSegment("c1", "m1", TrackType.VIDEO, 0L, 0L, 4_000_000L);
        TimelineEngine.appendClip(project, "t_main", clip);

        ClipSegment secondClip = TimelineEngine.splitClip(project, "t_main", "c1", 1_500_000L);
        assertNotNull(secondClip);
        assertEquals(2, mainTrack.getClips().size());

        assertEquals(0L, mainTrack.getClips().get(0).getTimelineStartUs());
        assertEquals(1_500_000L, mainTrack.getClips().get(0).getTimelineDurationUs());
        assertEquals(1_500_000L, secondClip.getTimelineStartUs());
        assertEquals(2_500_000L, secondClip.getTimelineDurationUs());
        assertEquals(4_000_000L, project.getProjectDurationUs());
    }

    @Test
    public void testHeadTrimmingWithMagneticRipple() {
        ProjectDocument project = new ProjectDocument("p1", "TrimTest");
        Track mainTrack = new Track("t_main", TrackType.VIDEO, true, 0);
        project.getTracks().add(mainTrack);

        ClipSegment c1 = new ClipSegment("c1", "m1", TrackType.VIDEO, 0L, 0L, 3_000_000L);
        ClipSegment c2 = new ClipSegment("c2", "m2", TrackType.VIDEO, 3_000_000L, 0L, 2_000_000L);
        TimelineEngine.appendClip(project, "t_main", c1);
        TimelineEngine.appendClip(project, "t_main", c2);

        boolean trimmed = TimelineEngine.trimClip(project, "t_main", "c1", true, 500_000L);
        assertTrue(trimmed);
        assertEquals(500_000L, c1.getSourceInUs());
        assertEquals(2_500_000L, c1.getTimelineDurationUs());
        assertEquals(2_500_000L, c2.getTimelineStartUs());
        assertEquals(4_500_000L, project.getProjectDurationUs());
    }

    @Test
    public void testBidirectionalUndoRedo() {
        ProjectDocument project = new ProjectDocument("p1", "UndoRedoTest");
        Track mainTrack = new Track("t_main", TrackType.VIDEO, true, 0);
        project.getTracks().add(mainTrack);

        ClipSegment clip = new ClipSegment("c1", "m1", TrackType.VIDEO, 0L, 0L, 4_000_000L);
        TimelineEngine.appendClip(project, "t_main", clip);

        EditorViewModel vm = new EditorViewModel(project);
        vm.selectClip("t_main", "c1");
        vm.seekTo(2_000_000L);

        vm.splitSelectedClip();
        assertEquals(2, mainTrack.getClips().size());

        assertTrue(vm.canUndo());
        vm.undo();
        assertEquals(1, mainTrack.getClips().size());
        assertEquals(4_000_000L, mainTrack.getClips().get(0).getSourceOutUs());

        assertTrue(vm.canRedo());
        vm.redo();
        assertEquals(2, mainTrack.getClips().size());
    }

    @Test
    public void testProjectJsonAutosave() throws IOException {
        ProjectDocument project = new ProjectDocument("proj-save-test", "PersistenceDemo");
        Track mainTrack = new Track("track_0", TrackType.VIDEO, true, 0);
        project.getTracks().add(mainTrack);

        ClipSegment c1 = new ClipSegment("c1", "m1", TrackType.VIDEO, 0L, 0L, 3_000_000L);
        c1.getTransform().setScale(1.2f);
        mainTrack.getClips().add(c1);

        File tempDraft = File.createTempFile("draft_content_", ".json");
        tempDraft.deleteOnExit();

        PersistenceEngine.saveProject(project, tempDraft);
        assertTrue(tempDraft.exists() && tempDraft.length() > 0);

        String json = PersistenceEngine.readProjectJson(tempDraft);
        assertTrue(json.contains("\"schema_version\": \"1.0.0\""));
        assertTrue(json.contains("\"proj-save-test\""));
    }

    @Test
    public void testAudioEnvelopeFades() {
        AudioEnvelope env = new AudioEnvelope(1_000_000L, 1_000_000L);
        long clipDuration = 5_000_000L;

        assertEquals(0.0f, env.computeGain(0L, clipDuration), 0.001f);
        assertEquals(0.5f, env.computeGain(500_000L, clipDuration), 0.001f);
        assertEquals(1.0f, env.computeGain(1_000_000L, clipDuration), 0.001f);
        assertEquals(1.0f, env.computeGain(3_000_000L, clipDuration), 0.001f);
        assertEquals(0.5f, env.computeGain(4_500_000L, clipDuration), 0.001f);
        assertEquals(0.0f, env.computeGain(5_000_000L, clipDuration), 0.001f);
    }

    @Test
    public void testKeyframeInterpolation() {
        ClipSegment clip = new ClipSegment("c1", "m1", TrackType.VIDEO, 0L, 0L, 4_000_000L);
        clip.getKeyframes().add(new Keyframe(0L, Keyframe.Property.OPACITY, 0.0f, Keyframe.Interpolation.LINEAR));
        clip.getKeyframes().add(new Keyframe(2_000_000L, Keyframe.Property.OPACITY, 1.0f, Keyframe.Interpolation.LINEAR));

        assertEquals(0.0f, EffectsEngine.evaluateKeyframeProperty(clip, Keyframe.Property.OPACITY, -100L, 1.0f), 0.001f);
        assertEquals(0.5f, EffectsEngine.evaluateKeyframeProperty(clip, Keyframe.Property.OPACITY, 1_000_000L, 1.0f), 0.001f);
        assertEquals(1.0f, EffectsEngine.evaluateKeyframeProperty(clip, Keyframe.Property.OPACITY, 2_000_000L, 1.0f), 0.001f);
    }

    @Test
    public void testMagneticSnapping() {
        ProjectDocument project = new ProjectDocument("p1", "SnapTest");
        Track track = new Track("t1", TrackType.VIDEO, true, 0);
        project.getTracks().add(track);

        ClipSegment c1 = new ClipSegment("c1", "m1", TrackType.VIDEO, 0L, 0L, 2_000_000L);
        track.getClips().add(c1);

        long snapped = SnappingEngine.calculateSnappedPosition(project, 1_980_000L, SnappingEngine.DEFAULT_SNAP_THRESHOLD_US);
        assertEquals(2_000_000L, snapped);

        long unsnapped = SnappingEngine.calculateSnappedPosition(project, 1_800_000L, SnappingEngine.DEFAULT_SNAP_THRESHOLD_US);
        assertEquals(1_800_000L, unsnapped);
    }

    @Test
    public void testExportPipelineSequencer() throws IOException {
        ProjectDocument project = new ProjectDocument("export_proj", "ExportTest");
        Track track = new Track("t1", TrackType.VIDEO, true, 0);
        project.getTracks().add(track);
        ClipSegment clip = new ClipSegment("c1", "m1", TrackType.VIDEO, 0L, 0L, 2_000_000L);
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
                assertTrue(progressFraction >= 0.0f && progressFraction <= 1.0f);
            }

            @Override
            public void onCompleted(String outputFilePath) {
                completed.set(true);
            }

            @Override
            public void onError(Exception exception) {
                fail("Export failed: " + exception.getMessage());
            }
        });

        assertTrue(completed.get());
        assertTrue(outFile.exists() && outFile.length() > 32);
    }
}
