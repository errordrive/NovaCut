package com.procut.model

import kotlinx.serialization.Serializable

@Serializable
enum class TrackType(val isMagnetic: Boolean, val displayName: String) {
    /**
     * The primary magnetic video track.
     * Enforces continuous, gapless playback starting at t=0.
     * Any insert, delete, split, or trim operation triggers automatic downstream ripple.
     */
    MAIN_VIDEO(isMagnetic = true, displayName = "Main Video"),

    /**
     * Picture-in-picture overlay tracks.
     * Allows free positioning, gaps, and multi-layer visual stacking above the main track.
     */
    PIP_OVERLAY(isMagnetic = false, displayName = "Overlay (PIP)"),

    /**
     * Audio tracks for background music, extracted audio, sound effects, and voiceover.
     * Allows free positioning and independent volume/panning envelopes.
     */
    AUDIO(isMagnetic = false, displayName = "Audio"),

    /**
     * Text and sticker tracks for titles, captions, and animated overlays.
     */
    TEXT(isMagnetic = false, displayName = "Text & Captions"),

    /**
     * Global and adjustment effect tracks applying filters across underlying tracks.
     */
    EFFECT(isMagnetic = false, displayName = "Effects")
}
