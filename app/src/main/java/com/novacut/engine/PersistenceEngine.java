package com.novacut.engine;

import com.novacut.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PersistenceEngine {

    /**
     * Serializes a ProjectDocument into JSON string format matching the specification.
     */
    public static String serializeToJson(ProjectDocument project) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"schema_version\": \"").append(project.getSchemaVersion()).append("\",\n");
        sb.append("  \"project_id\": \"").append(project.getProjectId()).append("\",\n");
        sb.append("  \"name\": \"").append(escapeJson(project.getName())).append("\",\n");
        sb.append("  \"created_at\": ").append(project.getCreatedAt()).append(",\n");
        sb.append("  \"updated_at\": ").append(project.getUpdatedAt()).append(",\n");
        sb.append("  \"output_fps\": ").append(project.getOutputFps()).append(",\n");
        sb.append("  \"main_track_adsorb\": ").append(project.isMainTrackAdsorb()).append(",\n");
        sb.append("  \"snap_enabled\": ").append(project.isSnapEnabled()).append(",\n");

        // Canvas
        CanvasConfig c = project.getCanvas();
        sb.append("  \"canvas\": {\n");
        sb.append("    \"width\": ").append(c.getWidth()).append(",\n");
        sb.append("    \"height\": ").append(c.getHeight()).append(",\n");
        sb.append("    \"ratio\": \"").append(c.getAspectRatio().name()).append("\",\n");
        sb.append("    \"background_color\": \"").append(c.getBackgroundColor()).append("\",\n");
        sb.append("    \"blur_intensity\": ").append(c.getBlurIntensity()).append("\n");
        sb.append("  },\n");

        // Tracks
        sb.append("  \"tracks\": [\n");
        for (int i = 0; i < project.getTracks().size(); i++) {
            Track t = project.getTracks().get(i);
            sb.append("    {\n");
            sb.append("      \"id\": \"").append(t.getId()).append("\",\n");
            sb.append("      \"type\": \"").append(t.getType().name()).append("\",\n");
            sb.append("      \"is_main_track\": ").append(t.isMainTrack()).append(",\n");
            sb.append("      \"order\": ").append(t.getOrder()).append(",\n");
            sb.append("      \"muted\": ").append(t.isMuted()).append(",\n");
            sb.append("      \"locked\": ").append(t.isLocked()).append(",\n");
            sb.append("      \"clips\": [\n");
            for (int j = 0; j < t.getClips().size(); j++) {
                ClipSegment clip = t.getClips().get(j);
                sb.append("        {\n");
                sb.append("          \"id\": \"").append(clip.getId()).append("\",\n");
                sb.append("          \"material_id\": \"").append(clip.getMaterialId()).append("\",\n");
                sb.append("          \"type\": \"").append(clip.getType().name()).append("\",\n");
                sb.append("          \"timeline_start_us\": ").append(clip.getTimelineStartUs()).append(",\n");
                sb.append("          \"timeline_duration_us\": ").append(clip.getTimelineDurationUs()).append(",\n");
                sb.append("          \"source_in_us\": ").append(clip.getSourceInUs()).append(",\n");
                sb.append("          \"source_out_us\": ").append(clip.getSourceOutUs()).append(",\n");
                sb.append("          \"speed\": ").append(clip.getSpeed()).append(",\n");
                sb.append("          \"volume\": ").append(clip.getVolume()).append(",\n");
                sb.append("          \"opacity\": ").append(clip.getOpacity()).append(",\n");

                // Transform
                Transform tr = clip.getTransform();
                sb.append("          \"transform\": {\"translation_x\": ").append(tr.getTranslationX())
                        .append(", \"translation_y\": ").append(tr.getTranslationY())
                        .append(", \"scale\": ").append(tr.getScale())
                        .append(", \"rotation_deg\": ").append(tr.getRotationDeg())
                        .append(", \"flip_h\": ").append(tr.isFlipH())
                        .append(", \"flip_v\": ").append(tr.isFlipV()).append("},\n");

                // Color
                ColorAdjustments ca = clip.getColorAdjustments();
                sb.append("          \"color\": {\"brightness\": ").append(ca.getBrightness())
                        .append(", \"contrast\": ").append(ca.getContrast())
                        .append(", \"saturation\": ").append(ca.getSaturation())
                        .append(", \"temperature\": ").append(ca.getTemperature())
                        .append(", \"vignette\": ").append(ca.getVignette()).append("},\n");

                // Audio envelope
                AudioEnvelope ae = clip.getAudioEnvelope();
                sb.append("          \"envelope\": {\"fade_in_us\": ").append(ae.getFadeInUs())
                        .append(", \"fade_out_us\": ").append(ae.getFadeOutUs()).append("}\n");

                sb.append("        }").append(j < t.getClips().size() - 1 ? "," : "").append("\n");
            }
            sb.append("      ]\n");
            sb.append("    }").append(i < project.getTracks().size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Atomically writes project JSON to destination file using a temporary file.
     */
    public static void saveProject(ProjectDocument project, File destFile) throws IOException {
        if (destFile.getParentFile() != null) {
            destFile.getParentFile().mkdirs();
        }
        File tmpFile = new File(destFile.getAbsolutePath() + ".tmp");
        String json = serializeToJson(project);
        try (FileOutputStream fos = new FileOutputStream(tmpFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            osw.write(json);
            osw.flush();
            fos.getFD().sync();
        }

        if (destFile.exists()) {
            destFile.delete();
        }
        if (!tmpFile.renameTo(destFile)) {
            throw new IOException("Failed to atomically rename tmp file to " + destFile.getAbsolutePath());
        }
    }

    public static String readProjectJson(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}
