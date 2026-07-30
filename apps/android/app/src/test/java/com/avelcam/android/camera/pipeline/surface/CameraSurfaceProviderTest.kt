package com.avelcam.android.camera.pipeline.surface

import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface
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

        val request = FakeCameraSurfaceRequest(Size(640, 360))
        provider.handleSurfaceRequest(request)

        request.emitResult(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)

        assertEquals(1, fakeSurface.releaseCount)
        assertEquals(
            listOf(
                CameraSurfaceRequestLifecycleState.CREATING_SURFACE,
                CameraSurfaceRequestLifecycleState.PROVIDED,
                CameraSurfaceRequestLifecycleState.COMPLETED,
                CameraSurfaceRequestLifecycleState.RELEASED
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

        val request = FakeCameraSurfaceRequest(Size(320, 240))
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

        val request = FakeCameraSurfaceRequest(Size(320, 240))
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

        val first = FakeCameraSurfaceRequest(Size(640, 360))
        val second = FakeCameraSurfaceRequest(Size(640, 360))

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
        val request = FakeCameraSurfaceRequest(Size(640, 360))

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

        val request = FakeCameraSurfaceRequest(Size(640, 360))
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
    override val resolution: Size
) : CameraSurfaceRequest {
    var transformationListener: ((CameraSurfaceTransformationInfo) -> Unit)? = null
    var resultConsumer: ((CameraSurfaceRequestResultCode) -> Unit)? = null

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
        listener // no-op path for compatibility
    }

    override fun willNotProvideSurface() {
        willNotProvideCalled = true
    }

    override fun provideSurface(
        surface: Surface,
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
}

private data class FakeTransformationInfo(
    override val rotationDegrees: Int
) : CameraSurfaceTransformationInfo {
    override val cropRect: Rect = Rect(0, 0, 100, 100)
    override fun hasCameraTransform(): Boolean = false
}

private class FakeCameraInputSurfaceFactory(
    private vararg val surfaces: CameraSurfaceOwnedSurface
) : CameraInputSurfaceFactory {
    private var index = 0
    var createCount = 0

    override fun create(resolution: Size): CameraSurfaceOwnedSurface {
        return if (index < surfaces.size) {
            createCount += 1
            surfaces[index++]
        } else {
            throw CameraInputSurfaceFailure.InvalidResolution(resolution.width, resolution.height)
        }
    }
}

private class FakeOwnedSurface : CameraSurfaceOwnedSurface {
    override val surface: Surface by lazy { Surface(SurfaceTexture(0)) }
    var releaseCount = 0

    override fun release() {
        releaseCount += 1
    }
}
