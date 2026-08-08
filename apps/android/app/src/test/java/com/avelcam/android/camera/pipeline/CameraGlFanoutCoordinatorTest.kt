package com.avelcam.android.camera.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class CountingDestination(
    override val spec: CameraGlFanoutOutputSpec,
) : CameraGlFanoutDestination {
    var rendered = 0
    var released = 0
    override fun render(frame: CameraGlFanoutFrame): CameraGlFanoutRenderResult {
        rendered++
        return CameraGlFanoutRenderResult(spec.role, rendered = true)
    }

    override fun release() {
        released++
    }
}

class CameraGlFanoutCoordinatorTest {
    private val config = CameraGlFanoutCoordinatorConfig(
        pipelineConfig = CameraEncoderPipelineConfig(
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
    )

    private val metadata = CameraFrameMetadata(
        sourceTimestampNs = 100L,
        mappedTimestampNs = 100L,
        rotationDegrees = 0,
        isFrontCamera = false,
        surfaceTextureTransformMatrix = FloatArray(16) { if (it % 5 == 0) 1f else 0f },
    )

    @Test
    fun coordinatorConfiguresAndRendersOnAllDestinations() {
        val coordinator = CameraGlFanoutCoordinator()
        val preview = CountingDestination(
            spec = CameraGlFanoutOutputSpec(CameraGlFanoutOutputRole.PREVIEW, 1080, 1920)
        )
        val encoder = CountingDestination(
            spec = CameraGlFanoutOutputSpec(CameraGlFanoutOutputRole.ENCODER, 1280, 720)
        )

        coordinator.configure(config)
        coordinator.registerDestination(preview)
        coordinator.registerDestination(encoder)
        coordinator.start()
        coordinator.onCameraFrame(1920, 1080, metadata)

        val summary = coordinator.snapshot()
        assertTrue(summary.configured)
        assertTrue(summary.running)
        assertEquals(2, summary.destinationsRegistered)
        assertEquals(1L, summary.pipelineSummary.framesSeen)
        assertEquals(1L, summary.pipelineSummary.previewFramesRendered)
        assertEquals(1L, summary.pipelineSummary.encoderFramesRendered)
    }

    @Test
    fun coordinatorRequiresConfigurationBeforeStart() {
        val coordinator = CameraGlFanoutCoordinator()
        try {
            coordinator.start()
            assertFalse("Expected exception", true)
        } catch (expected: IllegalStateException) {
            assertEquals("Coordinator is not configured.", expected.message)
        }
    }

    @Test
    fun coordinatorStopsWithoutFrames() {
        val coordinator = CameraGlFanoutCoordinator()
        coordinator.configure(config)
        coordinator.start()
        coordinator.stop()

        val summary = coordinator.snapshot()
        assertFalse(summary.running)
    }

    @Test
    fun coordinatorDropsAndReroutesRelease() {
        val coordinator = CameraGlFanoutCoordinator()
        val destination = CountingDestination(
            spec = CameraGlFanoutOutputSpec(CameraGlFanoutOutputRole.PREVIEW, 1080, 1920)
        )

        coordinator.configure(config)
        coordinator.registerDestination(destination)
        coordinator.start()
        coordinator.release()
        coordinator.release()

        assertEquals(1, destination.released)
        assertFalse(coordinator.snapshot().running)
    }
}

