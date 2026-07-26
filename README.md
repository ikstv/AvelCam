# AvelCam

[![Status: Early Development](https://img.shields.io/badge/status-early%20development-yellow)](#english)
![Platform: Android](https://img.shields.io/badge/platform-Android-blue)
![Platform: Windows](https://img.shields.io/badge/platform-Windows-lightgray)
![Integration: OBS Studio](https://img.shields.io/badge/integration-OBS%20Studio-ff9900)

## Український опис

## Опис

**AvelCam** — це застосунок, що робить Android-пристрій камерою для OBS Studio.

**Статус:** ранній етап розробки.

## Мета

AvelCam побудований як pipeline з локального перегляду на Android до майбутньої передачі кадрів на десктоп через USB (потім Wi-Fi).

## Етапи

1. Репозиторій і документація: completed
2. Android camera preview: completed
3. H.264 encoder: in development
4. USB streaming через ADB: not implemented
5. Desktop receiver and decoder: not implemented
6. OBS plugin: not implemented
7. Audio: not implemented
8. Wi-Fi fallback: not implemented
9. Direct USB без ADB: not implemented
10. Packaging/реліз: not implemented

## Поточний статус

- Android модуль працює з CameraX preview.
- Фізичний smoke test CameraX preview завершений.
- Відкритий PR 1 вже змерджено в `main`.
- Починаємо Phase 3: інженерний тест енкодера на синтетичних кадрах.

## Посилання

- [Phase roadmap](docs/roadmap.md)
- [Android module README](apps/android/README.md)
- [Camera smoke test report](docs/qa/reports/android-camera-smoke-test-2026-07-27.md)
- [Encoder smoke test protocol](docs/qa/android-h264-encoder-smoke-test.md)

## English

## Project

**AvelCam** turns an Android phone into a camera source for OBS Studio with a staged engineering approach starting from local preview, then H.264 encoding, then transport.

**Status:** early development.

## Roadmap status

1. Repository foundation — completed.
2. Android camera preview — completed.
3. H.264 encoder — in development.
4. USB streaming through ADB — not implemented.
5. Desktop receiver and decoder — not implemented.
6. OBS plugin — not implemented.
7. Audio support — not implemented.
8. Wi-Fi fallback — not implemented.
9. Direct USB without mandatory ADB — not implemented.
10. Packaging, installers, and releases — not implemented.

## Current note

- Android preview with permission handling and front/rear switch is on `main`.
- Phase 3 now focuses on a production-oriented encoder foundation:
  - MediaCodec H.264
  - Surface input
  - Synthetic OpenGL frame source
  - Encoded output callback + NAL parsing
  - Diagnostics and unit coverage

## Contributing notes

- No USB/Wi-Fi transport is implemented in this phase.
- Phase 3 intentionally excludes CameraX to encoder handoff.

