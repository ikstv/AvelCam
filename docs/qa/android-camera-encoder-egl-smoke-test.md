# Android CameraX EGL Input Surface Smoke Test

## Scope

Phase 3A validation for the new CameraX EGL/SurfaceTexture input path.

- CameraX frame source into GL fan-out pipeline
- On-screen preview still running
- Shared camera texture path works under real device conditions

This test is intentionally limited to:

- camera input surface mode switching
- startup/recovery behavior with EGL enabled
- no USB transport, Wi-Fi transport, audio, or desktop receiver

## 1) Test environment

- Repository: `ikstv/AvelCam`
- Branch: `feat/android-camera-encoder-pipeline`
- PR: `#3`
- Package: `com.avelcam.android`
- Activity: `com.avelcam.android.MainActivity`

```powershell
$adb = "C:\Users\<USER>\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$device = "<ADB_DEVICE_SERIAL>"
$pkg = "com.avelcam.android"
$activity = "com.avelcam.android.MainActivity"
```

## 2) Build and install

Build with EGL camera input surface enabled:

```powershell
cd C:\AI\AvelCam\apps\android
./gradlew.bat assembleDebug -PavelcamEnableEglInputSurface=true
```

Install debug APK from local artifact:

```powershell
& $adb -s $device install -r "C:\AI\AvelCam\artifacts\app-debug.apk"
```

Expected:

```text
Success
```

## 3) Runtime validation checklist

| ID | Scenario | Result | Notes |
| --- | --- | --- | --- |
| EGL-INSTALL-001 | Install in debug with EGL flag | Not run | |
| EGL-LAUNCH-001 | App starts without crash | Not run | |
| EGL-MODE-001 | EGL input surface selected | Not run | log/UX confirms shared GL source path |
| EGL-PREVIEW-001 | Rear preview is visible | Not run | no black frame / no freeze |
| EGL-SWITCH-001 | Rear → Front | Not run | still renders |
| EGL-SWITCH-002 | Front → Rear | Not run | still renders |
| EGL-SWITCH-003 | 10 repeats | Not run | no deadlock |
| EGL-ROTATE-001 | Portrait/Landscape | Not run | rotates without crash |
| EGL-BGFG-001 | background/foreground | Not run | restores preview |
| EGL-RESTART-001 | repeated start/stop cycle | Not run | no fatal EGL error |
| EGL-LOG-001 | No fatal exceptions | Not run | no `FATAL EXCEPTION` and no EGL init failure |

## 4) Commands for evidence collection

```powershell
& $adb -s $device logcat -c
& $adb -s $device shell am force-stop $pkg
& $adb -s $device shell pm clear $pkg
& $adb -s $device shell am start -W -n "$pkg/$activity"
```

After testing:

```powershell
New-Item -ItemType Directory -Force "C:\AI\AvelCam\artifacts\device-smoke-test\egl" | Out-Null

& $adb -s $device logcat -d AndroidRuntime:E "*:S" |
  Set-Content "C:\AI\AvelCam\artifacts\device-smoke-test\egl\android-runtime-errors.txt"

& $adb -s $device logcat -d |
  Select-String "AvelCam|CameraX|SurfaceTexture|EGL|AndroidRuntime|FATAL EXCEPTION|codec" |
  Set-Content "C:\AI\AvelCam\artifacts\device-smoke-test\egl\avelcam-egl-logcat.txt"
```

## 5) Exit criteria

- No app crash during launch and basic flow.
- Camera preview works on rear camera with EGL mode enabled.
- Front/rear switching works.
- Preview survives orientation/background transitions.
- Ten switch cycles do not destabilize the app.
- Logcat shows no fatal runtime errors.

Any `FatalException`, `eglCreateContext` failures, or unrecoverable surface errors fail this smoke test.

