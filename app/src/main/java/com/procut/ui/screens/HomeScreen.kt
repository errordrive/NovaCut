package com.procut.ui.screens

import android.content.Context
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.procut.model.*
import com.procut.repository.DraftMetadata
import com.procut.repository.FileDraftRepository
import com.procut.ui.components.MediaPickerSheet
import com.procut.ui.components.PickerMediaItem
import com.procut.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenProject: (EditorProject) -> Unit,
    onNewProject: (EditorProject) -> Unit
) {
    val context = LocalContext.current
    val repository = remember {
        val draftsDir = File(context.filesDir, "drafts")
        FileDraftRepository(draftsDir)
    }

    var drafts by remember { mutableStateOf<List<DraftMetadata>>(emptyList()) }
    var showMediaPicker by remember { mutableStateOf(false) }

    // Rename dialog state
    var renameDraftId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

    // Delete dialog state
    var deleteDraftId by remember { mutableStateOf<String?>(null) }

    fun refreshDrafts() {
        val list = repository.listDrafts()
        if (list.isEmpty()) {
            // Seed initial demo project so user always has an active project
            val demo = createDefaultDemoProject()
            repository.saveDraft(demo)
            drafts = repository.listDrafts()
        } else {
            drafts = list
        }
    }

    LaunchedEffect(Unit) {
        refreshDrafts()
    }

    Scaffold(
        topBar = {
            Surface(
                color = StudioDarkSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo & Brand Title
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(StudioCyanAccent, Color(0xFF0077FF))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MovieFilter,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "PRO CUT",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                fontSize = 18.sp
                            ),
                            color = StudioWhite
                        )
                    }

                    // Settings / Pro Badge
                    Surface(
                        color = StudioDarkSurfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "STUDIO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioCyanAccent
                            )
                        }
                    }
                }
            }
        },
        containerColor = StudioDarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. "New Project" Hero Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                StudioDarkSurfaceVariant,
                                Color(0xFF1E2638)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(StudioCyanAccent.copy(alpha = 0.5f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { showMediaPicker = true }
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Plus Icon Circle
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(StudioCyanAccent)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Project",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column {
                        Text(
                            text = "New Project",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = StudioWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select videos and photos to edit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StudioGrey,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Recent Drafts Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Recent Projects",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = StudioWhite
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = StudioDarkSurfaceVariant,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${drafts.size}",
                            color = StudioCyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Drafts Grid
            if (drafts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = StudioDarkGrey,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No projects yet",
                            color = StudioWhite,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap New Project above to start editing",
                            color = StudioGrey,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(drafts, key = { it.id }) { draft ->
                        DraftCard(
                            draft = draft,
                            onClick = {
                                val loaded = repository.loadDraft(draft.id)
                                if (loaded != null) {
                                    onOpenProject(loaded)
                                }
                            },
                            onRename = {
                                renameDraftId = draft.id
                                renameText = draft.name
                            },
                            onDuplicate = {
                                repository.duplicateDraft(draft.id, "${draft.name} (Copy)")
                                refreshDrafts()
                            },
                            onDelete = {
                                deleteDraftId = draft.id
                            }
                        )
                    }
                }
            }
        }
    }

    // Media Picker Bottom Sheet
    if (showMediaPicker) {
        MediaPickerSheet(
            onDismiss = { showMediaPicker = false },
            onMediaSelected = { selectedList ->
                showMediaPicker = false
                val newProject = createProjectFromSelectedMedia(selectedList)
                repository.saveDraft(newProject)
                onNewProject(newProject)
            }
        )
    }

    // Rename Dialog
    if (renameDraftId != null) {
        AlertDialog(
            onDismissRequest = { renameDraftId = null },
            title = { Text("Rename Project", color = StudioWhite) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StudioWhite,
                        unfocusedTextColor = StudioWhite,
                        focusedBorderColor = StudioCyanAccent,
                        unfocusedBorderColor = StudioBorder
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = renameDraftId
                        if (id != null && renameText.isNotBlank()) {
                            val original = repository.loadDraft(id)
                            if (original != null) {
                                repository.saveDraft(original.copy(name = renameText.trim()))
                                refreshDrafts()
                            }
                        }
                        renameDraftId = null
                    }
                ) {
                    Text("Rename", color = StudioCyanAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameDraftId = null }) {
                    Text("Cancel", color = StudioGrey)
                }
            },
            containerColor = StudioDarkSurface
        )
    }

    // Delete Confirmation Dialog
    if (deleteDraftId != null) {
        AlertDialog(
            onDismissRequest = { deleteDraftId = null },
            title = { Text("Delete Project?", color = StudioWhite) },
            text = { Text("This will permanently remove the project draft.", color = StudioGrey) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = deleteDraftId
                        if (id != null) {
                            repository.deleteDraft(id)
                            refreshDrafts()
                        }
                        deleteDraftId = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDraftId = null }) {
                    Text("Cancel", color = StudioGrey)
                }
            },
            containerColor = StudioDarkSurface
        )
    }
}

