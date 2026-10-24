package com.novacut.model;

public class CanvasConfig {
    public enum AspectRatio {
        RATIO_9_16("9:16", 1080, 1920),
        RATIO_16_9("16:9", 1920, 1080),
        RATIO_1_1("1:1", 1080, 1080),
        RATIO_4_3("4:3", 1440, 1080),
        RATIO_3_4("3:4", 1080, 1440),
        RATIO_2_1("2:1", 1920, 960),
        RATIO_CUSTOM("Custom", 1080, 1920);

        public final String label;
        public final int defaultWidth;
        public final int defaultHeight;

        AspectRatio(String label, int width, int height) {
            this.label = label;
            this.defaultWidth = width;
            this.defaultHeight = height;
        }
    }

    private int width;
    private int height;
    private AspectRatio aspectRatio;
    private String backgroundColor;
    private float blurIntensity;

    public CanvasConfig() {
        this(1080, 1920, AspectRatio.RATIO_9_16, "#000000FF", 0.0f);
    }

    public CanvasConfig(int width, int height, AspectRatio aspectRatio, String backgroundColor, float blurIntensity) {
        this.width = width;
        this.height = height;
        this.aspectRatio = aspectRatio;
        this.backgroundColor = backgroundColor;
        this.blurIntensity = blurIntensity;
    }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public AspectRatio getAspectRatio() { return aspectRatio; }
    public void setAspectRatio(AspectRatio aspectRatio) {
        this.aspectRatio = aspectRatio;
        if (aspectRatio != AspectRatio.RATIO_CUSTOM) {
            this.width = aspectRatio.defaultWidth;
            this.height = aspectRatio.defaultHeight;
        }
    }

    public String getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }

    public float getBlurIntensity() { return blurIntensity; }
    public void setBlurIntensity(float blurIntensity) { this.blurIntensity = blurIntensity; }
}
