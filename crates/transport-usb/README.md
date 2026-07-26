## USB Transport Crate

### Responsibility

This crate handles USB-level transport semantics for the MVP, including device discovery, stream handoff, and transport-specific error/reconnect behavior.

### Planned technology

- Rust
- USB transport abstractions
- ADB-oriented transport lifecycle management (MVP)

### Current status

Not implemented yet.

### Dependencies

- `crates/protocol` for frame/control envelope
- `apps/desktop` for receiving transport events

