package com.avelcam.android.camera.pipeline

import com.avelcam.android.encoder.EncoderConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class RuntimeTestOutputManager : CameraEncoderOutputManagerContract {
    private var running = false
    private var startAttempts = 0L

    override fun start(): Result<Unit> {
        startAttempts++
        running = true
        return Result.success(Unit)
    }

    override fun stop(): Result<Unit> {
        running = false
        return Result.success(Unit)
    }

    override fun snapshot(): CameraEncoderOutputManagerState = CameraEncoderOutputManagerState(
        isRunning = running,
        startAttempts = startAttempts,
        selectedCodecName = null,
        startResult = null,
    )

    override fun release() {
        running = false
    }
}

class CameraGlFanoutRuntimeTest {
    private val defaultRuntimeConfig = CameraGlFanoutRuntimeConfig(
        cameraWidth = 1920,
        cameraHeight = 1080,
        previewWidth = 1080,
        previewHeight = 1920,
        encoderWidth = 1280,
        encoderHeight = 720,
        encoderConfig = EncoderConfig(
            width = 1280,
            height = 720
        ),
        cropMode = CameraPipelineCropMode.FIT
    )

    private val metadata = CameraFrameMetadata(
        sourceTimestampNs = 100L,
        mappedTimestampNs = 100L,
        rotationDegrees = 0,
        isFrontCamera = false,
        surfaceTextureTransformMatrix = FloatArray(16) { if (it % 5 == 0) 1f else 0f }
    )

    private fun createRuntime(): CameraGlFanoutRuntime = CameraGlFanoutRuntime(
        CameraGlFanoutController(
            outputManagerFactory = { _, _, _, _ -> RuntimeTestOutputManager() }
        )
    )

    @Test
    fun startRequiresConfiguration() {
        val runtime = createRuntime()

        val result = runtime.start()

        assertFalse(result.isSuccess)
        assertFalse(runtime.isConfigured())
    }

    @Test
    fun configureStartStopLifecycle() {
        val runtime = createRuntime()
        runtime.configure(defaultRuntimeConfig)
        runtime.onCameraFrame(1920, 1080, metadata)

        val started = runtime.start()
        assertTrue(started.isSuccess)

        val startedSnapshot = runtime.snapshot()
        assertTrue(startedSnapshot.configured)
        assertTrue(startedSnapshot.controller.running)

        runtime.stop()
        val stoppedSnapshot = runtime.snapshot()
        assertFalse(stoppedSnapshot.controller.running)
    }

    @Test
    fun registerPreviewDestinationRequiresConfiguration() {
        val runtime = createRuntime()
        val destination = object : CameraGlFanoutDestination {
            override val spec: CameraGlFanoutOutputSpec = CameraGlFanoutOutputSpec(
                role = CameraGlFanoutOutputRole.PREVIEW,
                width = 640,
                height = 480
            )

            override fun render(frame: CameraGlFanoutFrame): CameraGlFanoutRenderResult {
                return CameraGlFanoutRenderResult(role = spec.role, rendered = true)
            }

            override fun release() {}
        }

        var thrown = false
        try {
            runtime.registerPreviewDestination(destination)
        } catch (_: IllegalStateException) {
            thrown = true
        }

        assertTrue(thrown)

        runtime.configure(defaultRuntimeConfig)
        runtime.registerPreviewDestination(destination)
        runtime.start()
        val snapshot = runtime.snapshot()
        assertEquals(1, snapshot.controller.coordinator.destinationsRegistered)

        runtime.unregisterPreviewDestination(destination)
        runtime.stop()
        val afterSnapshot = runtime.snapshot()
        assertEquals(0, afterSnapshot.controller.coordinator.destinationsRegistered)
    }

    @Test
    fun releaseResetsConfiguration() {
        val runtime = createRuntime()
        runtime.configure(defaultRuntimeConfig)
        runtime.start()
        runtime.release()

        assertFalse(runtime.isConfigured())
        assertFalse(runtime.isRunning())
        assertFalse(runtime.snapshot().controller.running)
    }
}
