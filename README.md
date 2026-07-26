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

Мета AvelCam — дати змогу виводити камеру телефону в OBS Studio з максимально низькою затримкою, спочатку через USB, а потім через Wi‑Fi як резервний шлях.

## Пріоритет підключення

- **USB** — основний шлях.
- **Wi‑Fi** — резервний шлях (планується після стабільного USB-пайплайну).

## Запланована архітектура

- Android-додаток на Kotlin із CameraX для локального захоплення кадрів камери.
- Апаратне H.264-кодування через MediaCodec (планується на наступних етапах).
- Локальна передача через USB/ADB (планується на наступних етапах).
- Десктопний сервіс на Rust для прийому та декодування потоку (планується).
- Нативний плагін OBS Studio на C/C++ (планується).
- Підтримка Wi‑Fi буде додана після стабільного USB-потоку.

## Початкова реалізація Android

- Базовий Android проєкт на Kotlin + Jetpack Compose у `apps/android`.
- Runtime запит дозволу на камеру.
- Локальний live preview через CameraX.
- Перемикання між front/rear камерами.
- Стани: доступ дозволено, відмовлено, постійна відмова.

## Дорожня карта (високий рівень)

1. Підготовка репозиторію — виконано.
2. Android camera preview — в розробці (CameraX preview та перемикання камер).
3. H.264 encoder.
4. USB streaming через ADB.
5. Десктопний приймач і декодер.
6. OBS source plugin.
7. Аудіо підтримка.
8. Wi‑Fi fallback.
9. Прямий USB без обов'язкового ADB.
10. Пакування, інсталяція та релізи.

## Структура репозиторію

- `apps/android` — Android додаток (початкова preview-реалізація).
- `apps/desktop` — Rust-сервіс для майбутнього десктопного прийому/декодування.
- `plugins/obs-avelcam` — плагін OBS Studio.
- `crates/protocol` — спільний протокол.
- `crates/transport-usb` — USB transport (планується).
- `crates/transport-wifi` — Wi‑Fi transport (планується).
- `crates/media-decoder` — модулі декодування (планується).
- `docs` — документація.

## Платформи

- Android
- Windows 10/11
- OBS Studio

## Статус внесків

Інструкції для внесків буде додано пізніше.

## Ліцензія

Ліцензію ще не обрано.

## Попередження

Проєкт перебуває на ранній стадії розробки та **не готовий для продакшн-використання**.

[Перейти до English](#english)

## English

## Project

**AvelCam** is an open-source project that turns an Android phone into a low-latency camera source for OBS Studio, with USB prioritized first and Wi‑Fi as a fallback.

**Status:** early development.

## Overview

The goal of AvelCam is to provide a practical, low-latency camera path into OBS Studio, prioritizing a stable USB pipeline first and Wi‑Fi as a secondary path.

## Connection priority

- **USB** — primary connection.
- **Wi‑Fi** — fallback connection (after the USB pipeline is stable).

## Planned architecture

- Android app in Kotlin using CameraX for local capture.
- Hardware H.264 encoding via MediaCodec (planned).
- USB transport through ADB in a later stage.
- Rust desktop service for receiving and decoding streams (planned).
- Native OBS Studio source plugin in C/C++ (planned).
- Wi‑Fi support planned after USB becomes stable.

## Android preview foundation

- Kotlin + Jetpack Compose app under `apps/android`.
- Runtime camera permission handling.
- CameraX live preview.
- Front and rear camera switching support.
- Permission denied and permanently denied guidance states.

## High-level roadmap

1. Repository foundation — completed.
2. Android camera preview — in development.
3. H.264 encoder.
4. USB streaming through ADB.
5. Desktop receiver and decoder.
6. OBS source plugin.
7. Audio support.
8. Wi‑Fi fallback.
9. Direct USB transport without mandatory ADB.
10. Packaging, installers and releases.

## Repository structure

- `android` — Android app for camera capture.
- `desktop` — Rust service for future stream receive/decode.
- `plugins/obs-avelcam` — OBS source plugin.
- `crates/protocol` — shared protocol definitions.
- `crates/transport-usb` — USB transport.
- `crates/transport-wifi` — Wi‑Fi transport.
- `crates/media-decoder` — media decoding modules.
- `docs` — architectural and roadmap documentation.

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

