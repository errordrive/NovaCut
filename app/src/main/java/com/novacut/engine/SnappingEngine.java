package com.novacut.engine;

import com.novacut.model.ClipSegment;
import com.novacut.model.ProjectDocument;
import com.novacut.model.Track;

import java.util.TreeSet;

public class SnappingEngine {
    public static final long DEFAULT_SNAP_THRESHOLD_US = 50_000L; // 50 milliseconds

    /**
     * Finds the nearest snap target (clip boundary or project origin) within threshold.
     * Returns the snapped timestamp, or original playheadUs if no snap target is nearby.
     */
    public static long calculateSnappedPosition(ProjectDocument project, long playheadUs, long thresholdUs) {
        if (project == null || !project.isSnapEnabled()) {
            return playheadUs;
        }

        TreeSet<Long> snapPoints = new TreeSet<>();
        snapPoints.add(0L); // Project start

        for (Track track : project.getTracks()) {
            for (ClipSegment clip : track.getClips()) {
                snapPoints.add(clip.getTimelineStartUs());
                snapPoints.add(clip.getTimelineEndUs());
            }
        }

        Long floor = snapPoints.floor(playheadUs);
        Long ceiling = snapPoints.ceiling(playheadUs);

        long closestPoint = playheadUs;
        long minDelta = thresholdUs + 1;

        if (floor != null && Math.abs(playheadUs - floor) <= thresholdUs) {
            minDelta = Math.abs(playheadUs - floor);
            closestPoint = floor;
        }

        if (ceiling != null && Math.abs(playheadUs - ceiling) < minDelta) {
            closestPoint = ceiling;
        }

        return closestPoint;
    }
}
