package com.procut.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.procut.model.EditorProject
import com.procut.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

@Composable
fun TimelineContainer(
    project: EditorProject,
    playheadUs: Long,
    zoomScaleDpPerUs: Float,
    selectedTrackId: String?,
    selectedClipId: String?,
    onPlayheadChange: (Long) -> Unit,
    onZoomScaleChange: (Float) -> Unit,
    onClipSelect: (String, String) -> Unit,
    onClipTrim: (String, String, Long, Long) -> Unit,
    onTransitionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val horizontalScrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioDarkBackground)
    ) {
        val viewportWidthPx = constraints.maxWidth.toFloat()
        val centerPaddingPx = viewportWidthPx / 2f
        val centerPaddingDp = with(density) { centerPaddingPx.toDp() }

        val pxPerUs = zoomScaleDpPerUs * density.density
        val totalTrackWidthDp = with(density) {
            (project.totalDurationUs * pxPerUs).toDp()
        }

        // Sync scroll state with playhead during playback
        LaunchedEffect(playheadUs, zoomScaleDpPerUs) {
            val targetScrollPx = (playheadUs * pxPerUs).toInt()
            if (kotlin.math.abs(horizontalScrollState.value - targetScrollPx) > 3) {
                horizontalScrollState.scrollTo(targetScrollPx)
            }
        }

        // Report scroll changes back to playhead when user drags timeline
        LaunchedEffect(horizontalScrollState.value) {
            if (horizontalScrollState.isScrollInProgress) {
                val newPlayheadUs = (horizontalScrollState.value / pxPerUs).roundToLong()
                onPlayheadChange(newPlayheadUs)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Pinch to zoom scaling
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        if (zoom != 1.0f) {
                            val newScale = zoomScaleDpPerUs * zoom
                            onZoomScaleChange(newScale)
                        }
                    }
                }
        ) {
            // 1. Time Ruler at Top
            TimelineRuler(
                totalDurationUs = project.totalDurationUs,
                zoomScaleDpPerUs = zoomScaleDpPerUs,
                scrollOffsetPx = horizontalScrollState.value.toFloat(),
                viewportWidthPx = viewportWidthPx,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = StudioDarkGrey.copy(alpha = 0.3f), thickness = 0.5.dp)

            // 2. Vertically Stacked Track Lanes with Horizontal Scroller
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Scrollable Tracks Container
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(horizontalScrollState)
                ) {
                    // Left spacer: W/2 padding so timestamp 0 aligns with centered playhead
                    Spacer(modifier = Modifier.width(centerPaddingDp))

                    // Track stack
                    Column(
                        modifier = Modifier
                            .width(totalTrackWidthDp.coerceAtLeast(100.dp))
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        project.tracks.forEach { track ->
                            TimelineTrackView(
                                track = track,
                                isSelectedTrack = (track.id == selectedTrackId),
                                selectedClipId = selectedClipId,
                                playheadUs = playheadUs,
                                zoomScaleDpPerUs = zoomScaleDpPerUs,
                                onClipSelect = { clipId -> onClipSelect(track.id, clipId) },
                                onClipTrim = { clipId, startUs, durUs ->
                                    onClipTrim(track.id, clipId, startUs, durUs)
                                },
                                onTransitionClick = onTransitionClick
                            )
                        }
                    }

                    // Right spacer: W/2 padding so end of project aligns with centered playhead
                    Spacer(modifier = Modifier.width(centerPaddingDp + 100.dp))
                }

                // 3. Stationary Centered Playhead Line (Overlayed at X = W/2)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(StudioPlayhead)
                ) {
                    // Top Playhead Pointer Marker (Inverted Triangle)
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(14.dp, 10.dp)
                            .offset(y = (-1).dp)
                    ) {
                        val path = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width / 2f, size.height)
                            close()
                        }
                        drawPath(path, color = StudioPlayhead)
                    }
                }
            }
        }
    }
}
