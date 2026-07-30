package com.avelcam.android.camera.pipeline.surface

import android.util.Size

internal fun interface CameraInputSurfaceFactory {
    @Throws(CameraInputSurfaceFailure::class)
    fun create(resolution: Size): CameraSurfaceOwnedSurface
}

internal class DefaultCameraInputSurfaceFactory : CameraInputSurfaceFactory {
    override fun create(resolution: Size): CameraInputSurface {
        if (resolution.width <= 0 || resolution.height <= 0) {
            throw CameraInputSurfaceFailure.InvalidResolution(resolution.width, resolution.height)
        }

        return try {
            CameraInputSurface.create(resolution)
        } catch (error: Throwable) {
            throw CameraInputSurfaceFailure.AllocationFailure(
                width = resolution.width,
                height = resolution.height,
                reason = error.message ?: "unknown",
                failure = error
            )
        }
    }
}
