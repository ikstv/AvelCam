package com.avelcam.android.camera.pipeline

import com.avelcam.android.encoder.EncoderConfig
import com.avelcam.android.encoder.EncodedFrameSink

data class CameraGlFanoutControllerConfig(
    val pipelineConfig: CameraEncoderPipelineConfig,
    val encoderConfig: EncoderConfig,
    val frontCameraPreviewMirrored: Boolean = false,
    val frontCameraEncoderMirrored: Boolean = false,
)

class CameraGlFanoutController(
    private val coordinator: CameraGlFanoutCoordinator = CameraGlFanoutCoordinator(),
    private val outputManagerFactory: (
        coordinator: CameraGlFanoutCoordinator,
        config: EncoderConfig,
        sink: EncodedFrameSink,
        frontCameraEncoderMirrored: Boolean
    ) -> CameraEncoderOutputManagerContract = { coord, config, sink, _ ->
        CameraEncoderOutputManager(config, sink, coord).asContract
    },
    private val outputSink: EncodedFrameSink = NoopEncodedFrameSink(),
) {
    private var configured = false
    private var outputManager: CameraEncoderOutputManagerContract? = null
    private var startAttempts = 0L

    fun configure(config: CameraGlFanoutControllerConfig) {
        if (configured) return
        coordinator.configure(
            CameraGlFanoutCoordinatorConfig(
                pipelineConfig = config.pipelineConfig,
                frontCameraPreviewMirrored = config.frontCameraPreviewMirrored,
                frontCameraEncoderMirrored = config.frontCameraEncoderMirrored
            )
        )
        outputManager = outputManagerFactory(
            coordinator,
            config.encoderConfig,
            outputSink,
            config.frontCameraEncoderMirrored
        )
        configured = true
    }

    fun start() : Result<Unit> {
        ensureConfigured()
        if (!isRunning()) {
            startAttempts++
            coordinator.start()
            val started = outputManager?.start() ?: return Result.success(Unit)
            return started.onSuccess {
            }.onFailure {
                coordinator.stop()
            }
        }
        return Result.failure(IllegalStateException("Controller already running."))
    }

    fun stop() : Result<Unit> {
        val stopResult = outputManager?.stop() ?: Result.success(Unit)
        coordinator.stop()
        return stopResult
    }

    fun release() {
        outputManager?.release()
        outputManager = null
        coordinator.release()
        configured = false
    }

    fun onCameraFrame(
        sourceWidth: Int,
        sourceHeight: Int,
        metadata: CameraFrameMetadata,
        sourceTextureId: Int = 0,
    ) {
        if (!isRunning()) {
            return
        }
        coordinator.onCameraFrame(sourceWidth, sourceHeight, metadata, sourceTextureId)
    }

    fun registerPreviewDestination(destination: CameraGlFanoutDestination) {
        ensureConfigured()
        coordinator.registerDestination(destination)
    }

    fun unregisterPreviewDestination(destination: CameraGlFanoutDestination) {
        coordinator.unregisterDestination(destination)
    }

    fun snapshot(): CameraGlFanoutControllerSnapshot {
        return CameraGlFanoutControllerSnapshot(
            configured = configured,
            coordinator = coordinator.snapshot(),
            outputManager = outputManager?.snapshot(),
            startAttempts = startAttempts,
            running = isRunning(),
        )
    }

    private fun isRunning(): Boolean = configured && coordinator.snapshot().running
    private fun ensureConfigured() {
        if (!configured) throw IllegalStateException("Controller not configured.")
    }

    fun isConfigured(): Boolean = configured
}

data class CameraGlFanoutControllerSnapshot(
    val configured: Boolean,
    val running: Boolean,
    val startAttempts: Long,
    val coordinator: CameraGlFanoutCoordinatorSnapshot,
    val outputManager: CameraEncoderOutputManagerState? = null,
)

class NoopEncodedFrameSink : EncodedFrameSink {
    override fun onEncodedAccessUnit(accessUnit: com.avelcam.android.encoder.EncodedAccessUnit) {
        // reserved for integration path: currently consumed by encoder diagnostic/logging layer.
    }

    override fun onFrameSinkError(error: Throwable) {
        // intentionally no-op; controller owner can swap via factory.
    }
}
