# Android App

## Current Android scope

The app currently contains:

- CameraX preview pipeline (single-screen Compose UI)
- Runtime camera permission handling
- Front/rear camera switch support
- Error and permission-denied screens
- New encoder foundation (debug-only, synthetic input)

## H.264 encoder foundation

Phase 3 adds a dedicated encoder package under:

`app/src/main/java/com/avelcam/android/encoder/`

Main components:

- `H264Encoder`: `MediaCodec` encoder lifecycle and output callback.
- `EncoderConfig`: immutable configuration for H.264 defaults.
- `H264CodecSelector`: compatible AVC encoder discovery.
- `EncodedAccessUnit`: immutable output model.
- `EncoderStatistics`: runtime counters and derived metrics.
- `gl/` package: EGL/OpenGL synthetic frame rendering into codec input surface.

## Encoder diagnostic harness

A debug-only Compose panel is available while building debug artifacts:

- Start/stop synthetic encoder test
- Display selected codec
- Show encoded units, keyframes, codec-config units, and throughput metrics

The diagnostic panel is shown only when `BuildConfig.DEBUG` is true and is intended for local verification only.

## Test configuration

Current default config:

- MIME: `video/avc`
- Resolution: `1280x720`
- Frame rate: `30`
- Bitrate: `4_000_000`
- I-frame interval: `1`
- Surface input (`MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface`)

## How to run tests

### Unit tests

```bash
cd apps/android
./gradlew.bat testDebugUnitTest
```

### Instrumented tests

```bash
cd apps/android
./gradlew.bat connectedDebugAndroidTest
```

Instrumented tests are optional and expected to run on a compatible device.

### Build

```bash
cd apps/android
./gradlew.bat assembleDebug
```

## Current limitations

- CameraX is still separate from the encoder input path in this phase.
- No file recording or raw H.264 output file writing by default.
- No USB transport, Wi-Fi transport, audio, desktop receiver, or OBS plugin integration yet.
- Encoder diagnostics are debug-only and should not be exposed as required release functionality.

## Physical-device validation protocol

Use:

`docs/qa/android-h264-encoder-smoke-test.md`

Mark test as passed only after executing on a physical Android device.

