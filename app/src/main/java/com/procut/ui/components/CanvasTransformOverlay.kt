package com.procut.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import com.procut.model.Transform2D
import com.procut.ui.theme.StudioCyanAccent
import com.procut.ui.theme.StudioGuideLine
import com.procut.ui.theme.StudioWhite
import kotlin.math.abs

@Composable
fun CanvasTransformOverlay(
    transform: Transform2D,
    onTransformChange: (Transform2D) -> Unit,
    onTransformCommit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isSnappedX by remember { mutableStateOf(false) }
    var isSnappedY by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(transform) {
                // Multi-touch gestures: Pinch scale, pan, rotate
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    var newScaleX = (transform.scaleX * zoom).coerceIn(0.1f, 10.0f)
                    var newScaleY = (transform.scaleY * zoom).coerceIn(0.1f, 10.0f)

                    // Normalized pan (-1 to 1 space)
                    val panNormX = (pan.x / size.width.toFloat()) * 2f
                    val panNormY = (pan.y / size.height.toFloat()) * 2f

                    var newX = transform.translationX + panNormX
                    var newY = transform.translationY + panNormY

                    // Magnetic snapping to Center X = 0
                    val snapThreshold = 0.035f
                    if (abs(newX) < snapThreshold) {
                        newX = 0f
                        if (!isSnappedX) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isSnappedX = true
                        }
                    } else {
                        isSnappedX = false
                    }

                    // Magnetic snapping to Center Y = 0
                    if (abs(newY) < snapThreshold) {
                        newY = 0f
                        if (!isSnappedY) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isSnappedY = true
                        }
                    } else {
                        isSnappedY = false
                    }

                    // Angle rotation with snap to 0, 90, 180, 270 degrees
                    var newAngle = transform.rotationDeg + rotation
                    val normAngle = ((newAngle % 360f) + 360f) % 360f
                    val snapAngles = floatArrayOf(0f, 90f, 180f, 270f, 360f)
                    for (target in snapAngles) {
                        if (abs(normAngle - target) < 3.0f) {
                            newAngle = if (target == 360f) 0f else target
                            break
                        }
                    }

                    onTransformChange(
                        transform.copy(
                            translationX = newX,
                            translationY = newY,
                            scaleX = newScaleX,
                            scaleY = newScaleY,
                            rotationDeg = newAngle
                        )
                    )
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw magnetic center guidelines if snapped
            if (isSnappedX) {
                drawLine(
                    color = StudioGuideLine,
                    start = Offset(canvasW / 2f, 0f),
                    end = Offset(canvasW / 2f, canvasH),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }
            if (isSnappedY) {
                drawLine(
                    color = StudioGuideLine,
                    start = Offset(0f, canvasH / 2f),
                    end = Offset(canvasW, canvasH / 2f),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }

            // 2. Compute layer bounding box in screen coordinates
            val centerX = (canvasW / 2f) + (transform.translationX * (canvasW / 2f))
            val centerY = (canvasH / 2f) + (transform.translationY * (canvasH / 2f))

            val boxW = canvasW * transform.scaleX
            val boxH = canvasH * transform.scaleY

            val left = centerX - (boxW / 2f)
            val top = centerY - (boxH / 2f)

            rotate(degrees = transform.rotationDeg, pivot = Offset(centerX, centerY)) {
                // Bounding rectangle
                drawRect(
                    color = StudioCyanAccent,
                    topLeft = Offset(left, top),
                    size = Size(boxW, boxH),
                    style = Stroke(width = 2.5f)
                )

                // 4 Corner handles
                val handleRadius = 9f
                val handleColor = StudioWhite
                val handleBorder = StudioCyanAccent

                val corners = listOf(
                    Offset(left, top),
                    Offset(left + boxW, top),
                    Offset(left, top + boxH),
                    Offset(left + boxW, top + boxH)
                )

                corners.forEach { corner ->
                    drawCircle(color = handleBorder, radius = handleRadius + 2f, center = corner)
                    drawCircle(color = handleColor, radius = handleRadius, center = corner)
                }

                // Top center rotation handle
                val rotHandlePos = Offset(centerX, top - 32f)
                drawLine(
                    color = StudioCyanAccent,
                    start = Offset(centerX, top),
                    end = rotHandlePos,
                    strokeWidth = 2f
                )
                drawCircle(color = handleBorder, radius = handleRadius + 2f, center = rotHandlePos)
                drawCircle(color = StudioCyanAccent, radius = handleRadius, center = rotHandlePos)
            }
        }
    }
}
