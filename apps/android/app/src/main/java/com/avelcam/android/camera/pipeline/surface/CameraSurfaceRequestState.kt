package com.avelcam.android.camera.pipeline.surface

import java.util.concurrent.atomic.AtomicLong

data class CameraSurfaceRequestResolution(
    val width: Int,
    val height: Int
)

enum class CameraSurfaceRequestLifecycleState {
    RECEIVED,
    CREATING_SURFACE,
    PROVIDED,
    CANCELLED,
    COMPLETED,
    REJECTED,
    FAILED,
    RELEASED
}

data class CameraSurfaceRequestState(
    val requestId: Long,
    val requestedResolution: CameraSurfaceRequestResolution,
    val state: CameraSurfaceRequestLifecycleState,
    val surfaceRequestResultCode: Int?,
    val result: CameraSurfaceRequestResult?,
    val lastError: String?,
    val transformation: CameraSurfaceTransformation?,
) {
    fun isTerminal(): Boolean = when (state) {
        CameraSurfaceRequestLifecycleState.RELEASED,
        CameraSurfaceRequestLifecycleState.CANCELLED,
        CameraSurfaceRequestLifecycleState.COMPLETED,
        CameraSurfaceRequestLifecycleState.REJECTED,
        CameraSurfaceRequestLifecycleState.FAILED -> true
        else -> false
    }
}

internal object CameraSurfaceRequestIdSequence {
    private val counter = AtomicLong(0L)

    fun next(): Long = counter.incrementAndGet()
    fun resetForTests(): Unit {
        while (counter.get() != 0L) {
            if (counter.compareAndSet(counter.get(), 0L)) break
        }
    }
}

class CameraSurfaceRequestStateMachine(
    requestId: Long,
    requestedResolution: CameraSurfaceRequestResolution
) {
    private var state = CameraSurfaceRequestState(
        requestId = requestId,
        requestedResolution = requestedResolution,
        state = CameraSurfaceRequestLifecycleState.RECEIVED,
        surfaceRequestResultCode = null,
        result = null,
        lastError = null,
        transformation = null
    )

    fun snapshot(): CameraSurfaceRequestState = state

    fun transitionToCreatingSurface(): Boolean = transition(
        from = setOf(
            CameraSurfaceRequestLifecycleState.RECEIVED
        ),
        to = CameraSurfaceRequestLifecycleState.CREATING_SURFACE,
    )

    fun transitionToProvided(): Boolean = transition(
        from = setOf(
            CameraSurfaceRequestLifecycleState.CREATING_SURFACE
        ),
        to = CameraSurfaceRequestLifecycleState.PROVIDED,
    )

    fun transitionToResult(resultCode: Int, result: CameraSurfaceRequestResult): Boolean {
        val nextState = when (result) {
            is CameraSurfaceRequestResult.SurfaceUsedSuccessfully -> CameraSurfaceRequestLifecycleState.COMPLETED
            is CameraSurfaceRequestResult.RequestCancelled -> CameraSurfaceRequestLifecycleState.CANCELLED
            is CameraSurfaceRequestResult.InvalidSurface,
            is CameraSurfaceRequestResult.SurfaceAlreadyProvided,
            is CameraSurfaceRequestResult.WillNotProvideSurface,
            is CameraSurfaceRequestResult.Unknown -> CameraSurfaceRequestLifecycleState.FAILED
        }

        return if (state.state in setOf(
                CameraSurfaceRequestLifecycleState.CREATING_SURFACE,
                CameraSurfaceRequestLifecycleState.PROVIDED
            ) && !isTerminal(state.state)
        ) {
            state = state.copy(
                state = nextState,
                surfaceRequestResultCode = resultCode,
                result = result
            )
            true
        } else {
            false
        }
    }

    fun transitionToCancelled(reason: String? = null): Boolean = transition(
        from = setOf(
            CameraSurfaceRequestLifecycleState.RECEIVED,
            CameraSurfaceRequestLifecycleState.CREATING_SURFACE,
            CameraSurfaceRequestLifecycleState.PROVIDED
        ),
        to = CameraSurfaceRequestLifecycleState.CANCELLED,
        error = reason
    )

    fun transitionToRejected(reason: String? = null): Boolean = transition(
        from = setOf(
            CameraSurfaceRequestLifecycleState.RECEIVED,
            CameraSurfaceRequestLifecycleState.CREATING_SURFACE
        ),
        to = CameraSurfaceRequestLifecycleState.REJECTED,
        error = reason
    )

    fun transitionToFailed(resultCode: Int, error: String, result: CameraSurfaceRequestResult): Boolean = transition(
        from = setOf(
            CameraSurfaceRequestLifecycleState.RECEIVED,
            CameraSurfaceRequestLifecycleState.CREATING_SURFACE,
            CameraSurfaceRequestLifecycleState.PROVIDED
        ),
        to = CameraSurfaceRequestLifecycleState.FAILED,
        resultCode = resultCode,
        result = result,
        error = error
    )

    fun updateTransformation(transformation: CameraSurfaceTransformation) {
        if (isTerminal(state.state)) {
            return
        }
        state = state.copy(transformation = transformation)
    }

    fun markRelease(): Boolean = transition(
        from = setOf(
            CameraSurfaceRequestLifecycleState.RECEIVED,
            CameraSurfaceRequestLifecycleState.CREATING_SURFACE,
            CameraSurfaceRequestLifecycleState.PROVIDED,
            CameraSurfaceRequestLifecycleState.CANCELLED,
            CameraSurfaceRequestLifecycleState.COMPLETED,
            CameraSurfaceRequestLifecycleState.REJECTED,
            CameraSurfaceRequestLifecycleState.FAILED
        ),
        to = CameraSurfaceRequestLifecycleState.RELEASED
    )

    private fun isTerminal(state: CameraSurfaceRequestLifecycleState): Boolean {
        return when (state) {
            CameraSurfaceRequestLifecycleState.CANCELLED,
            CameraSurfaceRequestLifecycleState.COMPLETED,
            CameraSurfaceRequestLifecycleState.REJECTED,
            CameraSurfaceRequestLifecycleState.FAILED,
            CameraSurfaceRequestLifecycleState.RELEASED -> true
            else -> false
        }
    }

    private fun transition(
        from: Set<CameraSurfaceRequestLifecycleState>,
        to: CameraSurfaceRequestLifecycleState,
        resultCode: Int? = null,
        result: CameraSurfaceRequestResult? = null,
        error: String? = null
    ): Boolean {
        if (state.state == CameraSurfaceRequestLifecycleState.RELEASED) {
            return false
        }
        if (isTerminal(state.state) && to != CameraSurfaceRequestLifecycleState.RELEASED) {
            return false
        }
        if (!from.contains(state.state)) {
            return false
        }
        state = state.copy(
            state = to,
            surfaceRequestResultCode = resultCode,
            result = result,
            lastError = error
        )
        return true
    }
}
