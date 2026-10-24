package com.novacut.history;

import com.novacut.engine.TimelineEngine;
import com.novacut.model.ClipSegment;
import com.novacut.model.ProjectDocument;
import com.novacut.model.Track;

public class TrimClipCommand implements EditorCommand {
    private final String trackId;
    private final String clipId;
    private final boolean isHead;
    private final long deltaUs;

    private long origSourceInUs;
    private long origSourceOutUs;
    private long origTimelineStartUs;

    public TrimClipCommand(String trackId, String clipId, boolean isHead, long deltaUs) {
        this.trackId = trackId;
        this.clipId = clipId;
        this.isHead = isHead;
        this.deltaUs = deltaUs;
    }

    @Override
    public String getName() {
        return isHead ? "Trim Head" : "Trim Tail";
    }

    @Override
    public void execute(ProjectDocument project) {
        Track track = TimelineEngine.findTrack(project, trackId);
        if (track == null) return;
        ClipSegment clip = TimelineEngine.findClip(track, clipId);
        if (clip == null) return;

        this.origSourceInUs = clip.getSourceInUs();
        this.origSourceOutUs = clip.getSourceOutUs();
        this.origTimelineStartUs = clip.getTimelineStartUs();

        TimelineEngine.trimClip(project, trackId, clipId, isHead, deltaUs);
    }

    @Override
    public void undo(ProjectDocument project) {
        Track track = TimelineEngine.findTrack(project, trackId);
        if (track == null) return;
        ClipSegment clip = TimelineEngine.findClip(track, clipId);
        if (clip == null) return;

        clip.setSourceInUs(origSourceInUs);
        clip.setSourceOutUs(origSourceOutUs);
        clip.setTimelineStartUs(origTimelineStartUs);

        if (track.isMainTrack() && project.isMainTrackAdsorb()) {
            TimelineEngine.ripplePackMainTrack(track);
        }
    }
}
