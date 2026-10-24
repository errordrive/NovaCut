package com.novacut.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectDocument {
    public static final String CURRENT_SCHEMA_VERSION = "1.0.0";

    private String schemaVersion;
    private String projectId;
    private String name;
    private long createdAt;
    private long updatedAt;

    private CanvasConfig canvas;
    private int outputFps;
    private boolean mainTrackAdsorb; // Magnet snapping on main track
    private boolean snapEnabled;

    private Map<String, MediaAsset> materials;
    private List<Track> tracks;

    public ProjectDocument(String projectId, String name) {
        this.schemaVersion = CURRENT_SCHEMA_VERSION;
        this.projectId = projectId;
        this.name = name;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
        this.canvas = new CanvasConfig();
        this.outputFps = 30;
        this.mainTrackAdsorb = true;
        this.snapEnabled = true;
        this.materials = new HashMap<>();
        this.tracks = new ArrayList<>();
    }

    public ProjectDocument copy() {
        ProjectDocument copy = new ProjectDocument(projectId, name);
        copy.schemaVersion = this.schemaVersion;
        copy.createdAt = this.createdAt;
        copy.updatedAt = this.updatedAt;
        copy.canvas = new CanvasConfig(canvas.getWidth(), canvas.getHeight(), canvas.getAspectRatio(), canvas.getBackgroundColor(), canvas.getBlurIntensity());
        copy.outputFps = this.outputFps;
        copy.mainTrackAdsorb = this.mainTrackAdsorb;
        copy.snapEnabled = this.snapEnabled;
        for (Map.Entry<String, MediaAsset> entry : this.materials.entrySet()) {
            copy.materials.put(entry.getKey(), entry.getValue());
        }
        for (Track t : this.tracks) {
            copy.tracks.add(t.copy());
        }
        return copy;
    }

    public long getProjectDurationUs() {
        long maxDuration = 0L;
        for (Track track : tracks) {
            maxDuration = Math.max(maxDuration, track.getTrackDurationUs());
        }
        return maxDuration;
    }

    public Track getMainVideoTrack() {
        for (Track track : tracks) {
            if (track.isMainTrack() && track.getType() == TrackType.VIDEO) {
                return track;
            }
        }
        return null;
    }

    // Getters and Setters
    public String getSchemaVersion() { return schemaVersion; }
    public String getProjectId() { return projectId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public CanvasConfig getCanvas() { return canvas; }
    public void setCanvas(CanvasConfig canvas) { this.canvas = canvas; }

    public int getOutputFps() { return outputFps; }
    public void setOutputFps(int outputFps) { this.outputFps = outputFps; }

    public boolean isMainTrackAdsorb() { return mainTrackAdsorb; }
    public void setMainTrackAdsorb(boolean mainTrackAdsorb) { this.mainTrackAdsorb = mainTrackAdsorb; }

    public boolean isSnapEnabled() { return snapEnabled; }
    public void setSnapEnabled(boolean snapEnabled) { this.snapEnabled = snapEnabled; }

    public Map<String, MediaAsset> getMaterials() { return materials; }
    public List<Track> getTracks() { return tracks; }
}
