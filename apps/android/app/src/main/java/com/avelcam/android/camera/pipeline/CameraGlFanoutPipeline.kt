package com.avelcam.android.camera.pipeline

import java.util.concurrent.CopyOnWriteArrayList

data class CameraGlFanoutFrame(
    val sourceWidth: Int,
    val sourceHeight: Int,
    val transform: FloatArray,
    val sourceTimestampNs: Long,
    val presentationTimestampNs: Long,
    val sourceTextureId: Int = 0,
) {
    init {
        require(sourceWidth > 0) { "sourceWidth must be > 0." }
        require(sourceHeight > 0) { "sourceHeight must be > 0." }
        require(transform.size == 16) { "transform must contain exactly 16 values." }
        require(sourceTextureId >= 0) { "sourceTextureId must be >= 0." }
    }

    val transformSnapshot: FloatArray = transform.copyOf()
}

enum class CameraGlFanoutOutputRole {
    PREVIEW,
    ENCODER
}

data class CameraGlFanoutOutputSpec(
    val role: CameraGlFanoutOutputRole,
    val width: Int,
    val height: Int,
) {
    init {
        require(width > 0) { "width must be > 0." }
        require(height > 0) { "height must be > 0." }
    }
}

data class CameraGlFanoutRenderResult(
    val role: CameraGlFanoutOutputRole,
    val rendered: Boolean,
    val isFatal: Boolean = false,
    val message: String? = null,
)

data class CameraGlFanoutRenderSummary(
    val framesSeen: Long,
    val previewFramesRendered: Long,
    val encoderFramesRendered: Long,
    val previewFailures: Long,
    val encoderFailures: Long,
    val lastFrameError: String?,
    val renderedSuccessfully: Boolean,
)

interface CameraGlFanoutDestination {
    val spec: CameraGlFanoutOutputSpec
    fun render(frame: CameraGlFanoutFrame): CameraGlFanoutRenderResult
    fun release()
}

class CameraGlFanoutPipeline {
    private val destinations = CopyOnWriteArrayList<CameraGlFanoutDestination>()
    private var started = false
    private var released = false
    private var framesSeen = 0L
    private var previewFramesRendered = 0L
    private var encoderFramesRendered = 0L
    private var previewFailures = 0L
    private var encoderFailures = 0L
    private var lastFrameError: String? = null

    fun start() {
        ensureNotReleased()
        started = true
    }

    fun stop() {
        if (released || !started) {
            return
        }
        started = false
    }

    fun registerDestination(destination: CameraGlFanoutDestination) {
        ensureNotReleased()
        destinations.addIfAbsent(destination)
    }

    fun unregisterDestination(destination: CameraGlFanoutDestination) {
        destinations.remove(destination)
    }

    fun renderFrame(frame: CameraGlFanoutFrame) {
        ensureStarted()

        val frameSnapshot = frame.copy(
            transform = frame.transformSnapshot,
            sourceTimestampNs = frame.sourceTimestampNs,
            presentationTimestampNs = frame.presentationTimestampNs,
        )
        framesSeen++
        var frameFailed = false

        destinations.forEach { destination ->
            val result = runCatching { destination.render(frameSnapshot) }
                .getOrElse { exception ->
                    lastFrameError = exception.message
                    frameFailed = true
                    CameraGlFanoutRenderResult(
                        role = destination.spec.role,
                        rendered = false,
                        isFatal = true,
                        message = exception.message,
                    )
                }

            when (result.role) {
                CameraGlFanoutOutputRole.PREVIEW -> {
                    if (!result.rendered || result.isFatal) {
                        previewFailures++
                        frameFailed = true
                    }
                    if (result.rendered) {
                        previewFramesRendered++
                    }
                }
                CameraGlFanoutOutputRole.ENCODER -> {
                    if (!result.rendered || result.isFatal) {
                        encoderFailures++
                        frameFailed = true
                    }
                    if (result.rendered) {
                        encoderFramesRendered++
                    }
                }
            }
        }

        if (frameFailed) {
            lastFrameError = lastFrameError ?: "Frame fan-out reported failures."
        }
    }

    fun release() {
        if (released) {
            return
        }
        released = true
        started = false
        destinations.forEach { it.release() }
        destinations.clear()
    }

    fun snapshot(): CameraGlFanoutRenderSummary {
        return CameraGlFanoutRenderSummary(
            framesSeen = framesSeen,
            previewFramesRendered = previewFramesRendered,
            encoderFramesRendered = encoderFramesRendered,
            previewFailures = previewFailures,
            encoderFailures = encoderFailures,
            lastFrameError = lastFrameError,
            renderedSuccessfully = !released && framesSeen > 0L && previewFailures == 0L && encoderFailures == 0L,
        )
    }

    private fun ensureStarted() {
        ensureNotReleased()
        if (!started) {
            throw IllegalStateException("Fanout pipeline is not started.")
        }
    }

    private fun ensureNotReleased() {
        if (released) {
            throw IllegalStateException("Fanout pipeline is released.")
        }
    }
}
