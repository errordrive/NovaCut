package com.novacut.model;

public class ColorAdjustments {
    private float brightness;   // -1.0 to +1.0
    private float contrast;     // -1.0 to +1.0
    private float saturation;   // -1.0 to +1.0
    private float temperature;  // -1.0 to +1.0
    private float vignette;     // 0.0 to 1.0

    public ColorAdjustments() {
        this(0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public ColorAdjustments(float brightness, float contrast, float saturation, float temperature, float vignette) {
        this.brightness = brightness;
        this.contrast = contrast;
        this.saturation = saturation;
        this.temperature = temperature;
        this.vignette = vignette;
    }

    public ColorAdjustments copy() {
        return new ColorAdjustments(brightness, contrast, saturation, temperature, vignette);
    }

    public float getBrightness() { return brightness; }
    public void setBrightness(float brightness) { this.brightness = brightness; }

    public float getContrast() { return contrast; }
    public void setContrast(float contrast) { this.contrast = contrast; }

    public float getSaturation() { return saturation; }
    public void setSaturation(float saturation) { this.saturation = saturation; }

    public float getTemperature() { return temperature; }
    public void setTemperature(float temperature) { this.temperature = temperature; }

    public float getVignette() { return vignette; }
    public void setVignette(float vignette) { this.vignette = vignette; }
}
