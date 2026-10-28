package com.procut.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun TextEditorSheet(
    currentConfig: TextConfig,
    onConfigChange: (TextConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentConfig.text) }
    var fontSize by remember { mutableStateOf(currentConfig.fontSizeSp) }
    var selectedColor by remember { mutableStateOf(currentConfig.textColorHex) }
    var selectedAnimation by remember { mutableStateOf(currentConfig.animationIn?.type ?: TextAnimationType.NONE) }
    var hasStroke by remember { mutableStateOf(currentConfig.stroke.hasStroke) }
    var hasShadow by remember { mutableStateOf(currentConfig.shadow.hasShadow) }
    var hasBackground by remember { mutableStateOf(currentConfig.background.hasBackground) }

    val presetColors = listOf(
        "#FFFFFFFF", "#FF00E5FF", "#FFFFD600", "#FFFF4081",
        "#FF00E676", "#FFFF6E40", "#FFE040FB", "#FF000000"
    )

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
                text = "Text & Typography Studio",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = StudioWhite
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Text Input Field
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    onConfigChange(currentConfig.copy(text = it))
                },
                label = { Text("Text Content") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = StudioWhite,
                    unfocusedTextColor = StudioWhite,
                    focusedBorderColor = StudioCyanAccent,
                    unfocusedBorderColor = StudioDarkGrey
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Font Size Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Font Size", style = MaterialTheme.typography.bodyMedium, color = StudioGrey)
                Text(
                    text = "${fontSize.toInt()} sp",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = StudioCyanAccent
                )
            }
            Slider(
                value = fontSize,
                onValueChange = {
                    fontSize = it
                    onConfigChange(currentConfig.copy(fontSizeSp = it))
                },
                valueRange = 12f..60f,
                colors = SliderDefaults.colors(
                    thumbColor = StudioCyanAccent,
                    activeTrackColor = StudioCyanAccent,
                    inactiveTrackColor = StudioDarkSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Color Palette
            Text(text = "Text Color", style = MaterialTheme.typography.bodySmall, color = StudioGrey)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presetColors.forEach { hex ->
                    val color = try {
                        Color(android.graphics.Color.parseColor(hex))
                    } catch (e: Exception) {
                        Color.White
                    }
                    val isSelected = (hex == selectedColor)

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) StudioCyanAccent else StudioDarkGrey,
                                shape = CircleShape
                            )
                            .clickable {
                                selectedColor = hex
                                onConfigChange(currentConfig.copy(textColorHex = hex))
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Entrance Animations
            Text(text = "Entrance Animation", style = MaterialTheme.typography.bodySmall, color = StudioGrey)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    TextAnimationType.NONE,
                    TextAnimationType.TYPEWRITER,
                    TextAnimationType.FADE_UP,
                    TextAnimationType.BOUNCE,
                    TextAnimationType.POP,
                    TextAnimationType.GLITCH,
                    TextAnimationType.ZOOM,
                    TextAnimationType.SLIDE_UP
                ).forEach { animType ->
                    val isSelected = (animType == selectedAnimation)
                    Surface(
                        color = if (isSelected) StudioCyanAccent.copy(alpha = 0.2f) else StudioDarkSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                if (isSelected) StudioCyanAccent else StudioDarkGrey.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedAnimation = animType
                                val animConfig = if (animType == TextAnimationType.NONE) null
                                else TextAnimationConfig(type = animType, durationUs = 600_000L)
                                onConfigChange(currentConfig.copy(animationIn = animConfig))
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                            Text(
                                text = animType.name.replace("_", " "),
                                color = if (isSelected) StudioCyanAccent else StudioWhite,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Styling Toggles: Stroke, Shadow, Background
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Stroke
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = hasStroke,
                        onCheckedChange = {
                            hasStroke = it
                            onConfigChange(currentConfig.copy(stroke = currentConfig.stroke.copy(hasStroke = it)))
                        },
                        colors = CheckboxDefaults.colors(checkedColor = StudioCyanAccent)
                    )
                    Text(text = "Stroke", color = StudioWhite, fontSize = 12.sp)
                }

                // Shadow
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = hasShadow,
                        onCheckedChange = {
                            hasShadow = it
                            onConfigChange(currentConfig.copy(shadow = currentConfig.shadow.copy(hasShadow = it)))
                        },
                        colors = CheckboxDefaults.colors(checkedColor = StudioCyanAccent)
                    )
                    Text(text = "Shadow", color = StudioWhite, fontSize = 12.sp)
                }

                // Background
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = hasBackground,
                        onCheckedChange = {
                            hasBackground = it
                            onConfigChange(currentConfig.copy(background = currentConfig.background.copy(hasBackground = it)))
                        },
                        colors = CheckboxDefaults.colors(checkedColor = StudioCyanAccent)
                    )
                    Text(text = "Box", color = StudioWhite, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
