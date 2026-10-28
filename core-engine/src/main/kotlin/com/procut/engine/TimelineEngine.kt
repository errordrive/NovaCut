package com.procut.engine

import com.procut.model.*
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

class TimelineEngine {

    /**
     * Re-ripples a magnetic track to ensure clips start at t=0 and are strictly contiguous with zero gaps.
     */
    fun rippleMagneticTrack(track: Track): Track {
        if (!track.isMagnetic) return track
        var currentStartUs = 0L
        val rippledClips = track.clips.map { clip ->
            val updatedTarget = clip.targetTimeRangeUs.copy(startUs = currentStartUs)
            currentStartUs += updatedTarget.durationUs
            clip.copy(targetTimeRangeUs = updatedTarget)
        }
        return track.copy(clips = rippledClips)
    }

    /**
     * Adds a media clip to the specified track.
     * If track is magnetic, it is appended to the end of the track (or ripple-inserted).
     * If track is free, it is placed at [startUs] (defaulting to playhead or 0).
     */
    fun addClip(
        project: EditorProject,
        trackId: String,
        asset: MediaAsset,
        sourceStartUs: Long = 0L,
        sourceDurationUs: Long = asset.durationUs,
        startUs: Long? = null
    ): EditorProject {
        val track = project.findTrackById(trackId)
            ?: throw IllegalArgumentException("Track not found: $trackId")

        val validSourceDuration = sourceDurationUs.coerceIn(0L, asset.durationUs - sourceStartUs)
        val sourceTimeRange = TimeRangeUs(sourceStartUs, validSourceDuration)
        val speedConfig = SpeedConfig.NORMAL
        val targetDurationUs = speedConfig.computeTargetDurationUs(validSourceDuration)

        val targetStartUs = if (track.isMagnetic) {
            track.totalDurationUs
        } else {
            startUs ?: project.playheadPositionUs
        }

        val targetTimeRange = TimeRangeUs(targetStartUs, targetDurationUs)
        val newClip = ClipSegment(
            id = UUID.randomUUID().toString(),
            assetId = asset.id,
            trackId = track.id,
            sourceTimeRangeUs = sourceTimeRange,
            targetTimeRangeUs = targetTimeRange,
            speedConfig = speedConfig
        )

        val updatedClips = track.clips + newClip
        val updatedTrack = if (track.isMagnetic) {
            rippleMagneticTrack(track.copy(clips = updatedClips))
        } else {
            track.copy(clips = updatedClips.sortedBy { it.targetTimeRangeUs.startUs })
        }

        val updatedTracks = project.tracks.map { if (it.id == track.id) updatedTrack else it }
        val updatedAssets = if (project.assets.none { it.id == asset.id }) {
            project.assets + asset
        } else {
            project.assets
        }

        return project.copy(
            tracks = updatedTracks,
            assets = updatedAssets,
            updatedAtUs = System.currentTimeMillis() * 1000L
        )
    }

