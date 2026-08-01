package com.avelcam.android.camera.pipeline

import com.avelcam.android.encoder.EncoderConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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

    @Test
    fun startRequiresConfiguration() {
        val runtime = CameraGlFanoutRuntime()

        val result = runtime.start()

        assertFalse(result.isSuccess)
        assertFalse(runtime.isConfigured())
    }

    @Test
    fun configureStartStopLifecycle() {
        val runtime = CameraGlFanoutRuntime()
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
    fun releaseResetsConfiguration() {
        val runtime = CameraGlFanoutRuntime()
        runtime.configure(defaultRuntimeConfig)
        runtime.start()
        runtime.release()

        assertFalse(runtime.isConfigured())
        assertFalse(runtime.isRunning())
        assertFalse(runtime.snapshot().controller.running)
    }
}
