package com.novacut;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.novacut.engine.TimelineEngine;
import com.novacut.model.ClipSegment;
import com.novacut.model.ProjectDocument;
import com.novacut.model.Track;
import com.novacut.model.TrackType;
import com.novacut.ui.EditorViewModel;

public class MainActivity extends Activity {

    private EditorViewModel viewModel;
    private TextView tvPlayheadTimecode;
    private TextView tvTotalTimecode;
    private TextView tvTimelineInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize NovaCut Project and Sample Track
        ProjectDocument project = new ProjectDocument("project_demo", "NovaCut Default Project");
        Track mainTrack = new Track("track_main_video", TrackType.VIDEO, true, 0);
        project.getTracks().add(mainTrack);

        // Add initial sample clip (4 seconds = 4,000,000 us)
        ClipSegment initialClip = new ClipSegment(
                "clip_sample_1",
                "asset_sample_1",
                TrackType.VIDEO,
                0L,
                0L,
                4_000_000L
        );
        TimelineEngine.appendClip(project, "track_main_video", initialClip);

        viewModel = new EditorViewModel(project);

        // Bind UI Components
        tvPlayheadTimecode = findViewById(R.id.tv_playhead_timecode);
        tvTotalTimecode = findViewById(R.id.tv_total_timecode);
        tvTimelineInfo = findViewById(R.id.tv_timeline_info);

        Button btnUndo = findViewById(R.id.btn_undo);
        Button btnRedo = findViewById(R.id.btn_redo);
        Button btnSplit = findViewById(R.id.btn_split);
        Button btnTrim = findViewById(R.id.btn_trim);
        Button btnDelete = findViewById(R.id.btn_delete);
        Button btnExport = findViewById(R.id.btn_export);

        btnUndo.setOnClickListener(v -> {
            if (viewModel.undo()) {
                updateUI();
                Toast.makeText(this, "Undo executed", Toast.LENGTH_SHORT).show();
            }
        });

        btnRedo.setOnClickListener(v -> {
            if (viewModel.redo()) {
                updateUI();
                Toast.makeText(this, "Redo executed", Toast.LENGTH_SHORT).show();
            }
        });

        btnSplit.setOnClickListener(v -> {
            viewModel.selectClip("track_main_video", viewModel.getProject().getMainVideoTrack().getClips().get(0).getId());
            viewModel.seekTo(2_000_000L); // Seek to 2 seconds
            if (viewModel.splitSelectedClip()) {
                updateUI();
                Toast.makeText(this, "Split at 2s executed", Toast.LENGTH_SHORT).show();
            }
        });

        btnTrim.setOnClickListener(v -> {
            if (!viewModel.getProject().getMainVideoTrack().getClips().isEmpty()) {
                String firstClipId = viewModel.getProject().getMainVideoTrack().getClips().get(0).getId();
                viewModel.selectClip("track_main_video", firstClipId);
                viewModel.trimSelectedClip(true, 500_000L); // Trim head 500ms
                updateUI();
                Toast.makeText(this, "Trimmed 500ms from head", Toast.LENGTH_SHORT).show();
            }
        });

        btnDelete.setOnClickListener(v -> {
            if (viewModel.getProject().getMainVideoTrack().getClips().size() > 1) {
                String lastClipId = viewModel.getProject().getMainVideoTrack().getClips().get(1).getId();
                viewModel.selectClip("track_main_video", lastClipId);
                viewModel.deleteSelectedClip();
                updateUI();
                Toast.makeText(this, "Deleted second clip", Toast.LENGTH_SHORT).show();
            }
        });

        btnExport.setOnClickListener(v -> {
            Toast.makeText(this, "Export pipeline ready", Toast.LENGTH_SHORT).show();
        });

        updateUI();
    }

    private void updateUI() {
        if (tvPlayheadTimecode != null && tvTotalTimecode != null && tvTimelineInfo != null) {
            tvPlayheadTimecode.setText(viewModel.getFormattedPlayheadTimecode());
            tvTotalTimecode.setText(viewModel.getFormattedTotalDurationTimecode());
            int clipCount = viewModel.getProject().getMainVideoTrack() != null 
                    ? viewModel.getProject().getMainVideoTrack().getClips().size() 
                    : 0;
            tvTimelineInfo.setText(String.format("Magnetic Main Track (%d clips, Total: %s)", 
                    clipCount, viewModel.getFormattedTotalDurationTimecode()));
        }
    }
}