    /**
     * Splits a clip at [splitTimestampUs].
     * Recalculates source and target time ranges for both halves and splits keyframes.
     * Downstream clips on magnetic tracks remain intact.
     */
    fun splitClip(
        project: EditorProject,
        trackId: String,
        clipId: String,
        splitTimestampUs: Long
    ): EditorProject {
        val track = project.findTrackById(trackId)
            ?: throw IllegalArgumentException("Track not found: $trackId")
        val clip = track.findClipById(clipId)
            ?: throw IllegalArgumentException("Clip not found: $clipId")

        val targetRange = clip.targetTimeRangeUs
        require(splitTimestampUs > targetRange.startUs && splitTimestampUs < targetRange.endUs) {
            "splitTimestampUs ($splitTimestampUs) must be strictly inside clip target range [${targetRange.startUs}, ${targetRange.endUs})"
        }

        val targetOffset1Us = splitTimestampUs - targetRange.startUs
        val targetDuration1Us = targetOffset1Us
        val targetDuration2Us = targetRange.durationUs - targetOffset1Us

        // Map split point to source time
        val sourceOffsetSplitUs = clip.speedConfig.mapTargetTimeToSourceTime(
            targetOffsetUs = targetOffset1Us,
            targetDurationUs = targetRange.durationUs,
            sourceDurationUs = clip.sourceTimeRangeUs.durationUs
        )

        val sourceDuration1Us = sourceOffsetSplitUs
        val sourceDuration2Us = clip.sourceTimeRangeUs.durationUs - sourceOffsetSplitUs

        val sourceRange1 = TimeRangeUs(clip.sourceTimeRangeUs.startUs, sourceDuration1Us)
        val sourceRange2 = TimeRangeUs(clip.sourceTimeRangeUs.startUs + sourceOffsetSplitUs, sourceDuration2Us)

        val targetRange1 = TimeRangeUs(targetRange.startUs, targetDuration1Us)
        val targetRange2 = TimeRangeUs(splitTimestampUs, targetDuration2Us)

        // Partition keyframes
        val keyframes1 = clip.keyframes.filter { it.timeOffsetUs < targetOffset1Us }
        val keyframes2 = clip.keyframes.filter { it.timeOffsetUs >= targetOffset1Us }.map {
            it.copy(
                id = UUID.randomUUID().toString(),
                timeOffsetUs = it.timeOffsetUs - targetOffset1Us
            )
        }

        val clip1 = clip.copy(
            sourceTimeRangeUs = sourceRange1,
            targetTimeRangeUs = targetRange1,
            keyframes = keyframes1
        )

        val clip2 = clip.copy(
            id = UUID.randomUUID().toString(),
            sourceTimeRangeUs = sourceRange2,
            targetTimeRangeUs = targetRange2,
            keyframes = keyframes2,
            transitionIn = null // New right half resets transition
        )

        val clipIndex = track.clips.indexOfFirst { it.id == clipId }
        val newClips = track.clips.toMutableList()
        newClips.removeAt(clipIndex)
        newClips.add(clipIndex, clip2)
        newClips.add(clipIndex, clip1)

        val updatedTrack = if (track.isMagnetic) {
            rippleMagneticTrack(track.copy(clips = newClips))
        } else {
            track.copy(clips = newClips.sortedBy { it.targetTimeRangeUs.startUs })
        }

        return project.copy(
            tracks = project.tracks.map { if (it.id == track.id) updatedTrack else it },
            updatedAtUs = System.currentTimeMillis() * 1000L
        )
    }

    /**
     * Trims the start (left edge) of a clip.
     * Advances source start timestamp and shortens target duration.
     * On magnetic track, ripples downstream clips left.
     */
    fun trimClipStart(
        project: EditorProject,
        trackId: String,
        clipId: String,
        newTargetStartUs: Long
    ): EditorProject {
        val track = project.findTrackById(trackId)
            ?: throw IllegalArgumentException("Track not found: $trackId")
        val clip = track.findClipById(clipId)
            ?: throw IllegalArgumentException("Clip not found: $clipId")

        val oldTarget = clip.targetTimeRangeUs
        val minDurationUs = 100_000L // 100ms minimum clip length

        val clampedNewStart = newTargetStartUs.coerceIn(
            oldTarget.startUs - clip.speedConfig.computeTargetDurationUs(clip.sourceTimeRangeUs.startUs),
            oldTarget.endUs - minDurationUs
        )

        val deltaTargetUs = clampedNewStart - oldTarget.startUs
        if (deltaTargetUs == 0L) return project

        val deltaSourceUs = clip.speedConfig.mapTargetTimeToSourceTime(
            targetOffsetUs = abs(deltaTargetUs),
            targetDurationUs = oldTarget.durationUs,
            sourceDurationUs = clip.sourceTimeRangeUs.durationUs
        ) * if (deltaTargetUs > 0) 1 else -1

        val newSourceStartUs = (clip.sourceTimeRangeUs.startUs + deltaSourceUs).coerceAtLeast(0L)
        val newSourceDurationUs = (clip.sourceTimeRangeUs.durationUs - deltaSourceUs).coerceAtLeast(10_000L)
        val newTargetDurationUs = clip.speedConfig.computeTargetDurationUs(newSourceDurationUs)

        val updatedClip = clip.copy(
            sourceTimeRangeUs = TimeRangeUs(newSourceStartUs, newSourceDurationUs),
            targetTimeRangeUs = TimeRangeUs(clampedNewStart, newTargetDurationUs),
            // Adjust keyframe offsets
            keyframes = clip.keyframes.filter { it.timeOffsetUs >= deltaTargetUs }.map {
                it.copy(timeOffsetUs = it.timeOffsetUs - deltaTargetUs)
            }
        )

        val updatedClips = track.clips.map { if (it.id == clipId) updatedClip else it }
        val updatedTrack = if (track.isMagnetic) {
            rippleMagneticTrack(track.copy(clips = updatedClips))
        } else {
            track.copy(clips = updatedClips.sortedBy { it.targetTimeRangeUs.startUs })
        }

        return project.copy(
            tracks = project.tracks.map { if (it.id == track.id) updatedTrack else it },
            updatedAtUs = System.currentTimeMillis() * 1000L
        )
    }

