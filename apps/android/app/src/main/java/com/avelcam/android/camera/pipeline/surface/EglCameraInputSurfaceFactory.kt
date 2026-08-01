package com.avelcam.android.camera.pipeline.surface

import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface

internal class EglCameraInputSurfaceFactory(
    private val externalTextureFactory: () -> ExternalOesTexture = { ExternalOesTexture.create() },
) : CameraInputSurfaceFactory {
    override fun create(resolution: CameraSurfaceRequestResolution): CameraInputSurface {
        if (resolution.width <= 0 || resolution.height <= 0) {
            throw CameraInputSurfaceFailure.InvalidResolution(resolution.width, resolution.height)
        }

        val externalTexture = try {
            externalTextureFactory()
        } catch (error: Throwable) {
            throw CameraInputSurfaceFailure.AllocationFailure(
                width = resolution.width,
                height = resolution.height,
                reason = error.message ?: "Failed to create external OES texture.",
                failure = error,
            )
        }

        return try {
            val surfaceTexture = SurfaceTexture(externalTexture.textureId)
            surfaceTexture.setDefaultBufferSize(resolution.width, resolution.height)
            val surface = Surface(surfaceTexture)
            CameraInputSurface(
                resolution = Size(resolution.width, resolution.height),
                surfaceTexture = surfaceTexture,
                sourceTextureId = externalTexture.textureId,
                surface = AndroidSurfaceRequestToken(surface),
                onRelease = { externalTexture.close() },
            )
        } catch (error: Throwable) {
            externalTexture.close()
            throw CameraInputSurfaceFailure.AllocationFailure(
                width = resolution.width,
                height = resolution.height,
                reason = error.message ?: "Failed to create EGL camera input surface.",
                failure = error,
            )
        }
    }
}
