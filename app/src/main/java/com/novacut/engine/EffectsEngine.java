package com.novacut.engine;

import com.novacut.model.ClipSegment;
import com.novacut.model.Keyframe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EffectsEngine {

    /**
     * Evaluates an animated property on a clip at a given timeline relative offset using keyframes.
     * Falls back to defaultValue if no keyframes are defined for this property.
     */
    public static float evaluateKeyframeProperty(ClipSegment clip, Keyframe.Property property, long clipRelativeUs, float defaultValue) {
        if (clip == null || clip.getKeyframes() == null || clip.getKeyframes().isEmpty()) {
            return defaultValue;
        }

        List<Keyframe> matching = new ArrayList<>();
        for (Keyframe kf : clip.getKeyframes()) {
            if (kf.getProperty() == property) {
                matching.add(kf);
            }
        }

        if (matching.isEmpty()) {
            return defaultValue;
        }

        matching.sort(Comparator.comparingLong(Keyframe::getTimeOffsetUs));

        // If before first keyframe
        if (clipRelativeUs <= matching.get(0).getTimeOffsetUs()) {
            return matching.get(0).getValue();
        }

        // If after last keyframe
        if (clipRelativeUs >= matching.get(matching.size() - 1).getTimeOffsetUs()) {
            return matching.get(matching.size() - 1).getValue();
        }

        // Find surrounding keyframe pair
        for (int i = 0; i < matching.size() - 1; i++) {
            Keyframe k1 = matching.get(i);
            Keyframe k2 = matching.get(i + 1);

            if (clipRelativeUs >= k1.getTimeOffsetUs() && clipRelativeUs <= k2.getTimeOffsetUs()) {
                long duration = k2.getTimeOffsetUs() - k1.getTimeOffsetUs();
                if (duration <= 0) return k1.getValue();

                float progress = (float) (clipRelativeUs - k1.getTimeOffsetUs()) / duration;
                float easedProgress = applyInterpolation(progress, k1.getInterpolation());
                return k1.getValue() + (k2.getValue() - k1.getValue()) * easedProgress;
            }
        }

        return defaultValue;
    }

    private static float applyInterpolation(float t, Keyframe.Interpolation interpolation) {
        if (t <= 0.0f) return 0.0f;
        if (t >= 1.0f) return 1.0f;

        switch (interpolation) {
            case EASE_IN:
                return t * t;
            case EASE_OUT:
                return t * (2.0f - t);
            case EASE_IN_OUT:
                return t < 0.5f ? 2.0f * t * t : -1.0f + (4.0f - 2.0f * t) * t;
            case LINEAR:
            default:
                return t;
        }
    }
}
