# Android Camera Encoder Pipeline Architecture

## Goal

The pipeline connects live CameraX preview frames to `MediaCodec` through a shared GPU path while keeping the on-screen preview active independently from the encoder.

The validated architecture is:

```text
CameraX
  -> SurfaceRequest / SurfaceProvider
  -> SurfaceTexture backed by GL external OES texture
  -> Single GL render thread
  -> Preview EGL output surface
  -> MediaCodec input EGL surface
```

The synthetic encoder diagnostic path remains unchanged and is kept for regression validation.

## CameraX SurfaceProvider ownership

* CameraX owns the capture session and requests a `Surface` to deliver camera frames.
* The app owns a dedicated input `Surface` created from `SurfaceTexture`, and this surface is provided to CameraX via `SurfaceRequest.provideSurface(...)`.
* The surface is released only after CameraX confirms it is safe to do so (or during controlled pipeline release).
* The camera use case is not simultaneously bound directly to the app preview widget and the encoder surface.

## External OES texture input

* A camera-backed `SurfaceTexture` is created from an external OES texture ID (`GL_TEXTURE_EXTERNAL_OES`).
* The `SurfaceTexture` callback notifies frame availability without blocking the CameraX callback thread.
* Frame timestamp is taken from the `SurfaceTexture` when valid, and used as primary presentation timing input.

## SurfaceTexture ownership and lifecycle

* `SurfaceTexture` object is created on GL thread.
* `updateTexImage()` is called only from the GL thread.
* Surface binding/unbinding and buffer release are synchronized with GL thread lifecycle.
* The GL thread owns texture/EGL resources and is responsible for deterministic shutdown.

## Frame-available callback flow

1. CameraX writes frame into provided surface.
2. `SurfaceTexture.OnFrameAvailableListener` callback signals a new frame.
3. Pipeline enqueues a single pending render request (coalesced).
4. GL thread processes the latest pending frame:
   1. `updateTexImage()`
   2. read transform matrix
   3. render the frame into both output surfaces
   4. swap buffers independently for preview and encoder

## Single GL render thread

A single render thread is mandatory and handles:

* EGL context setup/teardown
* external texture creation
* `SurfaceTexture.updateTexImage()`
* shader program usage and frame drawing
* preview and encoder surface rendering
* swap operations and GL-side error handling

No OpenGL calls should execute on UI thread.

## Preview and encoder fan-out

The pipeline produces two output targets from the same camera texture:

* Preview output surface: used for user-facing live view.
* Encoder output surface: MediaCodec input surface.

Both outputs share one source texture and frame timestamp, minimizing latency and drift.

## Texture transform handling

* Surface texture transform matrix from camera frame source is applied each frame.
* Sensor and display rotation are represented in a transform model.
* Cropping preserves aspect ratio for both preview and encoder outputs.
* Front camera mirror policy is explicit per destination:
  * Preview: mirror enabled for natural user-facing behavior.
  * Encoder output: behavior defined by pipeline configuration.

## Crop and aspect ratio

The GL renderer calculates destination rectangles to keep source/destination aspect ratio consistent and avoid stretching.

## Rotation and mirroring

Rotation policy is derived from:

* camera sensor orientation
* display orientation
* lens facing (rear/front)
* preview vs encoder mirror decision

Transform matrices are testable and should be used for both rendering paths.

## Presentation timestamps

* Primary source: `SurfaceTexture.timestamp` (nanoseconds).
* Timestamp is mapped to encoder input with EGL presentation time.
* If timestamp is invalid or zero, fallback uses monotonic local correction.
* Encoded output presentation must be monotonically non-decreasing.
* Timestamp corrections are tracked as statistics.

## Backpressure

The callback rate is controlled by coalescing:

* At most one pending render task is kept.
* Multiple frame callbacks before render are collapsed.
* Only the newest frame timestamp is rendered.
* No unbounded queueing.
* Camera callback and UI threads are never blocked by rendering.

## CameraX switching and lifecycle

* Camera start is independent and can run with encoding disabled.
* Camera switching is allowed from rear to front with controlled pipeline behavior.
* Encoder start is valid only when preview pipeline is active.
* Encoder stop does not require full camera teardown.
* Activity recreation or pipeline failure triggers safe release and rebind as needed.

## Pipeline states

Suggested state machine:

1. `IDLE`
2. `STARTING_PREVIEW`
3. `PREVIEW_RUNNING`
4. `STARTING_ENCODER`
5. `ENCODING`
6. `STOPPING_ENCODER`
7. `STOPPING_PREVIEW`
8. `STOPPED`
9. `ERROR`
10. `RELEASED`

## Failure recovery

* All GL and camera operations report structured errors into pipeline state.
* Failure in one output (preview or encoder) does not collapse both if recovery is possible.
* Preview remains preferredly available when encoder is stopped or temporarily unavailable.
* On fatal errors, all owned resources are released deterministically.

## Failure boundaries and exclusions

This implementation intentionally excludes:

* USB transport
* Wi-Fi transport
* audio capture
* desktop receiver
* OBS plugin
* MP4 recording

## Reference architecture

```text
CameraX Preview UseCase
   -> SurfaceRequest
   CameraInputSurface
      -> Surface
      -> SurfaceTexture
         -> External OES texture
            -> CameraGlPipeline
               -> Preview EGL surface
               -> MediaCodec EGL surface
```
