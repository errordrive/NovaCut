package com.procut.ui.components

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.procut.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PickerMediaItem(
    val id: String,
    val uriString: String,
    val title: String,
    val durationUs: Long,
    val isVideo: Boolean
)

enum class MediaPickerTab(val label: String) {
    ALL("All"),
    VIDEOS("Videos"),
    PHOTOS("Photos")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPickerSheet(
    onDismiss: () -> Unit,
    onMediaSelected: (List<PickerMediaItem>) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(MediaPickerTab.ALL) }
    var mediaList by remember { mutableStateOf<List<PickerMediaItem>>(emptyList()) }
    val selectedItems = remember { mutableStateListOf<PickerMediaItem>() }

    // Load media from MediaStore + fallback presets
    LaunchedEffect(Unit) {
        mediaList = loadMediaItems(context)
    }

    val filteredList = remember(mediaList, selectedTab) {
        when (selectedTab) {
            MediaPickerTab.ALL -> mediaList
            MediaPickerTab.VIDEOS -> mediaList.filter { it.isVideo }
            MediaPickerTab.PHOTOS -> mediaList.filter { !it.isVideo }
        }
    }

    val totalDurationUs = remember(selectedItems.size) {
        selectedItems.sumOf { it.durationUs }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioDarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.75f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Media",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = StudioWhite
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = StudioGrey
                    )
                }
            }

            // Tabs: All / Videos / Photos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaPickerTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    Surface(
                        color = if (isSelected) StudioCyanAccent.copy(alpha = 0.15f) else StudioDarkSurfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) StudioCyanAccent else Color.Transparent
                        ),
                        modifier = Modifier.clickable { selectedTab = tab }
                    ) {
                        Text(
                            text = tab.label,
                            color = if (isSelected) StudioCyanAccent else StudioGrey,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Media Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(filteredList, key = { it.id }) { item ->
                    val selectionIndex = selectedItems.indexOfFirst { it.id == item.id }
                    val isSelected = selectionIndex != -1

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(StudioClipMain)
                            .border(
                                width = if (isSelected) 2.5.dp else 0.dp,
                                color = if (isSelected) StudioCyanAccent else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                if (isSelected) {
                                    selectedItems.removeIf { it.id == item.id }
                                } else {
                                    selectedItems.add(item)
                                }
                            }
                    ) {
                        // Media Type Icon Placeholder
                        Icon(
                            imageVector = if (item.isVideo) Icons.Default.Videocam else Icons.Default.Image,
                            contentDescription = null,
                            tint = StudioGrey.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.Center)
                        )

                        // Title / Duration at bottom
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            if (item.isVideo) {
                                val seconds = item.durationUs / 1_000_000L
                                val mm = seconds / 60
                                val ss = seconds % 60
                                Text(
                                    text = String.format("%02d:%02d", mm, ss),
                                    color = StudioWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                )
                            } else {
                                Text(
                                    text = "PHOTO",
                                    color = StudioGrey,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                )
                            }
                        }

                        // Top-right selection badge with order number (1, 2, 3...)
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) StudioCyanAccent else Color.Black.copy(alpha = 0.4f))
                                .border(1.5.dp, if (isSelected) StudioCyanAccent else StudioWhite, CircleShape)
                                .align(Alignment.TopEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Text(
                                    text = "${selectionIndex + 1}",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Action Bar
            Surface(
                color = StudioDarkSurfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (selectedItems.isEmpty()) "Select clips to edit" else "${selectedItems.size} selected",
                            color = StudioWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (selectedItems.isNotEmpty()) {
                            val totalSec = totalDurationUs / 1_000_000L
                            Text(
                                text = "Total duration: %02d:%02d".format(totalSec / 60, totalSec % 60),
                                color = StudioCyanAccent,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (selectedItems.isNotEmpty()) {
                                onMediaSelected(selectedItems.toList())
                            }
                        },
                        enabled = selectedItems.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioCyanAccent,
                            contentColor = Color.Black,
                            disabledContainerColor = StudioDarkGrey,
                            disabledContentColor = StudioGrey
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Add to Project",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

private suspend fun loadMediaItems(context: Context): List<PickerMediaItem> = withContext(Dispatchers.IO) {
    val items = mutableListOf<PickerMediaItem>()

    // Query Device MediaStore Videos
    try {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION
        )
        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )
        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (it.moveToNext() && items.size < 30) {
                val id = it.getLong(idCol)
                val name = it.getString(nameCol) ?: "Video"
                val durationMs = it.getLong(durationCol)
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                items.add(
                    PickerMediaItem(
                        id = "video_$id",
                        uriString = uri.toString(),
                        title = name,
                        durationUs = (durationMs * 1000L).coerceAtLeast(3_000_000L),
                        isVideo = true
                    )
                )
            }
        }
    } catch (e: Exception) {
        // Ignored if permissions not granted yet
    }

    // Always append rich built-in demo video/photo assets to ensure immediate out-of-the-box readiness
    val demoPresets = listOf(
        PickerMediaItem("demo_clip_1", "demo://cyberpunk_city.mp4", "Cyberpunk Neon City", 8_000_000L, true),
        PickerMediaItem("demo_clip_2", "demo://hyper_lapse.mp4", "Highway Hyper-Lapse", 5_000_000L, true),
        PickerMediaItem("demo_clip_3", "demo://nature_drone.mp4", "Alpine Drone Flyover", 12_000_000L, true),
        PickerMediaItem("demo_clip_4", "demo://portrait_bokeh.mp4", "Night Bokeh Portrait", 6_000_000L, true),
        PickerMediaItem("demo_photo_1", "demo://neon_sign.jpg", "Tokyo Neon Sign", 3_000_000L, false),
        PickerMediaItem("demo_photo_2", "demo://skate_jump.jpg", "Action Skate Freeze", 3_000_000L, false)
    )

    items.addAll(demoPresets)
    items
}
