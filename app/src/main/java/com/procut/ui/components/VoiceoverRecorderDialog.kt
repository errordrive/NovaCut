package com.procut.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.procut.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun VoiceoverRecorderDialog(
    onRecordFinished: (durationUs: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationMs by remember { mutableStateOf(0L) }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRecording) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(isRecording) {
        if (isRecording) {
            val startMs = System.currentTimeMillis()
            while (isRecording) {
                delay(50)
                recordingDurationMs = System.currentTimeMillis() - startMs
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isRecording) onDismiss()
        },
        containerColor = StudioDarkSurface,
        title = {
            Text(
                text = if (isRecording) "Recording Voiceover..." else "Voiceover Studio",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = StudioWhite
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Timer
                val sec = recordingDurationMs / 1000
                val ms = (recordingDurationMs % 1000) / 100
                Text(
                    text = String.format("00:%02d.%d", sec, ms),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecording) Color(0xFFFF5252) else StudioWhite
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Microphone Circular Button
                Box(contentAlignment = Alignment.Center) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Color(0x33FF5252))
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (isRecording) Color(0xFFFF5252) else StudioCyanAccent,
                        modifier = Modifier
                            .size(68.dp)
                            .clickable {
                                if (isRecording) {
                                    isRecording = false
                                    onRecordFinished(recordingDurationMs * 1000L)
                                } else {
                                    isRecording = true
                                    recordingDurationMs = 0L
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isRecording) "Stop" else "Record",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isRecording) "Tap to Stop & Place on Timeline" else "Tap Mic to Start Recording",
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioGrey
                )
            }
        },
        confirmButton = {
            if (!isRecording) {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = StudioGrey)
                }
            }
        }
    )
}
