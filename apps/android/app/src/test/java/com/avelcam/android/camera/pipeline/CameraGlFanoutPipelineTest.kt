package com.avelcam.android.camera.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

private class TestDestination(
    override val spec: CameraGlFanoutOutputSpec,
    private val onRender: (CameraGlFanoutFrame) -> CameraGlFanoutRenderResult
) : CameraGlFanoutDestination {
    var releaseCalls = 0
    var framesRendered = 0

    override fun render(frame: CameraGlFanoutFrame): CameraGlFanoutRenderResult {
        framesRendered++
        return onRender(frame)
    }

    override fun release() {
        releaseCalls++
    }
}

class CameraGlFanoutPipelineTest {
    private val frame = CameraGlFanoutFrame(
        sourceWidth = 1920,
        sourceHeight = 1080,
        transform = FloatArray(16) { if (it % 5 == 0) 1f else 0f },
        sourceTimestampNs = 10L,
        presentationTimestampNs = 10L,
    )

    @Test
    fun startStopRenderAndSummary() {
        val pipeline = CameraGlFanoutPipeline()
        val preview = TestDestination(
            spec = CameraGlFanoutOutputSpec(
                role = CameraGlFanoutOutputRole.PREVIEW,
                width = 1080,
                height = 1920
            ),
            onRender = { CameraGlFanoutRenderResult(CameraGlFanoutOutputRole.PREVIEW, rendered = true) }
        )
        val encoder = TestDestination(
            spec = CameraGlFanoutOutputSpec(
                role = CameraGlFanoutOutputRole.ENCODER,
                width = 1280,
                height = 720
            ),
            onRender = { CameraGlFanoutRenderResult(CameraGlFanoutOutputRole.ENCODER, rendered = true) }
        )

        pipeline.registerDestination(preview)
        pipeline.registerDestination(encoder)
        pipeline.start()

        pipeline.renderFrame(frame)
        pipeline.renderFrame(frame)

        pipeline.stop()

        val summary = pipeline.snapshot()
        assertEquals(2L, summary.framesSeen)
        assertEquals(2L, summary.previewFramesRendered)
        assertEquals(2L, summary.encoderFramesRendered)
        assertEquals(0L, summary.previewFailures)
        assertEquals(0L, summary.encoderFailures)
        assertTrue(summary.renderedSuccessfully)

        pipeline.start()
        assertEquals(2L, pipeline.snapshot().framesSeen)
    }

    @Test
    fun unregisterStopsDestinationRender() {
        val pipeline = CameraGlFanoutPipeline()
        val preview = TestDestination(
            spec = CameraGlFanoutOutputSpec(CameraGlFanoutOutputRole.PREVIEW, 1080, 1920),
            onRender = { CameraGlFanoutRenderResult(CameraGlFanoutOutputRole.PREVIEW, rendered = true) }
        )
        pipeline.registerDestination(preview)
        pipeline.start()
        pipeline.renderFrame(frame)
        pipeline.unregisterDestination(preview)
        pipeline.renderFrame(frame)

        assertEquals(1, preview.framesRendered)
    }

    @Test
    fun renderFailureIncrementsCounterAndKeepsRunning() {
        val pipeline = CameraGlFanoutPipeline()
        val preview = TestDestination(
            spec = CameraGlFanoutOutputSpec(CameraGlFanoutOutputRole.PREVIEW, 1080, 1920),
            onRender = { throw IllegalStateException("preview failure") }
        )
        val encoder = TestDestination(
            spec = CameraGlFanoutOutputSpec(CameraGlFanoutOutputRole.ENCODER, 1280, 720),
            onRender = { CameraGlFanoutRenderResult(CameraGlFanoutOutputRole.ENCODER, rendered = true) }
        )
        pipeline.registerDestination(preview)
        pipeline.registerDestination(encoder)
        pipeline.start()
        pipeline.renderFrame(frame)

        val summary = pipeline.snapshot()
        assertEquals(1L, summary.previewFailures)
        assertEquals(0L, summary.encoderFailures)
        assertFalse(summary.renderedSuccessfully)
        assertEquals("preview failure", summary.lastFrameError)
    }

    @Test
    fun releaseStopsAndClears() {
        val pipeline = CameraGlFanoutPipeline()
        val preview = TestDestination(
            spec = CameraGlFanoutOutputSpec(CameraGlFanoutOutputRole.PREVIEW, 1080, 1920),
            onRender = { CameraGlFanoutRenderResult(CameraGlFanoutOutputRole.PREVIEW, rendered = true) }
        )
        pipeline.registerDestination(preview)
        pipeline.start()
        pipeline.release()
        pipeline.release()

        assertEquals(1, preview.releaseCalls)
        assertEquals(0, preview.framesRendered)
        assertEquals(0L, pipeline.snapshot().framesSeen)
    }

    @Test
    fun renderWithoutStartThrows() {
        val pipeline = CameraGlFanoutPipeline()
        pipeline.registerDestination(
            TestDestination(
                spec = CameraGlFanoutOutputSpec(CameraGlFanoutOutputRole.ENCODER, 1280, 720),
                onRender = { CameraGlFanoutRenderResult(CameraGlFanoutOutputRole.ENCODER, rendered = true) }
            )
        )
        try {
            pipeline.renderFrame(frame)
            fail("Expected IllegalStateException")
        } catch (expected: IllegalStateException) {
            assertEquals("Fanout pipeline is not started.", expected.message)
        }
    }
}
