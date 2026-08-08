package com.avelcam.android.camera.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraEncoderPipelineStateMachineTest {

    @Test
    fun previewStartsFromIdle() {
        val machine = CameraEncoderPipelineStateMachine()

        val started = machine.transition(StartPreviewRequested)
        val startedPreview = machine.transition(PreviewStarted)

        assertEquals(CameraEncoderPipelineState.STARTING_PREVIEW, started.state)
        assertEquals(CameraEncoderPipelineState.PREVIEW_RUNNING, startedPreview.state)
    }

    @Test
    fun encoderStartsOnlyFromPreviewRunning() {
        val machine = CameraEncoderPipelineStateMachine()
        val illegal = machine.transition(StartEncoderRequested)

        assertEquals(CameraEncoderPipelineState.IDLE, illegal.state)
        assertFalse(illegal.changed)
        assertEquals("state_transition_denied", illegal.error)
    }

    @Test
    fun encoderStopReturnsToPreviewRunning() {
        val machine = CameraEncoderPipelineStateMachine()
        machine.transition(StartPreviewRequested)
        machine.transition(PreviewStarted)
        machine.transition(StartEncoderRequested)
        machine.transition(EncoderStarted)
        val stop = machine.transition(StopEncoderRequested)
        val stopped = machine.transition(StopEncoderCompleted)

        assertEquals(CameraEncoderPipelineState.STOPPING_ENCODER, stop.state)
        assertEquals(CameraEncoderPipelineState.PREVIEW_RUNNING, stopped.state)
    }

    @Test
    fun duplicatePreviewStartIsRejected() {
        val machine = CameraEncoderPipelineStateMachine()
        machine.transition(StartPreviewRequested)
        machine.transition(PreviewStarted)
        val duplicate = machine.transition(StartPreviewRequested)

        assertEquals(CameraEncoderPipelineState.PREVIEW_RUNNING, duplicate.state)
        assertFalse(duplicate.changed)
    }

    @Test
    fun duplicateEncoderStartIsRejected() {
        val machine = CameraEncoderPipelineStateMachine()
        machine.transition(StartPreviewRequested)
        machine.transition(PreviewStarted)
        machine.transition(StartEncoderRequested)
        machine.transition(EncoderStarted)
        val duplicate = machine.transition(StartEncoderRequested)

        assertEquals(CameraEncoderPipelineState.ENCODING, duplicate.state)
        assertFalse(duplicate.changed)
        assertEquals("state_transition_denied", duplicate.error)
    }

    @Test
    fun errorTransitionAndRelease() {
        val machine = CameraEncoderPipelineStateMachine()
        machine.transition(StartPreviewRequested)
        machine.transition(PreviewStarted)
        val failed = machine.transition(CameraEncoderPipelineEvent.ErrorOccurred("camera", "camera"))
        val released = machine.transition(ReleaseRequested)

        assertEquals(CameraEncoderPipelineState.ERROR, failed.state)
        assertEquals(CameraEncoderPipelineState.RELEASED, released.state)
    }

    @Test
    fun noTransitionAfterRelease() {
        val machine = CameraEncoderPipelineStateMachine()
        machine.transition(ReleaseRequested)
        val afterRelease = machine.transition(StartPreviewRequested)

        assertEquals(CameraEncoderPipelineState.RELEASED, afterRelease.state)
        assertFalse(afterRelease.changed)
    }
}

