# AvelCam

[![Status: Early Development](https://img.shields.io/badge/status-early%20development-yellow)](#english)
![Platform: Android](https://img.shields.io/badge/platform-Android-blue)
![Platform: Windows](https://img.shields.io/badge/platform-Windows-lightgray)
![Integration: OBS Studio](https://img.shields.io/badge/integration-OBS%20Studio-ff9900)

## Українська

## Проєкт

**AvelCam** — це відкритий проєкт, який перетворює Android-пристрій на низьколатентне джерело відео для OBS Studio з пріоритетом підключення через USB.

**Статус:** ранній етап розробки (Early Development).

## Огляд

Мета AvelCam — дати змогу виводити камеру телефону в OBS Studio з максимально можливою затримкою, спочатку через USB, а потім через Wi‑Fi як резервний шлях. Спочатку зосереджуємося на стабільному MVP для Windows, після чого поступово розширюємо функціонал.

## Пріоритет підключення

- **USB** — основний шлях (перший).
- **Wi‑Fi** — резервний шлях (планується після стабільного USB-пайплайна).

## Запланована архітектура

- Android-додаток на Kotlin із CameraX для захоплення потоку камери.
- Аппаратне H.264-кодування через MediaCodec.
- Передача потоків через USB (MVP: через ADB).
- Десктопний сервіс на Rust для прийому та декодування потоку.
- Нативний плагін OBS Studio на C/C++ для інтеграції з джерелом.
- Wi‑Fi підключення додасться пізніше.

## Обсяг початкового MVP

- Платформа: Android → Windows.
- Інтеграція з OBS Studio через нативний source plugin.
- Передача по USB через ADB.
- Відео в H.264, 1280×720 @ 30 FPS.
- Вибір передньої або задньої камери.
- Автоперепідключення по USB.
- На старті — лише відео (без аудіо).

## Дорожня карта (високий рівень)

1. Підготовка репозиторію.
2. Попередній перегляд камери на Android.
3. Nабір H.264 кодувальника.
4. USB-стрімінг через ADB.
5. Десктопний приймач і декодер.
6. Плагін джерела OBS.
7. Підтримка аудіо.
8. Резервне підключення через Wi‑Fi.
9. Пряме USB-підключення без обов'язкового ADB.
10. Пакування, інсталяція та релізи.

## Структура репозиторію

- `android/` — Android-додаток (захоплення відео та керування камерою).
- `desktop/` — Rust-сервіс для прийому/декодування потоків.
- `plugins/obs-avelcam/` — плагін OBS Studio.
- `crates/protocol/` — спільний протокол між компонами.
- `crates/transport-usb/` — транспортний рівень USB.
- `crates/transport-wifi/` — транспортний рівень Wi‑Fi.
- `crates/media-decoder/` — модулі декодування медіапотоків.
- `docs/` — архітектурна та процесна документація.

## Платформи

- Android
- Windows 10/11
- OBS Studio

## Статус внесків

Інструкції щодо внесків будуть додані пізніше.

## Ліцензія

Ліцензія ще не обрана.

## Попередження

Проєкт перебуває на ранній стадії розробки та **не готовий до продакшн-використання**.

[Перейти до розділу English](#english)

## English

## Project

**AvelCam** is an open-source project that turns an Android phone into a low-latency camera source for OBS Studio, with USB used first and Wi‑Fi as a fallback.

**Status:** early development.

## Overview

The goal of AvelCam is to provide a practical, low-latency camera source path into OBS Studio for Android devices, prioritizing a stable USB pipeline first and Wi‑Fi as a secondary path afterward.

## Connection priority

- **USB** — primary connection.
- **Wi‑Fi** — fallback connection (after USB pipeline is stable).

## Planned architecture

- Android app in Kotlin using CameraX for camera capture.
- Hardware H.264 encoding through MediaCodec.
- Transport over USB (MVP: via ADB).
- Rust desktop service for receiving and decoding streams.
- Native OBS Studio source plugin in C/C++.
- Wi‑Fi support will be added after the USB path is stable.

## Initial MVP scope

- Android to Windows.
- OBS Studio integration through a native source plugin.
- USB connection via ADB.
- H.264 video at 1280×720 and 30 FPS.
- Front and rear camera selection.
- Automatic USB reconnection.
- Video only for the initial phase.

## High-level roadmap

1. Repository foundation.
2. Android camera preview.
3. H.264 encoder implementation.
4. USB streaming through ADB.
5. Desktop receiver and decoder.
6. OBS source plugin.
7. Audio support.
8. Wi‑Fi fallback.
9. Direct USB transport without mandatory ADB.
10. Packaging, installers and releases.

## Repository structure

- `android/` — Android app (capture and camera controls).
- `desktop/` — Rust service for stream ingest and decode.
- `plugins/obs-avelcam/` — OBS Studio plugin.
- `crates/protocol/` — shared protocol definitions used across components.
- `crates/transport-usb/` — USB transport implementation.
- `crates/transport-wifi/` — Wi‑Fi transport implementation.
- `crates/media-decoder/` — media decoding modules.
- `docs/` — architectural and planning documentation.

## Supported platforms

- Android
- Windows 10/11
- OBS Studio

## Contribution status

Contribution guidance will be added later.

## License status

No license has been selected yet.

## Disclaimer

The project is in early development and **not ready for production use**.
