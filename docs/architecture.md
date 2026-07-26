# AvelCam Architecture

## Purpose

AvelCam is split into independent layers to keep capture, transport, decoding, and OBS integration replaceable and independently evolvable.

## Android capture layer

The Android application is responsible for:

- Enumerating and selecting camera devices (front/rear).
- Capturing frames via CameraX in a format that can be efficiently encoded.
- Passing frames to the platform encoder with low-latency settings.

## H.264 encoding

MediaCodec-based H.264 encoding is used for MVP because:

- It is hardware-accelerated on common devices.
- It balances quality, latency, and power usage better than full software encoders in this context.
- It produces a transport-friendly stream suitable for real-time consumption.

## USB-first transport

The transport architecture prioritizes USB in phase 1.

- USB is typically lower latency and more deterministic.
- ADB-assisted transport lowers initial integration complexity on Windows.
- The USB layer must report transport health and connection lifecycle events so the desktop service can reconnect quickly.

## Future Wi‑Fi fallback

Wi‑Fi support will be a secondary path after USB stabilizes.

- It adds flexibility where USB cables are not available.
- It should keep the same protocol and framing where possible to reduce fragmentation.
- Additional security and pairing controls are required here and are explicitly planned.

## Desktop core service

The desktop service is the central coordinator outside OBS:

- Keeps the active transport connection.
- Buffers/retries and enforces stream timing expectations.
- Hands decoded frames to downstream consumers (OBS plugin).
- Exposes local IPC control points for status and future controls.

## OBS plugin

The OBS plugin is a thin integration layer:

- Registers a source in OBS Studio.
- Requests decoded frames from local service APIs/IPC.
- Passes frames into OBS rendering pipeline with minimal transformations.

## Local IPC between desktop service and OBS plugin

A dedicated local IPC layer is required to:

- decouple transport/decode timing from OBS render thread timing;
- reduce coupling between service failures and plugin lifecycle;
- keep security boundaries local and explicit.

## Why transport stays separate from OBS plugin

Keeping transport separate provides:

- clearer boundaries for testing transport stability independently from OBS;
- easier swapping or extension of transport implementations;
- cleaner isolation for recovery logic (e.g., reconnect and fallback behavior);
- a service process model that can later support additional clients.

```text
      +-------------------+        H.264 frames        +------------------+
      | Android App       | -------------------------> | Desktop Service  |
      | CameraX +         |  (USB in MVP, Wi‑Fi later) | Rust             |
      | MediaCodec        | <------------------------- |                  |
      +-------------------+         status/control      +---------+--------+
                                                                    |
                                                                    | local IPC
                                                                    v
                                                        +-----------+-----------+
                                                        | OBS Plugin (C/C++)    |
                                                        | OBS source integration |
                                                        +-----------------------+
```

