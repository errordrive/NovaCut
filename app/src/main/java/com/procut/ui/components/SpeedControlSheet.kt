package com.procut.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.procut.model.CurvePoint
import com.procut.model.SpeedConfig
import com.procut.model.SpeedMode
import com.procut.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedControlSheet(
    currentSpeedConfig: SpeedConfig,
    onSpeedConfigChange: (SpeedConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(if (currentSpeedConfig.mode == SpeedMode.CURVE) 1 else 0) }
    var normalMultiplier by remember { mutableStateOf(currentSpeedConfig.speedMultiplier.toFloat()) }
    var keepPitch by remember { mutableStateOf(currentSpeedConfig.isPitchPreserved) }

    val presetCurves = listOf(
        "Hero Montage" to SpeedConfig.MONTAGE_HERO,
        "Bullet Time" to SpeedConfig.BULLET_CURVE,
        "Jump Cut" to SpeedConfig.JUMP_CUT,
        "Flash Ramp" to SpeedConfig(
            mode = SpeedMode.CURVE,
            curvePoints = listOf(
                CurvePoint(0.0f, 4.0f),
                CurvePoint(0.3f, 1.0f),
                CurvePoint(0.7f, 1.0f),
                CurvePoint(1.0f, 4.0f)
            )
        )
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
                text = "Speed Ramping",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = StudioWhite
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs: Normal vs Curve
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = StudioDarkSurfaceVariant,
                contentColor = StudioCyanAccent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Normal Speed") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Speed Curves") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // NORMAL SPEED
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Speed Multiplier", style = MaterialTheme.typography.bodyMedium, color = StudioGrey)
                    Text(
                        text = "${String.format("%.1f", normalMultiplier)}x",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = StudioCyanAccent
                    )
                }

                Slider(
                    value = normalMultiplier,
                    onValueChange = {
                        normalMultiplier = it
                        onSpeedConfigChange(
                            SpeedConfig(
                                mode = SpeedMode.CONSTANT,
                                speedMultiplier = it.toDouble(),
                                isPitchPreserved = keepPitch
                            )
                        )
                    },
                    valueRange = 0.1f..10.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = StudioCyanAccent,
                        activeTrackColor = StudioCyanAccent,
                        inactiveTrackColor = StudioDarkSurfaceVariant
                    )
                )

                // Quick buttons: 0.5x, 1x, 2x, 5x, 10x
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(0.5f, 1.0f, 2.0f, 5.0f, 10.0f).forEach { speed ->
                        OutlinedButton(
                            onClick = {
                                normalMultiplier = speed
                                onSpeedConfigChange(
                                    SpeedConfig(
                                        mode = SpeedMode.CONSTANT,
                                        speedMultiplier = speed.toDouble(),
                                        isPitchPreserved = keepPitch
                                    )
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (normalMultiplier == speed) StudioCyanAccent else StudioWhite
                            )
                        ) {
                            Text(text = "${speed}x", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // SPEED CURVES
                Text(
                    text = "Variable Bezier Curves",
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioGrey,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetCurves.take(2).forEach { (name, curve) ->
                        Surface(
                            color = StudioDarkSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clickable { onSpeedConfigChange(curve.copy(isPitchPreserved = keepPitch)) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = name, color = StudioWhite, fontSize = 12.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetCurves.drop(2).forEach { (name, curve) ->
                        Surface(
                            color = StudioDarkSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clickable { onSpeedConfigChange(curve.copy(isPitchPreserved = keepPitch)) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = name, color = StudioWhite, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Keep Pitch Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Preserve Pitch (Audio)", color = StudioWhite, fontSize = 13.sp)
                Switch(
                    checked = keepPitch,
                    onCheckedChange = {
                        keepPitch = it
                        onSpeedConfigChange(currentSpeedConfig.copy(isPitchPreserved = it))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = StudioCyanAccent,
                        checkedTrackColor = StudioDarkSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
