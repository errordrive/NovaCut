package com.novacut.model;

public class Transform {
    private float translationX; // Normalized [-1.0, 1.0]
    private float translationY;
    private float scale;        // 1.0 = normal
    private float rotationDeg;  // 0.0 to 360.0
    private boolean flipH;
    private boolean flipV;

    public Transform() {
        this(0.0f, 0.0f, 1.0f, 0.0f, false, false);
    }

    public Transform(float translationX, float translationY, float scale, float rotationDeg, boolean flipH, boolean flipV) {
        this.translationX = translationX;
        this.translationY = translationY;
        this.scale = scale;
        this.rotationDeg = rotationDeg;
        this.flipH = flipH;
        this.flipV = flipV;
    }

    public Transform copy() {
        return new Transform(translationX, translationY, scale, rotationDeg, flipH, flipV);
    }

    public float getTranslationX() { return translationX; }
    public void setTranslationX(float translationX) { this.translationX = translationX; }

    public float getTranslationY() { return translationY; }
    public void setTranslationY(float translationY) { this.translationY = translationY; }

    public float getScale() { return scale; }
    public void setScale(float scale) { this.scale = scale; }

    public float getRotationDeg() { return rotationDeg; }
    public void setRotationDeg(float rotationDeg) { this.rotationDeg = rotationDeg; }

    public boolean isFlipH() { return flipH; }
    public void setFlipH(boolean flipH) { this.flipH = flipH; }

    public boolean isFlipV() { return flipV; }
    public void setFlipV(boolean flipV) { this.flipV = flipV; }
}
