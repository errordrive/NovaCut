package com.novacut.history;

import com.novacut.engine.TimelineEngine;
import com.novacut.model.ClipSegment;
import com.novacut.model.ProjectDocument;
import com.novacut.model.Track;

public class DeleteClipCommand implements EditorCommand {
    private final String trackId;
    private final String clipId;

    private ClipSegment removedClip;
    private int originalIndex;

    public DeleteClipCommand(String trackId, String clipId) {
        this.trackId = trackId;
        this.clipId = clipId;
    }

    @Override
    public String getName() {
        return "Delete Clip";
    }

    @Override
    public void execute(ProjectDocument project) {
        Track track = TimelineEngine.findTrack(project, trackId);
        if (track == null) return;
        ClipSegment clip = TimelineEngine.findClip(track, clipId);
        if (clip == null) return;

        this.removedClip = clip;
        this.originalIndex = track.getClips().indexOf(clip);

        TimelineEngine.deleteClip(project, trackId, clipId);
    }

    @Override
    public void undo(ProjectDocument project) {
        Track track = TimelineEngine.findTrack(project, trackId);
        if (track == null || removedClip == null) return;

        if (originalIndex >= 0 && originalIndex <= track.getClips().size()) {
            track.getClips().add(originalIndex, removedClip);
        } else {
            track.getClips().add(removedClip);
        }

        if (track.isMainTrack() && project.isMainTrackAdsorb()) {
            TimelineEngine.ripplePackMainTrack(track);
        }
    }
}
