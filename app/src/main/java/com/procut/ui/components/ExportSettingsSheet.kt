package com.procut.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.procut.model.*
import com.procut.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSettingsSheet(
    initialConfig: ExportConfig,
    durationUs: Long,
    aspectRatio: AspectRatio,
    onDismiss: () -> Unit,
    onStartExport: (ExportConfig) -> Unit
) {
    var selectedResolution by remember { mutableStateOf(initialConfig.resolution) }
    var selectedFps by remember { mutableStateOf(initialConfig.frameRate) }
    var selectedCodec by remember { mutableStateOf(initialConfig.codec) }
    var selectedBitrateLevel by remember { mutableStateOf(initialConfig.bitrateLevel) }

    val currentConfig = remember(selectedResolution, selectedFps, selectedCodec, selectedBitrateLevel) {
        ExportConfig(
            resolution = selectedResolution,
            frameRate = selectedFps,
            codec = selectedCodec,
            bitrateLevel = selectedBitrateLevel
        )
    }

    val (outputWidth, outputHeight) = currentConfig.calculateOutputDimensions(aspectRatio)
    val estimatedBytes = currentConfig.estimateOutputSizeBytes(durationUs)
    val estimatedMb = estimatedBytes / (1024.0 * 1024.0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioDarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VideoFile,
                        contentDescription = null,
                        tint = StudioAccentYellow,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Export Settings",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = StudioWhite
                    )
                }

                // Estimated File Size Badge
                Surface(
                    color = StudioDarkBg,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
                ) {
                    Text(
                        text = String.format("~%.1f MB", estimatedMb),
                        color = StudioAccentYellow,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Target Output: ${outputWidth} × ${outputHeight} px",
                color = StudioTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Resolution Selection
            Text(
                text = "RESOLUTION",
                color = StudioTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    ExportResolution.RES_720P to "720p",
                    ExportResolution.RES_1080P to "1080p",
                    ExportResolution.RES_2K to "2K",
                    ExportResolution.RES_4K to "4K"
                ).forEach { (res, label) ->
                    val isSelected = selectedResolution == res
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) StudioAccentYellow.copy(alpha = 0.15f) else StudioDarkBg)
                            .border(
                                width = 1.5.dp,
                                color = if (isSelected) StudioAccentYellow else StudioBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedResolution = res }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) StudioAccentYellow else StudioWhite,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Frame Rate (FPS)
            Text(
                text = "FRAME RATE",
                color = StudioTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(24 to "24 fps", 30 to "30 fps", 60 to "60 fps").forEach { (fps, label) ->
                    val isSelected = selectedFps == fps
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) StudioAccentYellow.copy(alpha = 0.15f) else StudioDarkBg)
                            .border(
                                width = 1.5.dp,
                                color = if (isSelected) StudioAccentYellow else StudioBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedFps = fps }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) StudioAccentYellow else StudioWhite,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Video Codec
            Text(
                text = "CODEC",
                color = StudioTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    VideoCodec.H264 to "H.264 (Compatible)",
                    VideoCodec.HEVC to "HEVC / H.265 (Smaller)"
                ).forEach { (codec, label) ->
                    val isSelected = selectedCodec == codec
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) StudioAccentYellow.copy(alpha = 0.15f) else StudioDarkBg)
                            .border(
                                width = 1.5.dp,
                                color = if (isSelected) StudioAccentYellow else StudioBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedCodec = codec }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) StudioAccentYellow else StudioWhite,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Bitrate / Quality Level
            Text(
                text = "QUALITY / BITRATE",
                color = StudioTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    BitrateLevel.LOW to "Low",
                    BitrateLevel.RECOMMENDED to "Standard",
                    BitrateLevel.HIGH to "High"
                ).forEach { (level, label) ->
                    val isSelected = selectedBitrateLevel == level
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) StudioAccentYellow.copy(alpha = 0.15f) else StudioDarkBg)
                            .border(
                                width = 1.5.dp,
                                color = if (isSelected) StudioAccentYellow else StudioBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedBitrateLevel = level }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) StudioAccentYellow else StudioWhite,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = { onStartExport(currentConfig) },
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioAccentYellow,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Export Video",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
