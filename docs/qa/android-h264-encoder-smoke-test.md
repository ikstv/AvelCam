# Android H.264 Encoder Smoke Test

## Scope

Phase 3 validation is based on synthetic OpenGL input into MediaCodec `Surface` input.
No CameraX frame integration, USB transport, Wi-Fi transport, audio, or desktop features are included in this protocol.

## Test environment

- Repository: `ikstv/AvelCam`
- Branch: `feat/android-h264-encoder`
- Branch base: latest `main`
- Device: physical Android device (required)

## Checklist

| ID | Case | Status | Notes |
| --- | --- | --- | --- |
| ENC-INSTALL-001 | Install APK | Not run | pending |
| ENC-START-001 | Start synthetic encoder test | Not run | pending |
| ENC-OUTPUT-001 | Receive encoded access units | Not run | pending |
| ENC-CSD-001 | Receive codec config / csd data | Not run | pending |
| ENC-KEYFRAME-001 | Receive keyframe indication | Not run | pending |
| ENC-PTS-001 | Validate monotonic PTS sequence | Not run | pending |
| ENC-STATS-001 | Show runtime encoder statistics | Not run | pending |
| ENC-STOP-001 | Stop encoder cleanly | Not run | pending |
| ENC-RESTART-001 | Start/Stop repeated cycles | Not run | pending |
| ENC-LOG-001 | No fatal runtime exceptions in logcat filter | Not run | pending |

## Exit criteria

- Encoder configuration succeeds.
- Selected codec is reported.
- Input surface is created.
- Synthetic frames are submitted.
- Encoded output is received.
- SPS/PPS or codec-config metadata is observed.
- At least one keyframe is observed.
- PTS is monotonic.
- Stop path completes.
- Repeated start/stop works.
- No fatal codec errors are observed.

## Local execution notes

- Test is not a CI gate yet.
- Keep this protocol status `blocked` until physical device execution is performed.

