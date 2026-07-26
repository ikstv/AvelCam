# AvelCam Roadmap

## Phase 1 — Repository foundation

- Create repository structure and documentation baseline.
- Define contribution conventions and architecture direction.
- Set up Android/desktop/transport/OBS module boundaries.

**Status: completed**

## Phase 2 — Android camera preview

- Scaffold CameraX capture pipeline.
- Add camera device enumeration and front/rear camera selection.
- Validate capture latency and frame rate behavior.
- Complete physical-device smoke testing:
  - APK install on real hardware
  - launch and permission behavior checks
  - preview and camera switching
  - rotation and recreation checks
- Record the passing physical-device report in `docs/qa/reports/android-camera-smoke-test-2026-07-27.md`.

**Status: completed**

## Phase 3 — H.264 encoder

- Integrate MediaCodec hardware encoding for capture frames.
- Tune bitrate/keyframe settings for low-latency operation.
- Define transport metadata and timestamps.

**Status: not implemented**

## Phase 4 — USB streaming through ADB

- Implement USB transport bootstrap and lifecycle management over ADB.
- Add reconnect and device-drop recovery.
- Stream metadata handshake and heartbeat.

**Status: not implemented**

## Phase 5 — Desktop receiver and decoder

- Implement Rust ingest service and decoder path.
- Add low-latency buffering policy and frame timing metrics.

**Status: not implemented**

## Phase 6 — OBS source plugin

- Build native OBS source plugin (C/C++).
- Render received frames with minimal latency in OBS.

**Status: not implemented**

## Phase 7 — Audio support

- Add microphone capture on Android.
- Add audio transport and mixed timing coordination in OBS.

**Status: not implemented**

## Phase 8 — Wi‑Fi fallback

- Implement Wi‑Fi transport implementation and fallback switch.
- Add connection selection and reconnection rules.

**Status: not implemented**

## Phase 9 — Direct USB transport without mandatory ADB

- Move beyond ADB-only transport for production-like USB operation.
- Add robust direct USB device protocol path.

**Status: not implemented**

## Phase 10 — Packaging, installers and releases

- Create installers for Windows and plugin distribution flow.
- Document release process and versioning.
- Publish binaries and release artifacts.

**Status: not implemented**