# Video Editor Test Plan & Quality Assurance Matrix

## 1. Test Strategy & Objectives

This test plan defines the automated, repeatable verification suite for NovaCut, our independent mobile video editor. The suite verifies data model integrity, timeline mathematics, ripple algorithms, undo/redo state preservation, audio envelope generation, transform mathematics, and export timeline consistency.

---

## 2. Test Cases Specification

| ID | Test Category | Description | Acceptance Criteria |
| :--- | :--- | :--- | :--- |
| **TC-01** | Timeline | Clip duration and microsecond time calculations | `timeline_duration_us == (source_out_us - source_in_us) / speed` with exact integer microsecond accuracy |
| **TC-02** | Editing | Playhead Split Operation | Splitting a 4-second clip at 1.5 seconds yields 2 clips: Clip 1 (0 to 1.5s, source [0, 1.5s]), Clip 2 (1.5s to 4.0s, source [1.5s, 4.0s]); track total duration remains unchanged |
| **TC-03** | Editing | Head & Tail Trimming with Main-Track Ripple | Trimming 500ms from head shifts start time, updates source in-point, and ripples subsequent clips to the left without gaps |
| **TC-04** | History | Bidirectional Undo & Redo | Executing an edit modifies state; calling `undo()` restores identical previous state; calling `redo()` restores modified state |
| **TC-05** | Persistence | JSON Serialization & Autosave Roundtrip | Serializing a project with multiple tracks, clips, keyframes, and canvas settings to JSON and deserializing produces an identical object |
| **TC-06** | Audio | Volume Envelope & Fade Calculation | Fade-in from 0 to 1s: gain at 0s is 0.0, gain at 0.5s is 0.5, gain at 1s is 1.0; fade-out ramps linearly from 1.0 down to 0.0 |
| **TC-07** | Effects | Keyframe Interpolation (Linear & Ease) | Interpolating opacity between 0.0 at t=0 and 1.0 at t=2s gives exact values at intermediate points; clamps at boundaries |
| **TC-08** | Compositing | Transform & Matrix Evaluation | Translation X/Y, scale, rotation, and flip correctly compute bounding box and normalized vertex coordinates |
| **TC-09** | Multitrack | Multitrack Audio & Video Synchronization | Overlay video and audio clips align to specific timeline start timestamps; total project duration equals max track extent |
| **TC-10** | Snapping | Magnetic Snap Detection | Playhead within snap threshold (e.g. 50,000 us = 50ms) snaps exactly to nearest clip edge; returns snap delta |
| **TC-11** | Edge Cases | Missing Media & Invalid File Recovery | Loading a project where asset file path is missing or inaccessible flags asset as `UNAVAILABLE` without crashing or corrupting project |
| **TC-12** | Export | Export Frame Sequencer & Time Alignment | Export engine iterates through discrete frame timestamps at target FPS (e.g. 30 fps), querying visible clips and rendering layers |
