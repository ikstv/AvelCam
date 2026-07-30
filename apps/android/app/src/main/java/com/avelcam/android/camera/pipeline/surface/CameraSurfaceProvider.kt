package com.avelcam.android.camera.pipeline.surface
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

internal class CameraSurfaceProvider(
    private val callbackExecutor: Executor,
    private val surfaceFactory: CameraInputSurfaceFactory,
    private val requestStateObserver: (CameraSurfaceRequestState) -> Unit = {},
    private val requestResultObserver: (
        requestId: Long,
        result: CameraSurfaceRequestResult,
        resultCode: Int,
        isFatal: Boolean
    ) -> Unit = { _, _, _, _ -> },
    private val transformationObserver: (Long, CameraSurfaceTransformation) -> Unit = { _, _ -> },
    private val requestAdapterProvider: (SurfaceRequest) -> CameraSurfaceRequest = ::CameraSurfaceRequestAdapter,
) : Preview.SurfaceProvider {

    private val lock = Any()
    private val released = AtomicBoolean(false)
    private var activeRequest: ActiveSurfaceRequest? = null

    override fun onSurfaceRequested(surfaceRequest: SurfaceRequest) {
        callbackExecutor.execute { handleSurfaceRequest(requestAdapterProvider(surfaceRequest)) }
    }

    internal fun handleSurfaceRequest(surfaceRequest: CameraSurfaceRequest) {
        val requestId = CameraSurfaceRequestIdSequence.next()
        val machine = CameraSurfaceRequestStateMachine(
            requestId = requestId,
            requestedResolution = CameraSurfaceRequestResolution(
                width = maxOf(1, surfaceRequest.requestedWidth),
                height = maxOf(1, surfaceRequest.requestedHeight)
            )
        )

        if (!machine.transitionToCreatingSurface()) {
            return
        }

        val requestState = ActiveSurfaceRequest(
            request = surfaceRequest,
            requestId = requestId,
            machine = machine,
        )

        val accepted = synchronized(lock) {
            if (released.get()) {
                machine.transitionToRejected("Provider is not accepting requests.")
                false
            } else if (activeRequest != null) {
                machine.transitionToRejected("Overlapping request is rejected.")
                false
            } else {
                activeRequest = requestState
                true
            }
        }

        requestStateObserver(machine.snapshot())

        if (!accepted) {
            surfaceRequest.willNotProvideSurface()
            requestResultObserver(
                requestId,
                CameraSurfaceRequestResult.WillNotProvideSurface,
                SurfaceRequest.Result.RESULT_WILL_NOT_PROVIDE_SURFACE,
                false
            )
            return
        }

        surfaceRequest.setTransformationInfoListener(callbackExecutor) { info ->
            if (requestState.shutdownRequested) {
                return@setTransformationInfoListener
            }
            if (machine.snapshot().isTerminal()) {
                return@setTransformationInfoListener
            }
            machine.updateTransformation(mapTransformation(info))
            requestStateObserver(machine.snapshot())
            machine.snapshot().transformation?.let { transformationObserver(requestId, it) }
        }

        surfaceRequest.addRequestCancellationListener(callbackExecutor) {
            if (machine.transitionToCancelled("Request cancelled by CameraX.")) {
                requestStateObserver(machine.snapshot())
            }
        }

        val inputSurface = try {
            surfaceFactory.create(
                machine.snapshot().requestedResolution
            )
        } catch (failure: CameraInputSurfaceFailure) {
            machine.transitionToFailed(
                resultCode = -1,
                result = CameraSurfaceRequestResult.InvalidSurface,
                error = failure.message ?: "Failed to create input surface."
            )
            requestStateObserver(machine.snapshot())
            requestResultObserver(
                requestId,
                CameraSurfaceRequestResult.InvalidSurface,
                -1,
                true
            )
            surfaceRequest.willNotProvideSurface()
            finalizeRequestNow(requestState)
            return
        }

        if (!requestState.trySetSurface(inputSurface)) {
            inputSurface.release()
            surfaceRequest.willNotProvideSurface()
            finalizeRequestNow(requestState)
            return
        }

        machine.transitionToProvided()
        requestStateObserver(machine.snapshot())

        surfaceRequest.provideSurface(inputSurface.surface, callbackExecutor) { result ->
            val mappedResult = mapResult(result.resultCode)
            if (requestState.resultReceived.getAndSet(true)) {
                return@provideSurface
            }

            val transitioned = machine.transitionToResult(resultCode = result.resultCode, result = mappedResult)
            if (!transitioned) {
                if (machine.snapshot().state == CameraSurfaceRequestLifecycleState.CANCELLED) {
                    requestStateObserver(machine.snapshot())
                    finalizeIfCompleted(requestState)
                }
                return@provideSurface
            }

            requestStateObserver(machine.snapshot())
            requestResultObserver(
                requestId,
                mappedResult,
                result.resultCode,
                mappedResult !is CameraSurfaceRequestResult.SurfaceUsedSuccessfully &&
                    mappedResult !is CameraSurfaceRequestResult.RequestCancelled
            )

            finalizeIfCompleted(requestState)
        }
    }

    fun release() {
        if (!released.compareAndSet(false, true)) {
            return
        }

        val target = synchronized(lock) {
            val current = activeRequest
            if (current != null) {
                current.shutdownRequested = true
            }
            current
        }

        if (target == null) {
            return
        }

        if (!target.surfaceProvided) {
            target.machine.transitionToRejected("Provider was released before surface provided.")
            target.request.willNotProvideSurface()
            requestResultObserver(
                target.requestId,
                CameraSurfaceRequestResult.WillNotProvideSurface,
                SurfaceRequest.Result.RESULT_WILL_NOT_PROVIDE_SURFACE,
                false
            )
            finalizeRequestNow(target)
            return
        }

        synchronized(lock) {
            if (target.resultReceived.get()) {
                finalizeIfCompleted(target)
                return
            }
            if (!target.machine.snapshot().isTerminal()) {
                target.request.invalidate()
            }
        }
    }

    private fun finalizeRequestNow(requestState: ActiveSurfaceRequest) {
        synchronized(lock) {
            if (activeRequest?.requestId != requestState.requestId) {
                return
            }
            activeRequest = null
        }
        finalizeIfCompleted(requestState)
    }

    private fun finalizeIfCompleted(requestState: ActiveSurfaceRequest) {
        if (!requestState.machine.snapshot().isTerminal()) {
            return
        }

        synchronized(lock) {
            if (activeRequest?.requestId == requestState.requestId) {
                activeRequest = null
            }
        }

        if (requestState.machine.markRelease()) {
            requestStateObserver(requestState.machine.snapshot())
        }

        requestState.releaseResourcesOnce()
    }

    private fun mapResult(resultCode: Int): CameraSurfaceRequestResult = when (resultCode) {
        SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY -> CameraSurfaceRequestResult.SurfaceUsedSuccessfully
        SurfaceRequest.Result.RESULT_REQUEST_CANCELLED -> CameraSurfaceRequestResult.RequestCancelled
        SurfaceRequest.Result.RESULT_INVALID_SURFACE -> CameraSurfaceRequestResult.InvalidSurface
        SurfaceRequest.Result.RESULT_SURFACE_ALREADY_PROVIDED -> CameraSurfaceRequestResult.SurfaceAlreadyProvided
        SurfaceRequest.Result.RESULT_WILL_NOT_PROVIDE_SURFACE -> CameraSurfaceRequestResult.WillNotProvideSurface
        else -> CameraSurfaceRequestResult.Unknown(resultCode)
    }

    private fun mapTransformation(info: CameraSurfaceTransformationInfo): CameraSurfaceTransformation {
        return CameraSurfaceTransformation(
            rotationDegrees = info.rotationDegrees,
            cropRect = CameraSurfaceCropRect(
                left = info.cropLeft,
                top = info.cropTop,
                right = info.cropRight,
                bottom = info.cropBottom,
            ),
        )
    }
}

