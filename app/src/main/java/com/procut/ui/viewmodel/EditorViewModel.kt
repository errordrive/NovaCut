package com.procut.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.procut.engine.KeyframeEvaluator
import com.procut.engine.TimelineEngine
import com.procut.export.ExportProgress
import com.procut.export.HardwareExportEngine
import com.procut.history.UndoRedoStack
import com.procut.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

class EditorViewModel(
    initialProject: EditorProject = createDefaultDemoProject()
) : ViewModel() {

    private val engine = TimelineEngine()
    private val keyframeEvaluator = KeyframeEvaluator()
    val undoRedoStack = UndoRedoStack()

    private val _project = MutableStateFlow(initialProject)
    val project: StateFlow<EditorProject> = _project.asStateFlow()

    private val _playheadUs = MutableStateFlow(0L)
    val playheadUs: StateFlow<Long> = _playheadUs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _selectedTrackId = MutableStateFlow<String?>(null)
    val selectedTrackId: StateFlow<String?> = _selectedTrackId.asStateFlow()

    private val _selectedClipId = MutableStateFlow<String?>(null)
    val selectedClipId: StateFlow<String?> = _selectedClipId.asStateFlow()

    // Timeline zoom: dp per microsecond (default: 50 dp per 1 second = 0.00005f)
    private val _zoomScale = MutableStateFlow(0.00005f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    private val _exportConfig = MutableStateFlow(ExportConfig())
    val exportConfig: StateFlow<ExportConfig> = _exportConfig.asStateFlow()

    private val _showExportSettings = MutableStateFlow(false)
    val showExportSettings: StateFlow<Boolean> = _showExportSettings.asStateFlow()

    private val _showExportProgress = MutableStateFlow(false)
    val showExportProgress: StateFlow<Boolean> = _showExportProgress.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow<ExportProgress?>(null)
    val exportProgress: StateFlow<ExportProgress?> = _exportProgress.asStateFlow()

    private val _exportResultFile = MutableStateFlow<File?>(null)
    val exportResultFile: StateFlow<File?> = _exportResultFile.asStateFlow()

    private val _exportErrorMessage = MutableStateFlow<String?>(null)
    val exportErrorMessage: StateFlow<String?> = _exportErrorMessage.asStateFlow()

    private var exportJob: Job? = null

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private var playbackJob: Job? = null

    init {
        // Record initial state in undo stack
        undoRedoStack.push(_project.value, "Initial State")
        updateUndoRedoStatus()
    }

    fun loadProject(project: EditorProject) {
        pause()
        _project.value = project
        _playheadUs.value = 0L
        _selectedTrackId.value = null
        _selectedClipId.value = null
        undoRedoStack.clear()
        undoRedoStack.push(project, "Project Loaded")
        updateUndoRedoStatus()
    }

    fun setPlayhead(timeUs: Long) {
        val total = _project.value.totalDurationUs.coerceAtLeast(1_000_000L)
        _playheadUs.value = timeUs.coerceIn(0L, total)
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        _isPlaying.value = true
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val startTimeReal = System.currentTimeMillis()
            val startPlayhead = _playheadUs.value
            val total = _project.value.totalDurationUs

            while (_isPlaying.value) {
                delay(16) // ~60 FPS update
                val elapsedUs = (System.currentTimeMillis() - startTimeReal) * 1000L
                val current = startPlayhead + elapsedUs
                if (current >= total) {
                    _playheadUs.value = total
                    _isPlaying.value = false
                    break
                } else {
                    _playheadUs.value = current
                }
            }
        }
    }

    fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
    }

    fun selectClip(trackId: String?, clipId: String?) {
        _selectedTrackId.value = trackId
        _selectedClipId.value = clipId
    }

    fun setZoomScale(scale: Float) {
        // Clamp scale: min 0.000005f (~5 dp/s, 10 min view) to max 0.0005f (~500 dp/s, frame view)
        _zoomScale.value = scale.coerceIn(0.000005f, 0.0005f)
    }

    fun splitSelectedClip(): Boolean {
        val trackId = _selectedTrackId.value ?: return false
        val clipId = _selectedClipId.value ?: return false
        val tUs = _playheadUs.value

        return try {
            val newProj = engine.splitClip(_project.value, trackId, clipId, tUs)
            recordMutation(newProj, "Split Clip")
            true
        } catch (e: Exception) {
            false
        }
    }

    fun trimSelectedClipStart(newTargetStartUs: Long): Boolean {
        val trackId = _selectedTrackId.value ?: return false
        val clipId = _selectedClipId.value ?: return false

        return try {
            val newProj = engine.trimClipStart(_project.value, trackId, clipId, newTargetStartUs)
            recordMutation(newProj, "Trim Clip Start")
            true
        } catch (e: Exception) {
            false
        }
    }

    fun trimSelectedClipEnd(newTargetEndUs: Long): Boolean {
        val trackId = _selectedTrackId.value ?: return false
        val clipId = _selectedClipId.value ?: return false

        return try {
            val newProj = engine.trimClipEnd(_project.value, trackId, clipId, newTargetEndUs)
            recordMutation(newProj, "Trim Clip End")
            true
        } catch (e: Exception) {
            false
        }
    }

    fun deleteSelectedClip(): Boolean {
        val trackId = _selectedTrackId.value ?: return false
        val clipId = _selectedClipId.value ?: return false

        val newProj = engine.deleteClip(_project.value, trackId, clipId)
        recordMutation(newProj, "Delete Clip")
        _selectedClipId.value = null
        _selectedTrackId.value = null
        return true
    }

    fun updateClipTransform(transform: Transform2D) {
        val trackId = _selectedTrackId.value ?: return
        val clipId = _selectedClipId.value ?: return

        val proj = _project.value
        val updatedTracks = proj.tracks.map { track ->
            if (track.id == trackId) {
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) clip.copy(transform = transform) else clip
                })
            } else track
        }
        _project.value = proj.copy(tracks = updatedTracks)
    }

    fun commitTransformMutation() {
        recordMutation(_project.value, "Transform Clip")
    }

    fun toggleKeyframeAtPlayhead(property: KeyframeProperty) {
        val trackId = _selectedTrackId.value ?: return
        val clipId = _selectedClipId.value ?: return
        val clip = getSelectedClip() ?: return
        val tUs = _playheadUs.value

        // Value to attach
        val value = when (property) {
            KeyframeProperty.POSITION_X -> clip.transform.translationX
            KeyframeProperty.POSITION_Y -> clip.transform.translationY
            KeyframeProperty.SCALE_X -> clip.transform.scaleX
            KeyframeProperty.SCALE_Y -> clip.transform.scaleY
            KeyframeProperty.ROTATION_DEG -> clip.transform.rotationDeg
            KeyframeProperty.OPACITY -> clip.transform.opacity
            KeyframeProperty.VOLUME -> 1.0f
            KeyframeProperty.BRIGHTNESS -> clip.colorAdjustment.brightness
            KeyframeProperty.CONTRAST -> clip.colorAdjustment.contrast
            KeyframeProperty.SATURATION -> clip.colorAdjustment.saturation
            KeyframeProperty.EXPOSURE -> clip.colorAdjustment.exposure
            KeyframeProperty.SHARPEN -> clip.colorAdjustment.sharpen
            KeyframeProperty.HIGHLIGHTS -> clip.colorAdjustment.highlights
            KeyframeProperty.SHADOWS -> clip.colorAdjustment.shadows
            KeyframeProperty.TEMPERATURE -> clip.colorAdjustment.temperature
            KeyframeProperty.TINT -> clip.colorAdjustment.tint
            KeyframeProperty.VIGNETTE -> clip.colorAdjustment.vignette
            KeyframeProperty.FADE -> clip.colorAdjustment.fade
            KeyframeProperty.GRAIN -> clip.colorAdjustment.grain
        }

        val updatedClip = keyframeEvaluator.toggleKeyframe(clip, tUs, property, value)
        val proj = _project.value
        val updatedTracks = proj.tracks.map { track ->
            if (track.id == trackId) {
                track.copy(clips = track.clips.map { c ->
                    if (c.id == clipId) updatedClip else c
                })
            } else track
        }
        recordMutation(proj.copy(tracks = updatedTracks), "Toggle Keyframe")
    }

    fun hasKeyframeAtPlayhead(property: KeyframeProperty): Boolean {
        val clip = getSelectedClip() ?: return false
        return keyframeEvaluator.hasKeyframeAt(clip, _playheadUs.value, property)
    }

    fun updateResolution(res: ExportResolution) {
        _exportConfig.value = _exportConfig.value.copy(resolution = res)
    }

    fun updateAudioConfig(audioConfig: AudioConfig) {
        val trackId = _selectedTrackId.value ?: return
        val clipId = _selectedClipId.value ?: return

        val proj = _project.value
        val updatedTracks = proj.tracks.map { track ->
            if (track.id == trackId) {
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) clip.copy(audioConfig = audioConfig) else clip
                })
            } else track
        }
        recordMutation(proj.copy(tracks = updatedTracks), "Update Audio Settings")
    }

    fun updateSpeedConfig(speedConfig: SpeedConfig) {
        val trackId = _selectedTrackId.value ?: return
        val clipId = _selectedClipId.value ?: return

        val proj = _project.value
        val updatedTracks = proj.tracks.map { track ->
            if (track.id == trackId) {
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) {
                        val newTargetDur = speedConfig.computeTargetDurationUs(clip.sourceTimeRangeUs.durationUs)
                        clip.copy(
                            speedConfig = speedConfig,
                            targetTimeRangeUs = clip.targetTimeRangeUs.copy(durationUs = newTargetDur)
                        )
                    } else clip
                })
            } else track
        }
        recordMutation(proj.copy(tracks = updatedTracks), "Update Clip Speed")
    }

    fun updateTextConfig(textConfig: TextConfig) {
        val trackId = _selectedTrackId.value ?: return
        val clipId = _selectedClipId.value ?: return

        val proj = _project.value
        val updatedTracks = proj.tracks.map { track ->
            if (track.id == trackId) {
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) clip.copy(textConfig = textConfig) else clip
                })
            } else track
        }
        recordMutation(proj.copy(tracks = updatedTracks), "Update Text Settings")
    }

    fun extractAudioFromClip() {
        val trackId = _selectedTrackId.value ?: return
        val clipId = _selectedClipId.value ?: return
        val clip = getSelectedClip() ?: return

        val proj = _project.value
        val audioTrack = proj.tracks.firstOrNull { it.type == TrackType.AUDIO }
        val newAudioClip = ClipSegment(
            id = UUID.randomUUID().toString(),
            assetId = clip.assetId,
            name = "Audio - ${clip.name}",
            sourceTimeRangeUs = clip.sourceTimeRangeUs,
            targetTimeRangeUs = clip.targetTimeRangeUs
        )

        val updatedTracks = if (audioTrack != null) {
            proj.tracks.map { t ->
                if (t.id == audioTrack.id) t.copy(clips = t.clips + newAudioClip) else t
            }
        } else {
            val newTrack = Track(
                id = UUID.randomUUID().toString(),
                type = TrackType.AUDIO,
                name = "Extracted Audio",
                clips = listOf(newAudioClip)
            )
            proj.tracks + newTrack
        }

        recordMutation(proj.copy(tracks = updatedTracks), "Extract Audio")
    }

    fun detectBeatsForSelectedClip() {
        val clip = getSelectedClip() ?: return
        val waveform = com.procut.ui.media.WaveformProvider.getWaveform(clip.assetId, clip.sourceTimeRangeUs.durationUs)
        val detectedBeats = com.procut.audio.BeatDetector.detectBeats(waveform)
        updateAudioConfig(clip.audioConfig.copy(beatMarkers = detectedBeats))
    }

    fun addVoiceoverClip(durationUs: Long) {
        val proj = _project.value
        val playhead = _playheadUs.value
        val newVoiceClip = ClipSegment(
            id = UUID.randomUUID().toString(),
            assetId = "voiceover_${System.currentTimeMillis()}",
            name = "Voiceover",
            sourceTimeRangeUs = TimeRangeUs(0L, durationUs),
            targetTimeRangeUs = TimeRangeUs(playhead, durationUs)
        )

        val audioTrack = proj.tracks.firstOrNull { it.type == TrackType.AUDIO }
        val updatedTracks = if (audioTrack != null) {
            proj.tracks.map { t ->
                if (t.id == audioTrack.id) t.copy(clips = t.clips + newVoiceClip) else t
            }
        } else {
            val newTrack = Track(
                id = UUID.randomUUID().toString(),
                type = TrackType.AUDIO,
                name = "Voiceover Track",
                clips = listOf(newVoiceClip)
            )
            proj.tracks + newTrack
        }

        recordMutation(proj.copy(tracks = updatedTracks), "Add Voiceover")
    }

    fun addTextClip() {
        val proj = _project.value
        val playhead = _playheadUs.value
        val durationUs = 3_000_000L
        val textClip = ClipSegment(
            id = UUID.randomUUID().toString(),
            assetId = "text_${System.currentTimeMillis()}",
            name = "Text Title",
            sourceTimeRangeUs = TimeRangeUs(0L, durationUs),
            targetTimeRangeUs = TimeRangeUs(playhead, durationUs),
            textConfig = TextConfig()
        )

        val textTrack = proj.tracks.firstOrNull { it.type == TrackType.TEXT }
        val updatedTracks = if (textTrack != null) {
            proj.tracks.map { t ->
                if (t.id == textTrack.id) t.copy(clips = t.clips + textClip) else t
            }
        } else {
            val newTrack = Track(
                id = UUID.randomUUID().toString(),
                type = TrackType.TEXT,
                name = "Text Track",
                clips = listOf(textClip)
            )
            proj.tracks + newTrack
        }

        recordMutation(proj.copy(tracks = updatedTracks), "Add Text")
    }

    fun duplicateSelectedClip() {
        val trackId = _selectedTrackId.value ?: return
        val clip = getSelectedClip() ?: return

        val proj = _project.value
        val newClip = clip.copy(
            id = UUID.randomUUID().toString(),
            name = "${clip.name} (Copy)",
            targetTimeRangeUs = clip.targetTimeRangeUs.copy(
                startUs = clip.targetTimeRangeUs.endUs
            )
        )

        val updatedTracks = proj.tracks.map { track ->
            if (track.id == trackId) {
                track.copy(clips = track.clips + newClip)
            } else track
        }
        recordMutation(proj.copy(tracks = updatedTracks), "Duplicate Clip")
    }

    fun updateFps(fps: Int) {
        _exportConfig.value = _exportConfig.value.copy(frameRate = fps)
    }

    fun openExportSettings() {
        _showExportSettings.value = true
    }

    fun closeExportSettings() {
        _showExportSettings.value = false
    }

    fun startExport(context: Context, config: ExportConfig) {
        _exportConfig.value = config
        _showExportSettings.value = false
        _showExportProgress.value = true
        _isExporting.value = true
        _exportProgress.value = null
        _exportResultFile.value = null
        _exportErrorMessage.value = null

        val outputDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "Exports")
        val outputFile = File(outputDir, "ProCut_${System.currentTimeMillis()}.mp4")

        val engine = HardwareExportEngine(context)

        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            try {
                val exported = engine.exportProject(
                    project = _project.value,
                    config = config,
                    outputFile = outputFile,
                    onProgress = { progress ->
                        _exportProgress.value = progress
                    }
                )
                _isExporting.value = false
                _exportResultFile.value = exported
            } catch (ce: CancellationException) {
                _isExporting.value = false
                _exportErrorMessage.value = "Export was cancelled."
            } catch (t: Throwable) {
                _isExporting.value = false
                _exportErrorMessage.value = t.localizedMessage ?: "Export failed with unknown error."
            }
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
        _isExporting.value = false
        _exportErrorMessage.value = "Export cancelled."
    }

    fun dismissExportDialog() {
        _showExportProgress.value = false
        _exportResultFile.value = null
        _exportErrorMessage.value = null
        _exportProgress.value = null
    }

    fun updateAspectRatio(ratio: AspectRatio) {
        val proj = _project.value
        val newProj = proj.copy(canvasConfig = proj.canvasConfig.copy(aspectRatio = ratio))
        recordMutation(newProj, "Change Aspect Ratio")
    }

    fun renameProject(newName: String) {
        val proj = _project.value.copy(name = newName)
        recordMutation(proj, "Rename Project")
    }

    fun undo(): Boolean {
        val result = undoRedoStack.undo(_project.value) ?: return false
        _project.value = result.first
        updateUndoRedoStatus()
        return true
    }

    fun redo(): Boolean {
        val result = undoRedoStack.redo(_project.value) ?: return false
        _project.value = result.first
        updateUndoRedoStatus()
        return true
    }

    fun getSelectedClip(): ClipSegment? {
        val trackId = _selectedTrackId.value ?: return null
        val clipId = _selectedClipId.value ?: return null
        return _project.value.tracks
            .firstOrNull { it.id == trackId }
            ?.clips
            ?.firstOrNull { it.id == clipId }
    }

    private fun recordMutation(newProject: EditorProject, actionLabel: String) {
        undoRedoStack.push(newProject, actionLabel)
        _project.value = newProject
        updateUndoRedoStatus()
    }

    private fun updateUndoRedoStatus() {
        _canUndo.value = undoRedoStack.canUndo
        _canRedo.value = undoRedoStack.canRedo
    }

    companion object {
        fun createDefaultDemoProject(): EditorProject {
            val mainClip1 = ClipSegment(
                id = "clip_main_1",
                assetId = "asset_video_1",
                name = "Intro Scene.mp4",
                sourceTimeRangeUs = TimeRangeUs(0L, 5_000_000L),
                targetTimeRangeUs = TimeRangeUs(0L, 5_000_000L)
            )
            val mainClip2 = ClipSegment(
                id = "clip_main_2",
                assetId = "asset_video_2",
                name = "City Action.mp4",
                sourceTimeRangeUs = TimeRangeUs(0L, 7_000_000L),
                targetTimeRangeUs = TimeRangeUs(5_000_000L, 7_000_000L),
                transitionIn = Transition(type = TransitionType.DISSOLVE, durationUs = 500_000L)
            )
            val mainClip3 = ClipSegment(
                id = "clip_main_3",
                assetId = "asset_video_3",
                name = "Outro Sunset.mp4",
                sourceTimeRangeUs = TimeRangeUs(0L, 4_000_000L),
                targetTimeRangeUs = TimeRangeUs(12_000_000L, 4_000_000L),
                transitionIn = Transition(type = TransitionType.GLITCH, durationUs = 600_000L)
            )

            val pipClip1 = ClipSegment(
                id = "clip_pip_1",
                assetId = "asset_pip_1",
                name = "Reaction Facecam.mp4",
                sourceTimeRangeUs = TimeRangeUs(0L, 4_000_000L),
                targetTimeRangeUs = TimeRangeUs(2_000_000L, 4_000_000L),
                transform = Transform2D(translationX = 0.35f, translationY = -0.35f, scaleX = 0.35f, scaleY = 0.35f)
            )

            val audioClip1 = ClipSegment(
                id = "clip_audio_1",
                assetId = "asset_audio_1",
                name = "Cinematic Beat.mp3",
                sourceTimeRangeUs = TimeRangeUs(0L, 16_000_000L),
                targetTimeRangeUs = TimeRangeUs(0L, 16_000_000L)
            )

            val textClip1 = ClipSegment(
                id = "clip_text_1",
                assetId = "asset_text_1",
                name = "Pro Studio Title",
                sourceTimeRangeUs = TimeRangeUs(0L, 3_000_000L),
                targetTimeRangeUs = TimeRangeUs(1_000_000L, 3_000_000L)
            )

            return EditorProject(
                id = UUID.randomUUID().toString(),
                name = "Cyberpunk Cinematic Edit",
                canvasConfig = CanvasConfig(aspectRatio = AspectRatio.RATIO_9_16),
                tracks = listOf(
                    Track(id = "track_main", type = TrackType.MAIN_VIDEO, name = "Main Video", clips = listOf(mainClip1, mainClip2, mainClip3)),
                    Track(id = "track_pip", type = TrackType.PIP_OVERLAY, name = "Overlay PIP", clips = listOf(pipClip1)),
                    Track(id = "track_text", type = TrackType.TEXT, name = "Text & Titles", clips = listOf(textClip1)),
                    Track(id = "track_audio", type = TrackType.AUDIO, name = "Soundtrack", clips = listOf(audioClip1))
                )
            )
        }
    }
}