    /**
     * Trims the end (right edge) of a clip.
     * Shortens or extends source and target duration.
     * On magnetic track, ripples downstream clips.
     */
    fun trimClipEnd(
        project: EditorProject,
        trackId: String,
        clipId: String,
        newTargetEndUs: Long
    ): EditorProject {
        val track = project.findTrackById(trackId)
            ?: throw IllegalArgumentException("Track not found: $trackId")
        val clip = track.findClipById(clipId)
            ?: throw IllegalArgumentException("Clip not found: $clipId")

        val asset = project.findAssetById(clip.assetId)
        val oldTarget = clip.targetTimeRangeUs
        val minDurationUs = 100_000L

        val maxAvailableSourceDuration = if (asset != null) {
            asset.durationUs - clip.sourceTimeRangeUs.startUs
        } else {
            Long.MAX_VALUE
        }
        val maxTargetDuration = clip.speedConfig.computeTargetDurationUs(maxAvailableSourceDuration)
        val maxTargetEndUs = oldTarget.startUs + maxTargetDuration

        val clampedEndUs = newTargetEndUs.coerceIn(oldTarget.startUs + minDurationUs, maxTargetEndUs)
        val newTargetDurationUs = clampedEndUs - oldTarget.startUs

        val deltaTargetUs = newTargetDurationUs - oldTarget.durationUs
        if (deltaTargetUs == 0L) return project

        val deltaSourceUs = clip.speedConfig.mapTargetTimeToSourceTime(
            targetOffsetUs = abs(deltaTargetUs),
            targetDurationUs = oldTarget.durationUs,
            sourceDurationUs = clip.sourceTimeRangeUs.durationUs
        ) * if (deltaTargetUs > 0) 1 else -1

        val newSourceDurationUs = (clip.sourceTimeRangeUs.durationUs + deltaSourceUs).coerceAtLeast(10_000L)

        val updatedClip = clip.copy(
            sourceTimeRangeUs = TimeRangeUs(clip.sourceTimeRangeUs.startUs, newSourceDurationUs),
            targetTimeRangeUs = TimeRangeUs(oldTarget.startUs, newTargetDurationUs),
            keyframes = clip.keyframes.filter { it.timeOffsetUs <= newTargetDurationUs }
        )

        val updatedClips = track.clips.map { if (it.id == clipId) updatedClip else it }
        val updatedTrack = if (track.isMagnetic) {
            rippleMagneticTrack(track.copy(clips = updatedClips))
        } else {
            track.copy(clips = updatedClips.sortedBy { it.targetTimeRangeUs.startUs })
        }

        return project.copy(
            tracks = project.tracks.map { if (it.id == track.id) updatedTrack else it },
            updatedAtUs = System.currentTimeMillis() * 1000L
        )
    }

    /**
     * Slips media content inside clip boundaries without changing its timeline position or duration.
     */
    fun slipClipContent(
        project: EditorProject,
        trackId: String,
        clipId: String,
        deltaSourceUs: Long
    ): EditorProject {
        val track = project.findTrackById(trackId)
            ?: throw IllegalArgumentException("Track not found: $trackId")
        val clip = track.findClipById(clipId)
            ?: throw IllegalArgumentException("Clip not found: $clipId")
        val asset = project.findAssetById(clip.assetId)

        val maxSourceStart = if (asset != null) {
            asset.durationUs - clip.sourceTimeRangeUs.durationUs
        } else {
            Long.MAX_VALUE
        }

        val newSourceStartUs = (clip.sourceTimeRangeUs.startUs + deltaSourceUs).coerceIn(0L, maxSourceStart)
        val updatedClip = clip.copy(
            sourceTimeRangeUs = clip.sourceTimeRangeUs.copy(startUs = newSourceStartUs)
        )

        val updatedClips = track.clips.map { if (it.id == clipId) updatedClip else it }
        return project.copy(
            tracks = project.tracks.map { if (it.id == track.id) track.copy(clips = updatedClips) else it },
            updatedAtUs = System.currentTimeMillis() * 1000L
        )
    }

