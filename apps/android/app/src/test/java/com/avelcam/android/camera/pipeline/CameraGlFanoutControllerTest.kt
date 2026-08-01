package com.avelcam.android.camera.pipeline

import com.avelcam.android.encoder.EncoderConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeEncoderOutputManager : CameraEncoderOutputManagerContract {
    var startCalls = 0
    var stopCalls = 0
    var released = 0
    var startResult: Result<Unit> = Result.success(Unit)
    var snapshotState = CameraEncoderOutputManagerState(
        isRunning = false,
        startAttempts = 0L,
        selectedCodecName = null,
        startResult = null
    )

    override fun start(): Result<Unit> {
        startCalls++
        return startResult.also { result ->
            snapshotState = snapshotState.copy(
                isRunning = result.isSuccess,
                startAttempts = snapshotState.startAttempts + 1L
            )
        }
    }

    override fun stop(): Result<Unit> {
        stopCalls++
        snapshotState = snapshotState.copy(isRunning = false)
        return Result.success(Unit)
    }

    override fun snapshot(): CameraEncoderOutputManagerState = snapshotState

    override fun release() {
        released++
    }
}

class CameraGlFanoutControllerTest {
    private val pipelineConfig = CameraEncoderPipelineConfig(
        cameraWidth = 1920,
        cameraHeight = 1080,
        previewWidth = 1080,
        previewHeight = 1920,
        encoderWidth = 1280,
        encoderHeight = 720,
        frontCameraPreviewMirrored = false,
        frontCameraEncoderMirrored = false,
        cropMode = CameraPipelineCropMode.FIT
    )

    private val controllerConfig = CameraGlFanoutControllerConfig(
        pipelineConfig = pipelineConfig,
        encoderConfig = EncoderConfig()
    )

    private val metadata = CameraFrameMetadata(
        sourceTimestampNs = 10L,
        mappedTimestampNs = 0L,
        rotationDegrees = 0,
        isFrontCamera = false,
        surfaceTextureTransformMatrix = FloatArray(16) { if (it % 5 == 0) 1f else 0f },
    )

    @Test
    fun frameIgnoredBeforeStart() {
        var frameRenderCalls = 0
        val controller = CameraGlFanoutController(
            coordinator = CameraGlFanoutCoordinator(),
            outputManagerFactory = { _, _, _, _ ->
                FakeEncoderOutputManager().also { fake ->
                    fake.snapshotState = fake.snapshotState.copy(isRunning = false)
                }.also { _ -> }
            }
        )
        controller.configure(controllerConfig)
        controller.onCameraFrame(1920, 1080, metadata)

        assertTrue(controller.snapshot().coordinator.pipelineSummary.framesSeen == 0L)
    }

    @Test
    fun startRequiresConfiguration() {
        val controller = CameraGlFanoutController()
        try {
            controller.start()
            assertFalse("expected IllegalStateException", true)
        } catch (e: IllegalStateException) {
            assertEquals("Controller not configured.", e.message)
        }
    }

    @Test
    fun startStopControllerUpdatesState() {
        val fakeOutputManager = FakeEncoderOutputManager()
        val coordinator = CameraGlFanoutCoordinator()
        val controller = CameraGlFanoutController(
            coordinator = coordinator,
            outputManagerFactory = { _, _, _, _ -> fakeOutputManager }
        )
        controller.configure(controllerConfig)

        val startResult = controller.start()
        assertTrue(startResult.isSuccess)
        assertTrue(coordinator.snapshot().running)
        assertEquals(1, fakeOutputManager.startCalls)

        controller.stop()
        assertFalse(coordinator.snapshot().running)
        assertEquals(1, fakeOutputManager.stopCalls)
        assertEquals(1, fakeOutputManager.startCalls)
    }

    @Test
    fun releaseStopsEverything() {
        val fakeOutputManager = FakeEncoderOutputManager()
        val controller = CameraGlFanoutController(
            outputManagerFactory = { _, _, _, _ -> fakeOutputManager }
        )
        controller.configure(controllerConfig)
        controller.start()

        controller.release()

        assertEquals(1, fakeOutputManager.released)
        val snapshot = controller.snapshot()
        assertFalse(snapshot.configured)
    }
}
