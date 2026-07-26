## Desktop Core Service

### Responsibility

The desktop service receives the incoming stream from the selected transport, performs low-latency buffering and decoding, and exposes frames to OBS plugin integration points.

### Planned technology

- Rust
- Frame queue and timing management
- Media codec interop utilities for decoding
- Local IPC endpoint for the OBS plugin

### Current status

Not implemented yet.

### Dependencies

- `crates/transport-usb`
- `crates/transport-wifi`
- `crates/media-decoder`
- `crates/protocol`
- `plugins/obs-avelcam`

