package com.novacut.engine;

import com.novacut.model.ClipSegment;
import com.novacut.model.ProjectDocument;
import com.novacut.model.Track;
import com.novacut.model.TrackType;

import java.util.ArrayList;
import java.util.List;

public class AudioEngine {

    public static class ActiveAudioSource {
        public final ClipSegment clip;
        public final long clipRelativeUs;
        public final float volume;

        public ActiveAudioSource(ClipSegment clip, long clipRelativeUs, float volume) {
            this.clip = clip;
            this.clipRelativeUs = clipRelativeUs;
            this.volume = volume;
        }
    }

    /**
     * Finds all active audio sources across all tracks at a specific playhead timestamp,
     * applying track muting, clip volume, and fade-in/fade-out envelopes.
     */
    public static List<ActiveAudioSource> getActiveAudioSources(ProjectDocument project, long playheadUs) {
        List<ActiveAudioSource> sources = new ArrayList<>();
        if (project == null) return sources;

        for (Track track : project.getTracks()) {
            if (track.isMuted()) continue;
            if (track.getType() != TrackType.VIDEO && track.getType() != TrackType.AUDIO) continue;

            for (ClipSegment clip : track.getClips()) {
                if (clip.containsTimestamp(playheadUs)) {
                    long relativeUs = playheadUs - clip.getTimelineStartUs();
                    float envelopeGain = clip.getAudioEnvelope() != null 
                            ? clip.getAudioEnvelope().computeGain(relativeUs, clip.getTimelineDurationUs()) 
                            : 1.0f;
                    float effectiveVolume = clip.getVolume() * envelopeGain;

                    sources.add(new ActiveAudioSource(clip, relativeUs, effectiveVolume));
                }
            }
        }
        return sources;
    }

    /**
     * Generates normalized peak values (0.0 to 1.0) for waveform visualization.
     */
    public static float[] generateSyntheticWaveform(long durationUs, int pointsCount) {
        if (pointsCount <= 0) pointsCount = 100;
        float[] waveform = new float[pointsCount];
        for (int i = 0; i < pointsCount; i++) {
            double angle = (double) i / pointsCount * Math.PI * 8.0;
            double value = (Math.sin(angle) * 0.4) + (Math.sin(angle * 2.5) * 0.3) + 0.3;
            waveform[i] = (float) Math.max(0.05, Math.min(1.0, value));
        }
        return waveform;
    }
}
