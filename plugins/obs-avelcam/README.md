## OBS Plugin

### Responsibility

The OBS plugin will provide a native source implementation that reads decoded frames from the local desktop service and renders them inside OBS Studio.

### Planned technology

- C/C++
- OBS Studio plugin SDK (desktop integration layer)
- Frame source callbacks

### Current status

Not implemented yet.

### Dependencies

- `apps/desktop` for service integration and IPC
- `crates/protocol` for stream metadata and control messages
- `crates/media-decoder` for media format consistency

