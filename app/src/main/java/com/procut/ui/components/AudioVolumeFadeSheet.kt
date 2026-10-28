package com.procut.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.procut.model.AudioConfig
import com.procut.model.AudioFadeConfig
import com.procut.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioVolumeFadeSheet(
    currentConfig: AudioConfig,
    clipDurationUs: Long,
    onConfigChange: (AudioConfig) -> Unit,
    onDetectBeats: () -> Unit,
    onDismiss: () -> Unit
) {
    var volume by remember { mutableStateOf(currentConfig.volume) }
    var fadeInSec by remember { mutableStateOf(currentConfig.fade.fadeInDurationUs / 1_000_000f) }
    var fadeOutSec by remember { mutableStateOf(currentConfig.fade.fadeOutDurationUs / 1_000_000f) }
    val maxFadeSec = (clipDurationUs / 1_000_000f / 2f).coerceAtLeast(1.0f).coerceAtMost(10.0f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioDarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = StudioDarkGrey) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Audio & Volume Controls",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = StudioWhite
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Volume Slider (0% to 200%)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Volume", style = MaterialTheme.typography.bodyMedium, color = StudioGrey)
                    Text(
                        text = "${(volume * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = StudioCyanAccent
                    )
                }
                Slider(
                    value = volume,
                    onValueChange = {
                        volume = it
                        onConfigChange(currentConfig.copy(volume = it))
                    },
                    valueRange = 0.0f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = StudioCyanAccent,
                        activeTrackColor = StudioCyanAccent,
                        inactiveTrackColor = StudioDarkSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Fade In Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Fade In Duration", style = MaterialTheme.typography.bodyMedium, color = StudioGrey)
                    Text(
                        text = "${String.format("%.1f", fadeInSec)}s",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = StudioCyanAccent
                    )
                }
                Slider(
                    value = fadeInSec,
                    onValueChange = {
                        fadeInSec = it
                        val newFade = currentConfig.fade.copy(fadeInDurationUs = (it * 1_000_000L).toLong())
                        onConfigChange(currentConfig.copy(fade = newFade))
                    },
                    valueRange = 0.0f..maxFadeSec,
                    colors = SliderDefaults.colors(
                        thumbColor = StudioCyanAccent,
                        activeTrackColor = StudioCyanAccent,
                        inactiveTrackColor = StudioDarkSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Fade Out Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Fade Out Duration", style = MaterialTheme.typography.bodyMedium, color = StudioGrey)
                    Text(
                        text = "${String.format("%.1f", fadeOutSec)}s",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = StudioCyanAccent
                    )
                }
                Slider(
                    value = fadeOutSec,
                    onValueChange = {
                        fadeOutSec = it
                        val newFade = currentConfig.fade.copy(fadeOutDurationUs = (it * 1_000_000L).toLong())
                        onConfigChange(currentConfig.copy(fade = newFade))
                    },
                    valueRange = 0.0f..maxFadeSec,
                    colors = SliderDefaults.colors(
                        thumbColor = StudioCyanAccent,
                        activeTrackColor = StudioCyanAccent,
                        inactiveTrackColor = StudioDarkSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Auto Beat Marker Detection Button
            Button(
                onClick = {
                    onDetectBeats()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioKeyframeDiamond,
                    contentColor = androidx.compose.ui.graphics.Color.Black
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Detect Rhythm Beats (Auto Markers)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
