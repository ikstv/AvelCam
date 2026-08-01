package com.avelcam.android.camera.pipeline

import com.avelcam.android.encoder.EncoderConfig

data class CameraGlFanoutRuntimeConfig(
    val cameraWidth: Int,
    val cameraHeight: Int,
    val previewWidth: Int,
    val previewHeight: Int,
    val encoderWidth: Int,
    val encoderHeight: Int,
    val frontCameraPreviewMirrored: Boolean = false,
    val frontCameraEncoderMirrored: Boolean = false,
    val encoderConfig: EncoderConfig = EncoderConfig(),
    val cropMode: CameraPipelineCropMode = CameraPipelineCropMode.FIT
)

class CameraGlFanoutRuntime(
    private val controller: CameraGlFanoutController = CameraGlFanoutController()
) {
    private var configured = false

    fun configure(config: CameraGlFanoutRuntimeConfig) {
        if (configured) {
            return
        }

        controller.configure(
            CameraGlFanoutControllerConfig(
                pipelineConfig = CameraEncoderPipelineConfig(
                    cameraWidth = config.cameraWidth,
                    cameraHeight = config.cameraHeight,
                    previewWidth = config.previewWidth,
                    previewHeight = config.previewHeight,
                    encoderWidth = config.encoderWidth,
                    encoderHeight = config.encoderHeight,
                    frontCameraPreviewMirrored = config.frontCameraPreviewMirrored,
                    frontCameraEncoderMirrored = config.frontCameraEncoderMirrored,
                    cropMode = config.cropMode
                ),
                encoderConfig = config.encoderConfig
            )
        )
        configured = true
    }

    fun start(): Result<Unit> {
        if (!configured) {
            return Result.failure(IllegalStateException("Runtime not configured."))
        }
        return controller.start()
    }

    fun stop(): Result<Unit> = controller.stop()

    fun release() {
        controller.release()
        configured = false
    }

    fun onCameraFrame(
        sourceWidth: Int,
        sourceHeight: Int,
        metadata: CameraFrameMetadata
    ) {
        controller.onCameraFrame(sourceWidth, sourceHeight, metadata)
    }

    fun registerPreviewDestination(destination: CameraGlFanoutDestination) {
        if (!configured) {
            throw IllegalStateException("Runtime not configured.")
        }
        controller.registerPreviewDestination(destination)
    }

    fun unregisterPreviewDestination(destination: CameraGlFanoutDestination) {
        controller.unregisterPreviewDestination(destination)
    }

    fun snapshot(): CameraGlFanoutRuntimeSnapshot {
        return CameraGlFanoutRuntimeSnapshot(
            configured = configured,
            controller = controller.snapshot()
        )
    }

    fun isConfigured(): Boolean = configured
    fun isRunning(): Boolean = controller.snapshot().running
}

data class CameraGlFanoutRuntimeSnapshot(
    val configured: Boolean,
    val controller: CameraGlFanoutControllerSnapshot
)
