# CapCut 19.8.0 Observable Feature Inventory

This comprehensive inventory documents the observable feature set derived from static code extraction, resource analysis, manifest inspection, and architectural decomposition of CapCut 19.8.0 (`com.lemon.lvoverseas`).

---

## A. Project Management
- **Create & Open Projects:** New project wizard accepting mixed video/photo selection; instant draft directory allocation with UUID (`draftId`).
- **Import Formats:** Videos (MP4, MOV, MKV, 3GP, WebM), Images (JPEG, PNG, WebP, GIF, HEIF), Audio (MP3, AAC, M4A, WAV, FLAC, OGG).
- **Canvas & Aspect Ratios:** 9:16 (TikTok/Reels/Shorts), 16:9 (YouTube), 1:1 (Square), 4:3, 3:4, 2:1, 2.35:1 (Cinematic), and auto-fit original media aspect ratio.
- **Persistence & Autosave:** Continuous draft serialization to `draft_content.json` after every state alteration; crash recovery journal.
- **Missing-Media Handling:** Path verification against content URIs; media relinking interface upon detection of moved or deleted assets.
- **Draft Management:** Duplicate draft, rename, delete to trash, multi-draft selection.

---

## B. Preview and Timeline
- **Player Surface:** Hardware-accelerated OpenGL surface texture with 60fps refresh and real-time canvas resizing.
- **Playback Controls:** Play, pause, jump to beginning/end, frame-by-frame forward/back stepping.
- **Multitrack Hierarchy:**
  1. Primary Video Track (`main_track`, magnetic ripple editing).
  2. Overlay Video Tracks (Picture-in-Picture / PIP).
  3. Audio Tracks (BGM, sound effects, voiceovers).
  4. Text & Subtitle Tracks.
  5. Sticker & Graphic Overlay Tracks.
  6. Filter & Global Effect Tracks.
- **Scrubbing & Zooming:** Pinch-to-zoom timeline scale (from 1 second per width down to frame-level zoom); magnetic snapping to clip boundaries, playhead, and beat markers with haptic ticks.
- **Timecode Precision:** Integer microsecond (`us`) precision displayed as `MM:SS:FF` or `HH:MM:SS:FF`.

---

## C. Basic Editing
- **Trim & Cut:** Drag handles at clip head and tail; ripple trimming on main track.
- **Split:** Precision playhead splitting into two independent segments with retained attributes.
- **Delete & Duplicate:** Single-tap clip deletion with automatic gap closing; instant clip duplication.
- **Reordering:** Long-press drag-and-drop to reorder clips on main track and PIP tracks.
- **Transform:** Pan (X/Y translation), 2-finger pinch scale, rotation (90° increments or arbitrary angle), horizontal/vertical flip.
- **Speed & Timing:** Linear speed adjustment (0.1x to 100x), speed curve presets (Hero, Montage, Bullet, Jump Cut), audio pitch preservation toggle (`keep_pitch`), reverse video playback, freeze frame insertion.

---

## D. Advanced Timeline Editing
- **Keyframe Animation:** Diamond keyframe markers on transform (position, scale, rotation), opacity, and volume with linear, ease-in, ease-out, and Bezier curves.
- **Transitions:** Dissolve, wipe, slide, zoom, glitch, 3D, and light leak transitions with adjustable duration.
- **Compositing & Blending Modes:** Normal, Overlay, Multiply, Screen, Darken, Lighten, Color Dodge, Soft Light, Hard Light.
- **Masks:** Linear, Mirror, Radial, Rectangle, Heart, and Star masks with invert toggle and edge feathering.
- **Chroma Key (Green Screen):** Color picker dropper, intensity threshold, shadow adjustment, and edge spill reduction.
- **Picture-in-Picture (PIP):** Dedicated secondary tracks with full independent transforms and blend modes.

---

## E. Color, Effects, and Filters
- **Color Grading Adjustments:**
  - Exposure, Brightness, Contrast, Saturation.
  - Highlights, Shadows, Whites, Blacks.
  - Temperature (Warmth), Tint, Hue.
  - Vignette, Grain, Sharpen, Clarity, Fade.
- **Lookup Tables (LUTs):** 3D LUT texture mapping for cinematic and lifestyle color presets.
- **Video & Body Effects:** Blur, glow, strobe, edge highlight, retro VHS, zoom blur, and motion distortion.

---

## F. Text and Graphics
- **Text Layers:** Custom typography with system and embedded TTF/OTF fonts.
- **Styling:** Fill color, stroke/outline width & color, drop shadow, background bubble, glow, opacity, letter spacing, line height.
- **Text Animation:** In-animations, Out-animations, and Loop animations (Typewriter, Fade, Bounce, Slide).
- **Auto-Captions & Subtitles:** Speech-to-text subtitle generation, bulk subtitle editing, time synchronization, SRT import/export.

---

## G. Audio Engine
- **Multitrack Mixing:** Independent track volume sliders (-∞ to +20 dB), master volume, mute track toggle.
- **Envelopes:** Fade-in and fade-out duration handles with linear/exponential volume curves.
- **Audio Trimming & Splitting:** Waveform display rendering; beat detection marker integration.
- **Voiceover Recording:** Low-latency microphone recording directly onto an audio track synced with preview.
- **Voice Effects:** Pitch shifting, robotic, chipmunk, deep, megaphone, reverb, and noise reduction.

---

## H. Export Pipeline
- **Resolution Output:** 480p, 720p, 1080p (Full HD), 2K, 4K (UHD).
- **Frame Rates:** 24 fps, 25 fps, 30 fps, 50 fps, 60 fps.
- **Video Codecs:** H.264 (AVC baseline/high), H.265 (HEVC), ByteVC1.
- **Bitrate Modes:** Smart recommended bitrate, Higher bitrate (lossless visual quality), Lower bitrate (compact storage).
- **Audio Output:** AAC-LC (128 - 320 kbps), 44.1 kHz / 48 kHz stereo.
- **Export UX:** Background rendering service, percentage progress bar, cancel option, output preview, and direct sharing intent.

---

## I. AI-Assisted Features
- **Local On-Device Execution:**
  - Auto-captions via local speech model (`libwhisper.so` / `libspeechsdk.so`).
  - Chroma keying and color segmentation.
  - Beat detection via FFT audio analysis.
- **Hybrid / Model Required:**
  - Portrait cutout / smart background removal (`libbytenn.so` + TFLite/NCNN models).
  - Face tracking & smoothing (`libretouch_sdk.so`).
- **Cloud / Server-Dependent:**
  - Cloud text-to-image / AI video generation (`AiLabAgentUniversalPreviewActivity`).
  - Text-to-speech cloud voices and tone cloning.
