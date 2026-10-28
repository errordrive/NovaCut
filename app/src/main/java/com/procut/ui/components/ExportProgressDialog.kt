package com.procut.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.procut.export.ExportProgress
import com.procut.ui.theme.*
import java.io.File

@Composable
fun ExportProgressDialog(
    isExporting: Boolean,
    progress: ExportProgress?,
    resultFile: File?,
    errorMessage: String?,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onShare: (File) -> Unit
) {
    Dialog(
        onDismissRequest = {
            if (!isExporting) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isExporting,
            dismissOnClickOutside = !isExporting
        )
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = StudioDarkSurface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    isExporting -> {
                        // 1. Exporting State
                        val currentProgress = progress?.progress ?: 0f
                        val percent = (currentProgress * 100).toInt()

                        Text(
                            text = "Exporting Video",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = StudioWhite
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Circular Progress with percent in center
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(140.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { currentProgress },
                                modifier = Modifier.fillMaxSize(),
                                color = StudioAccentYellow,
                                trackColor = StudioDarkBg,
                                strokeWidth = 8.dp
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$percent%",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudioWhite
                                )
                                Text(
                                    text = "Rendering",
                                    fontSize = 11.sp,
                                    color = StudioTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Frame Count & ETA
                        if (progress != null && progress.totalFrames > 0) {
                            Text(
                                text = "Frame ${progress.currentFrame} / ${progress.totalFrames}",
                                color = StudioWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val etaSeconds = (progress.estimatedRemainingMs / 1000L).coerceAtLeast(0)
                            Text(
                                text = if (etaSeconds > 0) "Estimated time remaining: ${etaSeconds}s" else "Finalizing file...",
                                color = StudioTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedButton(
                            onClick = onCancel,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel Export")
                        }
                    }

                    resultFile != null -> {
                        // 2. Success State
                        val fileSizeBytes = resultFile.length()
                        val fileSizeMb = fileSizeBytes / (1024.0 * 1024.0)

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Export Complete!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = StudioWhite
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${resultFile.name} (%.1f MB)".format(fileSizeMb),
                            color = StudioTextSecondary,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioWhite),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Done")
                            }

                            Button(
                                onClick = { onShare(resultFile) },
                                modifier = Modifier.weight(1.2f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudioAccentYellow,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    errorMessage != null -> {
                        // 3. Error State
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF5252).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Export Failed",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = Color(0xFFFF5252)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = errorMessage,
                            color = StudioTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioDarkBg,
                                contentColor = StudioWhite
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}
