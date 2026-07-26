## Wi-Fi Transport Crate

### Responsibility

This crate will implement Wi‑Fi transport as the fallback path, including pairing, discovery, and packet transport for media/control streams.

### Planned technology

- Rust
- Local network transport abstraction
- Optional reliable transport strategy over UDP/TCP

### Current status

Not implemented yet.

### Dependencies

- `crates/protocol` for shared message formats
- `apps/desktop` for transport orchestration

