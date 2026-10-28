package com.procut.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.procut.model.KeyframeProperty
import com.procut.model.TrackType
import com.procut.ui.components.*
import com.procut.ui.theme.StudioDarkBackground
import com.procut.ui.viewmodel.EditorViewModel

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBackToDrafts: () -> Unit = {}
) {
    val project by viewModel.project.collectAsState()
    val playheadUs by viewModel.playheadUs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val selectedTrackId by viewModel.selectedTrackId.collectAsState()
    val selectedClipId by viewModel.selectedClipId.collectAsState()
    val zoomScale by viewModel.zoomScale.collectAsState()
    val exportConfig by viewModel.exportConfig.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    var isFullscreenPreview by remember { mutableStateOf(false) }
    var showAspectRatioSheet by remember { mutableStateOf(false) }
    var transitionTargetClipId by remember { mutableStateOf<String?>(null) }

    val selectedClip = viewModel.getSelectedClip()
    val hasKeyframeAtPlayhead = viewModel.hasKeyframeAtPlayhead(KeyframeProperty.POSITION_X)

    Scaffold(
        topBar = {
            EditorTopBar(
                projectTitle = project.name,
                exportConfig = exportConfig,
                canUndo = canUndo,
                canRedo = canRedo,
                isFullscreen = isFullscreenPreview,
                onBackClick = onBackToDrafts,
                onRenameClick = { viewModel.renameProject(it) },
                onConfigChange = { res, fps ->
                    viewModel.updateResolution(res)
                    viewModel.updateFps(fps)
                },
                onUndoClick = { viewModel.undo() },
                onRedoClick = { viewModel.redo() },
                onFullscreenToggle = { isFullscreenPreview = !isFullscreenPreview },
                onExportClick = {
                    // Export dialog or notification
                }
            )
        },
        bottomBar = {
            if (!isFullscreenPreview) {
                BottomToolbar(
                    selectedClip = selectedClip,
                    onAction = { action ->
                        when (action) {
                            ToolbarAction.ClipDeselect -> viewModel.selectClip(null, null)
                            ToolbarAction.ClipSplit -> viewModel.splitSelectedClip()
                            ToolbarAction.ClipDelete -> viewModel.deleteSelectedClip()
                            ToolbarAction.ClipKeyframe -> viewModel.toggleKeyframeAtPlayhead(KeyframeProperty.POSITION_X)
                            ToolbarAction.RootAspectRatio -> showAspectRatioSheet = true
                            else -> Unit
                        }
                    }
                )
            }
        },
        containerColor = StudioDarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top Section: Preview Monitor Viewport
            PreviewViewport(
                canvasConfig = project.canvasConfig,
                playheadUs = playheadUs,
                totalDurationUs = project.totalDurationUs,
                isPlaying = isPlaying,
                selectedClip = selectedClip,
                hasKeyframeAtPlayhead = hasKeyframeAtPlayhead,
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onJumpToStart = { viewModel.setPlayhead(0L) },
                onJumpToEnd = { viewModel.setPlayhead(project.totalDurationUs) },
                onToggleKeyframe = { viewModel.toggleKeyframeAtPlayhead(it) },
                onTransformChange = { viewModel.updateClipTransform(it) },
                onTransformCommit = { viewModel.commitTransformMutation() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isFullscreenPreview) 1f else 0.52f)
            )

            // Bottom Section: Multi-Track Timeline & Ruler (hidden in fullscreen preview)
            if (!isFullscreenPreview) {
                TimelineContainer(
                    project = project,
                    playheadUs = playheadUs,
                    zoomScaleDpPerUs = zoomScale,
                    selectedTrackId = selectedTrackId,
                    selectedClipId = selectedClipId,
                    onPlayheadChange = { viewModel.setPlayhead(it) },
                    onZoomScaleChange = { viewModel.setZoomScale(it) },
                    onClipSelect = { trackId, clipId -> viewModel.selectClip(trackId, clipId) },
                    onClipTrim = { trackId, clipId, startUs, durUs ->
                        viewModel.trimSelectedClip(startUs, durUs)
                    },
                    onTransitionClick = { clipId ->
                        transitionTargetClipId = clipId
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.48f)
                )
            }
        }
    }

    // Modal Sheets
    if (showAspectRatioSheet) {
        AspectRatioSelectorSheet(
            currentRatio = project.canvasConfig.aspectRatio,
            onRatioSelected = { viewModel.updateAspectRatio(it) },
            onDismiss = { showAspectRatioSheet = false }
        )
    }

    if (transitionTargetClipId != null) {
        val targetClip = project.tracks
            .firstOrNull { it.type == TrackType.MAIN_VIDEO }
            ?.clips
            ?.firstOrNull { it.id == transitionTargetClipId }

        TransitionPickerDialog(
            currentTransition = targetClip?.transitionIn,
            onTransitionSelected = { _ ->
                // Apply transition update
                transitionTargetClipId = null
            },
            onDismiss = { transitionTargetClipId = null }
        )
    }
}