    /**
     * Deletes a clip. On magnetic tracks, closes the resulting gap automatically.
     */
    fun deleteClip(
        project: EditorProject,
        trackId: String,
        clipId: String
    ): EditorProject {
        val track = project.findTrackById(trackId)
            ?: throw IllegalArgumentException("Track not found: $trackId")

        val remainingClips = track.clips.filterNot { it.id == clipId }
        val updatedTrack = if (track.isMagnetic) {
            rippleMagneticTrack(track.copy(clips = remainingClips))
        } else {
            track.copy(clips = remainingClips)
        }

        return project.copy(
            tracks = project.tracks.map { if (it.id == track.id) updatedTrack else it },
            updatedAtUs = System.currentTimeMillis() * 1000L
        )
    }

    /**
     * Duplicates a clip. On magnetic tracks, places it immediately after the original clip with ripple.
     */
    fun duplicateClip(
        project: EditorProject,
        trackId: String,
        clipId: String
    ): EditorProject {
        val track = project.findTrackById(trackId)
            ?: throw IllegalArgumentException("Track not found: $trackId")
        val clip = track.findClipById(clipId)
            ?: throw IllegalArgumentException("Clip not found: $clipId")

        val duplicatedClip = clip.copy(
            id = UUID.randomUUID().toString(),
            targetTimeRangeUs = TimeRangeUs(clip.targetTimeRangeUs.endUs, clip.targetTimeRangeUs.durationUs),
            keyframes = clip.keyframes.map { it.copy(id = UUID.randomUUID().toString()) }
        )

        val clipIndex = track.clips.indexOfFirst { it.id == clipId }
        val newClips = track.clips.toMutableList()
        newClips.add(clipIndex + 1, duplicatedClip)

        val updatedTrack = if (track.isMagnetic) {
            rippleMagneticTrack(track.copy(clips = newClips))
        } else {
            track.copy(clips = newClips.sortedBy { it.targetTimeRangeUs.startUs })
        }

        return project.copy(
            tracks = project.tracks.map { if (it.id == track.id) updatedTrack else it },
            updatedAtUs = System.currentTimeMillis() * 1000L
        )
    }

    /**
     * Reverses clip playback direction and inverts keyframe positions.
     */
    fun reverseClip(
        project: EditorProject,
        trackId: String,
        clipId: String
    ): EditorProject {
        val track = project.findTrackById(trackId)
            ?: throw IllegalArgumentException("Track not found: $trackId")
        val clip = track.findClipById(clipId)
            ?: throw IllegalArgumentException("Clip not found: $clipId")

        val reversedKeyframes = clip.keyframes.map {
            it.copy(timeOffsetUs = clip.targetTimeRangeUs.durationUs - it.timeOffsetUs)
        }

        val updatedClip = clip.copy(
            isReversed = !clip.isReversed,
            keyframes = reversedKeyframes
        )

        val updatedClips = track.clips.map { if (it.id == clipId) updatedClip else it }
        return project.copy(
            tracks = project.tracks.map { if (it.id == track.id) track.copy(clips = updatedClips) else it },
            updatedAtUs = System.currentTimeMillis() * 1000L
        )
    }

