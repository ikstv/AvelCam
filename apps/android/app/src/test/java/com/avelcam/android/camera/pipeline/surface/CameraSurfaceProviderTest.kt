package com.avelcam.android.camera.pipeline.surface

import androidx.camera.core.SurfaceRequest
import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraSurfaceProviderTest {

    @Test
    fun normalCompletionReleasesSurfaceOnce() {
        val fakeSurface = FakeOwnedSurface()
        val factory = FakeCameraInputSurfaceFactory(fakeSurface)
        val stateSnapshots = mutableListOf<CameraSurfaceRequestLifecycleState>()
        val resultSnapshots = mutableListOf<CameraSurfaceRequestResult>()

        val provider = CameraSurfaceProvider(
            callbackExecutor = ImmediateExecutor,
            surfaceFactory = factory,
            requestStateObserver = { stateSnapshots += it.state },
            requestResultObserver = { _, result, _, _ -> resultSnapshots += result }
        )

        val request = FakeCameraSurfaceRequest(640, 360)
        provider.handleSurfaceRequest(request)

        request.emitResult(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)

        assertEquals(1, fakeSurface.releaseCount)
        assertEquals(
            listOf(
                CameraSurfaceRequestLifecycleState.CREATING_SURFACE,
                CameraSurfaceRequestLifecycleState.PROVIDED,
                CameraSurfaceRequestLifecycleState.COMPLETED,
                CameraSurfaceRequestLifecycleState.RELEASED,
            ),
            stateSnapshots
        )
        assertEquals(listOf(CameraSurfaceRequestResult.SurfaceUsedSuccessfully), resultSnapshots)
    }

    @Test
    fun shutdownAfterProvideReleasesInResultCallbackOnly() {
        val fakeSurface = FakeOwnedSurface()
        val factory = FakeCameraInputSurfaceFactory(fakeSurface)
        val resultSnapshots = mutableListOf<CameraSurfaceRequestResult>()

        val provider = CameraSurfaceProvider(
            callbackExecutor = ImmediateExecutor,
            surfaceFactory = factory,
            requestResultObserver = { _, result, _, _ -> resultSnapshots += result }
        )

        val request = FakeCameraSurfaceRequest(320, 240)
        provider.handleSurfaceRequest(request)
        provider.release()

        assertTrue(request.invalidateCalled)
        assertEquals(0, fakeSurface.releaseCount)

        request.emitResult(SurfaceRequest.Result.RESULT_REQUEST_CANCELLED)

        assertEquals(1, fakeSurface.releaseCount)
        assertEquals(listOf(CameraSurfaceRequestResult.RequestCancelled), resultSnapshots)
    }

    @Test
    fun cancellationResultAndDuplicateResultAreHandledSafely() {
        val fakeSurface = FakeOwnedSurface()
        val factory = FakeCameraInputSurfaceFactory(fakeSurface)

        val provider = CameraSurfaceProvider(
            callbackExecutor = ImmediateExecutor,
            surfaceFactory = factory
        )

        val request = FakeCameraSurfaceRequest(320, 240)
        provider.handleSurfaceRequest(request)

        request.emitResult(SurfaceRequest.Result.RESULT_REQUEST_CANCELLED)
        request.emitResult(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)

        assertEquals(1, fakeSurface.releaseCount)
    }

    @Test
    fun overlappingRequestIsRejectedWithoutReplacingActive() {
        val firstSurface = FakeOwnedSurface()
        val secondSurface = FakeOwnedSurface()
        val factory = FakeCameraInputSurfaceFactory(firstSurface, secondSurface)
        val requestResultCount = mutableListOf<CameraSurfaceRequestResult>()

        val provider = CameraSurfaceProvider(
            callbackExecutor = ImmediateExecutor,
            surfaceFactory = factory,
            requestResultObserver = { _, result, _, _ -> requestResultCount += result }
        )

        val first = FakeCameraSurfaceRequest(640, 360)
        val second = FakeCameraSurfaceRequest(640, 360)

        provider.handleSurfaceRequest(first)
        provider.handleSurfaceRequest(second)

        assertTrue(first.provideCalled)
        assertFalse(second.provideCalled)
        assertEquals(1, factory.createCount)
        assertEquals(1, requestResultCount.count { it is CameraSurfaceRequestResult.WillNotProvideSurface })

        first.emitResult(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)
        assertEquals(1, firstSurface.releaseCount)
    }

    @Test
    fun repeatedShutdownIsIdempotent() {
        val fakeSurface = FakeOwnedSurface()
        val factory = FakeCameraInputSurfaceFactory(fakeSurface)
        val request = FakeCameraSurfaceRequest(640, 360)

        val provider = CameraSurfaceProvider(
            callbackExecutor = ImmediateExecutor,
            surfaceFactory = factory
        )

        provider.handleSurfaceRequest(request)
        provider.release()
        request.emitResult(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)

        provider.release()

        assertEquals(1, fakeSurface.releaseCount)
    }

    @Test
    fun transformationAfterCompletionIsIgnored() {
        val fakeSurface = FakeOwnedSurface()
        val factory = FakeCameraInputSurfaceFactory(fakeSurface)
        val seenTransformations = mutableListOf<CameraSurfaceTransformation>()

        val provider = CameraSurfaceProvider(
            callbackExecutor = ImmediateExecutor,
            surfaceFactory = factory,
            transformationObserver = { _, transformation -> seenTransformations += transformation },
            requestStateObserver = { }
        )

        val request = FakeCameraSurfaceRequest(640, 360)
        provider.handleSurfaceRequest(request)

        request.emitTransformation(0)
        request.emitResult(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)
        request.emitTransformation(90)

        assertEquals(1, seenTransformations.size)
    }
}

