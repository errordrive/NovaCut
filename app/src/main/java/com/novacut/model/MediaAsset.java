package com.novacut.model;

public class MediaAsset {
    public enum MediaType {
        VIDEO,
        AUDIO,
        IMAGE
    }

    private String id;
    private String path;
    private MediaType type;
    private long durationUs;
    private int width;
    private int height;
    private float fps;
    private int channels;
    private int sampleRate;
    private boolean available;

    public MediaAsset(String id, String path, MediaType type, long durationUs, int width, int height, float fps) {
        this.id = id;
        this.path = path;
        this.type = type;
        this.durationUs = durationUs;
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.channels = 2;
        this.sampleRate = 44100;
        this.available = true;
    }

    public String getId() { return id; }
    public String getPath() { return path; }
    public MediaType getType() { return type; }
    public long getDurationUs() { return durationUs; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public float getFps() { return fps; }
    public int getChannels() { return channels; }
    public void setChannels(int channels) { this.channels = channels; }
    public int getSampleRate() { return sampleRate; }
    public void setSampleRate(int sampleRate) { this.sampleRate = sampleRate; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