    /**
     * Freezes frame at playhead for [freezeDurationUs] (default 3 seconds).
     * Splits clip, inserts a 0-speed freeze slice, and ripples downstream clips.
     */
    fun freezeFrameAtPlayhead(
        project: EditorProject,
        trackId: String,
        clipId: String,
        playheadUs: Long,
        freezeDurationUs: Long = 3_000_000L
    ): EditorProject {
        val track = project.findTrackById(trackId)
            ?: throw IllegalArgumentException("Track not found: $trackId")
        val clip = track.findClipById(clipId)
            ?: throw IllegalArgumentException("Clip not found: $clipId")

        require(playheadUs in clip.targetTimeRangeUs.startUs..clip.targetTimeRangeUs.endUs) {
            "Playhead ($playheadUs) must be within clip boundaries"
        }

        // Map playhead to source timestamp
        val targetOffsetUs = playheadUs - clip.targetTimeRangeUs.startUs
        val sourceTimestampUs = clip.sourceTimeRangeUs.startUs + clip.speedConfig.mapTargetTimeToSourceTime(
            targetOffsetUs = targetOffsetUs,
            targetDurationUs = clip.targetTimeRangeUs.durationUs,
            sourceDurationUs = clip.sourceTimeRangeUs.durationUs
        )

        // Split original into left and right
        val targetDurationLeft = targetOffsetUs
        val targetDurationRight = clip.targetTimeRangeUs.durationUs - targetOffsetUs

        val sourceDurationLeft = sourceTimestampUs - clip.sourceTimeRangeUs.startUs
        val sourceDurationRight = clip.sourceTimeRangeUs.endUs - sourceTimestampUs

        val leftClip = clip.copy(
            sourceTimeRangeUs = TimeRangeUs(clip.sourceTimeRangeUs.startUs, sourceDurationLeft),
            targetTimeRangeUs = TimeRangeUs(clip.targetTimeRangeUs.startUs, targetDurationLeft)
        )

        // Freeze slice: 1 microsecond source duration stretched over freezeDurationUs
        val freezeClip = ClipSegment(
            id = UUID.randomUUID().toString(),
            assetId = clip.assetId,
            trackId = clip.trackId,
            sourceTimeRangeUs = TimeRangeUs(sourceTimestampUs, 1_000L),
            targetTimeRangeUs = TimeRangeUs(playheadUs, freezeDurationUs),
            speedConfig = SpeedConfig.NORMAL,
            transform = clip.evaluateTransformAt(targetOffsetUs)
        )

        val rightClip = clip.copy(
            id = UUID.randomUUID().toString(),
            sourceTimeRangeUs = TimeRangeUs(sourceTimestampUs, sourceDurationRight),
            targetTimeRangeUs = TimeRangeUs(playheadUs + freezeDurationUs, targetDurationRight),
            transitionIn = null
        )

        val clipIndex = track.clips.indexOfFirst { it.id == clipId }
        val newClips = track.clips.toMutableList()
        newClips.removeAt(clipIndex)

        if (targetDurationRight > 0) newClips.add(clipIndex, rightClip)
        newClips.add(clipIndex, freezeClip)
        if (targetDurationLeft > 0) newClips.add(clipIndex, leftClip)

        val updatedTrack = if (track.isMagnetic) {
            rippleMagneticTrack(track.copy(clips = newClips))
        } else {
            track.copy(clips = newClips.sortedBy { it.targetTimeRangeUs.startUs })
        }

        return project.copy(
            tracks = project.tracks.map { if (it.id == track.id) updatedTrack else it },
            updatedAtUs = System.currentTimeMillis() * 1000L
        )
    }

    /**
     * Extracts audio from a video clip into a dedicated AUDIO track, muting the original clip.
     */
    fun extractAudioToTrack(
        project: EditorProject,
        sourceClipId: String
    ): EditorProject {
        val (sourceTrack, sourceClip) = project.findClipById(sourceClipId)
            ?: throw IllegalArgumentException("Clip not found: $sourceClipId")

        // Mute source video clip
        val mutedSourceClip = sourceClip.copy(isMuted = true)
        val updatedSourceTrack = sourceTrack.copy(
            clips = sourceTrack.clips.map { if (it.id == sourceClipId) mutedSourceClip else it }
        )

        // Find or create an audio track
        var audioTrack = project.audioTracks.firstOrNull()
        val isNewTrack = audioTrack == null
        if (audioTrack == null) {
            audioTrack = Track(
                id = UUID.randomUUID().toString(),
                type = TrackType.AUDIO,
                name = "Extracted Audio"
            )
        }

        val extractedAudioClip = ClipSegment(
            id = UUID.randomUUID().toString(),
            assetId = sourceClip.assetId,
            trackId = audioTrack.id,
            sourceTimeRangeUs = sourceClip.sourceTimeRangeUs,
            targetTimeRangeUs = sourceClip.targetTimeRangeUs,
            speedConfig = sourceClip.speedConfig,
            volume = sourceClip.volume
        )

        val updatedAudioTrack = audioTrack.copy(
            clips = (audioTrack.clips + extractedAudioClip).sortedBy { it.targetTimeRangeUs.startUs }
        )

        val newTracks = project.tracks.map { track ->
            when (track.id) {
                sourceTrack.id -> updatedSourceTrack
                audioTrack.id -> updatedAudioTrack
                else -> track
            }
        }.toMutableList()

        if (isNewTrack) {
            newTracks.add(updatedAudioTrack)
        }

        return project.copy(
            tracks = newTracks,
            updatedAtUs = System.currentTimeMillis() * 1000L
        )
    }

