package com.novacut.history;

import com.novacut.engine.TimelineEngine;
import com.novacut.model.ClipSegment;
import com.novacut.model.ProjectDocument;
import com.novacut.model.Track;

public class SplitClipCommand implements EditorCommand {
    private final String trackId;
    private final String clipId;
    private final long playheadUs;

    private long origSourceOutUs;
    private ClipSegment generatedSecondClip;

    public SplitClipCommand(String trackId, String clipId, long playheadUs) {
        this.trackId = trackId;
        this.clipId = clipId;
        this.playheadUs = playheadUs;
    }

    @Override
    public String getName() {
        return "Split Clip";
    }

    @Override
    public void execute(ProjectDocument project) {
        Track track = TimelineEngine.findTrack(project, trackId);
        if (track == null) return;
        ClipSegment clip = TimelineEngine.findClip(track, clipId);
        if (clip == null) return;

        this.origSourceOutUs = clip.getSourceOutUs();
        this.generatedSecondClip = TimelineEngine.splitClip(project, trackId, clipId, playheadUs);
    }

    @Override
    public void undo(ProjectDocument project) {
        Track track = TimelineEngine.findTrack(project, trackId);
        if (track == null) return;
        ClipSegment clip = TimelineEngine.findClip(track, clipId);
        if (clip == null) return;

        // Restore original source out point
        clip.setSourceOutUs(origSourceOutUs);

        // Remove generated second clip
        if (generatedSecondClip != null) {
            track.getClips().remove(generatedSecondClip);
        }

        if (track.isMainTrack() && project.isMainTrackAdsorb()) {
            TimelineEngine.ripplePackMainTrack(track);
        }
    }
}
