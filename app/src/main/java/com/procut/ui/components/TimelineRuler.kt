package com.procut.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.procut.ui.theme.StudioDarkGrey
import com.procut.ui.theme.StudioGrey
import com.procut.ui.theme.StudioWhite
import kotlin.math.ceil
import kotlin.math.floor

// 19 Adaptive Ruler intervals from CapCut specification
private val RULER_INTERVALS_SEC = floatArrayOf(
    0.033334f, // 1 frame @ 30 FPS
    0.05f,      // 1 frame @ 20 FPS
    0.083334f, // 2.5 frames
    0.166667f, // 5 frames
    0.25f,     // 1/4 second
    0.5f,      // 1/2 second
    1.0f,      // 1 second
    1.5f,      // 1.5 seconds
    2.5f,      // 2.5 seconds
    5.0f,      // 5 seconds
    10.0f,     // 10 seconds
    15.0f,     // 15 seconds
    30.0f,     // 30 seconds
    60.0f,     // 1 minute
    90.0f,     // 1.5 minutes
    150.0f,    // 2.5 minutes
    300.0f,    // 5 minutes
    600.0f,    // 10 minutes
    900.0f     // 15 minutes
)

@Composable
fun TimelineRuler(
    totalDurationUs: Long,
    zoomScaleDpPerUs: Float,
    scrollOffsetPx: Float,
    viewportWidthPx: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        val density = this.density
        val pxPerUs = zoomScaleDpPerUs * density
        val pxPerSec = pxPerUs * 1_000_000f

        // Pick optimal interval such that tick spacing >= 32 dp (32 * density px)
        val minTickSpacingPx = 32f * density
        var chosenIntervalSec = RULER_INTERVALS_SEC.last()
        for (interval in RULER_INTERVALS_SEC) {
            val spacingPx = interval * pxPerSec
            if (spacingPx >= minTickSpacingPx) {
                chosenIntervalSec = interval
                break
            }
        }

        val intervalUs = (chosenIntervalSec * 1_000_000f).toLong().coerceAtLeast(33_333L)
        val centerOffsetPx = viewportWidthPx / 2f

        // Visible time window
        val startVisibleUs = (((scrollOffsetPx - centerOffsetPx) / pxPerUs).toLong()).coerceAtLeast(0L)
        val endVisibleUs = (((scrollOffsetPx + centerOffsetPx) / pxPerUs).toLong())
            .coerceAtMost(totalDurationUs + 5_000_000L)

        val firstTickIndex = floor(startVisibleUs.toDouble() / intervalUs).toLong()
        val lastTickIndex = ceil(endVisibleUs.toDouble() / intervalUs).toLong()

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(200, 160, 160, 170)
            textSize = 10f * density
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
        }

        for (i in firstTickIndex..lastTickIndex) {
            val tickTimeUs = i * intervalUs
            val x = centerOffsetPx + (tickTimeUs * pxPerUs) - scrollOffsetPx

            if (x < -50f || x > viewportWidthPx + 50f) continue

            val isMajor = (tickTimeUs % 1_000_000L == 0L)
            val tickHeight = if (isMajor) 12f * density else 6f * density
            val tickColor = if (isMajor) StudioWhite.copy(alpha = 0.7f) else StudioDarkGrey

            drawLine(
                color = tickColor,
                start = Offset(x, size.height - tickHeight),
                end = Offset(x, size.height),
                strokeWidth = if (isMajor) 1.5f * density else 1f * density
            )

            // Draw label on major ticks
            if (isMajor) {
                val totalSec = tickTimeUs / 1_000_000L
                val m = totalSec / 60
                val s = totalSec % 60
                val label = String.format("%02d:%02d", m, s)
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x + 4f,
                    size.height - tickHeight - 2f,
                    textPaint
                )
            } else if (chosenIntervalSec < 1.0f) {
                // Draw frame marks e.g. "15f" for sub-second ticks
                val remUs = tickTimeUs % 1_000_000L
                val frame = ((remUs / 1_000_000.0) * 30).toInt()
                if (frame > 0) {
                    val label = "${frame}f"
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        x + 2f,
                        size.height - tickHeight - 2f,
                        textPaint
                    )
                }
            }
        }
    }
}