    /**
     * Applies constant speed multiplier to clip, updating target duration and rippling downstream if magnetic.
     */
    fun applyConstantSpeed(
        project: EditorProject,
        trackId: String,
        clipId: String,
        speedMultiplier: Double
    ): EditorProject {
        require(speedMultiplier > 0.0) { "Speed multiplier must be positive" }
        val track = project.findTrackById(trackId)
            ?: throw IllegalArgumentException("Track not found: $trackId")
        val clip = track.findClipById(clipId)
            ?: throw IllegalArgumentException("Clip not found: $clipId")

        val newSpeedConfig = SpeedConfig(
            mode = SpeedMode.CONSTANT,
            speedMultiplier = speedMultiplier,
            isPitchPreserved = clip.speedConfig.isPitchPreserved
        )

        val newTargetDurationUs = newSpeedConfig.computeTargetDurationUs(clip.sourceTimeRangeUs.durationUs)
        val updatedClip = clip.copy(
            speedConfig = newSpeedConfig,
            targetTimeRangeUs = clip.targetTimeRangeUs.copy(durationUs = newTargetDurationUs)
        )

        val updatedClips = track.clips.map { if (it.id == clipId) updatedClip else it }
        val updatedTrack = if (track.isMagnetic) {
            rippleMagneticTrack(track.copy(clips = updatedClips))
        } else {
            track.copy(clips = updatedClips)
        }

        return project.copy(
            tracks = project.tracks.map { if (it.id == track.id) updatedTrack else it },
            updatedAtUs = System.currentTimeMillis() * 1000L
        )
    }

    /**
     * Applies curve speed ramp to clip, computing target duration via integral pacing.
     */
    fun applyCurveSpeedRamp(
        project: EditorProject,
        trackId: String,
        clipId: String,
        curveSpeed: SpeedConfig
    ): EditorProject {
        require(curveSpeed.mode == SpeedMode.CURVE) { "SpeedConfig must be CURVE mode" }
        val track = project.findTrackById(trackId)
            ?: throw IllegalArgumentException("Track not found: $trackId")
        val clip = track.findClipById(clipId)
            ?: throw IllegalArgumentException("Clip not found: $clipId")

        val newTargetDurationUs = curveSpeed.computeTargetDurationUs(clip.sourceTimeRangeUs.durationUs)
        val updatedClip = clip.copy(
            speedConfig = curveSpeed,
            targetTimeRangeUs = clip.targetTimeRangeUs.copy(durationUs = newTargetDurationUs)
        )

        val updatedClips = track.clips.map { if (it.id == clipId) updatedClip else it }
        val updatedTrack = if (track.isMagnetic) {
            rippleMagneticTrack(track.copy(clips = updatedClips))
        } else {
            track.copy(clips = updatedClips)
        }

        return project.copy(
            tracks = project.tracks.map { if (it.id == track.id) updatedTrack else it },
            updatedAtUs = System.currentTimeMillis() * 1000L
        )
    }

    /**
     * Computes magnetic snap timestamp for UI drag/trim operations.
     * Snaps to: playhead, 0, track boundaries, and clip boundaries within [attractionWindowUs].
     */
    fun computeMagneticSnap(
        project: EditorProject,
        candidateTimestampUs: Long,
        attractionWindowUs: Long = 100_000L, // 100ms default attraction window
        ignoredClipId: String? = null
    ): Long {
        val snapTargets = mutableSetOf<Long>()
        snapTargets.add(0L)
        snapTargets.add(project.playheadPositionUs)
        snapTargets.add(project.totalDurationUs)

        for (track in project.tracks) {
            for (clip in track.clips) {
                if (clip.id == ignoredClipId) continue
                snapTargets.add(clip.targetTimeRangeUs.startUs)
                snapTargets.add(clip.targetTimeRangeUs.endUs)
                for (kf in clip.keyframes) {
                    snapTargets.add(clip.targetTimeRangeUs.startUs + kf.timeOffsetUs)
                }
            }
        }

        var closestTarget: Long? = null
        var minDistance = Long.MAX_VALUE

        for (target in snapTargets) {
            val dist = abs(candidateTimestampUs - target)
            if (dist <= attractionWindowUs && dist < minDistance) {
                minDistance = dist
                closestTarget = target
            }
        }

        return closestTarget ?: candidateTimestampUs
    }
}
