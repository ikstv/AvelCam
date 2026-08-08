package com.avelcam.android.camera.pipeline.surface

internal fun interface CameraInputSurfaceFactory {
    @Throws(CameraInputSurfaceFailure::class)
    fun create(resolution: CameraSurfaceRequestResolution): CameraSurfaceOwnedSurface
}

internal class DefaultCameraInputSurfaceFactory : CameraInputSurfaceFactory {
    override fun create(resolution: CameraSurfaceRequestResolution): CameraInputSurface {
        if (resolution.width <= 0 || resolution.height <= 0) {
            throw CameraInputSurfaceFailure.InvalidResolution(resolution.width, resolution.height)
        }

        return try {
            CameraInputSurface.create(android.util.Size(resolution.width, resolution.height))
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
