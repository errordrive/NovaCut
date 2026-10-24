package com.novacut.model;

import java.util.ArrayList;
import java.util.List;

public class ClipSegment {
    private String id;
    private String materialId;
    private TrackType type;

    // Timeline positioning (all in microseconds)
    private long timelineStartUs;
    private long timelineDurationUs;
    private long sourceInUs;
    private long sourceOutUs;

    // Playback modifiers
    private float speed;
    private float volume;
    private float opacity;

    // Transforms, colors, envelopes, keyframes
    private Transform transform;
    private ColorAdjustments colorAdjustments;
    private AudioEnvelope audioEnvelope;
    private List<Keyframe> keyframes;

    // Text / subtitle overlay data (optional)
    private String textContent;
    private int fontSizeSp;
    private String textColor;

    public ClipSegment(String id, String materialId, TrackType type, long timelineStartUs, long sourceInUs, long sourceOutUs) {
        this.id = id;
        this.materialId = materialId;
        this.type = type;
        this.timelineStartUs = timelineStartUs;
        this.sourceInUs = sourceInUs;
        this.sourceOutUs = sourceOutUs;
        this.speed = 1.0f;
        this.volume = 1.0f;
        this.opacity = 1.0f;
        this.timelineDurationUs = Math.max(0L, sourceOutUs - sourceInUs);
        this.transform = new Transform();
        this.colorAdjustments = new ColorAdjustments();
        this.audioEnvelope = new AudioEnvelope();
        this.keyframes = new ArrayList<>();
    }

    public ClipSegment copy() {
        ClipSegment copy = new ClipSegment(id, materialId, type, timelineStartUs, sourceInUs, sourceOutUs);
        copy.timelineDurationUs = this.timelineDurationUs;
        copy.speed = this.speed;
        copy.volume = this.volume;
        copy.opacity = this.opacity;
        copy.transform = this.transform.copy();
        copy.colorAdjustments = this.colorAdjustments.copy();
        copy.audioEnvelope = this.audioEnvelope.copy();
        for (Keyframe kf : this.keyframes) {
            copy.keyframes.add(kf.copy());
        }
        copy.textContent = this.textContent;
        copy.fontSizeSp = this.fontSizeSp;
        copy.textColor = this.textColor;
        return copy;
    }

    public void recalculateDuration() {
        long sourceDuration = Math.max(0L, sourceOutUs - sourceInUs);
        if (speed > 0) {
            this.timelineDurationUs = Math.round(sourceDuration / speed);
        } else {
            this.timelineDurationUs = sourceDuration;
        }
    }

    public long getTimelineEndUs() {
        return timelineStartUs + timelineDurationUs;
    }

    public boolean containsTimestamp(long playheadUs) {
        return playheadUs >= timelineStartUs && playheadUs < getTimelineEndUs();
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getMaterialId() { return materialId; }
    public TrackType getType() { return type; }

    public long getTimelineStartUs() { return timelineStartUs; }
    public void setTimelineStartUs(long timelineStartUs) { this.timelineStartUs = timelineStartUs; }

    public long getTimelineDurationUs() { return timelineDurationUs; }
    public void setTimelineDurationUs(long timelineDurationUs) { this.timelineDurationUs = timelineDurationUs; }

    public long getSourceInUs() { return sourceInUs; }
    public void setSourceInUs(long sourceInUs) { 
        this.sourceInUs = sourceInUs; 
        recalculateDuration(); 
    }

    public long getSourceOutUs() { return sourceOutUs; }
    public void setSourceOutUs(long sourceOutUs) { 
        this.sourceOutUs = sourceOutUs; 
        recalculateDuration(); 
    }

    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { 
        this.speed = speed; 
        recalculateDuration(); 
    }

    public float getVolume() { return volume; }
    public void setVolume(float volume) { this.volume = volume; }

    public float getOpacity() { return opacity; }
    public void setOpacity(float opacity) { this.opacity = opacity; }

    public Transform getTransform() { return transform; }
    public void setTransform(Transform transform) { this.transform = transform; }

    public ColorAdjustments getColorAdjustments() { return colorAdjustments; }
    public void setColorAdjustments(ColorAdjustments colorAdjustments) { this.colorAdjustments = colorAdjustments; }

    public AudioEnvelope getAudioEnvelope() { return audioEnvelope; }
    public void setAudioEnvelope(AudioEnvelope audioEnvelope) { this.audioEnvelope = audioEnvelope; }

    public List<Keyframe> getKeyframes() { return keyframes; }
    public void setKeyframes(List<Keyframe> keyframes) { this.keyframes = keyframes; }

    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }

    public int getFontSizeSp() { return fontSizeSp; }
    public void setFontSizeSp(int fontSizeSp) { this.fontSizeSp = fontSizeSp; }

    public String getTextColor() { return textColor; }
    public void setTextColor(String textColor) { this.textColor = textColor; }
}
