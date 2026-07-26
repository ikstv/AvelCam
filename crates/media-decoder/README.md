## Media Decoder Crate

### Responsibility

This crate provides decoder interfaces and implementations for processing incoming H.264 frames before handing them to the desktop service/OBS pipeline.

### Planned technology

- Rust
- H.264 decode integration layer
- Decoder abstraction and format conversion helpers

### Current status

Not implemented yet.

### Dependencies

- `crates/protocol`
- `apps/desktop`
- `plugins/obs-avelcam` (for output format compatibility)

