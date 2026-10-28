package com.procut.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.procut.model.AspectRatio
import com.procut.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AspectRatioSelectorSheet(
    currentRatio: AspectRatio,
    onRatioSelected: (AspectRatio) -> Unit,
    onDismiss: () -> Unit
) {
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
            Text(
                text = "Canvas Aspect Ratio",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = StudioWhite
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AspectRatio.values().forEach { ratio ->
                    val isSelected = (ratio == currentRatio)
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onRatioSelected(ratio)
                                onDismiss()
                            }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Icon aspect preview box
                        val boxW = when (ratio) {
                            AspectRatio.RATIO_9_16 -> 24.dp
                            AspectRatio.RATIO_16_9 -> 44.dp
                            AspectRatio.RATIO_1_1 -> 32.dp
                            AspectRatio.RATIO_4_5 -> 28.dp
                            AspectRatio.RATIO_2_35_1 -> 48.dp
                            else -> 32.dp
                        }
                        val boxH = when (ratio) {
                            AspectRatio.RATIO_9_16 -> 42.dp
                            AspectRatio.RATIO_16_9 -> 25.dp
                            AspectRatio.RATIO_1_1 -> 32.dp
                            AspectRatio.RATIO_4_5 -> 35.dp
                            AspectRatio.RATIO_2_35_1 -> 20.dp
                            else -> 32.dp
                        }

                        Box(
                            modifier = Modifier
                                .size(width = boxW, height = boxH)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) StudioCyanAccent.copy(alpha = 0.2f) else StudioDarkSurfaceVariant)
                                .border(
                                    1.5.dp,
                                    if (isSelected) StudioCyanAccent else StudioDarkGrey,
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = ratio.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) StudioCyanAccent else StudioWhite,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
