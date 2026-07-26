## Android App

### Responsibility

The Android app will manage camera capture on the phone, provide front and rear camera selection, and expose a stream pipeline compatible with the desktop transport layer.

### Planned technology

- Kotlin
- CameraX
- MediaCodec (H.264)
- Android USB utilities for connectivity/state handling

### Current status

Not implemented yet.

### Dependencies

- `crates/protocol` for frame/control protocol schema
- `crates/transport-usb` for USB transport bindings
- `crates/media-decoder` (future integration for loopback/test verification)

