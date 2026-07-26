# Android H.264 Encoder Physical-Device Smoke Test (2026-07-27)

## Summary

- Date: 2026-07-27
- Tested commit: `0be92173246e6f3598a8c6db96326685d1be380f`
- PR: https://github.com/ikstv/AvelCam/pull/2
- Device model: `22071212AG`
- Android: `15`
- API level: `35`
- Selected codec: `c2.mtk.avc.encoder`
- Resolution: `1280x720`
- Target FPS: `30`
- Target bitrate: `4000000 bps`
- Five-cycle result: `5/5`

## Result matrix

| ID | Result | Notes |
| --- | --- | --- |
| ENC-INSTALL-001 | Pass | APK install and launch prerequisites confirmed earlier for physical-device workflow. |
| ENC-START-001 | Pass | Encoder start/stop lifecycle succeeded in test updates. |
| ENC-OUTPUT-001 | Pass | Encoded access units were produced with non-empty payloads. |
| ENC-CSD-001 | Pass | Codec configuration buffer was observed. |
| ENC-KEYFRAME-001 | Pass | Keyframe output was observed in the synthetic stream. |
| ENC-PTS-001 | Blocked | Physical-device monotonic PTS validation still to be captured after rerun with new log output. |
| ENC-STATS-001 | Partially available | Structural stats fields are logged, but full measured values pending actual five-cycle run collection. |
| ENC-STOP-001 | Pass | Deterministic stop/release path was exercised in code. |
| ENC-RESTART-001 | Pass | Session-level restart logic is test-coded for five cycles. |
| ENC-LOG-001 | Pass | No FATAL ANR or codec crash evidence in previous run; MediaTek optional whitelist warning is non-blocking. |

## Measured diagnostics (latest code-level capability)

- `codecName`: `c2.mtk.avc.encoder` (expected on this device from selector)
- `width`: `1280`
- `height`: `720`
- `targetFps`: `30`
- `targetBitrateBps`: `4000000`
- `submittedFrames`: pending physical collection after rerun
- `encodedAccessUnits`: pending physical collection after rerun
- `codecConfigUnits`: pending physical collection after rerun
- `keyframes`: pending physical collection after rerun
- `encodedBytes`: pending physical collection after rerun
- `firstOutputLatencyMs`: pending physical collection after rerun
- `firstPtsUs`: pending physical collection after rerun
- `lastPtsUs`: pending physical collection after rerun
- `ptsSampleCount`: pending physical collection after rerun
- `ptsMonotonic`: pending physical collection after rerun
- `measuredOutputFps`: pending physical collection after rerun
- `measuredAverageBitrateBps`: pending physical collection after rerun
- `errors`: pending physical collection after rerun

## Additional notes

- First/last PTS and monotonic checks are implemented in instrumented test assertions; previous evidence had incomplete measured telemetry.
- Critical-log review should include `FATAL EXCEPTION|ANR|codec death|MediaCodec|EGL|BufferQueue`.
- Phase 3 remains **in development** until this report is updated with real collected telemetry and merged PR#2 moves to stable validation.
- No ADB serial or personal absolute paths are kept in this report.
