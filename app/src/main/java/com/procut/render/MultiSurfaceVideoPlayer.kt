package com.procut.render

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.view.Surface
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.procut.model.ClipSegment
import com.procut.model.EditorProject
import java.io.File

class ClipDecoderHolder(
    val clipId: String,
    val textureId: Int,
    val surfaceTexture: SurfaceTexture,
    val surface: Surface,
    val player: ExoPlayer
) {
    fun release() {
        player.release()
        surface.release()
        surfaceTexture.release()
        val textures = intArrayOf(textureId)
        GLES30.glDeleteTextures(1, textures, 0)
    }
}

class MultiSurfaceVideoPlayer(private val context: Context) {

    private val decoders = mutableMapOf<String, ClipDecoderHolder>() // clipId -> ClipDecoderHolder

    @Synchronized
    fun syncWithProject(project: EditorProject, playheadUs: Long) {
        val activeClipIds = mutableSetOf<String>()

        // 1. Identify active clips at playheadUs
        project.mainTrack?.findClipAt(playheadUs)?.let { activeClipIds.add(it.id) }
        for (track in project.overlayTracks) {
            track.findClipAt(playheadUs)?.let { activeClipIds.add(it.id) }
        }

        // 2. Release decoders no longer needed
        val toRemove = decoders.keys.filterNot { it in activeClipIds }
        for (clipId in toRemove) {
            decoders.remove(clipId)?.release()
        }

        // 3. Create decoders for newly active clips
        for (clipId in activeClipIds) {
            if (!decoders.containsKey(clipId)) {
                val pair = project.findClipById(clipId) ?: continue
                val clip = pair.second
                val asset = project.findAssetById(clip.assetId) ?: continue
                createDecoderForClip(clip, asset.filePath)
            }
        }

        // 4. Update playhead time offset
        val playheadMs = playheadUs / 1000L
        for ((clipId, holder) in decoders) {
            val clip = project.findClipById(clipId)?.second ?: continue
            val clipStartMs = clip.targetTimeRangeUs.startUs / 1000L
            val clipTargetOffsetMs = playheadMs - clipStartMs

            // Map target time offset to source media offset
            val sourceOffsetUs = clip.speedConfig.mapTargetTimeToSourceTime(
                targetOffsetUs = clipTargetOffsetMs * 1000L,
                targetDurationUs = clip.targetTimeRangeUs.durationUs,
                sourceDurationUs = clip.sourceTimeRangeUs.durationUs
            )
            val desiredMediaPositionMs = (clip.sourceTimeRangeUs.startUs + sourceOffsetUs) / 1000L

            if (Math.abs(holder.player.currentPosition - desiredMediaPositionMs) > 100) {
                holder.player.seekTo(desiredMediaPositionMs)
            }
        }
    }

    private fun createDecoderForClip(clip: ClipSegment, filePath: String) {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        val texId = textures[0]

        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        val surfaceTexture = SurfaceTexture(texId)
        val surface = Surface(surfaceTexture)

        val exoPlayer = ExoPlayer.Builder(context).build().apply {
            setVideoSurface(surface)
            setMediaItem(MediaItem.fromUri(filePath))
            prepare()
            playWhenReady = false
        }

        decoders[clip.id] = ClipDecoderHolder(
            clipId = clip.id,
            textureId = texId,
            surfaceTexture = surfaceTexture,
            surface = surface,
            player = exoPlayer
        )
    }

    @Synchronized
    fun getResolvedTextureMap(): Map<String, Int> {
        return decoders.mapValues { (_, holder) ->
            holder.surfaceTexture.updateTexImage()
            holder.textureId
        }
    }

    @Synchronized
    fun release() {
        for (holder in decoders.values) {
            holder.release()
        }
        decoders.clear()
    }
}
