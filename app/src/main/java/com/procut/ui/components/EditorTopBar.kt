package com.procut.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.procut.model.ExportConfig
import com.procut.model.ExportResolution
import com.procut.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
    projectTitle: String,
    exportConfig: ExportConfig,
    canUndo: Boolean,
    canRedo: Boolean,
    isFullscreen: Boolean,
    onBackClick: () -> Unit,
    onRenameClick: (String) -> Unit,
    onConfigChange: (ExportResolution, Int) -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onFullscreenToggle: () -> Unit,
    onExportClick: () -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(projectTitle) }
    var showConfigMenu by remember { mutableStateOf(false) }

    Surface(
        color = StudioDarkSurface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Back button & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = StudioWhite
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = projectTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = StudioWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            renameText = projectTitle
                            showRenameDialog = true
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }

            // Right: Resolution/FPS Badge, Undo, Redo, Fullscreen, Export
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Resolution / FPS chip dropdown
                Box {
                    Surface(
                        color = StudioDarkSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .clickable { showConfigMenu = true }
                            .padding(end = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${exportConfig.resolution.label} · ${exportConfig.frameRate}fps",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = StudioCyanAccent
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Resolution Options",
                                tint = StudioCyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showConfigMenu,
                        onDismissRequest = { showConfigMenu = false },
                        modifier = Modifier.background(StudioDarkSurfaceVariant)
                    ) {
                        Text(
                            text = "Resolution",
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioGrey,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        ExportResolution.values().forEach { res ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${res.label} (${res.width}x${res.height})",
                                        color = if (res == exportConfig.resolution) StudioCyanAccent else StudioWhite
                                    )
                                },
                                onClick = {
                                    onConfigChange(res, exportConfig.frameRate)
                                    showConfigMenu = false
                                }
                            )
                        }
                        HorizontalDivider(color = StudioDarkGrey)
                        Text(
                            text = "Frame Rate",
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioGrey,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        listOf(24, 30, 60).forEach { fps ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "$fps FPS",
                                        color = if (fps == exportConfig.frameRate) StudioCyanAccent else StudioWhite
                                    )
                                },
                                onClick = {
                                    onConfigChange(exportConfig.resolution, fps)
                                    showConfigMenu = false
                                }
                            )
                        }
                    }
                }

                // Undo
                IconButton(
                    onClick = onUndoClick,
                    enabled = canUndo
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) StudioWhite else StudioDarkGrey
                    )
                }

                // Redo
                IconButton(
                    onClick = onRedoClick,
                    enabled = canRedo
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) StudioWhite else StudioDarkGrey
                    )
                }

                // Fullscreen preview
                IconButton(onClick = onFullscreenToggle) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen Preview",
                        tint = StudioWhite
                    )
                }

                // Export Button
                Button(
                    onClick = onExportClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioCyanAccent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Export",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(text = "Rename Project", color = StudioWhite) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StudioWhite,
                        unfocusedTextColor = StudioWhite,
                        focusedBorderColor = StudioCyanAccent,
                        unfocusedBorderColor = StudioDarkGrey
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameText.isNotBlank()) {
                        onRenameClick(renameText)
                    }
                    showRenameDialog = false
                }) {
                    Text("Save", color = StudioCyanAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = StudioGrey)
                }
            },
            containerColor = StudioDarkSurface
        )
    }
}