private object ImmediateExecutor : Executor {
    override fun execute(command: Runnable) = command.run()
}

private class FakeCameraSurfaceRequest(
    override val requestedWidth: Int,
    override val requestedHeight: Int
) : CameraSurfaceRequest {
    var transformationListener: ((CameraSurfaceTransformationInfo) -> Unit)? = null
    var resultConsumer: ((CameraSurfaceRequestResultCode) -> Unit)? = null
    var cancellationListener: (() -> Unit)? = null

    var provideCalled = false
    var invalidateCalled = false
    var willNotProvideCalled = false

    override fun setTransformationInfoListener(
        executor: Executor,
        listener: (CameraSurfaceTransformationInfo) -> Unit
    ) {
        transformationListener = {
            executor.execute { listener(it) }
        }
    }

    override fun addRequestCancellationListener(executor: Executor, listener: () -> Unit) {
        cancellationListener = { executor.execute(listener) }
    }

    override fun willNotProvideSurface() {
        willNotProvideCalled = true
    }

    override fun provideSurface(
        surface: CameraSurfaceRequestSurface,
        executor: Executor,
        listener: (CameraSurfaceRequestResultCode) -> Unit
    ) {
        provideCalled = true
        resultConsumer = {
            executor.execute { listener(it) }
        }

    }

    override fun invalidate() {
        invalidateCalled = true
    }

    fun emitTransformation(rotation: Int) {
        transformationListener?.invoke(FakeTransformationInfo(rotation))
    }

    fun emitResult(resultCode: Int) {
        resultConsumer?.let { callback -> callback(CameraSurfaceRequestResultCode(resultCode)) }
    }

    fun emitCancellation() {
        cancellationListener?.invoke()
    }
}

private data class FakeTransformationInfo(
    override val rotationDegrees: Int,
    override val cropLeft: Int = 0,
    override val cropTop: Int = 0,
    override val cropRight: Int = 100,
    override val cropBottom: Int = 100,
) : CameraSurfaceTransformationInfo

private class FakeCameraInputSurfaceFactory(
    private vararg val surfaces: CameraSurfaceOwnedSurface
) : CameraInputSurfaceFactory {
    private var index = 0
    var createCount = 0

    override fun create(resolution: CameraSurfaceRequestResolution): CameraSurfaceOwnedSurface {
        return if (index < surfaces.size) {
            createCount += 1
            surfaces[index++]
        } else {
            throw CameraInputSurfaceFailure.InvalidResolution(resolution.width, resolution.height)
        }
    }
}

private class FakeCameraSurfaceRequestToken : CameraSurfaceRequestSurface {
    var releaseCount = 0

    override fun resolveSurface() = throw IllegalStateException("Unit tests should not resolve surfaces.")

    override fun release() {
        releaseCount++
    }
}

private class FakeOwnedSurface : CameraSurfaceOwnedSurface {
    override val surface: CameraSurfaceRequestSurface by lazy { FakeCameraSurfaceRequestToken() }
    val token get() = surface as FakeCameraSurfaceRequestToken
    var releaseCount: Int
        get() = token.releaseCount
        set(value) {
            token.releaseCount = value
        }

    override fun release() {
        surface.release()
    }
}
