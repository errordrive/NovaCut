# Static APK Analysis Report

**Target Version:** CapCut 19.8.0 (`com.lemon.lvoverseas`)  
**Version Code:** `19801600`  
**Compile SDK Version:** 36 (Android 16 preview / Android 15 QPR)  
**Min SDK Version:** 23 (Android 6.0 Marshmallow)  
**Target SDK Version:** 36  
**Primary Architecture Split:** `arm64-v8a` (64-bit ARM)  

---

## 1. Application Metadata & Identifiers

- **Application ID:** `com.lemon.lvoverseas` (Lemon Inc. / ByteDance Overseas entity)
- **Version Name:** `19.8.0`
- **Build Identifier:** `19801600`
- **Application Class:** Inherits customized ByteDance multi-dex loader and APM (Application Performance Monitoring) initialization framework.
- **Main Launcher Activity:** `com.vega.launcher.precondition.DeeplinkActivity` aliasing to `com.vega.main.MainActivity`.

---

## 2. Manifest Architecture & Declared Permissions

The application declares **40 permissions**, categorized as follows:

### Storage & Media Access (Android 13/14/15 Modern Storage Model)
- `android.permission.READ_MEDIA_VIDEO`
- `android.permission.READ_MEDIA_AUDIO`
- `android.permission.READ_MEDIA_IMAGES`
- `android.permission.READ_MEDIA_VISUAL_USER_SELECTED`
- `android.permission.ACCESS_MEDIA_LOCATION`
- `android.permission.READ_EXTERNAL_STORAGE` (legacy fallback for SDK < 33)
- `android.permission.WRITE_EXTERNAL_STORAGE` (legacy fallback for SDK < 29)

### Hardware Capture & Recording
- `android.permission.CAMERA` (Declared with `android.hardware.camera` `required="false"`, allowing installation on tablets/Chromebooks without back cameras)
- `android.permission.RECORD_AUDIO` (For voiceovers, audio recording, and live vocal capture)
- `android.permission.HIGH_SAMPLING_RATE_SENSORS` (For motion sensing / stabilization)
- `android.permission.VIBRATE` (Haptic feedback during timeline scrubbing, snapping, and trimming)

### Background Operations & Export
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_DATA_SYNC` (For background media rendering, cloud sync, and asset downloads)
- `android.permission.WAKE_LOCK` (Prevents CPU/screen sleep during intense video encoding)
- `android.permission.POST_NOTIFICATIONS` (Export completion notifications)

### Network & Commercial
- `android.permission.INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CHANGE_NETWORK_STATE`
- `com.android.vending.BILLING` (CapCut Pro subscription processing via Google Play Billing)

---

## 3. Component Inventory

| Component Type | Total Count | Key Highlighted Components |
| :--- | :--- | :--- |
| **Activities** | 89 | `com.vega.main.MainActivity` (Primary host), `com.vega.feedx.main.ui.preview.VideoPreviewActivity` (Editor preview), `com.xt.retouch.draftbox.ui.DraftBoxActivity` (Project draft management), `com.vega.edward.templatecore.editor.edit.MinimalEditorActivity` (Quick editor), `com.vega.adeditor.*` (Commerce ad editor), `com.vega.texttovideo.*` (AIGC script editor) |
| **Services** | 51 | `IndependentProcessDownloadService`, `DownloadService`, `SqlDownloadCacheService` (Resilient asset streaming & caching), `MediaSessionService` (Android media controls integration) |
| **Content Providers** | 20 | `androidx.core.content.FileProvider` (Secure media sharing with external apps), `MultiProcessSharedPreferences` (Cross-process synchronization), `MonitorContentProvider` (APM metrics) |
| **Broadcast Receivers** | 28 | Dynamic receiver protection, network change listeners, app update migration hooks |

---

## 4. Native Library Architecture (162 Shared Libraries)

The `split_config.arm64_v8a.apk` houses a comprehensive native multimedia stack:

### A. Non-Linear Editing (NLE) & Pipeline Engine
1. **`libmiddle-bridge.so` (60.7 MB):** The central C++ NLE engine. Bridges UI models (tracks, clips, keyframes, transitions) with hardware decoders and graphics compositors. Implements the underlying timeline graph.
2. **`libcccreator.so` (39.3 MB):** The CapCut Creator rendering core. Coordinates real-time layer blending, composite video frames, and export pipeline.
3. **`libmiddleware.so` (24.3 MB):** Intermediate track, audio bus, and render orchestration layer.
4. **`libclipflow.so` (4.8 MB):** Graph execution engine for video clip transform pipelines.

### B. Video & Audio Codecs / Media I/O
1. **`libttffmpeg.so` (5.6 MB):** Optimized FFmpeg fork containing custom demuxers, muxers, and hardware-accelerated color conversion.
2. **`libbytevc1enc.so` (1.2 MB), `libByteVC1_dec.so` (467 KB), `libByteVC2dec.so` (499 KB):** ByteDance proprietary H.265 / ByteVC1/ByteVC2 video encoders and decoders.
3. **`libfdk-aac.so` (611 KB), `libopus.so` (402 KB):** High-fidelity audio encoding and streaming.
4. **`libttdemux.so` & `libbvcparser.so`:** Low-latency MP4/Matroska container parsers.

### C. Audio DSP & Voice Synthesis
1. **`libaudioeffect.so` (5.0 MB):** Real-time DSP engine supporting pitch shifting, voice modulation, equalization, noise suppression, and beat detection.
2. **`libspeechsdk.so` (932 KB) & `libwhisper.so` (37.7 KB):** Speech recognition, local automatic subtitle transcription, and voice-to-text alignment.

### D. Computer Vision & Machine Learning
1. **`libbytenn.so` (3.1 MB):** ByteDance on-device neural network runtime for ML inference.
2. **`libfastcv.so` (978 KB):** Accelerated computer vision primitives (edge detection, image warping, matrix transforms).
3. **`liblens.so` (3.6 MB) & `libretouch_sdk.so` (2.0 MB):** Facial beautification, portrait segmentation, and smart background removal.

### E. Graphics Rendering & Persistence
1. **`libskity.so` (1.4 MB) & `libAGFX.so` (738 KB):** Vector graphics rendering, canvas overlay drawing, and OpenGL ES / Vulkan pipelines.
2. **`libwcdb.so` (1.7 MB):** High-performance SQLite database engine with multi-threaded write support for draft storage.

---

## 5. Technology & Third-Party Dependency Indicators

- **UI Frameworks:** Hybrid architecture utilizing Jetpack Compose (`androidx.compose.*`), standard Android Views (`androidx.recyclerview`, `CoordinatorLayout`), and Lynx / Spark (`liblynx.so`) for dynamic web-view-like template components.
- **Reactive Streams:** Kotlin Coroutines (`kotlinx.coroutines`) and Flow for async processing.
- **Image Loading:** Customized ByteDance Fresco fork + WebP/GIF native decoders (`libgifimage.so`, `libstatic-webp.so`).
- **Database:** WeChat DB (`libwcdb.so`) + Room / SQLite.
- **Analytics & Crash Reporting:** ByteDance APM (`libnpth_*.so`, `libalog.so`).