internal interface CameraSurfaceRequest {
    val requestedWidth: Int
    val requestedHeight: Int

    fun setTransformationInfoListener(
        executor: Executor,
        listener: (CameraSurfaceTransformationInfo) -> Unit
    )

    fun addRequestCancellationListener(
        executor: Executor,
        listener: () -> Unit
    )

    fun willNotProvideSurface()

    fun provideSurface(
        surface: CameraSurfaceRequestSurface,
        executor: Executor,
        listener: (CameraSurfaceRequestResultCode) -> Unit
    )

    fun invalidate()
}

internal class CameraSurfaceRequestAdapter(
    private val request: SurfaceRequest
) : CameraSurfaceRequest {
    override val requestedWidth: Int
        get() = request.resolution.width

    override val requestedHeight: Int
        get() = request.resolution.height

    override fun setTransformationInfoListener(
        executor: Executor,
        listener: (CameraSurfaceTransformationInfo) -> Unit
    ) = request.setTransformationInfoListener(executor) { info ->
        listener(AndroidSurfaceTransformationInfo(info))
    }

    override fun addRequestCancellationListener(executor: Executor, listener: () -> Unit) =
        request.addRequestCancellationListener(executor, listener)

    override fun willNotProvideSurface() {
        request.willNotProvideSurface()
    }

    override fun provideSurface(
        surface: CameraSurfaceRequestSurface,
        executor: Executor,
        listener: (CameraSurfaceRequestResultCode) -> Unit
    ) {
        request.provideSurface(surface.resolveSurface(), executor) { result ->
            listener(CameraSurfaceRequestResultCode(result.resultCode))
        }
    }

    override fun invalidate() {
        request.invalidate()
    }
}

internal data class CameraSurfaceRequestResultCode(
    val resultCode: Int
)

internal interface CameraSurfaceTransformationInfo {
    val rotationDegrees: Int
    val cropLeft: Int
    val cropTop: Int
    val cropRight: Int
    val cropBottom: Int
}

private data class AndroidSurfaceTransformationInfo(
    private val delegate: SurfaceRequest.TransformationInfo
) : CameraSurfaceTransformationInfo {
    override val rotationDegrees: Int
        get() = delegate.rotationDegrees

    override val cropLeft: Int
        get() = delegate.cropRect.left

    override val cropTop: Int
        get() = delegate.cropRect.top

    override val cropRight: Int
        get() = delegate.cropRect.right

    override val cropBottom: Int
        get() = delegate.cropRect.bottom
}

private class ActiveSurfaceRequest(
    val request: CameraSurfaceRequest,
    val requestId: Long,
    val machine: CameraSurfaceRequestStateMachine,
    var surface: CameraSurfaceOwnedSurface? = null,
    var surfaceProvided: Boolean = false,
    var shutdownRequested: Boolean = false,
    val resultReceived: AtomicBoolean = AtomicBoolean(false),
    var resourcesReleased: Boolean = false,
) {
    fun trySetSurface(newSurface: CameraSurfaceOwnedSurface): Boolean {
        if (surfaceProvided || surface != null) {
            return false
        }
        surface = newSurface
        surfaceProvided = true
        return true
    }

    fun releaseResourcesOnce() {
        if (resourcesReleased) {
            return
        }
        resourcesReleased = true
        surface?.release()
        surface = null
    }
}
