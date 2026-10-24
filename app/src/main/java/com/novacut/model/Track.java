package com.novacut.model;

import java.util.ArrayList;
import java.util.List;

public class Track {
    private String id;
    private TrackType type;
    private boolean isMainTrack;
    private int order;
    private boolean muted;
    private boolean locked;
    private List<ClipSegment> clips;

    public Track(String id, TrackType type, boolean isMainTrack, int order) {
        this.id = id;
        this.type = type;
        this.isMainTrack = isMainTrack;
        this.order = order;
        this.muted = false;
        this.locked = false;
        this.clips = new ArrayList<>();
    }

    public Track copy() {
        Track copy = new Track(id, type, isMainTrack, order);
        copy.muted = this.muted;
        copy.locked = this.locked;
        for (ClipSegment clip : this.clips) {
            copy.clips.add(clip.copy());
        }
        return copy;
    }

    public long getTrackDurationUs() {
        long maxEnd = 0L;
        for (ClipSegment clip : clips) {
            maxEnd = Math.max(maxEnd, clip.getTimelineEndUs());
        }
        return maxEnd;
    }

    public String getId() { return id; }
    public TrackType getType() { return type; }
    public boolean isMainTrack() { return isMainTrack; }
    public void setMainTrack(boolean mainTrack) { isMainTrack = mainTrack; }
    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
    public boolean isMuted() { return muted; }
    public void setMuted(boolean muted) { this.muted = muted; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public List<ClipSegment> getClips() { return clips; }
    public void setClips(List<ClipSegment> clips) { this.clips = clips; }
}
