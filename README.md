# AvelCam

[![Status: Early Development](https://img.shields.io/badge/status-early%20development-yellow)](#project)
![Platform: Android](https://img.shields.io/badge/platform-Android-blue)
![Platform: Windows](https://img.shields.io/badge/platform-Windows-lightgray)
![Integration: OBS Studio](https://img.shields.io/badge/integration-OBS%20Studio-ff9900)

## Project

**AvelCam** turns an Android phone into a camera source for OBS Studio through a staged engineering approach: local preview, H.264 encoding, transport, desktop decoding, and OBS integration.

**Status:** early development.

## Roadmap status

1. Repository foundation — completed.
2. Android camera preview — completed.
3. H.264 encoder and CameraX integration — in development.
4. USB streaming through ADB — not implemented.
5. Desktop receiver and decoder — not implemented.
6. OBS plugin — not implemented.
7. Audio support — not implemented.
8. Wi-Fi fallback — not implemented.
9. Direct USB without mandatory ADB — not implemented.
10. Packaging, installers, and releases — not implemented.

## Current status

- The Android application includes CameraX preview with camera permission handling and front/rear switching.
- Physical-device validation for the CameraX preview is complete.
- The synthetic MediaCodec H.264 encoder foundation is implemented and physically validated.
- Current Phase 3 work connects CameraX frames to the encoder through a GPU/OpenGL pipeline.

## Documentation

- [Phase roadmap](docs/roadmap.md)
- [Android module README](apps/android/README.md)
- [Camera smoke-test report](docs/qa/reports/android-camera-smoke-test-2026-07-27.md)
- [H.264 encoder smoke-test report](docs/qa/reports/android-h264-encoder-smoke-test-2026-07-27.md)
- [Camera encoder pipeline architecture](docs/architecture/android-camera-encoder-pipeline.md)

## Current engineering boundaries

- No USB or Wi-Fi transport is implemented yet.
- No audio capture is implemented yet.
- No desktop receiver or OBS plugin is implemented yet.
- CameraX-to-encoder integration remains in development.
