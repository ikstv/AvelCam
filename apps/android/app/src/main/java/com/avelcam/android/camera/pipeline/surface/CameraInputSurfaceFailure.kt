package com.avelcam.android.camera.pipeline.surface

sealed class CameraInputSurfaceFailure(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    data class InvalidResolution(val width: Int, val height: Int) : CameraInputSurfaceFailure(
        "Invalid surface resolution: ${width}x${height}"
    )

    data class AllocationFailure(
        val width: Int,
        val height: Int,
        val reason: String,
        val failure: Throwable
    ) : CameraInputSurfaceFailure(
        "Failed to allocate input surface ${width}x${height}: $reason",
        failure
    )
}
