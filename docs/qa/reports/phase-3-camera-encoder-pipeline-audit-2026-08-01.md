# Phase 3 Camera Encoder Pipeline Audit -- 2026-08-01

## Decision

`feat/android-camera-encoder-pipeline` is buildable and its existing encoder foundation is validated on a physical device. It is not ready for review or merge: the implementation does not yet deliver live CameraX pixels to the OpenGL fan-out or to the H.264 encoder.

Phase 3 remains **in development**.

## Scope

- Baseline branch head: `3e4f877`.
- Audit fixes: Kotlin compilation fixes and corrected unit-test isolation/expectations.
- Deliberately excluded: USB, Wi-Fi, audio, desktop receiver, OBS, and release packaging.

## Validation evidence

| Check | Result | Evidence |
| --- | --- | --- |
| Kotlin compilation | Pass | `testDebugUnitTest lintDebug assembleDebug` completed successfully. |
| JVM unit tests | Pass | 97 tests, 0 failures. |
| Android Lint | Pass with warnings | `lintDebug` completed; 65 non-blocking maintenance warnings remain. |
| Debug APK | Pass | `assembleDebug` completed successfully. |
| Physical instrumented tests | Pass | `connectedDebugAndroidTest`: 3 tests, 0 failures. |
| H.264 synthetic encoder | Pass | Five encoder start/stop cycles verified CSD, encoded output, keyframe, monotonic PTS, statistics, and clean release. |
| Camera input surface lifecycle | Pass | Physical test verified detached `SurfaceTexture`/`Surface` allocation and idempotent release. |
| Git hygiene | Pass | No tracked credentials or signing material found; `git diff --check` is clean. |

Physical test device:

- Model: `22071212AG`
- Android: `15`
- API level: `35`

The device serial, raw logcat, APK, and local paths are intentionally excluded from version control.

## Revalidation note

The physical suite passed before the final documentation-only and variable-renaming edits. A final repeat installation was blocked by Android with `INSTALL_FAILED_USER_RESTRICTED` because installation was cancelled on the device, so no tests started in that repeat. This is a device authorization condition, not a test or application failure; it must be retried after installation is allowed on the phone before a merge gate is considered fully fresh.

### Update 2026-08-01 (local continuation on-device run)

- Rebuilt on branch `feat/android-camera-encoder-pipeline` with `-PavelcamEnableEglInputSurface=true` and produced:
  - `C:\AI\AvelCam\apps\android\app\build\outputs\apk\debug\app-debug.apk`
  - `BUILD SUCCESSFUL`
- Installed on device `Q4OJSWUC4PYTZHRG`:
  - `Success` (ADB install return)
- Launched app:
  - `am start -W -n com.avelcam.android/com.avelcam.android.MainActivity`
  - `Status: ok`, `LaunchState: COLD`
- Device-visible runtime evidence from `logcat -d` (filtered for CameraX/Runtime/Codec tags):
  - Camera lifecycle transitions and `CameraDevice.onOpened()` observed
  - `Camera2CameraImpl` use cases active/detach events present
  - No `FATAL EXCEPTION` / `AndroidRuntime` crash markers in this specific run
- Limitation:
  - No scripted interaction was executed to verify front/rear switch, rotation, background/foreground resume, or encoder output counters in this run.
- Revalidation status unchanged:
  - `Phase 3 remains in development` until end-to-end fan-out and encoder output checks are completed in a dedicated on-device smoke protocol.

## Corrections made during audit

- Fixed a Kotlin smart-cast compilation error when unregistering the preview destination.
- Replaced an incompatible constructor function reference for `H264Encoder` with a lambda that uses its default parser.
- Corrected a unit test that expected frames to be rendered after restart without a render request.
- Isolated the runtime lifecycle unit test from real `MediaCodec`; codec behavior remains covered by the physical instrumented test.

## Findings that block merge

### P0 -- live camera pixels are not part of the fan-out

`CameraPreview` binds CameraX directly to `PreviewView` and separately consumes `ImageAnalysis`. The analyzer sends only width, height, timestamp, rotation, and an identity matrix to `CameraGlFanoutRuntime`; it does not transfer a camera texture or image pixels. The displayed preview is therefore CameraX's direct preview, while the fan-out receives metadata only.

`CameraSurfaceProvider` and `CameraInputSurface` have lifecycle tests but are not referenced by the app integration. The intended `SurfaceRequest -> SurfaceTexture -> external OES texture` path is consequently not active.

### P0 -- fan-out outputs do not draw the camera texture

`PreviewSurfaceGlDestination` and `EncoderSurfaceGlDestination` clear their EGL surfaces with colors. Neither destination binds an external OES texture, applies `SurfaceTexture` transforms, or draws a camera frame. A live H.264 stream cannot be produced from this implementation.

### P1 -- execution model conflicts with the documented architecture

The documented design requires one dedicated GL render thread and coalesced `SurfaceTexture` callbacks. The app currently performs the fan-out call synchronously from the `ImageAnalysis` executor. `CameraFrameCoalescer` is not connected to the runtime, and every destination owns a separate EGL context instead of sharing a single camera texture context.

### P1 -- encoder startup and output ownership need separation

Starting `CameraGlFanoutRuntime` starts `CameraEncoderOutputManager` immediately, although the architecture says preview must run independently from encoding. Its default `NoopEncodedFrameSink` discards encoded access units. The future public pipeline needs an explicit encoder start/stop action and an owned output consumer.

### P2 -- portability and maintenance backlog

- `EglCore` does not request `EGL_RECORDABLE_ANDROID`; the current device accepts the synthetic encoder surface, but codec-surface compatibility is not guaranteed across devices.
- Lint reports 65 maintenance warnings: Android Gradle Plugin/tooling and dependencies are outdated, data extraction rules and app icon are missing, and unused resources remain.
- `compileSdk` and `targetSdk` are 35 while the current lint environment reports a newer Android API/toolchain is available. This should be handled in a separate tooling upgrade PR.

## Required next implementation step

Create one focused PR for the actual CameraX-to-OES bridge:

1. Replace direct `PreviewView` camera binding with one app-owned `SurfaceView` output and one custom CameraX `SurfaceProvider`.
2. Create the external OES texture, camera `SurfaceTexture`, and its `Surface` on a dedicated GL thread.
3. Connect `SurfaceTexture.OnFrameAvailableListener` to `CameraFrameCoalescer`, then call `updateTexImage()` only on that GL thread.
4. Draw the transformed OES texture into the on-screen `SurfaceView` first.
5. Add the MediaCodec EGL destination only after preview pixels are proven; request a recordable EGL config and make encoder start/stop explicit.
6. Add a physical test proving that camera pixels, not synthetic color clears, reach both preview and encoder output through rotation, camera switching, and lifecycle recreation.

This next PR must not include transport, audio, desktop receiver, or OBS work.
