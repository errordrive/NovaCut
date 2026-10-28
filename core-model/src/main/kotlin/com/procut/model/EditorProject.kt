package com.procut.model

import kotlinx.serialization.Serializable

@Serializable
data class EditorProject(
    val id: String,
    val name: String,
    val createdAtUs: Long = System.currentTimeMillis() * 1000L,
    val updatedAtUs: Long = System.currentTimeMillis() * 1000L,
    val canvasConfig: CanvasConfig = CanvasConfig(),
    val tracks: List<Track> = emptyList(),
    val assets: List<MediaAsset> = emptyList(),
    val playheadPositionUs: Long = 0L
) {
    val totalDurationUs: Long
        get() = tracks.maxOfOrNull { it.totalDurationUs } ?: 0L

    val mainTrack: Track?
        get() = tracks.firstOrNull { it.type == TrackType.MAIN_VIDEO }

    val overlayTracks: List<Track>
        get() = tracks.filter { it.type == TrackType.PIP_OVERLAY }

    val audioTracks: List<Track>
        get() = tracks.filter { it.type == TrackType.AUDIO }

    val textTracks: List<Track>
        get() = tracks.filter { it.type == TrackType.TEXT }

    val effectTracks: List<Track>
        get() = tracks.filter { it.type == TrackType.EFFECT }

    fun findTrackById(trackId: String): Track? {
        return tracks.firstOrNull { it.id == trackId }
    }

    fun findClipById(clipId: String): Pair<Track, ClipSegment>? {
        for (track in tracks) {
            val clip = track.findClipById(clipId)
            if (clip != null) return Pair(track, clip)
        }
        return null
    }

    fun findAssetById(assetId: String): MediaAsset? {
        return assets.firstOrNull { it.id == assetId }
    }
}
