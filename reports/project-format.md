# Project Data Model & File Format Specification

**Document Schema Version:** `1.0.0`  
**File Name:** `draft_content.json`  
**Internal Time Representation:** Integer Microseconds (`1 us = 10^-6 s`; `1,000,000 us = 1 second`).

---

## 1. Schema Design Principles

1. **Precision Time Base:** All timestamps, in-points, out-points, and durations are stored as 64-bit integer microseconds (`Long`). Floating-point seconds are never used for timeline calculations to prevent cumulative drift.
2. **Normalized Coordinates:** Transforms (position, scale) use normalized coordinates (`-1.0` to `+1.0` or `0.0` to `1.0`) relative to the canvas bounding box, ensuring resolution-independent rendering across preview and export.
3. **Immutability & Serializable Dataclasses:** Every modification creates a new immutable state revision, enabling instant undo/redo without deep cloning overhead.

---

## 2. Complete Project JSON Specification

```json
{
  "schema_version": "1.0.0",
  "project_id": "novacut-d9f2e4b1-872a-43c2",
  "name": "Project_20261024_0415",
  "created_at": 1792815300000,
  "updated_at": 1792815305000,
  "canvas": {
    "width": 1080,
    "height": 1920,
    "ratio": "9:16",
    "background_type": "COLOR",
    "background_color": "#000000FF",
    "blur_intensity": 0.0
  },
  "config": {
    "fps": 30,
    "video_mute": false,
    "main_track_adsorb": true,
    "snap_enabled": true
  },
  "materials": {
    "videos": [
      {
        "id": "vid_01",
        "path": "content://media/external/video/media/104",
        "duration_us": 5000000,
        "width": 1080,
        "height": 1920,
        "fps": 30.0,
        "rotation": 0
      }
    ],
    "audios": [
      {
        "id": "aud_01",
        "path": "content://media/external/audio/media/52",
        "duration_us": 12000000,
        "channels": 2,
        "sample_rate": 44100
      }
    ],
    "texts": [
      {
        "id": "txt_01",
        "content": "Intro Title",
        "font_family": "DEFAULT_BOLD",
        "font_size_sp": 24,
        "text_color": "#FFFFFFFF",
        "outline_color": "#000000FF",
        "outline_width": 2.0
      }
    ]
  },
  "tracks": [
    {
      "id": "track_main_video",
      "type": "VIDEO",
      "is_main_track": true,
      "order": 0,
      "muted": false,
      "locked": false,
      "clips": [
        {
          "id": "clip_v1",
          "material_id": "vid_01",
          "clip_type": "VIDEO",
          "timeline_start_us": 0,
          "timeline_duration_us": 3000000,
          "source_in_us": 500000,
          "source_out_us": 3500000,
          "speed": 1.0,
          "volume": 1.0,
          "opacity": 1.0,
          "transform": {
            "translation_x": 0.0,
            "translation_y": 0.0,
            "scale": 1.0,
            "rotation_deg": 0.0,
            "flip_h": false,
            "flip_v": false
          },
          "color_adjustments": {
            "brightness": 0.0,
            "contrast": 0.0,
            "saturation": 0.0,
            "temperature": 0.0,
            "vignette": 0.0
          },
          "audio_envelope": {
            "fade_in_us": 200000,
            "fade_out_us": 200000
          },
          "keyframes": [
            {
              "time_offset_us": 0,
              "property": "OPACITY",
              "value": 0.0,
              "interpolation": "EASE_IN"
            },
            {
              "time_offset_us": 1000000,
              "property": "OPACITY",
              "value": 1.0,
              "interpolation": "LINEAR"
            }
          ]
        }
      ]
    },
    {
      "id": "track_bgm_1",
      "type": "AUDIO",
      "is_main_track": false,
      "order": 1,
      "muted": false,
      "locked": false,
      "clips": [
        {
          "id": "clip_a1",
          "material_id": "aud_01",
          "clip_type": "AUDIO",
          "timeline_start_us": 0,
          "timeline_duration_us": 3000000,
          "source_in_us": 0,
          "source_out_us": 3000000,
          "speed": 1.0,
          "volume": 0.8,
          "opacity": 1.0,
          "transform": {},
          "color_adjustments": {},
          "audio_envelope": {
            "fade_in_us": 500000,
            "fade_out_us": 800000
          },
          "keyframes": []
        }
      ]
    }
  ]
}
```

---

## 3. Migration and Compatibility Strategy

- Each project file begins with `"schema_version"`.
- When loading an older project file (e.g. `1.0.0` into a future `1.1.0` engine), a deterministic migration pipeline transforms the JSON document before state instantiation.
- Undefined fields always fall back to defined default values.
