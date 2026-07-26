# Android Camera Foundation Physical Device Smoke Test

## 1. Purpose

This protocol validates the Android Camera Foundation on a physical device before starting
Phase 3: H.264 encoding with MediaCodec. It verifies runtime behavior of CameraX preview
on real hardware and confirms the baseline flow is stable for the next engineering stage.

## 2. Scope

Covered:
- APK installation
- app launch
- runtime permission states
- rear camera preview
- front/rear switching
- repeated switching
- device rotation
- application recreation
- crash detection
- basic CameraX errors

Not covered:
- H.264 encoding
- USB streaming
- Wi-Fi transport
- audio capture
- desktop receiver
- OBS plugin
- performance benchmarking

## 3. Prerequisites

- Windows 10 or 11
- Android SDK Platform Tools
- physical Android device
- USB debugging enabled
- computer authorized on the device
- unlocked phone during installation
- debug APK produced by CI
- device serial available from `adb devices`

```powershell
$adb = "C:\Users\<USER>\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$apk = "C:\path\to\app-debug.apk"
$pkg = "com.avelcam.android"
$activity = "com.avelcam.android.MainActivity"
$device = "<ADB_DEVICE_SERIAL>"
```

## 4. Device detection

```powershell
& $adb devices -l
& $adb -s $device get-state
```

Expected result:
```text
device
```

Possible results:
- `device` – ready
- `unauthorized` – allow USB debugging trust prompt on phone
- `offline` – reconnect USB cable and retry
- missing entry – device not detected / wrong cable / not in USB mode

## 5. APK installation

```powershell
& $adb -s $device install -r $apk
```

Expected result:
```text
Success
```

If you receive:
```text
INSTALL_FAILED_USER_RESTRICTED
```
this usually means the installation was blocked or rejected on the phone.

Treat this as an OS/user trust/settings issue, not an application code issue.
Repeat install after confirming install permissions on the device.

## 6. Clean test state

```powershell
& $adb -s $device shell am force-stop $pkg
& $adb -s $device shell pm clear $pkg
```

`pm clear` removes app data and permission state for a deterministic test baseline.

## 7. Launch test

Preferred:

```powershell
& $adb -s $device shell am start -W -n "$pkg/$activity"
```

Fallback:

```powershell
& $adb -s $device shell monkey -p $pkg -c android.intent.category.LAUNCHER 1
```

Record for each launch:
- launch status
- displayed activity
- launch duration
- initial screen render status

## 8. Crash log collection

Clear logs before each scenario:

```powershell
& $adb -s $device logcat -c
```

After the scenario:

```powershell
& $adb -s $device logcat -d AndroidRuntime:E "*:S" |
    Set-Content -Path ".\artifacts\android-runtime-errors.txt"
```

Broader capture:

```powershell
& $adb -s $device logcat -d |
    Select-String -Pattern "AvelCam|CameraX|Camera2|AndroidRuntime|FATAL EXCEPTION" |
    Set-Content -Path ".\artifacts\avelcam-logcat.txt"
```

Create output directory before saving logs:

```powershell
New-Item -ItemType Directory -Force ".\artifacts" | Out-Null
```

Do not commit captured logs. Ensure local logs are ignored by `.gitignore` or manually exclude before commit.

## 9. Permission scenarios

### CAM-PERM-001: First launch

1. Clear application data.
2. Launch the app.
3. Confirm the system camera permission dialog appears.

Expected:
- no crash
- permission request visible
- UI remains responsive

### CAM-PERM-002: Allow

1. Tap Allow on the phone.
2. Observe the app screen.

Expected:
- permission state updates
- rear preview starts
- permission remains granted after relaunch

ADB check:

```powershell
& $adb -s $device shell dumpsys package $pkg |
    Select-String "android.permission.CAMERA"
```

### CAM-PERM-003: Deny

Use a real user denial on-device and verify behavior.

To reset for next run:

```powershell
& $adb -s $device shell pm revoke $pkg android.permission.CAMERA
```

Expected:
- denied state is shown
- retry action is available
- no crash
- no active camera preview behind the denied screen

### CAM-PERM-004: Permanently denied

Perform manually on-device (OEM behavior varies):

