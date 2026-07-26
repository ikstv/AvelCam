# Security Notes

## Local-first operation

The project is designed to operate locally by default:

- Stream capture and forwarding happen on the same physical machine network boundary.
- No cloud relay or remote media routing is planned for MVP.
- Device interaction remains local to the Android device and the host workstation.

## No cloud relay in MVP

The initial release path does not rely on any external relay servers.

This avoids unnecessary remote data exposure and keeps operational overhead minimal during validation.

## Device authorization

The desktop service and Android application must authenticate trust at the transport/session level:

- pairing/approval workflow on first connection,
- session reset on disconnect,
- explicit teardown on stop.

## ADB trust considerations

Using ADB in MVP requires careful trust handling:

- ADB is a local transport dependency, not a general-purpose secure tunnel.
- Users should connect only trusted devices.
- Desktop-side tooling should make trust state explicit.
- Disconnects and permission changes must trigger safe fallback behavior.

## Encrypted Wi‑Fi pairing (future)

Wi‑Fi fallback is not part of MVP security posture yet.

Planned future controls include:

- encrypted pairing flow,
- per-device session keys,
- short-lived tokens and nonce-based connection validation.

## Secrets and credentials

- `.env` files and API keys must never be committed.
- Signing keys, certificates, and keystores must never be committed.

## Responsible vulnerability handling

Vulnerability reporting instructions will be added later.
