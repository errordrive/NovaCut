package com.novacut.engine;

import com.novacut.model.ClipSegment;
import com.novacut.model.ProjectDocument;
import com.novacut.model.Track;
import com.novacut.model.TrackType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class TimelineEngine {

    /**
     * Re-packs clips on the main track to ensure zero gaps (magnetic ripple editing).
     */
    public static void ripplePackMainTrack(Track mainTrack) {
        if (mainTrack == null || !mainTrack.isMainTrack()) return;
        List<ClipSegment> clips = mainTrack.getClips();
        clips.sort(Comparator.comparingLong(ClipSegment::getTimelineStartUs));

        long currentUs = 0L;
        for (ClipSegment clip : clips) {
            clip.setTimelineStartUs(currentUs);
            currentUs += clip.getTimelineDurationUs();
        }
    }

    /**
     * Splits a clip at playhead position. Returns the newly created second clip segment, or null if playhead is out of bounds.
     */
    public static ClipSegment splitClip(ProjectDocument project, String trackId, String clipId, long playheadUs) {
        Track track = findTrack(project, trackId);
        if (track == null) return null;

        ClipSegment clip = findClip(track, clipId);
        if (clip == null) return null;

        if (playheadUs <= clip.getTimelineStartUs() || playheadUs >= clip.getTimelineEndUs()) {
            return null; // Cannot split outside clip boundaries
        }

        long splitDeltaTimelineUs = playheadUs - clip.getTimelineStartUs();
        long splitDeltaSourceUs = Math.round(splitDeltaTimelineUs * clip.getSpeed());

        long origSourceIn = clip.getSourceInUs();
        long origSourceOut = clip.getSourceOutUs();
        long splitSourcePoint = origSourceIn + splitDeltaSourceUs;

        // Clip 1 keeps the first half
        clip.setSourceOutUs(splitSourcePoint);

        // Clip 2 takes the second half
        String newId = "clip_" + UUID.randomUUID().toString().substring(0, 8);
        ClipSegment secondClip = new ClipSegment(
                newId,
                clip.getMaterialId(),
                clip.getType(),
                playheadUs,
                splitSourcePoint,
                origSourceOut
        );
        secondClip.setSpeed(clip.getSpeed());
        secondClip.setVolume(clip.getVolume());
        secondClip.setOpacity(clip.getOpacity());
        secondClip.setTransform(clip.getTransform().copy());
        secondClip.setColorAdjustments(clip.getColorAdjustments().copy());
        secondClip.setAudioEnvelope(clip.getAudioEnvelope().copy());
        secondClip.setTextContent(clip.getTextContent());

        int clipIndex = track.getClips().indexOf(clip);
        track.getClips().add(clipIndex + 1, secondClip);

        if (track.isMainTrack() && project.isMainTrackAdsorb()) {
            ripplePackMainTrack(track);
        }

        project.setUpdatedAt(System.currentTimeMillis());
        return secondClip;
    }

    /**
     * Trims a clip's head (start) or tail (end) by deltaUs.
     */
    public static boolean trimClip(ProjectDocument project, String trackId, String clipId, boolean isHead, long deltaUs) {
        Track track = findTrack(project, trackId);
        if (track == null) return false;

        ClipSegment clip = findClip(track, clipId);
        if (clip == null) return false;

        long deltaSourceUs = Math.round(deltaUs * clip.getSpeed());

        if (isHead) {
            long newSourceIn = clip.getSourceInUs() + deltaSourceUs;
            if (newSourceIn >= clip.getSourceOutUs()) {
                return false; // Cannot trim past end
            }
            clip.setSourceInUs(newSourceIn);
            if (!track.isMainTrack()) {
                clip.setTimelineStartUs(clip.getTimelineStartUs() + deltaUs);
            }
        } else {
            long newSourceOut = clip.getSourceOutUs() + deltaSourceUs;
            if (newSourceOut <= clip.getSourceInUs()) {
                return false; // Cannot trim past start
            }
            clip.setSourceOutUs(newSourceOut);
        }

        if (track.isMainTrack() && project.isMainTrackAdsorb()) {
            ripplePackMainTrack(track);
        }

        project.setUpdatedAt(System.currentTimeMillis());
        return true;
    }

    /**
     * Deletes a clip from a track, rippling subsequent clips if on the main track.
     */
    public static boolean deleteClip(ProjectDocument project, String trackId, String clipId) {
        Track track = findTrack(project, trackId);
        if (track == null) return false;

        ClipSegment clip = findClip(track, clipId);
        if (clip == null) return false;

        track.getClips().remove(clip);

        if (track.isMainTrack() && project.isMainTrackAdsorb()) {
            ripplePackMainTrack(track);
        }

        project.setUpdatedAt(System.currentTimeMillis());
        return true;
    }

    /**
     * Appends a new clip to the end of a track.
     */
    public static void appendClip(ProjectDocument project, String trackId, ClipSegment clip) {
        Track track = findTrack(project, trackId);
        if (track == null) return;

        if (track.isMainTrack() && project.isMainTrackAdsorb()) {
            long trackEnd = track.getTrackDurationUs();
            clip.setTimelineStartUs(trackEnd);
        }

        track.getClips().add(clip);
        project.setUpdatedAt(System.currentTimeMillis());
    }

    public static Track findTrack(ProjectDocument project, String trackId) {
        for (Track t : project.getTracks()) {
            if (t.getId().equals(trackId)) return t;
        }
        return null;
    }

    public static ClipSegment findClip(Track track, String clipId) {
        for (ClipSegment c : track.getClips()) {
            if (c.getId().equals(clipId)) return c;
        }
        return null;
    }
}
