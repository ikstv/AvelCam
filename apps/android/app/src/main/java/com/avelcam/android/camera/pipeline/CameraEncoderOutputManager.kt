package com.avelcam.android.camera.pipeline

import com.avelcam.android.encoder.EncodedFrameSink
import com.avelcam.android.encoder.EncoderConfig
import com.avelcam.android.encoder.H264Encoder
import com.avelcam.android.encoder.H264EncoderStartResult

data class CameraEncoderOutputManagerState(
    val isRunning: Boolean,
    val startAttempts: Long,
    val selectedCodecName: String?,
    val startResult: H264EncoderStartResult?,
    val lastError: String? = null,
)

class CameraEncoderOutputManager(
    private val encoderConfig: EncoderConfig,
    private val sink: EncodedFrameSink,
    private val coordinator: CameraGlFanoutCoordinator,
    private val encoderFactory: (EncoderConfig, EncodedFrameSink) -> H264Encoder = ::H264Encoder,
    private val destinationFactory: (CameraGlFanoutOutputSpec, android.view.Surface) -> CameraGlFanoutDestination =
        { spec, surface -> EncoderSurfaceGlDestination(spec, surface) },
) {
    private var encoder: H264Encoder? = null
    private var destination: CameraGlFanoutDestination? = null
    private var started = false
    private var startAttempts = 0L
    private var lastError: String? = null
    private var startResult: H264EncoderStartResult? = null

    fun start(): Result<Unit> {
        if (started) {
            return Result.failure(IllegalStateException("Output manager already running."))
        }

        startAttempts++
        val startedEncoder = runCatching {
            val next = encoderFactory(encoderConfig, sink)
            val result = next.start().getOrThrow()
            val inputSurface = next.getInputSurface() ?: throw IllegalStateException("No encoder input surface.")
            val spec = CameraGlFanoutOutputSpec(
                role = CameraGlFanoutOutputRole.ENCODER,
                width = encoderConfig.width,
                height = encoderConfig.height
            )
            val nextDestination = destinationFactory(spec, inputSurface)
            destination = nextDestination
            coordinator.registerDestination(nextDestination)
            encoder = next
            startResult = result
            lastError = null
            started = true
            next
        }
        return startedEncoder.map { Unit }
            .onFailure { error ->
                lastError = error.message
                stopInternal()
            }
    }

    fun stop(): Result<Unit> {
        return runCatching {
            stopInternal()
            startResult = null
            lastError = null
        }
    }

    fun snapshot(): CameraEncoderOutputManagerState {
        return CameraEncoderOutputManagerState(
            isRunning = started,
            startAttempts = startAttempts,
            selectedCodecName = encoder?.getSelectedCodec()?.codecName,
            startResult = startResult,
            lastError = lastError
        )
    }

    fun release() {
        stopInternal()
    }

    private fun stopInternal() {
        if (!started && encoder == null && destination == null) {
            return
        }

        destination?.let { coordinator.unregisterDestination(it) }
        destination?.release()
        destination = null
        encoder?.stop()?.onFailure {
            lastError = it.message
        }
        encoder = null
        started = false
    }
}
