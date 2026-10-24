package com.novacut;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import com.novacut.engine.AudioEngine;
import com.novacut.engine.ExportEngine;
import com.novacut.engine.SnappingEngine;
import com.novacut.engine.TimelineEngine;
import com.novacut.model.*;
import com.novacut.ui.EditorViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    // Microsecond timeline scale (1 second = 80dp on screen)
    private static final int PIXELS_PER_SECOND = 80;

    private ProjectDocument project;
    private EditorViewModel viewModel;

    // Top Bar Views
    private TextView tvProjectTitle;
    private TextView btnResolutionPill;
    private TextView btnUndo;
    private TextView btnRedo;
    private Button btnExport;

    // Canvas Preview Views
    private FrameLayout canvasFrame;
    private TextView tvCanvasPreview;
    private TextView btnPlayPause;
    private boolean isPlaying = false;
    private Handler playbackHandler;
    private Runnable playbackRunnable;

    // Timecode Bar Views
    private TextView tvPlayheadTimecode;
    private TextView tvTotalTimecode;
    private TextView tvAspectBadge;
    private TextView btnSnapToggle;
    private boolean isSnappingOn = true;

    // Multitrack Timeline Views
    private HorizontalScrollView timelineScroll;
    private LinearLayout trackMainContainer;
    private LinearLayout trackPipContainer;
    private LinearLayout trackAudioContainer;
    private LinearLayout trackTextContainer;

    // Toolbars & Panels
    private View toolbarGlobal;
    private View toolbarClip;
    private FrameLayout modalPanelDock;
    private View panelSpeed;
    private View panelVolume;
    private View panelAdjust;
    private View panelRatio;
    private View panelText;
    private View panelExport;

    // Panel controls
    private SeekBar seekSpeed;
    private TextView tvSpeedVal;
    private SeekBar seekVolume;
    private TextView tvVolumeVal;
    private SeekBar seekBrightness;
    private TextView tvBrightnessLabel;
    private SeekBar seekContrast;
    private TextView tvContrastLabel;
    private SeekBar seekSaturation;
    private TextView tvSaturationLabel;
    private EditText etTextInput;
    private ProgressBar progressExport;
    private TextView tvExportProgressPct;
    private Button btnStartRender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initDefaultProject();
        bindViews();
        setupEventListeners();
        renderAllTracks();
        updateTimecodeDisplay();
    }

    private void initDefaultProject() {
        project = new ProjectDocument("proj_001", "NovaCut Video 01");
        project.setSnapEnabled(isSnappingOn);

        // 1. Main Video Track
        Track mainTrack = new Track("track_main", TrackType.VIDEO, true, 0);
        mainTrack.getClips().add(new ClipSegment("clip_v1", "m_v1", TrackType.VIDEO, 0L, 0L, 2_500_000L)); // 2.5s
        mainTrack.getClips().add(new ClipSegment("clip_v2", "m_v2", TrackType.VIDEO, 2_500_000L, 0L, 2_000_000L)); // 2.0s
        project.getTracks().add(mainTrack);

        // 2. Audio BGM Track
        Track audioTrack = new Track("track_audio", TrackType.AUDIO, false, 1);
        ClipSegment audioClip = new ClipSegment("clip_a1", "m_a1", TrackType.AUDIO, 0L, 0L, 4_500_000L);
        audioClip.getAudioEnvelope().setFadeInUs(500_000L);
        audioClip.getAudioEnvelope().setFadeOutUs(500_000L);
        audioTrack.getClips().add(audioClip);
        project.getTracks().add(audioTrack);

        // 3. Text Subtitle Track
        Track textTrack = new Track("track_text", TrackType.TEXT, false, 2);
        ClipSegment textClip = new ClipSegment("clip_t1", "m_t1", TrackType.TEXT, 500_000L, 0L, 2_000_000L);
        textClip.setTextContent("Welcome to NovaCut");
        textTrack.getClips().add(textClip);
        project.getTracks().add(textTrack);

        // 4. Secondary PIP Overlay Track
        Track pipTrack = new Track("track_pip", TrackType.OVERLAY, false, 3);
        project.getTracks().add(pipTrack);

        viewModel = new EditorViewModel(project);
    }

    private void bindViews() {
        tvProjectTitle = findViewById(R.id.tv_project_title);
        btnResolutionPill = findViewById(R.id.btn_resolution_pill);
        btnUndo = findViewById(R.id.btn_undo);
        btnRedo = findViewById(R.id.btn_redo);
        btnExport = findViewById(R.id.btn_export);

        canvasFrame = findViewById(R.id.canvas_frame);
        tvCanvasPreview = findViewById(R.id.tv_canvas_preview);
        btnPlayPause = findViewById(R.id.btn_play_pause);

        tvPlayheadTimecode = findViewById(R.id.tv_playhead_timecode);
        tvTotalTimecode = findViewById(R.id.tv_total_timecode);
        tvAspectBadge = findViewById(R.id.tv_aspect_badge);
        btnSnapToggle = findViewById(R.id.btn_snap_toggle);

        timelineScroll = findViewById(R.id.timeline_horizontal_scroll);
        trackMainContainer = findViewById(R.id.track_main_container);
        trackPipContainer = findViewById(R.id.track_pip_container);
        trackAudioContainer = findViewById(R.id.track_audio_container);
        trackTextContainer = findViewById(R.id.track_text_container);

        toolbarGlobal = findViewById(R.id.toolbar_global);
        toolbarClip = findViewById(R.id.toolbar_clip);
        modalPanelDock = findViewById(R.id.modal_panel_dock);

        panelSpeed = findViewById(R.id.panel_speed);
        panelVolume = findViewById(R.id.panel_volume);
        panelAdjust = findViewById(R.id.panel_adjust);
        panelRatio = findViewById(R.id.panel_ratio);
        panelText = findViewById(R.id.panel_text);
        panelExport = findViewById(R.id.panel_export);

        seekSpeed = findViewById(R.id.seek_speed);
        tvSpeedVal = findViewById(R.id.tv_speed_val);
        seekVolume = findViewById(R.id.seek_volume);
        tvVolumeVal = findViewById(R.id.tv_volume_val);
        seekBrightness = findViewById(R.id.seek_brightness);
        tvBrightnessLabel = findViewById(R.id.tv_brightness_label);
        seekContrast = findViewById(R.id.seek_contrast);
        tvContrastLabel = findViewById(R.id.tv_contrast_label);
        seekSaturation = findViewById(R.id.seek_saturation);
        tvSaturationLabel = findViewById(R.id.tv_saturation_label);
        etTextInput = findViewById(R.id.et_text_input);
        progressExport = findViewById(R.id.progress_export);
        tvExportProgressPct = findViewById(R.id.tv_export_progress_pct);
        btnStartRender = findViewById(R.id.btn_start_render);
    }

    private void setupEventListeners() {
        // Back / Deselect
        findViewById(R.id.btn_back).setOnClickListener(v -> deselectClip());

        // Undo & Redo
        btnUndo.setOnClickListener(v -> {
            if (viewModel.undo()) {
                renderAllTracks();
                updateTimecodeDisplay();
                Toast.makeText(this, "Undo", Toast.LENGTH_SHORT).show();
            }
        });
        btnRedo.setOnClickListener(v -> {
            if (viewModel.redo()) {
                renderAllTracks();
                updateTimecodeDisplay();
                Toast.makeText(this, "Redo", Toast.LENGTH_SHORT).show();
            }
        });

        // Play / Pause Simulation
        playbackHandler = new Handler(Looper.getMainLooper());
        playbackRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPlaying) return;
                long current = viewModel.getCurrentPlayheadUs();
                long total = project.getProjectDurationUs();
                if (current >= total) {
                    togglePlayPause(false);
                    return;
                }
                long next = current + 33_333L; // ~30 fps step
                viewModel.seekTo(next);
                syncTimelineScrollToPlayhead();
                updateTimecodeDisplay();
                playbackHandler.postDelayed(this, 33);
            }
        };
        btnPlayPause.setOnClickListener(v -> togglePlayPause(!isPlaying));

        // Snapping Magnet Toggle
        btnSnapToggle.setOnClickListener(v -> {
            isSnappingOn = !isSnappingOn;
            project.setSnapEnabled(isSnappingOn);
            btnSnapToggle.setText(isSnappingOn ? "🧲 Snap ON" : "🧲 Snap OFF");
            btnSnapToggle.setTextColor(isSnappingOn ? Color.parseColor("#00E5FF") : Color.parseColor("#64748B"));
            Toast.makeText(this, isSnappingOn ? "Magnet Snapping Enabled" : "Snapping Disabled", Toast.LENGTH_SHORT).show();
        });

        // Resolution Selector
        btnResolutionPill.setOnClickListener(v -> {
            String[] resolutions = {"720P (HD)", "1080P (FHD)", "2K (QHD)", "4K (UHD)"};
            showOptionsPopup("Select Canvas Resolution", resolutions, (idx) -> {
                btnResolutionPill.setText(resolutions[idx].split(" ")[0] + " ▼");
            });
        });

        // Timeline Scroll Sync
        timelineScroll.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (!isPlaying) {
                long playheadUs = Math.round(((double) scrollX / PIXELS_PER_SECOND) * Timebase.ONE_SECOND_US);
                viewModel.seekTo(playheadUs);
                updateTimecodeDisplay();
            }
        });

        // Level 1 Toolbar: Global Actions
        findViewById(R.id.btn_menu_edit).setOnClickListener(v -> {
            // Select first clip on main track
            Track main = project.getMainVideoTrack();
            if (main != null && !main.getClips().isEmpty()) {
                selectClip("track_main", main.getClips().get(0).getId());
            }
        });
        findViewById(R.id.btn_menu_ratio).setOnClickListener(v -> openModalPanel(panelRatio));
        findViewById(R.id.btn_menu_text).setOnClickListener(v -> openModalPanel(panelText));
        findViewById(R.id.btn_menu_adjust).setOnClickListener(v -> openModalPanel(panelAdjust));
        findViewById(R.id.btn_menu_audio).setOnClickListener(v -> {
            Toast.makeText(this, "Audio Track Selected", Toast.LENGTH_SHORT).show();
        });

        // Level 2 Toolbar: Clip Actions
        findViewById(R.id.btn_clip_split).setOnClickListener(v -> executeSplit());
        findViewById(R.id.btn_clip_trim_head).setOnClickListener(v -> executeTrim(true));
        findViewById(R.id.btn_clip_trim_tail).setOnClickListener(v -> executeTrim(false));
        findViewById(R.id.btn_clip_delete).setOnClickListener(v -> executeDelete());
        findViewById(R.id.btn_clip_duplicate).setOnClickListener(v -> executeDuplicate());
        findViewById(R.id.btn_clip_speed).setOnClickListener(v -> openModalPanel(panelSpeed));
        findViewById(R.id.btn_clip_volume).setOnClickListener(v -> openModalPanel(panelVolume));
        findViewById(R.id.btn_clip_adjust).setOnClickListener(v -> openModalPanel(panelAdjust));
        findViewById(R.id.btn_clip_transform).setOnClickListener(v -> executeTransformRotate());

        // Panels: Speed Panel
        seekSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float speed = Math.max(0.1f, (float) progress / 10.0f);
                tvSpeedVal.setText(String.format("%.1fx", speed));
                applySelectedClipSpeed(speed);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        findViewById(R.id.btn_speed_05).setOnClickListener(v -> seekSpeed.setProgress(5));
        findViewById(R.id.btn_speed_10).setOnClickListener(v -> seekSpeed.setProgress(10));
        findViewById(R.id.btn_speed_20).setOnClickListener(v -> seekSpeed.setProgress(20));
        findViewById(R.id.btn_close_panel_speed).setOnClickListener(v -> closeModalPanel());

        // Panels: Volume Panel
        seekVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvVolumeVal.setText(progress + "%");
                applySelectedClipVolume((float) progress / 100.0f);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        findViewById(R.id.btn_toggle_fade_in).setOnClickListener(v -> {
            ClipSegment clip = getSelectedClip();
            if (clip != null) {
                long current = clip.getAudioEnvelope().getFadeInUs();
                clip.getAudioEnvelope().setFadeInUs(current == 0 ? 1_000_000L : 0L);
                Toast.makeText(this, "Fade In: " + Timebase.usToSeconds(clip.getAudioEnvelope().getFadeInUs()) + "s", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btn_toggle_fade_out).setOnClickListener(v -> {
            ClipSegment clip = getSelectedClip();
            if (clip != null) {
                long current = clip.getAudioEnvelope().getFadeOutUs();
                clip.getAudioEnvelope().setFadeOutUs(current == 0 ? 1_000_000L : 0L);
                Toast.makeText(this, "Fade Out: " + Timebase.usToSeconds(clip.getAudioEnvelope().getFadeOutUs()) + "s", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btn_close_panel_volume).setOnClickListener(v -> closeModalPanel());

        // Panels: Color Adjust Panel
        setupColorSlider(seekBrightness, tvBrightnessLabel, "Brightness", (val) -> {
            ClipSegment c = getSelectedClip();
            if (c != null) c.getColorAdjustments().setBrightness(val);
        });
        setupColorSlider(seekContrast, tvContrastLabel, "Contrast", (val) -> {
            ClipSegment c = getSelectedClip();
            if (c != null) c.getColorAdjustments().setContrast(val);
        });
        setupColorSlider(seekSaturation, tvSaturationLabel, "Saturation", (val) -> {
            ClipSegment c = getSelectedClip();
            if (c != null) c.getColorAdjustments().setSaturation(val);
        });
        findViewById(R.id.btn_close_panel_adjust).setOnClickListener(v -> closeModalPanel());

        // Panels: Aspect Ratio Panel
        findViewById(R.id.btn_ratio_9_16).setOnClickListener(v -> setCanvasAspectRatio(220, 390, "9:16"));
        findViewById(R.id.btn_ratio_16_9).setOnClickListener(v -> setCanvasAspectRatio(360, 202, "16:9"));
        findViewById(R.id.btn_ratio_1_1).setOnClickListener(v -> setCanvasAspectRatio(260, 260, "1:1"));
        findViewById(R.id.btn_ratio_4_3).setOnClickListener(v -> setCanvasAspectRatio(300, 225, "4:3"));
        findViewById(R.id.btn_close_panel_ratio).setOnClickListener(v -> closeModalPanel());

        // Panels: Text Overlay Panel
        findViewById(R.id.btn_add_text_to_timeline).setOnClickListener(v -> {
            String text = etTextInput.getText().toString().trim();
            if (!text.isEmpty()) {
                addTextSegment(text);
                etTextInput.setText("");
                closeModalPanel();
            }
        });

        // Top Export Action & Panel
        btnExport.setOnClickListener(v -> openModalPanel(panelExport));
        findViewById(R.id.btn_cancel_export).setOnClickListener(v -> closeModalPanel());
        btnStartRender = findViewById(R.id.btn_start_render);
        btnStartRender.setOnClickListener(v -> startExportProcess());
    }

    private void togglePlayPause(boolean play) {
        isPlaying = play;
        btnPlayPause.setText(isPlaying ? "❚❚" : "▶");
        if (isPlaying) {
            playbackHandler.post(playbackRunnable);
        } else {
            playbackHandler.removeCallbacks(playbackRunnable);
        }
    }

    private void syncTimelineScrollToPlayhead() {
        long currentUs = viewModel.getCurrentPlayheadUs();
        int scrollX = (int) (Timebase.usToSeconds(currentUs) * PIXELS_PER_SECOND);
        timelineScroll.scrollTo(scrollX, 0);
    }

    private void selectClip(String trackId, String clipId) {
        viewModel.selectClip(trackId, clipId);
        toolbarGlobal.setVisibility(View.GONE);
        toolbarClip.setVisibility(View.VISIBLE);
        renderAllTracks();
    }

    private void deselectClip() {
        viewModel.clearSelection();
        toolbarClip.setVisibility(View.GONE);
        toolbarGlobal.setVisibility(View.VISIBLE);
        closeModalPanel();
        renderAllTracks();
    }

    private ClipSegment getSelectedClip() {
        if (viewModel.getSelectedTrackId() == null || viewModel.getSelectedClipId() == null) return null;
        Track track = TimelineEngine.findTrack(project, viewModel.getSelectedTrackId());
        return track != null ? TimelineEngine.findClip(track, viewModel.getSelectedClipId()) : null;
    }

    private void executeSplit() {
        if (viewModel.splitSelectedClip()) {
            renderAllTracks();
            updateTimecodeDisplay();
            Toast.makeText(this, "Split Clip at Playhead", Toast.LENGTH_SHORT).show();
        }
    }

    private void executeTrim(boolean isHead) {
        if (viewModel.trimSelectedClip(isHead, 500_000L)) {
            renderAllTracks();
            updateTimecodeDisplay();
            Toast.makeText(this, isHead ? "Trimmed 500ms Head" : "Trimmed 500ms Tail", Toast.LENGTH_SHORT).show();
        }
    }

    private void executeDelete() {
        if (viewModel.deleteSelectedClip()) {
            deselectClip();
            renderAllTracks();
            updateTimecodeDisplay();
            Toast.makeText(this, "Deleted Clip", Toast.LENGTH_SHORT).show();
        }
    }

    private void executeDuplicate() {
        ClipSegment clip = getSelectedClip();
        if (clip != null && viewModel.getSelectedTrackId() != null) {
            ClipSegment dup = clip.copy();
            TimelineEngine.appendClip(project, viewModel.getSelectedTrackId(), dup);
            renderAllTracks();
            updateTimecodeDisplay();
            Toast.makeText(this, "Duplicated Clip", Toast.LENGTH_SHORT).show();
        }
    }

    private void executeTransformRotate() {
        ClipSegment clip = getSelectedClip();
        if (clip != null) {
            float rot = (clip.getTransform().getRotationDeg() + 90.0f) % 360.0f;
            clip.getTransform().setRotationDeg(rot);
            tvCanvasPreview.setText(String.format("Preview Canvas\n[Rotated %.0f°]", rot));
            Toast.makeText(this, "Rotated 90°", Toast.LENGTH_SHORT).show();
        }
    }

    private void applySelectedClipSpeed(float speed) {
        ClipSegment clip = getSelectedClip();
        if (clip != null) {
            clip.setSpeed(speed);
            Track track = TimelineEngine.findTrack(project, viewModel.getSelectedTrackId());
            if (track != null && track.isMainTrack() && project.isMainTrackAdsorb()) {
                TimelineEngine.ripplePackMainTrack(track);
            }
            renderAllTracks();
            updateTimecodeDisplay();
        }
    }

    private void applySelectedClipVolume(float vol) {
        ClipSegment clip = getSelectedClip();
        if (clip != null) {
            clip.setVolume(vol);
        }
    }

    private void setCanvasAspectRatio(int widthDp, int heightDp, String label) {
        float density = getResources().getDisplayMetrics().density;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) canvasFrame.getLayoutParams();
        params.width = (int) (widthDp * density);
        params.height = (int) (heightDp * density);
        canvasFrame.setLayoutParams(params);
        tvAspectBadge.setText(label);
        tvCanvasPreview.setText("Preview Canvas\n[" + label + " Aspect]");
        closeModalPanel();
    }

    private void addTextSegment(String text) {
        Track textTrack = TimelineEngine.findTrack(project, "track_text");
        if (textTrack != null) {
            long startUs = viewModel.getCurrentPlayheadUs();
            ClipSegment segment = new ClipSegment("txt_" + System.currentTimeMillis(), "m_txt", TrackType.TEXT, startUs, 0L, 2_000_000L);
            segment.setTextContent(text);
            textTrack.getClips().add(segment);
            renderAllTracks();
            Toast.makeText(this, "Added Subtitle Overlay", Toast.LENGTH_SHORT).show();
        }
    }

    private void startExportProcess() {
        progressExport.setVisibility(View.VISIBLE);
        tvExportProgressPct.setVisibility(View.VISIBLE);
        progressExport.setProgress(0);
        btnStartRender.setEnabled(false);

        File exportDir = new File(getFilesDir(), "exports");
        exportDir.mkdirs();
        File outFile = new File(exportDir, "NovaCut_" + System.currentTimeMillis() + ".mp4");

        ExportEngine.ExportConfig config = new ExportEngine.ExportConfig(
                1080, 1920, 30, 8_000_000, outFile.getAbsolutePath()
        );

        new Thread(() -> {
            ExportEngine.renderProject(project, config, new ExportEngine.ExportProgressListener() {
                @Override
                public void onProgress(float progressFraction, long currentFrame, long totalFrames) {
                    runOnUiThread(() -> {
                        int pct = Math.round(progressFraction * 100);
                        progressExport.setProgress(pct);
                        tvExportProgressPct.setText(pct + "%");
                    });
                }

                @Override
                public void onCompleted(String outputFilePath) {
                    runOnUiThread(() -> {
                        progressExport.setProgress(100);
                        tvExportProgressPct.setText("Export Completed!");
                        btnStartRender.setEnabled(true);
                        Toast.makeText(MainActivity.this, "Export Saved: " + outFile.getName(), Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onError(Exception exception) {
                    runOnUiThread(() -> {
                        btnStartRender.setEnabled(true);
                        Toast.makeText(MainActivity.this, "Export Error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }).start();
    }

    private void renderAllTracks() {
        renderMainVideoTrack();
        renderAudioTrack();
        renderTextTrack();
    }

    private void renderMainVideoTrack() {
        trackMainContainer.removeAllViews();
        Track mainTrack = project.getMainVideoTrack();
        if (mainTrack == null) return;

        float density = getResources().getDisplayMetrics().density;
        String selectedId = viewModel.getSelectedClipId();

        for (int i = 0; i < mainTrack.getClips().size(); i++) {
            ClipSegment clip = mainTrack.getClips().get(i);
            int widthPx = (int) (Timebase.usToSeconds(clip.getTimelineDurationUs()) * PIXELS_PER_SECOND * density);
            widthPx = Math.max(widthPx, (int) (60 * density)); // Minimum block width

            boolean isSelected = clip.getId().equals(selectedId);

            FrameLayout clipBlock = new FrameLayout(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(widthPx, (int) (56 * density));
            lp.setMargins(2, 0, 2, 0);
            clipBlock.setLayoutParams(lp);

            // Styling: Selected clip receives white boundary highlight
            clipBlock.setBackgroundColor(isSelected ? Color.parseColor("#3A3B44") : Color.parseColor("#2A2B32"));
            if (isSelected) {
                View border = new View(this);
                FrameLayout.LayoutParams borderLp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                border.setLayoutParams(borderLp);
                border.setBackgroundColor(Color.parseColor("#66FFFFFF"));
                clipBlock.addView(border);
            }

            TextView label = new TextView(this);
            label.setText(String.format("Clip %d (%.1fs)", i + 1, Timebase.usToSeconds(clip.getTimelineDurationUs())));
            label.setTextColor(Color.WHITE);
            label.setTextSize(11.0f);
            label.setGravity(Gravity.CENTER);
            clipBlock.addView(label);

            final String clipId = clip.getId();
            clipBlock.setOnClickListener(v -> selectClip("track_main", clipId));

            trackMainContainer.addView(clipBlock);
        }
    }

    private void renderAudioTrack() {
        trackAudioContainer.removeAllViews();
        Track audioTrack = TimelineEngine.findTrack(project, "track_audio");
        if (audioTrack == null) return;

        float density = getResources().getDisplayMetrics().density;
        for (ClipSegment clip : audioTrack.getClips()) {
            int widthPx = (int) (Timebase.usToSeconds(clip.getTimelineDurationUs()) * PIXELS_PER_SECOND * density);
            widthPx = Math.max(widthPx, (int) (60 * density));

            TextView audioBlock = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(widthPx, (int) (28 * density));
            lp.setMargins(2, 0, 2, 0);
            audioBlock.setLayoutParams(lp);
            audioBlock.setBackgroundColor(Color.parseColor("#064E3B"));
            audioBlock.setTextColor(Color.parseColor("#10B981"));
            audioBlock.setText(" 🎵 BGM (Waveform ∿) ");
            audioBlock.setTextSize(10.0f);
            audioBlock.setGravity(Gravity.CENTER_VERTICAL);
            trackAudioContainer.addView(audioBlock);
        }
    }

    private void renderTextTrack() {
        trackTextContainer.removeAllViews();
        Track textTrack = TimelineEngine.findTrack(project, "track_text");
        if (textTrack == null) return;

        float density = getResources().getDisplayMetrics().density;
        for (ClipSegment clip : textTrack.getClips()) {
            int widthPx = (int) (Timebase.usToSeconds(clip.getTimelineDurationUs()) * PIXELS_PER_SECOND * density);
            widthPx = Math.max(widthPx, (int) (50 * density));

            TextView textBadge = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(widthPx, (int) (24 * density));
            lp.setMargins(2, 0, 2, 0);
            textBadge.setLayoutParams(lp);
            textBadge.setBackgroundColor(Color.parseColor("#78350F"));
            textBadge.setTextColor(Color.parseColor("#F59E0B"));
            textBadge.setText(clip.getTextContent() != null ? " T " + clip.getTextContent() : " T Subtitle ");
            textBadge.setTextSize(10.0f);
            textBadge.setSingleLine(true);
            textBadge.setGravity(Gravity.CENTER_VERTICAL);
            trackTextContainer.addView(textBadge);
        }
    }

    private void updateTimecodeDisplay() {
        tvPlayheadTimecode.setText(viewModel.getFormattedPlayheadTimecode());
        tvTotalTimecode.setText(viewModel.getFormattedTotalDurationTimecode());
    }

    private void openModalPanel(View panel) {
        modalPanelDock.setVisibility(View.VISIBLE);
        panelSpeed.setVisibility(View.GONE);
        panelVolume.setVisibility(View.GONE);
        panelAdjust.setVisibility(View.GONE);
        panelRatio.setVisibility(View.GONE);
        panelText.setVisibility(View.GONE);
        panelExport.setVisibility(View.GONE);

        panel.setVisibility(View.VISIBLE);
    }

    private void closeModalPanel() {
        modalPanelDock.setVisibility(View.GONE);
    }

    private void setupColorSlider(SeekBar seekBar, TextView label, String name, FloatConsumer consumer) {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                float val = ((float) progress - 50.0f) / 50.0f; // Range: [-1.0, 1.0]
                label.setText(String.format("%s: %.2f", name, val));
                consumer.accept(val);
                tvCanvasPreview.setText(String.format("Preview Canvas\n[Color Adjusted: %s]", name));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    private void showOptionsPopup(String title, String[] options, IndexConsumer onSelect) {
        PopupMenu popup = new PopupMenu(this, btnResolutionPill);
        for (int i = 0; i < options.length; i++) {
            popup.getMenu().add(0, i, i, options[i]);
        }
        popup.setOnMenuItemClickListener(item -> {
            onSelect.accept(item.getItemId());
            return true;
        });
        popup.show();
    }

    @FunctionalInterface
    interface FloatConsumer {
        void accept(float value);
    }

    @FunctionalInterface
    interface IndexConsumer {
        void accept(int index);
    }
}
