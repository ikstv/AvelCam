# Android App Foundation

`AvelCam` Android module is the local camera preview foundation for the project.

## Purpose

- Capture and display a live camera preview in a Compose UI.
- Request and handle runtime camera permission.
- Support rear and front camera switching when both lenses are available.
- Keep implementation focused on local preview only in this task.

## Technology stack

- Kotlin
- Jetpack Compose
- CameraX (`camera-view`, `camera-lifecycle`, `camera-camera2`)
- Android Gradle Plugin + Kotlin Android plugin
- Gradle Kotlin DSL

## Current implemented scope

- ✅ Compose application with single-screen preview
- ✅ Runtime camera permission flow
- ✅ Permission-denied and permanently denied UI states
- ✅ Camera switching control
- ✅ Basic error reporting for camera initialization and binding
- ❌ No streaming, no USB transport, no Wi-Fi transport, no audio pipeline
- ❌ No OBS integration

## Prerequisites

- Android Studio (2024+)
- Android SDK 35 installed
- JDK 17
- Android device or emulator with camera support

## How to build

```bash
cd apps/android
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## How to install on a connected Android device

```bash
cd apps/android
./gradlew installDebug
```

## How to run tests

```bash
cd apps/android
./gradlew testDebugUnitTest
```

## Current limitations

- Local preview only in this milestone.
- No MediaCodec encoding.
- No USB transport.
- No Wi-Fi transport.
- No desktop service or OBS plugin integration.
- No audio capture.

## Physical-device validation

CI validates compilation and tests, but real-device behavior requires a separate protocol.
Use the checklist in [Physical-device smoke test](../../docs/qa/android-camera-smoke-test.md) before moving to Phase 3.
