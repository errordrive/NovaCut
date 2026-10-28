# Technical Specification 04: Audio Engine, Typography/Stickers & Hardware Export Pipeline

## Executive Overview
This specification details the architecture, data models, DSP algorithms, typography rendering pipeline, and hardware-accelerated video export subsystem reverse-engineered from `./target-artifacts/capcut.apk` (CapCut v19.8.0). It establishes the technical foundation for clean-room implementation of a premium, multi-track Android video editing and rendering engine.

---

## 1. Multi-Track Audio & Waveform Subsystem

### 1.1 Observed Facts (Static Evidence & JNI Bindings)
- **Native Audio DSP Library**: `lib/arm64-v8a/libaudioeffect.so` (Size: 5,083,304 bytes).
  - Contains ByteDance audio DSP engines: `mammon` and `mammonengine::DSP`.
  - Core exported native symbols:
    - Multi-track mixing: `mammonengine::DSP::Mixer::compute(std::vector<AudioStream const*> const&, float**, unsigned long, unsigned long, std::vector<std::vector<float>>&)`
    - Pitch/Tempo time-stretching: `mammon::PitchTempoAdjuster::process(float const* const*, unsigned long, bool)`, `mammon::PitchTempoAdjuster::cutShiftAndFold(float*, int, float*, mammon::Window<float>*)`, `mammon::TimeStretcherFast::process(float const* const*, int, bool)`, `mammon::StretchCalculator::mapPeaks`
    - Equalizer & Biquad filters: `mammon::EqualizerParametricX::setParameters`, `webrtcimported::CascadedBiQuadFilter::BiQuad`
    - Loudness / Volume: `mammon::VolumeDetectorX::getOverallFeatures()`, `mammon::LoudnessExtractor::getOverallFeatures()`, `Process_VolumeInst(VolumeDetectionObj&, std::vector<short> const&, std::vector<std::pair<float, float>>&)`
    - High-quality resampling: `speex_resampler_process_interleaved_float`, `webrtcimported::PushSincResampler`
