package com.novacut.model;

public class AudioEnvelope {
    private long fadeInUs;
    private long fadeOutUs;

    public AudioEnvelope() {
        this(0L, 0L);
    }

    public AudioEnvelope(long fadeInUs, long fadeOutUs) {
        this.fadeInUs = Math.max(0L, fadeInUs);
        this.fadeOutUs = Math.max(0L, fadeOutUs);
    }

    public AudioEnvelope copy() {
        return new AudioEnvelope(fadeInUs, fadeOutUs);
    }

    public long getFadeInUs() { return fadeInUs; }
    public void setFadeInUs(long fadeInUs) { this.fadeInUs = Math.max(0L, fadeInUs); }

    public long getFadeOutUs() { return fadeOutUs; }
    public void setFadeOutUs(long fadeOutUs) { this.fadeOutUs = Math.max(0L, fadeOutUs); }

    /**
     * Calculates the gain multiplier [0.0, 1.0] at a given clip relative timestamp.
     */
    public float computeGain(long clipRelativeUs, long clipDurationUs) {
        if (clipRelativeUs < 0 || clipRelativeUs > clipDurationUs) {
            return 0.0f;
        }
        float gain = 1.0f;
        if (fadeInUs > 0 && clipRelativeUs < fadeInUs) {
            gain = Math.min(gain, (float) clipRelativeUs / fadeInUs);
        }
        long timeFromEnd = clipDurationUs - clipRelativeUs;
        if (fadeOutUs > 0 && timeFromEnd < fadeOutUs) {
            gain = Math.min(gain, (float) timeFromEnd / fadeOutUs);
        }
        return Math.max(0.0f, Math.min(1.0f, gain));
    }
}
