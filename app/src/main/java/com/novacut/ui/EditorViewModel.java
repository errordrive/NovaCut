package com.novacut.ui;

import com.novacut.engine.PersistenceEngine;
import com.novacut.engine.SnappingEngine;
import com.novacut.engine.TimelineEngine;
import com.novacut.history.CommandHistory;
import com.novacut.history.DeleteClipCommand;
import com.novacut.history.EditorCommand;
import com.novacut.history.SplitClipCommand;
import com.novacut.history.TrimClipCommand;
import com.novacut.model.ClipSegment;
import com.novacut.model.ProjectDocument;
import com.novacut.model.Timebase;
import com.novacut.model.Track;

import java.io.File;
import java.io.IOException;

public class EditorViewModel {
    private ProjectDocument project;
    private final CommandHistory history;
    private long currentPlayheadUs;
    private String selectedTrackId;
    private String selectedClipId;
    private boolean isPlaying;

    public EditorViewModel(ProjectDocument project) {
        this.project = project;
        this.history = new CommandHistory();
        this.currentPlayheadUs = 0L;
        this.isPlaying = false;
    }

    public void seekTo(long targetUs) {
        if (targetUs < 0) targetUs = 0L;
        long maxDuration = project.getProjectDurationUs();
        if (targetUs > maxDuration) targetUs = maxDuration;

        if (project.isSnapEnabled()) {
            this.currentPlayheadUs = SnappingEngine.calculateSnappedPosition(
                    project, 
                    targetUs, 
                    SnappingEngine.DEFAULT_SNAP_THRESHOLD_US
            );
        } else {
            this.currentPlayheadUs = targetUs;
        }
    }

    public void selectClip(String trackId, String clipId) {
        this.selectedTrackId = trackId;
        this.selectedClipId = clipId;
    }

    public void clearSelection() {
        this.selectedTrackId = null;
        this.selectedClipId = null;
    }

    public boolean splitSelectedClip() {
        if (selectedTrackId == null || selectedClipId == null) return false;
        EditorCommand splitCmd = new SplitClipCommand(selectedTrackId, selectedClipId, currentPlayheadUs);
        history.executeCommand(project, splitCmd);
        return true;
    }

    public boolean trimSelectedClip(boolean isHead, long deltaUs) {
        if (selectedTrackId == null || selectedClipId == null) return false;
        EditorCommand trimCmd = new TrimClipCommand(selectedTrackId, selectedClipId, isHead, deltaUs);
        history.executeCommand(project, trimCmd);
        return true;
    }

    public boolean deleteSelectedClip() {
        if (selectedTrackId == null || selectedClipId == null) return false;
        EditorCommand deleteCmd = new DeleteClipCommand(selectedTrackId, selectedClipId);
        history.executeCommand(project, deleteCmd);
        clearSelection();
        return true;
    }

    public boolean undo() {
        if (!history.canUndo()) return false;
        history.undo(project);
        return true;
    }

    public boolean redo() {
        if (!history.canRedo()) return false;
        history.redo(project);
        return true;
    }

    public void autoSave(File targetFile) throws IOException {
        PersistenceEngine.saveProject(project, targetFile);
    }

    public String getFormattedPlayheadTimecode() {
        return Timebase.formatTimecode(currentPlayheadUs, project.getOutputFps());
    }

    public String getFormattedTotalDurationTimecode() {
        return Timebase.formatTimecode(project.getProjectDurationUs(), project.getOutputFps());
    }

    public ProjectDocument getProject() { return project; }
    public long getCurrentPlayheadUs() { return currentPlayheadUs; }
    public String getSelectedTrackId() { return selectedTrackId; }
    public String getSelectedClipId() { return selectedClipId; }
    public boolean isPlaying() { return isPlaying; }
    public void setPlaying(boolean playing) { isPlaying = playing; }
    public boolean canUndo() { return history.canUndo(); }
    public boolean canRedo() { return history.canRedo(); }
}