- **AAC Audio Encoding Library**: `lib/arm64-v8a/libfdk-aac.so` (Size: 625,312 bytes).
- **DEX Model Classes**:
  - `com.vega.draft.data.template.material.MaterialAudio` ([`MaterialAudio.java`](file:///storage/emulated/0/ApkLab/CapCutREA/scratch/capcut-decompiled/jadx-out/single/sources/com/vega/draft/data/template/material/MaterialAudio.java)): Defines `duration` (long, $\mu\text{s}$), `path` (String), `wavePoints` (`List<Float>`), `musicId`, `toneType`, `intensifiesPath`.
  - `com.vega.draft.data.template.material.MaterialAudioFade` ([`MaterialAudioFade.java`](file:///storage/emulated/0/ApkLab/CapCutREA/scratch/capcut-decompiled/jadx-out/single/sources/com/vega/draft/data/template/material/MaterialAudioFade.java)): Defines `fadeInDuration` (long, $\mu\text{s}$), `fadeOutDuration` (long, $\mu\text{s}$), `type = "audio_fade"`.
  - `com.vega.middlebridge.swig.AudioBalanceParamModuleJNI` ([`AudioBalanceParamModuleJNI.java`](file:///storage/emulated/0/ApkLab/CapCutREA/scratch/capcut-decompiled/jadx-out/single/sources/com/vega/middlebridge/swig/AudioBalanceParamModuleJNI.java)): Native methods `AudioBalanceParam_average_loudness_get/set`, `AudioBalanceParam_peak_loudness_get/set`, `AudioBalanceParam_target_loudness_get/set`, `AudioBalanceParam_enable_balance_get/set`.
  - `com.vega.middlebridge.swig.MaterialAudioPitchShiftModuleJNI` ([`MaterialAudioPitchShiftModuleJNI.java`](file:///storage/emulated/0/ApkLab/CapCutREA/scratch/capcut-decompiled/jadx-out/single/sources/com/vega/middlebridge/swig/MaterialAudioPitchShiftModuleJNI.java)): Native methods `MaterialAudioPitchShift_getEnablePitchShift`, `MaterialAudioPitchShift_getSemitones`, `MaterialAudioPitchShift_getCents`.
  - `com.ss.android.ttve.nativePort.TEVideoUtils` ([`TEVideoUtils.java`](file:///storage/emulated/0/ApkLab/CapCutREA/scratch/capcut-decompiled/jadx-out/single/sources/com/ss/android/ttve/nativePort/TEVideoUtils.java)):
    - `public static native VEMusicWaveBean nativeGetAudioWaveData(String str, int visualizerType, int startTimeMs, int durationMs, int pointsPerSec, int downsampleMode);`
    - `public static native long nativeCreateRTAudioWaveformMgr(int sampleRate, int channels, float pointsPerSec, int windowSize);`
    - `public static native int nativeRTAudioWaveformProcess(long handle, float[][] pcmBuffers, int samplesPerChannel, int flag);`
    - `public static native int nativeRTAudioWaveformGetPoints(long handle, float[] outPoints, int offset, int count);`
    - `public static native int nativeDeleteRTAudioWaveformMgr(long handle);`
  - `com.vega.audio.library.MusicWavePreview` ([`MusicWavePreview.java`](file:///storage/emulated/0/ApkLab/CapCutREA/scratch/capcut-decompiled/jadx-out/single/sources/com/vega/audio/library/MusicWavePreview.java)): Custom View rendering waveform bar paths on Android Canvas.

### 1.2 Derived Conclusions (Architecture & Mathematical Mechanics)

```
 +-------------------------------------------------------------------------+
 |                     MULTI-TRACK AUDIO PIPELINE                          |
 +-------------------------------------------------------------------------+
 |                                                                         |
 |  [Track 0: Main Video Audio]       [Track 1: BGM]      [Track 2: Voice] |
 |              |                            |                   |         |
 |        Speed / WSOLA                Speed / WSOLA       Speed / WSOLA   |
 |        Pitch Shifter                Pitch Shifter       Pitch Shifter   |
 |              |                            |                   |         |
 |       Volume / Fades               Volume / Fades      Volume / Fades   |
 |              |                            |                   |         |
 |       Loudness Balance             Loudness Balance    Loudness Balance |
 |              \                            |                  /          |
 |               +---------------------------+-----------------+           |
 |                                           |                             |
 |                                           v                             |
 |                         [mammonengine::DSP::Mixer]                      |
 |                                           |                             |
 |                                 N-Channel Summation                     |
 |                                 Soft Peak Limiter                       |
 |                                           |                             |
 |                        +------------------+------------------+          |
 |                        |                                     |          |
 |                        v                                     v          |
 |             [AudioTrack Playback]                   [libfdk-aac Encoder] |
 |             (Preview Audio Sink)                    (Final Export Track)|
 +-------------------------------------------------------------------------+
```

#### A. Waveform Amplitude Downsampling Math
Waveforms are pre-computed either offline asynchronously via `nativeGetAudioWaveData` or computed on the fly during voiceover recording using `nativeCreateRTAudioWaveformMgr`.
1. **Window Size Calculation**:
   Given audio sampling rate $F_s$ (e.g., $44,100\text{ Hz}$ or $48,000\text{ Hz}$), let $P$ be points per second (default $P = 10\text{ to }25\text{ points/sec}$ on timeline zoom levels).
   The window size in samples $N$ is:
   $$N = \left\lfloor \frac{F_s}{P} \right\rfloor$$
2. **Peak Amplitude Extraction**:
   For each window $k$, over raw 16-bit PCM samples $x_i \in [-32768, 32767]$ or 32-bit float $x_i \in [-1.0, 1.0]$:
   $$A_{peak}[k] = \max_{i=0}^{N-1} |x_{k \cdot N + i}|$$
3. **RMS (Root-Mean-Square) Energy Extraction**:
   $$A_{RMS}[k] = \sqrt{\frac{1}{N} \sum_{i=0}^{N-1} x_{k \cdot N + i}^2}$$
4. **Normalized Amplitude Normalization**:
   The native engine normalizes points into float array $V[k] \in [0.0, 1.0]$ stored in `MaterialAudio.wavePoints`:
   $$V[k] = \min\left(1.0, \; \frac{A_{peak}[k]}{\text{PEAK\_MAX}}\right)$$

#### B. Waveform UI View Rendering Algorithm (`MusicWavePreview.onDraw`)
In `MusicWavePreview.java`, the timeline viewport culls off-screen waveform bars and draws active bars using single-pass `Path` batching:
- **Horizontal Bar Pitch**: $g = \text{barWidth} (f) + \text{barSpacing}$.
- **Visible Range Culling**:
  $$\text{startIndex} = \max\left(0, \; \left\lfloor \frac{\text{scrollX} - \text{paddingLeft}}{g} \right\rfloor\right)$$
  $$\text{endIndex} = \min\left(V.\text{length}, \; \text{startIndex} + \left\lceil \frac{\text{viewWidth}}{g} \right\rceil + 1\right)$$
- **Vertical Geometry**:
  For bar $k \in [\text{startIndex}, \text{endIndex})$:
  - Half-height excursion: $H_{half} = V[k] \times \left(\frac{\text{viewHeight}}{2}\right)$
  - Bar center coordinate: $X_k = \text{paddingLeft} + k \cdot g + \frac{f}{2}$
  - Midline: $Y_{mid} = \frac{\text{viewHeight}}{2}$
  - If $H_{half} == 0$: Draws horizontal base dot from $(X_k - f/2, Y_{mid})$ to $(X_k + f/2, Y_{mid})$.
  - Otherwise: Adds line segment from $(X_k, Y_{mid} - H_{half})$ to $(X_k, Y_{mid} + H_{half})$.
- **Path Batching**: Drawn with `Paint.Style.STROKE`, rounded caps, anti-aliased.

#### C. Audio Fade In / Fade Out Curves
In `MaterialAudioFade`, durations $T_{in}$ and $T_{out}$ are stored in microseconds ($10^{-6}\text{s}$).
Gain multiplier $G(t)$ for segment duration $T_{total}$ at local timestamp $t \in [0, T_{total}]$ follows logarithmic gain response matching human auditory perception:
- **Fade In** ($0 \le t < T_{in}$):
  $$G_{in}(t) = \sin\left(\frac{\pi}{2} \cdot \frac{t}{T_{in}}\right) \quad \text{or} \quad G_{in}(t) = \left(\frac{t}{T_{in}}\right)^2$$
- **Fade Out** ($T_{total} - T_{out} < t \le T_{total}$):
  $$G_{out}(t) = \cos\left(\frac{\pi}{2} \cdot \frac{t - (T_{total} - T_{out})}{T_{out}}\right) \quad \text{or} \quad G_{out}(t) = \left(\frac{T_{total} - t}{T_{out}}\right)^2$$
- **Middle Section**: $G(t) = 1.0$.

#### D. Pitch Shifting & Pitch-Preserved Speed Stretching
In `MaterialAudioPitchShift` and `libaudioeffect.so`:
- **Pitch Shift Frequency Ratio**:
  Given semitones $\Delta S \in [-24, +24]$ and fine-tune cents $C \in [-100, +100]$:
  $$f_{ratio} = 2^{\frac{\Delta S + C / 100}{12}}$$
- **Pitch-Preserved Speed (WSOLA / Phase Vocoder)**:
  When speed changes by factor $s \in [0.1, 100.0]$:
  - If `Keep Pitch = true`: Time-stretching is performed using `mammon::TimeStretcherFast` or WSOLA (Waveform Similarity Overlap-Add) to stretch/compress time by $1/s$ while holding spectral pitch fixed ($f_{ratio} = 1.0$).
  - If `Keep Pitch = false`: Audio resampling factor is scaled directly: $f_{ratio} = s$. Pitch changes naturally by $\Delta S = 12 \log_2(s)$ semitones (e.g. $2\times$ speed doubles frequency = $+12$ semitones up, "chipmunk" effect).

#### E. Automatic Loudness Normalization (EBU R128 / ITU-R BS.1770)
In `AudioBalanceParam` and `VEAudioLoudnessBalanceFilter`:
- Engine measures integrated loudness $L_{source}$ (LUFS) and true peak $P_{source}$ (dBFS).
- Computes target gain $G_{norm}$:
  $$\Delta L = L_{target} - L_{source} \quad (\text{typically } L_{target} = -14.0\text{ LUFS for short-form video})$$
  $$G_{norm} = 10^{\frac{\Delta L}{20}}$$
- Applies true peak limiter if $P_{source} + \Delta L > -1.0\text{ dBFS}$ to eliminate digital clipping.

---

## 2. Text, Typography, Stickers & Captions Subsystem

### 2.1 Observed Facts (Data Models & Filter Serialization)
- **Draft Persistence Model**: `com.vega.draft.data.template.material.MaterialText` ([`MaterialText.java`](file:///storage/emulated/0/ApkLab/CapCutREA/scratch/capcut-decompiled/jadx-out/single/sources/com/vega/draft/data/template/material/MaterialText.java)) & [`MaterialText$$serializer.java`](file:///storage/emulated/0/ApkLab/CapCutREA/scratch/capcut-decompiled/jadx-out/single/sources/com/vega/draft/data/template/material/MaterialText$$serializer.java).
  Contains 61 serialized typography and styling fields:
  ```json
  {
    "platform": "all",
    "id": "text_material_uuid",
    "type": "text",
    "content": "{\"styles\":[],\"text\":\"Clean Room Editor\"}",
    "font_size": 15.0,
    "font_name": "AlbertSans-ExtraBold",
    "font_path": "/data/user/0/.../fonts/font.ttf",
    "fontUrl": "https://...",
    "alignment": 1,
    "line_spacing": 0.02,
    "letter_spacing": 0.0,
    "bold_width": 0.0,
    "italic_degree": 0,
    "underline": false,
    "underline_width": 1.0,
    "underline_offset": 0.0,
    "text_color": "#FFFFFFFF",
    "text_alpha": 1.0,
    "border_width": 0.08,
    "border_color": "#FF000000",
    "has_shadow": true,
    "shadow_color": "#80000000",
    "shadow_alpha": 0.8,
    "shadow_smoothing": 0.45,
    "shadow_distance": 0.15,
    "shadow_angle": -45.0,
    "shadow_point": { "x": 0.1, "y": -0.1, "blur": 0.45, "color": "#80000000" },
    "background_color": "#B3000000",
    "background_alpha": 0.7,
    "background_round_radius": 0.2,
    "background_width": 0.15,
    "background_height": 0.1,
    "background_horizontal_offset": 0.0,
    "background_vertical_offset": 0.0,
    "background_style": 0,
    "bloom_color": "#FF00E5FF",
    "bloom_strength": 0.6,
    "bloom_range": 0.4,
    "ktv_color": "#FF00FF00"
  }
  ```
- **Sticker Animation Model**: `com.vega.draft.data.template.material.MaterialAnimation` ([`MaterialAnimation.java`](file:///storage/emulated/0/ApkLab/CapCutREA/scratch/capcut-decompiled/jadx-out/single/sources/com/vega/draft/data/template/material/MaterialAnimation.java)):
  Contains `type = "sticker_animation"` and list of `Anim` objects:
  - `type`: Phase identifier (`"in"`, `"out"`, `"loop"`, or `"combo"`).
  - `duration`: Microseconds duration.
  - `path`: Effect package directory containing animation matrix curves.
  - `resourceId`: Animation template UUID.
- **Native Layout Data Transfer Object**: `com.ss.android.vesdk.model.BefTextLayout` ([`BefTextLayout.java`](file:///storage/emulated/0/ApkLab/CapCutREA/scratch/capcut-decompiled/jadx-out/vesdk/sources/com/ss/android/vesdk/model/BefTextLayout.java)):
  - Fields: `charSize`, `letterSpacing`, `lineWidth`, `lineHeight`, `textAlign`, `textIndent`, `split`, `lineCount`, `familyName`, `textColor`, `backColor`, `isPlaceholder`.
- **Layout Raster Output**: `com.ss.android.vesdk.model.BefTextLayoutResult` ([`BefTextLayoutResult.java`](file:///storage/emulated/0/ApkLab/CapCutREA/scratch/capcut-decompiled/jadx-out/vesdk/sources/com/ss/android/vesdk/model/BefTextLayoutResult.java)):
  - Fields: `Bitmap bitmap` (ARGB_8888 rendered texture), `int width`, `int height`, `int lineCount`.
- **Native 2D Vector Engine**: `lib/arm64-v8a/libskity.so` (Size: 1,427,736 bytes). ByteDance's Skia-derived vector graphics engine providing FreeType font rasterization, HarfBuzz text shaping, Bézier stroke expansion, and Gaussian drop shadows.
- **Filter Bridge Mapping**: `com.ss.android.vesdk.util.TEBundleFactory.fromInfoStickerFilterInternal` and `VEStickerAnimationFilter` ([`TEBundleFactory.java`](file:///storage/emulated/0/ApkLab/CapCutREA/scratch/capcut-decompiled/jadx-out/vesdk/sources/com/ss/android/vesdk/util/TEBundleFactory.java#L444-L447)).

### 2.2 Derived Conclusions (Typography Engine & Vector Styling Pipeline)

```
 +-------------------------------------------------------------------------+
 |                     TEXT & STICKER RENDERING ENGINE                     |
 +-------------------------------------------------------------------------+
 |                                                                         |
 |  [Typography Input]                                                     |
 |  String + Font + FontSize + LetterSpacing + LineSpacing + Alignment     |
 |                        |                                                |
 |                        v                                                |
 |  [Glyph Shaping & Layout] (HarfBuzz / FreeType via libskity.so)         |
 |  1. Measure glyph advances & compute line wrapping at maxWidth          |
 |  2. Align lines: Left (0), Center (1), Right (2)                        |
 |  3. Compute bounding box: Width W_text, Height H_text                   |
 |                        |                                                |
 |                        v                                                |
 |  [Multi-Pass Rasterizer / Canvas Generator]                             |
 |  Pass 1: Background Box                                                 |
 |          Round rect at [X_bg, Y_bg, W_bg, H_bg] with radius r_bg        |
 |  Pass 2: Drop Shadow                                                    |
 |          Offset (dx, dy) = d * (cos theta, sin theta), Gaussian blur sigma|
 |  Pass 3: Outer Stroke                                                   |
 |          Stroke path expanded by width w_stroke, color C_stroke         |
 |  Pass 4: Text Fill Glyph Glyphs                                         |
 |          Filled glyph contours with color C_text                        |
 |  Pass 5: Bloom / Glow Pass                                              |
 |          Dual blur buffer blended additively with C_bloom               |
 |                        |                                                |
 |                        v                                                |
 |  [OpenGL ES Texture Upload]                                             |
 |  glTexImage2D -> GL_TEXTURE_2D (Premultiplied RGBA8)                    |
 |                        |                                                |
 |                        v                                                |
 |  [Matrix Animation Transform] (VEStickerAnimationFilter)                |
 |  M_final(t) = M_timeline * M_animIn(t) * M_animLoop(t) * M_animOut(t)  |
 +-------------------------------------------------------------------------+
```

#### A. Multi-Pass Text Decorator Math
1. **Drop Shadow Vector Math**:
   Given shadow distance $d$, angle $\theta$ (in degrees, default $-45^\circ$), and smoothing factor $\sigma_{blur}$:
   $$dx = d \cdot \cos\left(\theta \cdot \frac{\pi}{180^\circ}\right)$$
   $$dy = d \cdot \sin\left(\theta \cdot \frac{\pi}{180^\circ}\right)$$
   Shadow color: $C_{shadow} = (R_s, G_s, B_s, A_s \cdot \text{shadow\_alpha})$.
2. **Stroke Outline**:
   Computed using vector outline inflation (or dual-pass distance field rendering):
   - Path stroke join style: `SkPaint::kRound_Join`.
   - Stroke width: $W_{stroke} = \text{fontSize} \times \text{border\_width} \times 2$.
3. **Background Rounded Box**:
   Bounding box coordinates expanded by horizontal and vertical padding:
   $$W_{box} = W_{text} + 2 \cdot (\text{fontSize} \times \text{background\_width})$$
   $$H_{box} = H_{text} + 2 \cdot (\text{fontSize} \times \text{background\_height})$$
   Corner radius: $R_{corner} = \min(W_{box}, H_{box}) \times \text{background\_round\_radius}$.
4. **Glow / Bloom Math**:
   Dual Gaussian downsample blur at scales $1/2$ and $1/4$:
   $$I_{bloom} = I_{text} \otimes G(\sigma_{bloom})$$
   $$C_{final} = C_{base} + \text{bloom\_strength} \cdot C_{bloom} \cdot I_{bloom}$$

#### B. Text Animation State Machine
`VEStickerAnimationFilter` splits text duration $T$ into three discrete phases:
- **Entrance Phase** ($0 \le t < T_{in}$):
  Local progress: $\tau_{in} = t / T_{in}$.
  Evaluates in-animation transform $M_{in}(\tau_{in})$ (e.g. Scale 0.0 to 1.0, Alpha 0.0 to 1.0, or Typewriter text reveal index $N_{chars} = \lfloor \tau_{in} \cdot N_{total} \rfloor$).
- **Loop Phase** ($T_{in} \le t \le T - T_{out}$):
  Loop period $T_{loop}$. Phase angle $\phi = \frac{2\pi \cdot (t - T_{in})}{T_{loop}} \pmod{2\pi}$.
  Evaluates periodic transform $M_{loop}(\phi)$ (e.g. sinusoidal wobble $y(t) = A \sin(\phi)$ or pulsing scale $s(t) = 1.0 + \Delta s \sin(\phi)$).
- **Exit Phase** ($T - T_{out} < t \le T$):
  Local progress: $\tau_{out} = \frac{t - (T - T_{out})}{T_{out}}$.
  Evaluates out-animation transform $M_{out}(\tau_{out})$ (e.g. Alpha 1.0 to 0.0, Slide Down $y = \tau_{out} \cdot H$).

---

## 3. Hardware-Accelerated Video Export Subsystem

### 3.1 Observed Facts (Encoding Pipeline & Resolution Table)
- **Video Encode Configuration**: `com.ss.android.vesdk.VEVideoEncodeSettings` ([`VEVideoEncodeSettings.java`](file:///storage/emulated/0/ApkLab/CapCutREA/scratch/capcut-decompiled/jadx-out/vesdk/sources/com/ss/android/vesdk/VEVideoEncodeSettings.java)).
  - Supported Codecs:
    - `ENCODE_STANDARD_H264` (H.264 / AVC)
    - `ENCODE_STANDARD_ByteVC1` (HEVC / H.265)
    - `ENCODE_STANDARD_MPEG4`
  - Bitrate Modes: `ENCODE_BITRATE_ABR`, `ENCODE_BITRATE_CRF`, `ENCODE_BITRATE_QP`, `ENCODE_BITRATE_VBR`.
  - H.264 Profiles: `ENCODE_PROFILE_BASELINE`, `ENCODE_PROFILE_MAIN`, `ENCODE_PROFILE_HIGH`.
  - Compile Types: `COMPILE_TYPE_MP4`, `COMPILE_TYPE_GIF`, `COMPILE_TYPE_HIGH_GIF`, `COMPILE_TYPE_WEBP`, `COMPILE_TYPE_JPEG`.
  - Resolution Alignment: `mResolutionAlign = 16` (multiples of 16 for hardware macroblock compatibility).
  - Maximum Pixel Budget: `8,294,400` pixels (4K UHD: $3840 \times 2160$).
- **Audio Encode Configuration**: `com.ss.android.vesdk.VEAudioEncodeSettings` ([`VEAudioEncodeSettings.java`](file:///storage/emulated/0/ApkLab/CapCutREA/scratch/capcut-decompiled/jadx-out/vesdk/sources/com/ss/android/vesdk/VEAudioEncodeSettings.java)).
  - Standard: `ENCODE_STANDARD_AAC` (default), `ENCODE_STANDARD_WAV`, `ENCODE_STANDARD_PCM`.
  - Sampling Rate: Default `44,100 Hz` (or `48,000 Hz`).
  - Bitrate: Default `131,072 bps` (128 kbps stereo).
  - Channels: 2 (Stereo).
  - Hardware Acceleration: `mHwEnc` boolean flag.
- **Hardware Profile Ratios**: `com.ss.android.vesdk.settings.VEVideoHWEncodeSettings` ([`VEVideoHWEncodeSettings.java`](file:///storage/emulated/0/ApkLab/CapCutREA/scratch/capcut-decompiled/jadx-out/vesdk/sources/com/ss/android/vesdk/settings/VEVideoHWEncodeSettings.java)):
  - Base Bitrate: `6,000,000 bps` (6 Mbps).
  - `mHp_bitrate_ratio = 0.75` (High Profile compression ratio).
  - `mHevc_bitrate_ratio = 0.65` (ByteVC1 HEVC compression ratio).
  - `mGop = 35` (Keyframe interval).
- **Bitrate Determination Engine**: `com.lemon.lv.BitrateLevelUtils` ([`BitrateLevelUtils.java`](file:///storage/emulated/0/ApkLab/CapCutREA/scratch/capcut-decompiled/jadx-out/single/sources/com/lemon/lv/BitrateLevelUtils.java)) & `CutSameBitrateLevelConfig` ([`CutSameBitrateLevelConfig.java`](file:///storage/emulated/0/ApkLab/CapCutREA/scratch/capcut-decompiled/jadx-out/single/sources/com/lemon/lv/config/CutSameBitrateLevelConfig.java)):
  - Bitrate Level: `0 = Low`, `1 = Recommended`, `2 = High`.
  - Bitrate calculation method:
    `BitrateLevelUtils.e(int resolution, int fps, int bitrateLevel)` $\to \text{table}[resolution][fps][level] \times 1,000,000\text{ bps}$.
  - Baseline: 720p @ 25 FPS = 6.5 Mbps.

### 3.2 Derived Conclusions (Hardware Export Architecture & Handshake)

```
 +-------------------------------------------------------------------------+
 |                     HARDWARE EXPORT PIPELINE                            |
 +-------------------------------------------------------------------------+
 |                                                                         |
 |  [Timeline Clock Step]  t_pts += (1,000,000 / FPS) microseconds         |
 |                                                                         |
 |  +-----------------------------------+-------------------------------+  |
 |  | VIDEO RENDERING PIPELINE          | AUDIO RENDERING PIPELINE      |  |
 |  |                                   |                               |  |
 |  | 1. Compositor Renders Frame to    | 1. Mix Audio Tracks for       |  |
 |  |    MediaCodec Input Surface       |    Time Slice [t_pts, t_pts+dt|  |
 |  |    via OpenGL ES 3.0 Framebuffer  |                               |  |
 |  |                                   | 2. Feed PCM Float/Short into  |  |
 |  | 2. EGLExt.eglPresentationTime-    |    Audio Encoder:             |  |
 |  |    ANDROID(eglDisplay, eglSurface,|    MediaCodec AAC or          |  |
 |  |    t_pts * 1000)                  |    libfdk-aac (128-320 kbps)  |  |
 |  |                                   |                               |  |
 |  | 3. EGL14.eglSwapBuffers(eglDisplay| 3. Dequeue AAC Access Units   |  |
 |  |    eglSurface)                    |    (ADTS / Raw AAC Frames)    |  |
 |  |                                   |                               |  |
 |  | 4. Hardware Encoder Encodes:      |                               |  |
 |  |    MediaCodec (H.264 / HEVC)      |                               |  |
 |  |                                   |                               |  |
 |  | 5. Dequeue Output NAL Units       |                               |  |
 |  |    (SPS, PPS, IDR, Non-IDR)       |                               |  |
 |  +-----------------------------------+-------------------------------+  |
 |                                  |                                      |
 |                                  v                                      |
 |                 [MediaMuxer / libttffmpeg Multiplexer]                  |
 |                 Interleaves Audio & Video Packets                       |
 |                 Writes 'moov' atom at end of stream                     |
 |                 Outputs Final MP4 File                                  |
 +-------------------------------------------------------------------------+
```

#### A. Video Bitrate Determination Matrix
Based on decompiled lookups in `BitrateLevelUtils` and `VEVideoHWEncodeSettings`, the clean-room implementation enforces the following bitrate allocation matrix:

| Resolution Name | Output Dimensions ($W \times H$) | FPS Range | Low Bitrate ($L=0$) | Recommended Bitrate ($L=1$) | High Bitrate ($L=2$) |
|:---|:---|:---|:---|:---|:---|
| **480p (SD)** | $854 \times 480$ (aligned 16: $864 \times 480$) | 24 - 30 | 2.5 Mbps | 3.5 Mbps | 5.0 Mbps |
| **720p (HD)** | $1280 \times 720$ | 24 - 30 | 4.5 Mbps | 6.5 Mbps | 9.0 Mbps |
| **720p (HD)** | $1280 \times 720$ | 50 - 60 | 6.0 Mbps | 8.5 Mbps | 12.0 Mbps |
| **1080p (FHD)** | $1920 \times 1080$ (aligned: $1920 \times 1088$) | 24 - 30 | 8.0 Mbps | 12.0 Mbps | 16.0 Mbps |
| **1080p (FHD)** | $1920 \times 1080$ | 50 - 60 | 11.0 Mbps | 16.0 Mbps | 22.0 Mbps |
| **2K (QHD)** | $2560 \times 1440$ | 24 - 30 | 15.0 Mbps | 20.0 Mbps | 28.0 Mbps |
| **2K (QHD)** | $2560 \times 1440$ | 50 - 60 | 20.0 Mbps | 28.0 Mbps | 38.0 Mbps |
| **4K (UHD)** | $3840 \times 2160$ | 24 - 30 | 25.0 Mbps | 35.0 Mbps | 45.0 Mbps |
| **4K (UHD)** | $3840 \times 2160$ | 50 - 60 | 35.0 Mbps | 50.0 Mbps | 65.0 Mbps |

**Codec Discount Multipliers**:
- If codec is `ByteVC1` (HEVC/H.265): Bitrate is scaled by `mHevc_bitrate_ratio = 0.65` (saves 35% bandwidth at equivalent perceptual VMAF score).
- If codec profile is `High Profile`: Bitrate is scaled by `mHp_bitrate_ratio = 0.75` vs Baseline.

#### B. MediaCodec Hardware Surface Handshake Protocol
1. **Encoder Initialization**:
   ```kotlin
   val format = MediaFormat.createVideoFormat(mimeType, width, height).apply {
       setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
       setInteger(MediaFormat.KEY_BIT_RATE, targetBitrate)
       setInteger(MediaFormat.KEY_FRAME_RATE, fps)
       setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // GOP = 1 second
       setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
       setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
   }
   codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
   val inputSurface = codec.createInputSurface()
   codec.start()
   ```
2. **EGL Presentation Timestamp Tagging**:
   Hardware encoders require presentation timestamps set directly on the EGL surface before swapping buffers:
   ```kotlin
   val framePtsNanos = (frameIndex * 1_000_000_000L) / fps
   EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, framePtsNanos)
   EGL14.eglSwapBuffers(eglDisplay, eglSurface)
   ```
3. **End of Stream (EOS) Signaling**:
   When the last frame has been rendered:
   ```kotlin
   codec.signalEndOfInputStream()
   ```
   The drainage loop continues dequeuing buffers with `MediaCodec.BUFFER_FLAG_END_OF_STREAM` before closing the muxer.

---

## 4. Unknowns & Target Boundaries (`NOT_DETERMINED`)

1. **Hardware Encoder Driver Quirks**:
   - `NOT_DETERMINED`: Specific vendor workarounds for Exynos and MediaTek hardware encoders that drop frames when `KEY_BITRATE_MODE = BITRATE_MODE_VBR` is set on Android 10 and below.
2. **Dynamic Resolution Downscale Fallback**:
   - `NOT_DETERMINED`: The exact runtime heuristics under which `HWCodecService` automatically downscales from 4K to 1080p when thermal throttling or device memory pressure alerts are triggered by `PerformanceManagerHelper`.
3. **Lynx Text Template Shader Bundles**:
   - `NOT_DETERMINED`: Whether certain complex kinetic typography text animations (e.g. 3D bending cylinders) execute exclusively via Lynx/Krypton C++ or fallback to standard OpenGL ES shaders on lower-tier GPUs.

---

## 5. Architectural Verification & Clean-Room Implementation Directives

1. **Audio Engine**:
   - Build a clean C++20 / Kotlin audio mixing pipeline using Oboe / AAudio / OpenSL ES for playback.
   - Implement WSOLA for pitch-preserved audio stretching and biquad parametric EQ.
   - Implement ITU-R BS.1770-4 loudness measurement and true-peak limiting.
2. **Typography Engine**:
   - Integrate FreeType 2.13+ and HarfBuzz for unicode text shaping and font loading.
   - Implement dual-pass SDF / Gaussian drop-shadow rasterization into 2D RGBA textures.
3. **Hardware Export**:
   - Implement zero-copy OpenGL ES rendering directly to `MediaCodec.createInputSurface()`.
   - Implement standard `MediaMuxer` MP4 multiplexing with H.264/HEVC and AAC audio streams.
   - Enforce macroblock alignment (`width % 16 == 0`, `height % 16 == 0`).
