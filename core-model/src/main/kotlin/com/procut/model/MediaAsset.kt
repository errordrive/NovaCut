package com.procut.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaAsset(
    val id: String,
    val filePath: String,
    val mimeType: String = "video/mp4",
    val durationUs: Long,
    val width: Int = 1920,
    val height: Int = 1080,
    val frameRate: Double = 30.0,
    val audioChannels: Int = 2,
    val sampleRate: Int = 44100
) {
    val isVideo: Boolean
        get() = mimeType.startsWith("video/")

    val isAudio: Boolean
        get() = mimeType.startsWith("audio/")

    val isImage: Boolean
        get() = mimeType.startsWith("image/")
}
