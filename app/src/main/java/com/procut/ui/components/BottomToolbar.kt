package com.procut.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.procut.model.ClipSegment
import com.procut.ui.theme.*

sealed class ToolbarAction {
    // Root Actions
    object RootEdit : ToolbarAction()
    object RootAudio : ToolbarAction()
    object RootVoiceover : ToolbarAction()
    object RootText : ToolbarAction()
    object RootStickers : ToolbarAction()
    object RootOverlay : ToolbarAction()
    object RootEffects : ToolbarAction()
    object RootFilters : ToolbarAction()
    object RootAspectRatio : ToolbarAction()
    object RootCanvas : ToolbarAction()
    object RootAdjust : ToolbarAction()

    // Clip Selected Actions
    object ClipDeselect : ToolbarAction()
    object ClipSplit : ToolbarAction()
    object ClipSpeed : ToolbarAction()
    object ClipAnimation : ToolbarAction()
    object ClipKeyframe : ToolbarAction()
    object ClipVolume : ToolbarAction()
    object ClipDelete : ToolbarAction()
    object ClipExtractAudio : ToolbarAction()
    object ClipCopy : ToolbarAction()
    object ClipReverse : ToolbarAction()
    object ClipFreeze : ToolbarAction()
    object ClipTransform : ToolbarAction()
    object ClipChromaKey : ToolbarAction()
    object ClipMask : ToolbarAction()
    object ClipFilter : ToolbarAction()
    object ClipAdjust : ToolbarAction()
}

@Composable
fun BottomToolbar(
    selectedClip: ClipSegment?,
    onAction: (ToolbarAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioDarkSurface,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        val scrollState = rememberScrollState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedClip == null) {
                // ROOT MODE ACTIONS
                ToolbarItem(icon = Icons.Default.ContentCut, label = "Edit") { onAction(ToolbarAction.RootEdit) }
                ToolbarItem(icon = Icons.Default.Audiotrack, label = "Audio") { onAction(ToolbarAction.RootAudio) }
                ToolbarItem(icon = Icons.Default.Mic, label = "Voiceover", tint = Color(0xFFFF5252)) { onAction(ToolbarAction.RootVoiceover) }
                ToolbarItem(icon = Icons.Default.TextFields, label = "Text", tint = StudioCyanAccent) { onAction(ToolbarAction.RootText) }
                ToolbarItem(icon = Icons.Default.Mood, label = "Stickers") { onAction(ToolbarAction.RootStickers) }
                ToolbarItem(icon = Icons.Default.Layers, label = "Overlay") { onAction(ToolbarAction.RootOverlay) }
                ToolbarItem(icon = Icons.Default.AutoAwesome, label = "Effects") { onAction(ToolbarAction.RootEffects) }
                ToolbarItem(icon = Icons.Default.FilterVintage, label = "Filters") { onAction(ToolbarAction.RootFilters) }
                ToolbarItem(icon = Icons.Default.AspectRatio, label = "Ratio") { onAction(ToolbarAction.RootAspectRatio) }
                ToolbarItem(icon = Icons.Default.Palette, label = "Canvas") { onAction(ToolbarAction.RootCanvas) }
                ToolbarItem(icon = Icons.Default.Tune, label = "Adjust") { onAction(ToolbarAction.RootAdjust) }
            } else {
                // CLIP SELECTED MODE ACTIONS (16+ Pro Tools)
                ToolbarItem(icon = Icons.Default.Close, label = "Close", tint = StudioGrey) { onAction(ToolbarAction.ClipDeselect) }
                ToolbarItem(icon = Icons.Default.CallSplit, label = "Split", tint = StudioCyanAccent) { onAction(ToolbarAction.ClipSplit) }
                ToolbarItem(icon = Icons.Default.Speed, label = "Speed") { onAction(ToolbarAction.ClipSpeed) }
                ToolbarItem(icon = Icons.Default.VolumeUp, label = "Volume / Fade") { onAction(ToolbarAction.ClipVolume) }
                ToolbarItem(icon = Icons.Default.Diamond, label = "Keyframe", tint = StudioKeyframeDiamond) { onAction(ToolbarAction.ClipKeyframe) }
                ToolbarItem(icon = Icons.Default.Animation, label = "Animation") { onAction(ToolbarAction.ClipAnimation) }
                ToolbarItem(icon = Icons.Default.Audiotrack, label = "Extract Audio") { onAction(ToolbarAction.ClipExtractAudio) }
                ToolbarItem(icon = Icons.Default.ContentCopy, label = "Duplicate") { onAction(ToolbarAction.ClipCopy) }
                ToolbarItem(icon = Icons.Default.Delete, label = "Delete", tint = Color(0xFFFF5252)) { onAction(ToolbarAction.ClipDelete) }
                ToolbarItem(icon = Icons.Default.FastRewind, label = "Reverse") { onAction(ToolbarAction.ClipReverse) }
                ToolbarItem(icon = Icons.Default.AcUnit, label = "Freeze") { onAction(ToolbarAction.ClipFreeze) }
                ToolbarItem(icon = Icons.Default.Transform, label = "Transform") { onAction(ToolbarAction.ClipTransform) }
                ToolbarItem(icon = Icons.Default.ColorLens, label = "Chroma") { onAction(ToolbarAction.ClipChromaKey) }
                ToolbarItem(icon = Icons.Default.Crop, label = "Mask") { onAction(ToolbarAction.ClipMask) }
                ToolbarItem(icon = Icons.Default.FilterVintage, label = "Filter") { onAction(ToolbarAction.ClipFilter) }
                ToolbarItem(icon = Icons.Default.Tune, label = "Adjust") { onAction(ToolbarAction.ClipAdjust) }
            }
        }
    }
}

@Composable
private fun ToolbarItem(
    icon: ImageVector,
    label: String,
    tint: Color = StudioWhite,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = tint
            )
        )
    }
}
