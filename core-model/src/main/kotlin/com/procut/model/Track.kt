package com.procut.model

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: String,
    val type: TrackType,
    val name: String,
    val isMuted: Boolean = false,
    val isLocked: Boolean = false,
    val isHidden: Boolean = false,
    val volume: Float = 1.0f,
    val clips: List<ClipSegment> = emptyList()
) {
    val totalDurationUs: Long
        get() = clips.maxOfOrNull { it.targetTimeRangeUs.endUs } ?: 0L

    val isMagnetic: Boolean
        get() = type.isMagnetic

    fun findClipAt(timestampUs: Long): ClipSegment? {
        return clips.firstOrNull { it.targetTimeRangeUs.contains(timestampUs) }
    }

    fun findClipById(clipId: String): ClipSegment? {
        return clips.firstOrNull { it.id == clipId }
    }
}
