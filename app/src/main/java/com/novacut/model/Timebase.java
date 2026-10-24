package com.novacut.model;

/**
 * Standard integer microsecond timebase utilities.
 * 1 second = 1,000,000 microseconds (us).
 */
public final class Timebase {
    public static final long ONE_SECOND_US = 1_000_000L;
    public static final long ONE_MILLIS_US = 1_000L;

    private Timebase() {}

    public static long secondsToUs(double seconds) {
        return Math.round(seconds * ONE_SECOND_US);
    }

    public static double usToSeconds(long us) {
        return (double) us / ONE_SECOND_US;
    }

    public static long millisToUs(long millis) {
        return millis * ONE_MILLIS_US;
    }

    public static long usToMillis(long us) {
        return us / ONE_MILLIS_US;
    }

    public static long framesToUs(long frameIndex, int fps) {
        if (fps <= 0) throw new IllegalArgumentException("fps must be > 0");
        return (frameIndex * ONE_SECOND_US) / fps;
    }

    public static long usToFrames(long us, int fps) {
        if (fps <= 0) throw new IllegalArgumentException("fps must be > 0");
        return (us * fps) / ONE_SECOND_US;
    }

    public static String formatTimecode(long us, int fps) {
        if (us < 0) us = 0;
        long totalSeconds = us / ONE_SECOND_US;
        long remUs = us % ONE_SECOND_US;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        long frames = (remUs * fps) / ONE_SECOND_US;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d:%02d", hours, minutes, seconds, frames);
        } else {
            return String.format("%02d:%02d:%02d", minutes, seconds, frames);
        }
    }
}
