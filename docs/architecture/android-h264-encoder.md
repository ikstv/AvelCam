# Android H.264 Encoder Architecture

## Why MediaCodec

The current phase uses Android `MediaCodec` because it gives platform-native H.264 encoding paths and avoids shipping custom codecs in the app process.

## Why H.264/AVC first

H.264 is widely supported by desktop and toolchain stacks used by future AvelCam transport and decoding stages. It is also available in hardware on most target Android devices.

## Why Surface input

Surface input is required for a GPU-first pipeline. `CameraX` frames can be fed in a later phase via a shared producer graph; for now we use deterministic synthetic OpenGL content to validate encoding semantics before camera integration.

## Why OpenGL for synthetic input

The synthetic source is rendered into the codec input surface via EGL/OpenGL so that:

- Frames are submitted deterministically,
- No CPU copy path is introduced,
- Encoder behavior under surface input can be validated early.

## Encoder state machine

```text
EncoderConfig  ->  Codec selection  ->  MediaCodec.configure(CONFIGURE_FLAG_ENCODE)
    -> createInputSurface()  -> start
    -> Running  -> Stopping  -> Stopped / Error / Released
```

## Configuration flow

1. Validate `EncoderConfig`.
2. Select a compatible H.264 encoder.
3. Configure `MediaFormat` for:
   - `KEY_MIME`
   - `KEY_WIDTH`
   - `KEY_HEIGHT`
   - `KEY_COLOR_FORMAT`
   - `KEY_BIT_RATE`
   - `KEY_FRAME_RATE`
   - `KEY_I_FRAME_INTERVAL`
4. Configure codec with `CONFIGURE_FLAG_ENCODE`.
5. Create surface and attach OpenGL renderer.

## Output callback flow

- `MediaCodec.Callback` receives:
  - output format,
  - output buffer,
  - codec exception.
- Buffers are copied before release and transformed into `EncodedAccessUnit`.
- Callback preserves:
  - `presentationTimeUs`,
  - flags,
  - and emitted data.

## Timestamp behavior

- Input frames are produced by the synthetic source with monotonic presentation timestamps.
- Clock is derived from monotonic source (`System.nanoTime`/`elapsedRealtimeNanos`) and converted to microseconds for output metadata.
- Timestamp monotonicity is validated in unit tests.

## SPS/PPS and codec config

- Codec config output (`csd-0` and `csd-1`) is captured from output `MediaFormat` when available.
- NAL inspection identifies SPS/PPS and keyframe slices in emitted access units.

## Keyframe strategy

- Initial phase validates that codec can emit `KEY_FRAME`/IDR signals and codec-config frames.
- Frame rate and bitrate are fixed defaults and can be explicitly overridden in tests and future UI.

## Shutdown and ownership

- `stop()` and `release()` are expected to be idempotent and safe after failure.
- All codec and EGL resources are released in stop/release paths.

## Failure behavior

- Unavailable encoder selection returns a controlled error.
- Unsupported state transitions fail predictably.
- Runtime codec exceptions move the encoder state to `ERROR` and notify sink.

## Next step with CameraX

This phase validates a pure synthetic source path:

`Synthetic OpenGL -> MediaCodec input surface -> EncodedAccessUnit`

Next step is to switch synthetic source input to CameraX preview surfaces while preserving the same encoder contract.

```text
EncoderConfig
    -> Codec selection
    -> MediaCodec.configure(CONFIGURE_FLAG_ENCODE)
    -> createInputSurface()
    -> OpenGL renderer
    -> MediaCodec output callback
    -> EncodedAccessUnit
    -> EncodedFrameSink
```

## Explicit exclusions for this step

- No USB transport.
- No Wi-Fi transport.
- No audio processing.
- No desktop/RX path.
- No OBS integration.