@Composable
private fun DraftCard(
    draft: DraftMetadata,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        color = StudioDarkSurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            // Card Thumbnail Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(StudioClipMain, StudioClipOverlay)
                        )
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircleOutline,
                    contentDescription = null,
                    tint = StudioWhite.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                )

                // Duration Badge
                val sec = draft.durationUs / 1_000_000L
                val mm = sec / 60
                val ss = sec % 60
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = String.format("%02d:%02d", mm, ss),
                        color = StudioWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // 3-Dots Menu Button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = StudioWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(StudioDarkSurfaceVariant)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename", color = StudioWhite) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = StudioWhite) },
                            onClick = {
                                showMenu = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate", color = StudioWhite) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = StudioWhite) },
                            onClick = {
                                showMenu = false
                                onDuplicate()
                            }
                        )
                        HorizontalDivider(color = StudioDarkGrey)
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color(0xFFFF5252)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Title & Timestamp
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = draft.name,
                    color = StudioWhite,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val formattedDate = remember(draft.updatedAtUs) {
                    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                    sdf.format(Date(draft.updatedAtUs / 1000L))
                }
                Text(
                    text = formattedDate,
                    color = StudioGrey,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Creates a brand new EditorProject given media selected from MediaPickerSheet.
 */
private fun createProjectFromSelectedMedia(selectedItems: List<PickerMediaItem>): EditorProject {
    val projectId = UUID.randomUUID().toString()
    val nowUs = System.currentTimeMillis() * 1000L

    var currentStartUs = 0L
    val clips = mutableListOf<ClipSegment>()
    val assets = mutableListOf<MediaAsset>()

    selectedItems.forEachIndexed { index, item ->
        val assetId = "asset_${projectId}_$index"
        val clipId = "clip_${projectId}_$index"
        val durationUs = item.durationUs

        assets.add(
            MediaAsset(
                id = assetId,
                filePath = item.uriString,
                mediaType = if (item.isVideo) MediaType.VIDEO else MediaType.IMAGE,
                durationUs = durationUs,
                naturalWidth = 1080,
                naturalHeight = 1920
            )
        )

        clips.add(
            ClipSegment(
                id = clipId,
                assetId = assetId,
                sourceTimeRangeUs = TimeRangeUs(0L, durationUs),
                targetTimeRangeUs = TimeRangeUs(currentStartUs, durationUs)
            )
        )
        currentStartUs += durationUs
    }

    val mainTrack = Track(
        id = "track_main_$projectId",
        type = TrackType.MAIN_VIDEO,
        name = "Main Video",
        clips = clips
    )

    return EditorProject(
        id = projectId,
        name = "Project ${SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date())}",
        createdAtUs = nowUs,
        updatedAtUs = nowUs,
        canvasConfig = CanvasConfig(
            aspectRatio = AspectRatio.RATIO_9_16,
            width = 1080,
            height = 1920
        ),
        tracks = listOf(mainTrack),
        mediaAssets = assets
    )
}
