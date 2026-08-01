package com.avelcam.android.camera.pipeline

data class CameraGlFanoutCoordinatorConfig(
    val pipelineConfig: CameraEncoderPipelineConfig,
    val frontCameraPreviewMirrored: Boolean = false,
    val frontCameraEncoderMirrored: Boolean = false,
)

class CameraGlFanoutCoordinator(
    private val timestampMapper: CameraFrameTimestampMapper = CameraFrameTimestampMapper()
) {
    private val pipeline = CameraGlFanoutPipeline()
    private var configured = false
    private var plan: CameraGlFanoutPlan? = null
    private var running = false
    private var destinationsRegistered = 0

    fun configure(config: CameraGlFanoutCoordinatorConfig) {
        if (configured) {
            return
        }
        plan = config.pipelineConfig.toGlFanoutOutputSpecs()
        configured = true
    }

    fun registerDestination(destination: CameraGlFanoutDestination) {
        ensureConfigured()
        pipeline.registerDestination(destination)
        destinationsRegistered++
    }

    fun unregisterDestination(destination: CameraGlFanoutDestination) {
        pipeline.unregisterDestination(destination)
        destinationsRegistered = maxOf(0, destinationsRegistered - 1)
    }

    fun start() {
        ensureConfigured()
        if (running) {
            return
        }
        pipeline.start()
        running = true
    }

    fun stop() {
        pipeline.stop()
        running = false
    }

    fun release() {
        pipeline.release()
        running = false
    }

    fun onCameraFrame(
        sourceWidth: Int,
        sourceHeight: Int,
        metadata: CameraFrameMetadata
    ) {
        ensureConfigured()
        ensureRunning()

        val mappedPresentationTimestamp = timestampMapper.mapTimestamp(metadata.sourceTimestampNs)
        val frame = CameraGlFanoutFrame(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            transform = metadata.copySurfaceTextureTransformMatrix(),
            sourceTimestampNs = metadata.sourceTimestampNs,
            presentationTimestampNs = mappedPresentationTimestamp,
        )
        pipeline.renderFrame(frame)
    }

    fun snapshot(): CameraGlFanoutCoordinatorSnapshot {
        return CameraGlFanoutCoordinatorSnapshot(
            configured = configured,
            running = running,
            destinationsRegistered = destinationsRegistered,
            planOutputs = plan?.outputs.orEmpty(),
            pipelineSummary = pipeline.snapshot(),
            timestampSnapshot = timestampMapper.snapshot()
        )
    }

    fun getPlan(): CameraGlFanoutPlan {
        return plan ?: throw IllegalStateException("Coordinator is not configured.")
    }

    private fun ensureConfigured() {
        if (!configured) {
            throw IllegalStateException("Coordinator is not configured.")
        }
    }

    private fun ensureRunning() {
        if (!running) {
            throw IllegalStateException("Coordinator is not running.")
        }
    }
}

data class CameraGlFanoutCoordinatorSnapshot(
    val configured: Boolean,
    val running: Boolean,
    val destinationsRegistered: Int,
    val planOutputs: List<CameraGlFanoutOutputSpec>,
    val pipelineSummary: CameraGlFanoutRenderSummary,
    val timestampSnapshot: CameraTimestampSnapshot,
)

