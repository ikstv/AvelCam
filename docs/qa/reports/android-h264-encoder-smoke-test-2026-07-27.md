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
| ENC-OUTPUT-001 | Pass | Encoded access units were produced with non-empty payloads on all 5 cycles. |
| ENC-CSD-001 | Pass | Codec configuration buffer was observed on all 5 cycles. |
| ENC-KEYFRAME-001 | Pass | Keyframe output was observed on all 5 cycles. |
| ENC-PTS-001 | Pass | PTS are non-negative, strictly monotonic (>= previous), and collected for at least 2 samples per cycle. |
| ENC-STATS-001 | Pass | Measured stats are present for each of 5 successful start/stop cycles. |
| ENC-STOP-001 | Pass | Deterministic stop/release path was exercised in code. |
| ENC-RESTART-001 | Pass | Session-level restart logic is test-coded for five cycles. |
| ENC-LOG-001 | Pass | No `FATAL EXCEPTION`/`ANR` observed. Repeated `C2MtkVenc` whitelist file warnings are non-blocking. |

## Measured diagnostics (latest code-level capability)

- `codecName`: `c2.mtk.avc.encoder` (expected on this device from selector)
- `width`: `1280`
- `height`: `720`
- `targetFps`: `30`
- `targetBitrateBps`: `4000000`
- `submittedFrames`: `2` (per cycle, cycle 1..5)
- `encodedAccessUnits`: `3` (per cycle, cycle 1..5)
- `codecConfigUnits`: `1` (per cycle, cycle 1..5)
- `keyframes`: `1` (per cycle, cycle 1..5)
- `encodedBytes`: `2958` (per cycle, cycle 1..5)
- `firstOutputLatencyMs`: `117.47, 69.59, 63.89, 65.45, 80.95`
- `firstPtsUs`: `71235706320, 71235799944, 71235895046, 71235986923, 71236085462`
- `lastPtsUs`: `71235739653, 71235833277, 71235928379, 71236020256, 71236118795`
- `ptsSampleCount`: `2` (per cycle, cycle 1..5)
- `ptsMonotonic`: `true` (per cycle, cycle 1..5)
- `measuredOutputFps`: `30.000` (all cycles)
- `measuredAverageBitrateBps`: `709927.10` (all cycles)
- `errors`: `0` (per cycle, cycle 1..5)
- `cycleCount`: `5`
- `allCyclesPass`: `true`

### Cycle summary (latest five-cycle run, physical device)

| Cycle | firstOutputLatencyMs | firstPtsUs | lastPtsUs | ptsSampleCount | ptsMonotonic | measuredOutputFps | measuredAverageBitrateBps |
| --- | ---: | ---: | ---: | ---: | --- | ---: | ---: |
| 1 | 117.47 | 71235706320 | 71235739653 | 2 | true | 30.000 | 709927.10 |
| 2 | 69.59 | 71235799944 | 71235833277 | 2 | true | 30.000 | 709927.10 |
| 3 | 63.89 | 71235895046 | 71235928379 | 2 | true | 30.000 | 709927.10 |
| 4 | 65.45 | 71235986923 | 71236020256 | 2 | true | 30.000 | 709927.10 |
| 5 | 80.95 | 71236085462 | 71236118795 | 2 | true | 30.000 | 709927.10 |

## Additional notes

- First/last PTS and monotonic checks are implemented in instrumented test assertions.
- Critical-log review includes `FATAL EXCEPTION|ANR|codec death|MediaCodec|EGL|BufferQueue`.
- `MediaCodec` runs succeeded with the selected codec `c2.mtk.avc.encoder`.
- Repeated `Failed to open: /vendor/etc/mtk_platform_codecs_whitelist.xml` warning is present and treated as non-blocking.
- Phase 3 remains **in development** because CameraX-to-encoder integration is still outside PR #2 scope.
- No ADB serial or personal absolute paths are kept in this report.
