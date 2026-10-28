package com.procut.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.procut.model.Transition
import com.procut.model.TransitionType
import com.procut.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransitionPickerDialog(
    currentTransition: Transition?,
    onTransitionSelected: (Transition?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(currentTransition?.type) }
    var durationMs by remember { mutableStateOf((currentTransition?.durationUs ?: 500_000L) / 1000L) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioDarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = StudioDarkGrey) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Clip Transition",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = StudioWhite
                    )
                )
                TextButton(onClick = {
                    onTransitionSelected(null)
                    onDismiss()
                }) {
                    Text("Remove", color = StudioGrey)
                }
            }

            // Duration Slider
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Transition Duration",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioGrey
                    )
                    Text(
                        text = "${String.format("%.1f", durationMs / 1000f)}s",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = StudioCyanAccent
                    )
                }
                Slider(
                    value = durationMs.toFloat(),
                    onValueChange = { durationMs = it.toLong() },
                    valueRange = 100f..2000f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = StudioCyanAccent,
                        activeTrackColor = StudioCyanAccent,
                        inactiveTrackColor = StudioDarkSurfaceVariant
                    )
                )
            }

            // Grid of Transitions
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                items(TransitionType.values()) { type ->
                    val isSelected = (type == selectedType)
                    Surface(
                        color = if (isSelected) StudioCyanAccent.copy(alpha = 0.2f) else StudioDarkSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                1.5.dp,
                                if (isSelected) StudioCyanAccent else StudioDarkGrey.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedType = type
                                onTransitionSelected(Transition(type = type, durationUs = durationMs * 1000L))
                                onDismiss()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = type.name.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) StudioCyanAccent else StudioWhite,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
