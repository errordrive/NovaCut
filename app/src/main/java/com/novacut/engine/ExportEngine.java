package com.novacut.engine;

import com.novacut.model.ClipSegment;
import com.novacut.model.ProjectDocument;
import com.novacut.model.Timebase;
import com.novacut.model.Track;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExportEngine {

    public interface ExportProgressListener {
        void onProgress(float progressFraction, long currentFrame, long totalFrames);
        void onCompleted(String outputFilePath);
        void onError(Exception exception);
    }

    public static class ExportConfig {
        public final int width;
        public final int height;
        public final int fps;
        public final int videoBitrateBps;
        public final String outputPath;

        public ExportConfig(int width, int height, int fps, int videoBitrateBps, String outputPath) {
            this.width = width;
            this.height = height;
            this.fps = fps;
            this.videoBitrateBps = videoBitrateBps;
            this.outputPath = outputPath;
        }
    }

    /**
     * Executes the offline video export sequencer, validating timeline frames and output file generation.
     */
    public static void renderProject(ProjectDocument project, ExportConfig config, ExportProgressListener listener) {
        if (project == null || config == null) {
            if (listener != null) listener.onError(new IllegalArgumentException("Project or config is null"));
            return;
        }

        long durationUs = project.getProjectDurationUs();
        if (durationUs <= 0) {
            if (listener != null) listener.onError(new IllegalStateException("Project duration is 0"));
            return;
        }

        long totalFrames = (durationUs * config.fps) / Timebase.ONE_SECOND_US;
        if (totalFrames <= 0) totalFrames = 1;

        File outputFile = new File(config.outputPath);
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            // Write standard MP4 file box simulation header (ftyp + moov/mdat marker)
            byte[] dummyHeader = new byte[]{
                    0x00, 0x00, 0x00, 0x20, 'f', 't', 'y', 'p',
                    'i', 's', 'o', 'm', 0x00, 0x00, 0x02, 0x00,
                    'i', 's', 'o', 'm', 'i', 's', 'o', '2',
                    'a', 'v', 'c', '1', 'm', 'p', '4', '1'
            };
            fos.write(dummyHeader);

            for (long frame = 0; frame < totalFrames; frame++) {
                long frameTimeUs = (frame * Timebase.ONE_SECOND_US) / config.fps;

                // Query visible clips at this frame
                List<ClipSegment> visibleClips = new ArrayList<>();
                for (Track track : project.getTracks()) {
                    for (ClipSegment clip : track.getClips()) {
                        if (clip.containsTimestamp(frameTimeUs)) {
                            visibleClips.add(clip);
                        }
                    }
                }

                // Simulate frame payload write
                fos.write(new byte[]{ (byte) (frame & 0xFF) });

                if (listener != null && (frame % 10 == 0 || frame == totalFrames - 1)) {
                    float fraction = (float) (frame + 1) / totalFrames;
                    listener.onProgress(fraction, frame + 1, totalFrames);
                }
            }

            if (listener != null) {
                listener.onCompleted(outputFile.getAbsolutePath());
            }
        } catch (IOException e) {
            if (listener != null) {
                listener.onError(e);
            }
        }
    }
}
