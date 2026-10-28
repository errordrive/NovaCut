package com.procut.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.procut.model.CanvasConfig
import com.procut.model.ClipSegment
import com.procut.model.KeyframeProperty
import com.procut.model.Transform2D
import com.procut.ui.theme.*

@Composable
fun PreviewViewport(
    canvasConfig: CanvasConfig,
    playheadUs: Long,
    totalDurationUs: Long,
    isPlaying: Boolean,
    selectedClip: ClipSegment?,
    hasKeyframeAtPlayhead: Boolean,
    onPlayPauseClick: () -> Unit,
    onJumpToStart: () -> Unit,
    onJumpToEnd: () -> Unit,
    onToggleKeyframe: (KeyframeProperty) -> Unit,
    onTransformChange: (Transform2D) -> Unit,
    onTransformCommit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioDarkBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Preview Monitor Area (Letterboxed/Pillarboxed)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val containerW = maxWidth.value
            val containerH = maxHeight.value

            val targetRatio = canvasConfig.aspectRatio.floatRatio
            val containerRatio = containerW / containerH

            val (surfaceWVal, surfaceHVal) = if (containerRatio > targetRatio) {
                // Pillarbox: container is wider than canvas
                (containerH * targetRatio) to containerH
            } else {
                // Letterbox: container is taller than canvas
                containerW to (containerW / targetRatio)
            }
            val surfaceW = surfaceWVal.dp
            val surfaceH = surfaceHVal.dp

            // The actual canvas container
            Box(
                modifier = Modifier
                    .size(surfaceW, surfaceH)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black)
                    .border(1.dp, StudioDarkGrey.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            ) {
                // Background visual representation
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PREVIEW · ${canvasConfig.aspectRatio.label}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = StudioGrey.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // If a clip is selected and visible, overlay the interactive bounding box & transform controls
                if (selectedClip != null && selectedClip.isLayerVisibleAt(playheadUs)) {
                    CanvasTransformOverlay(
                        transform = selectedClip.transform,
                        onTransformChange = onTransformChange,
                        onTransformCommit = onTransformCommit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // 2. Playback Control & Timecode Bar
        Surface(
            color = StudioDarkSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Current Playhead & Total Timecode (HH:MM:SS:FF)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTimecode(playheadUs),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = StudioWhite
                        )
                    )
                    Text(
                        text = " / ${formatTimecode(totalDurationUs)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = StudioGrey
                        )
                    )
                }

                // Center Playback Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Jump to Start
                    IconButton(onClick = onJumpToStart, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Jump to Start",
                            tint = StudioWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Play / Pause Circular Button
                    Surface(
                        shape = CircleShape,
                        color = StudioCyanAccent,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onPlayPauseClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Jump to End
                    IconButton(onClick = onJumpToEnd, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Jump to End",
                            tint = StudioWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Right: Keyframe Toggle Button (Diamond indicator)
                IconButton(
                    onClick = { onToggleKeyframe(KeyframeProperty.POSITION_X) },
                    enabled = selectedClip != null,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = "Keyframe",
                        tint = when {
                            selectedClip == null -> StudioDarkGrey
                            hasKeyframeAtPlayhead -> StudioKeyframeDiamond
                            else -> StudioWhite
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

fun formatTimecode(timeUs: Long, fps: Int = 30): String {
    val totalSeconds = (timeUs / 1_000_000L).coerceAtLeast(0L)
    val remainingUs = timeUs % 1_000_000L
    val frames = ((remainingUs / 1_000_000.0) * fps).toInt().coerceIn(0, fps - 1)

    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return String.format("%02d:%02d:%02d:%02d", hours, minutes, seconds, frames)
}