- choose “Don’t ask again” when denying permission (or equivalent flow on your Android version

Expected:
- app explains that permission must be enabled in system settings
- settings action opens the app settings page
- returning from settings refreshes permission state

Note: ADB cannot perfectly emulate the full “Don’t ask again” UX on all devices.

## 10. Preview scenarios

### CAM-PREV-001: Rear camera startup

Expected:
- rear camera selected by default
- live image visible
- no long black frame after initialization
- no persistent CameraX error
- preview fills intended viewport
- orientation is correct

### CAM-PREV-002: Background and foreground

```powershell
& $adb -s $device shell input keyevent KEYCODE_HOME
Start-Sleep -Seconds 3
& $adb -s $device shell am start -W -n "$pkg/$activity"
```

Expected:
- preview resumes
- no frozen frame
- no duplicate camera binding error

### CAM-PREV-003: Activity recreation

Enable “Don’t keep activities” in Developer Options or recreate by changing orientation.

Expected:
- preview rebinds
- state remains valid
- no crash

## 11. Camera switching

### CAM-SWITCH-001: Rear to front

Expected:
- front camera opens
- preview stays live
- camera indicator updates
- no crash

### CAM-SWITCH-002: Front to rear

Expected:
- rear camera opens
- no frozen preview
- indicator updates

### CAM-SWITCH-003: Repeated switching

Switch at least 10 times and record:
- successful switches
- failed switches
- freezes
- crashes
- subjective transition quality

Expected:
- all switches complete
- no permanent black screen
- no `FATAL EXCEPTION`

## 12. Rotation

Store original rotation settings:

```powershell
$originalAccelerometer = & $adb -s $device shell settings get system accelerometer_rotation
$originalRotation = & $adb -s $device shell settings get system user_rotation
```

Disable auto-rotation temporarily:

```powershell
& $adb -s $device shell settings put system accelerometer_rotation 0
```

Portrait:

```powershell
& $adb -s $device shell settings put system user_rotation 0
Start-Sleep -Seconds 3
```

Landscape:

```powershell
& $adb -s $device shell settings put system user_rotation 1
Start-Sleep -Seconds 3
```

Return to portrait:

```powershell
& $adb -s $device shell settings put system user_rotation 0
Start-Sleep -Seconds 3
```

Restore original values:

```powershell
& $adb -s $device shell settings put system accelerometer_rotation $originalAccelerometer
& $adb -s $device shell settings put system user_rotation $originalRotation
```

Expected:
- preview adapts correctly
- no stretched/incorrect orientation
- no frozen frame
- no crash
- camera remains usable after returning to portrait

## 13. Pass/fail checklist

| ID | Scenario | Result | Evidence | Notes |
| --- | --- | --- | --- | --- |
| INSTALL-001 | APK install (physical device) | Not run | | |
| CAM-LAUNCH-001 | App launch | Not run | | |
| CAM-PERM-001 | First launch permission request | Not run | | |
| CAM-PERM-002 | Allow permission | Not run | | |
| CAM-PERM-003 | Deny permission | Not run | | |
| CAM-PERM-004 | Permanently denied flow | Not run | | |
| CAM-PREV-001 | Rear camera startup | Not run | | |
| CAM-PREV-002 | Background and foreground | Not run | | |
| CAM-PREV-003 | Activity recreation | Not run | | |
| CAM-SWITCH-001 | Rear to front | Not run | | |
| CAM-SWITCH-002 | Front to rear | Not run | | |
| CAM-SWITCH-003 | Repeated switching | Not run | | |
| CAM-ROTATE-001 | Portrait/landscape sequence | Not run | | |
| CAM-LOG-001 | Crash collection completed | Not run | | |

Use one of:
`Not run`, `Pass`, `Fail`, `Blocked`

## 14. Exit criteria

Phase 2 can be considered physically validated only when all criteria pass:
- APK installs successfully
- application launches without crash
- permission states behave correctly
- rear preview works
- front/rear switching works repeatedly
- portrait and landscape work
- no fatal exception in collected logs
- observed limitations are documented

Passing CI alone does not guarantee CameraX behavior on physical hardware.

## 15. Test report template

Use this section when reporting executed smoke test:

- Date:
- Tester:
- Commit SHA:
- CI run ID:
- APK source:
- APK checksum:
- Device manufacturer:
- Device model:
- Android version:
- API level:
- ADB serial:
- Result:
- Failed scenarios:
- Log paths:
- Screenshots/video evidence:
- Notes:

```powershell
Get-FileHash $apk -Algorithm SHA256
```

Do not record real serial numbers or real personal absolute paths in version-controlled reports.
