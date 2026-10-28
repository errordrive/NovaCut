package com.procut.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.procut.model.*
import com.procut.ui.media.ThumbnailProvider
import com.procut.ui.media.WaveformProvider
import com.procut.ui.theme.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToLong

@Composable
fun TimelineTrackView(
    track: Track,
    isSelectedTrack: Boolean,
    selectedClipId: String?,
    playheadUs: Long,
    zoomScaleDpPerUs: Float,
    onClipSelect: (String) -> Unit,
    onClipTrimStart: (String, Long) -> Unit,
    onClipTrimEnd: (String, Long) -> Unit,
    onTransitionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val trackHeight = when (track.type) {
        TrackType.MAIN_VIDEO -> 54.dp
        TrackType.PIP_OVERLAY -> 48.dp
        TrackType.AUDIO -> 42.dp
        TrackType.TEXT -> 36.dp
        TrackType.EFFECT -> 32.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .background(StudioTrackBackground)
    ) {
        // Render clips in this track
        track.clips.forEachIndexed { index, clip ->
            val startDp = (clip.targetTimeRangeUs.startUs * zoomScaleDpPerUs).dp
            val widthDp = (clip.targetTimeRangeUs.durationUs * zoomScaleDpPerUs).dp.coerceAtLeast(36.dp)
            val isSelected = (clip.id == selectedClipId)

            val clipBgColor = when (track.type) {
                TrackType.MAIN_VIDEO -> StudioClipMain
                TrackType.PIP_OVERLAY -> StudioClipOverlay
                TrackType.AUDIO -> StudioClipAudio
                TrackType.TEXT -> StudioClipText
                TrackType.EFFECT -> StudioClipEffect
            }

            Box(
                modifier = Modifier
                    .offset(x = startDp)
                    .width(widthDp)
                    .fillMaxHeight()
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(clipBgColor)
                    .then(
                        if (isSelected) Modifier.border(2.dp, StudioSelectedBorder, RoundedCornerShape(6.dp))
                        else Modifier.border(0.5.dp, StudioDarkGrey.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    )
                    .clickable { onClipSelect(clip.id) }
            ) {
                // 1. Content: Filmstrip thumbnails for video, or Waveform for audio
                when (track.type) {
                    TrackType.MAIN_VIDEO, TrackType.PIP_OVERLAY -> {
                        ClipFilmstrip(
                            clip = clip,
                            clipWidthDp = widthDp,
                            trackHeightDp = trackHeight
                        )
                    }
                    TrackType.AUDIO -> {
                        ClipWaveform(
                            clip = clip,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> Unit
                }

                // 2. Clip Title & Duration overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = clip.name,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = StudioWhite
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "${String.format("%.1f", clip.targetTimeRangeUs.durationUs / 1_000_000.0)}s",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = StudioGrey
                        )
                    )
                }

                // 3. Keyframe Diamond Markers
                if (clip.keyframes.isNotEmpty()) {
                    clip.keyframes.forEach { kf ->
                        val kfOffsetUs = kf.timeOffsetUs
                        if (kfOffsetUs in 0L..clip.targetTimeRangeUs.durationUs) {
                            val kfOffsetDp = (kfOffsetUs * zoomScaleDpPerUs).dp
                            Icon(
                                imageVector = Icons.Default.Diamond,
                                contentDescription = null,
                                tint = StudioKeyframeDiamond,
                                modifier = Modifier
                                    .offset(x = kfOffsetDp - 6.dp, y = trackHeight - 16.dp)
                                    .size(12.dp)
                            )
                        }
                    }
                }

                // 4. Trim Handles when selected
                if (isSelected) {
                    // Left Trim Handle (<)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(16.dp)
                            .fillMaxHeight()
                            .background(StudioTrimHandle)
                            .pointerInput(clip.id) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    change.consume()
                                    val deltaUs = (dragAmount / (zoomScaleDpPerUs * density.density)).roundToLong()
                                    var targetStartUs = clip.targetTimeRangeUs.startUs + deltaUs

                                    // Magnetic snap to playhead (100 ms threshold)
                                    if (abs(targetStartUs - playheadUs) < 100_000L) {
                                        targetStartUs = playheadUs
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    onClipTrimStart(clip.id, targetStartUs)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "‹",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                fontSize = 14.sp
                            )
                        )
                    }

                    // Right Trim Handle (>)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(16.dp)
                            .fillMaxHeight()
                            .background(StudioTrimHandle)
                            .pointerInput(clip.id) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    change.consume()
                                    val deltaUs = (dragAmount / (zoomScaleDpPerUs * density.density)).roundToLong()
                                    var targetEndUs = clip.targetTimeRangeUs.endUs + deltaUs

                                    // Magnetic snap to playhead (100 ms threshold)
                                    if (abs(targetEndUs - playheadUs) < 100_000L) {
                                        targetEndUs = playheadUs
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    onClipTrimEnd(clip.id, targetEndUs)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "›",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }

            // 5. Transition badge between consecutive clips on Main Video track
            if (track.type == TrackType.MAIN_VIDEO && clip.transitionIn != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = StudioTransitionBadge,
                    modifier = Modifier
                        .offset(x = startDp - 10.dp, y = (trackHeight / 2) - 10.dp)
                        .size(20.dp)
                        .clickable { onTransitionClick(clip.id) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "⧖",
                            color = StudioWhite,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipFilmstrip(
    clip: ClipSegment,
    clipWidthDp: androidx.compose.ui.unit.Dp,
    trackHeightDp: androidx.compose.ui.unit.Dp
) {
    val tileWidthDp = 44.dp
    val tileCount = ceil(clipWidthDp / tileWidthDp).toInt().coerceIn(1, 40)

    Row(modifier = Modifier.fillMaxSize()) {
        for (i in 0 until tileCount) {
            val sampleUs = clip.sourceTimeRangeUs.startUs + (i * 500_000L)
            var thumbBitmap by remember(clip.id, i) { mutableStateOf<Bitmap?>(null) }

            LaunchedEffect(clip.assetId, sampleUs) {
                ThumbnailProvider.getThumbnail(clip.assetId, sampleUs) { bmp ->
                    thumbBitmap = bmp
                }
            }

            Box(
                modifier = Modifier
                    .width(tileWidthDp)
                    .fillMaxHeight()
                    .padding(end = 1.dp)
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                thumbBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun ClipWaveform(
    clip: ClipSegment,
    modifier: Modifier = Modifier
) {
    val waveform = remember(clip.id, clip.sourceTimeRangeUs.durationUs) {
        WaveformProvider.getWaveform(clip.assetId, clip.sourceTimeRangeUs.durationUs)
    }

    Canvas(modifier = modifier) {
        val count = waveform.size
        val barWidth = size.width / count
        val centerY = size.height / 2f

        for (i in 0 until count) {
            val amp = waveform[i]
            val barH = size.height * amp * 0.8f
            val x = i * barWidth

            drawLine(
                color = StudioCyanAccent.copy(alpha = 0.7f),
                start = Offset(x, centerY - (barH / 2f)),
                end = Offset(x, centerY + (barH / 2f)),
                strokeWidth = barWidth * 0.7f
            )
        }
    }
}
