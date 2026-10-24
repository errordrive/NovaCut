package com.novacut.model;

public class Keyframe {
    public enum Property {
        OPACITY,
        SCALE,
        TRANSLATION_X,
        TRANSLATION_Y,
        ROTATION,
        VOLUME
    }

    public enum Interpolation {
        LINEAR,
        EASE_IN,
        EASE_OUT,
        EASE_IN_OUT
    }

    private long timeOffsetUs;
    private Property property;
    private float value;
    private Interpolation interpolation;

    public Keyframe(long timeOffsetUs, Property property, float value, Interpolation interpolation) {
        this.timeOffsetUs = timeOffsetUs;
        this.property = property;
        this.value = value;
        this.interpolation = interpolation != null ? interpolation : Interpolation.LINEAR;
    }

    public Keyframe copy() {
        return new Keyframe(timeOffsetUs, property, value, interpolation);
    }

    public long getTimeOffsetUs() { return timeOffsetUs; }
    public void setTimeOffsetUs(long timeOffsetUs) { this.timeOffsetUs = timeOffsetUs; }

    public Property getProperty() { return property; }
    public float getValue() { return value; }
    public void setValue(float value) { this.value = value; }

    public Interpolation getInterpolation() { return interpolation; }
    public void setInterpolation(Interpolation interpolation) { this.interpolation = interpolation; }
}
