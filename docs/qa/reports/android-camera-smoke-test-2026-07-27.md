# Android Camera Physical Device Smoke-Test Report

## Summary

The Android Camera Foundation physical-device smoke test passed on real hardware.

- Date: 2026-07-27
- Tested commit: `9d6d476` or later on `feat/android-camera-foundation`
- CI run: `30220086113`
- CI artifact: `avelcam-debug-apk`
- Device model: `22071212AG`
- Android version: not recorded
- Overall result: **Pass**
- Failed scenarios: none

> Privacy note: the ADB serial and personal absolute filesystem paths are intentionally not recorded in this version-controlled report.

## Results

| ID | Scenario | Result | Evidence / notes |
| --- | --- | --- | --- |
| INSTALL-001 | APK installation | Pass | ADB installation returned `Success`. |
| CAM-LAUNCH-001 | Application launch | Pass | Activity launch returned `Status: ok`. |
| CAM-PERM-001 | First permission request | Pass | Camera permission prompt appeared on first launch. |
| CAM-PERM-002 | Allow permission | Pass | Permission was granted and preview started. |
| CAM-PERM-003 | Deny permission | Pass | Denied-state flow behaved correctly. |
| CAM-PERM-004 | Permanently denied flow | Pass | Settings recovery flow behaved correctly. |
| CAM-PREV-001 | Rear camera startup | Pass | Rear preview produced live frames at approximately 24 FPS. |
| CAM-PREV-002 | Background and foreground | Pass | Preview recovered after background/foreground transition. |
| CAM-PREV-003 | Activity recreation | Pass | Camera preview recovered after recreation. |
| CAM-SWITCH-001 | Rear to front | Pass | Front camera opened correctly. |
| CAM-SWITCH-002 | Front to rear | Pass | Rear camera reopened correctly. |
| CAM-SWITCH-003 | Ten repeated switches | Pass | Ten switch cycles completed without freeze or crash. |
| CAM-ROTATE-001 | Portrait and landscape | Pass | Preview remained functional through rotation. |
| CAM-LOG-001 | Fatal-error inspection | Pass | No `FATAL EXCEPTION`; crash buffer contained no application crash. |

## Additional observations

- The preview produced frames at approximately 24 FPS during the smoke test.
- Camera permission was restored after testing.
- Local log artifacts were retained outside version control.
- No crash or fatal Android runtime exception was observed.

## Exit criteria

All Phase 2 physical-device exit criteria passed:

- [x] APK installed successfully.
- [x] Application launched without crashing.
- [x] Camera permission flows behaved correctly.
- [x] Rear camera preview worked.
- [x] Front/rear camera switching worked repeatedly.
- [x] Portrait and landscape behavior passed.
- [x] Background/foreground recovery passed.
- [x] Activity recreation passed.
- [x] No fatal exception was present in the collected logs.

## Decision

**Phase 2 — Android camera preview is physically validated and completed.**

Phase 3 may begin with the design and implementation of the low-latency H.264 MediaCodec encoder.