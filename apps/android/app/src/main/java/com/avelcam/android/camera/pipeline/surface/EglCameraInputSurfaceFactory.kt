package com.avelcam.android.camera.pipeline.surface

import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface

internal class EglCameraInputSurfaceFactory(
    private val externalTextureFactory: () -> ExternalOesTexture = { ExternalOesTexture.create() },
    private val glDispatch: ((() -> Any?) -> Any?)? = null,
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
            val resources = dispatch {
                val surfaceTexture = SurfaceTexture(externalTexture.textureId)
                surfaceTexture.setDefaultBufferSize(resolution.width, resolution.height)
                surfaceTexture to Surface(surfaceTexture)
            }
            val surfaceTexture = resources.first
            val surface = resources.second
            CameraInputSurface(
                resolution = Size(resolution.width, resolution.height),
                surfaceTexture = surfaceTexture,
                sourceTextureId = externalTexture.textureId,
                surface = AndroidSurfaceRequestToken(surface),
                onRelease = { dispatch { externalTexture.close() } },
            )
        } catch (error: Throwable) {
            dispatch { externalTexture.close() }
            throw CameraInputSurfaceFailure.AllocationFailure(
                width = resolution.width,
                height = resolution.height,
                reason = error.message ?: "Failed to create EGL camera input surface.",
                failure = error,
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> dispatch(block: () -> T): T {
        val dispatcher = glDispatch ?: return block()
        return dispatcher(block as () -> Any?) as T
    }
}
