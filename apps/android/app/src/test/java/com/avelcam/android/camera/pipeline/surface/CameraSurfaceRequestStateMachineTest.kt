package com.avelcam.android.camera.pipeline.surface

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraSurfaceRequestStateMachineTest {

    @Test
    fun firstRequestAccepted() {
        val machine = CameraSurfaceRequestStateMachine(
            requestId = 1L,
            requestedResolution = CameraSurfaceRequestResolution(1280, 720)
        )

        assertEquals(CameraSurfaceRequestLifecycleState.RECEIVED, machine.snapshot().state)
        assertTrue(machine.transitionToCreatingSurface())
        assertEquals(CameraSurfaceRequestLifecycleState.CREATING_SURFACE, machine.snapshot().state)
    }

    @Test
    fun requestTransitionsToProvided() {
        val machine = CameraSurfaceRequestStateMachine(
            requestId = 1L,
            requestedResolution = CameraSurfaceRequestResolution(1280, 720)
        )

        machine.transitionToCreatingSurface()
        assertTrue(machine.transitionToProvided())
        assertEquals(CameraSurfaceRequestLifecycleState.PROVIDED, machine.snapshot().state)
    }

    @Test
    fun cancellationIsNotFatal() {
        val machine = CameraSurfaceRequestStateMachine(
            requestId = 1L,
            requestedResolution = CameraSurfaceRequestResolution(1280, 720)
        )

        machine.transitionToCreatingSurface()
        assertTrue(machine.transitionToCancelled("camerax cancelled"))
        assertEquals(CameraSurfaceRequestLifecycleState.CANCELLED, machine.snapshot().state)
        assertEquals(CameraSurfaceRequestLifecycleState.CANCELLED, machine.snapshot().state)
    }

    @Test
    fun completionAfterCancellationIgnored() {
        val machine = CameraSurfaceRequestStateMachine(
            requestId = 1L,
            requestedResolution = CameraSurfaceRequestResolution(1280, 720)
        )

        machine.transitionToCreatingSurface()
        machine.transitionToCancelled("cancelled")
        assertFalse(machine.transitionToResult(
            resultCode = SurfaceRequestResultCodes.INVALID,
            result = CameraSurfaceRequestResult.SurfaceUsedSuccessfully
        ))
        assertEquals(CameraSurfaceRequestLifecycleState.CANCELLED, machine.snapshot().state)
    }

    @Test
    fun invalidSurfaceTransitionIsFailure() {
        val machine = CameraSurfaceRequestStateMachine(
            requestId = 1L,
            requestedResolution = CameraSurfaceRequestResolution(1280, 720)
        )

        machine.transitionToCreatingSurface()
        machine.transitionToProvided()
        assertTrue(machine.transitionToResult(
            resultCode = SurfaceRequestResultCodes.INVALID,
            result = CameraSurfaceRequestResult.InvalidSurface
        ))
        assertEquals(CameraSurfaceRequestLifecycleState.FAILED, machine.snapshot().state)
    }

    @Test
    fun transitionToResultCanCompleteAndRelease() {
        val machine = CameraSurfaceRequestStateMachine(
            requestId = 1L,
            requestedResolution = CameraSurfaceRequestResolution(1280, 720)
        )
        machine.transitionToCreatingSurface()
        machine.transitionToProvided()
        assertTrue(machine.transitionToResult(
            resultCode = SurfaceRequestResultCodes.SUCCESS,
            result = CameraSurfaceRequestResult.SurfaceUsedSuccessfully
        ))
        assertEquals(CameraSurfaceRequestLifecycleState.COMPLETED, machine.snapshot().state)
        assertTrue(machine.markRelease())
        assertEquals(CameraSurfaceRequestLifecycleState.RELEASED, machine.snapshot().state)
    }

    @Test
    fun transformationStoredAndIgnoredAfterRelease() {
        val machine = CameraSurfaceRequestStateMachine(
            requestId = 1L,
            requestedResolution = CameraSurfaceRequestResolution(1280, 720)
        )

        val first = CameraSurfaceTransformation(
            rotationDegrees = 0,
            cropRect = CameraSurfaceCropRect(0, 0, 10, 10),
            hasCameraTransform = true
        )
        val second = CameraSurfaceTransformation(
            rotationDegrees = 90,
            cropRect = CameraSurfaceCropRect(1, 1, 11, 11),
            hasCameraTransform = false
        )

        machine.transitionToCreatingSurface()
        machine.transitionToProvided()
        machine.updateTransformation(first)
        assertEquals(first, machine.snapshot().transformation)
        machine.markRelease()
        machine.updateTransformation(second)
        assertEquals(first, machine.snapshot().transformation)
    }

    @Test
    fun duplicateTransitionsAreIgnored() {
        val machine = CameraSurfaceRequestStateMachine(
            requestId = 1L,
            requestedResolution = CameraSurfaceRequestResolution(1280, 720)
        )
        machine.transitionToCreatingSurface()
        machine.transitionToProvided()
        machine.transitionToProvided()
        assertEquals(CameraSurfaceRequestLifecycleState.PROVIDED, machine.snapshot().state)
    }

    @Test
    fun idMonotonicity() {
        val first = CameraSurfaceRequestIdSequence.next()
        val second = CameraSurfaceRequestIdSequence.next()
        assertTrue(second > first)
    }
}

private object SurfaceRequestResultCodes {
    const val SUCCESS = 0
    const val INVALID = 2
}
