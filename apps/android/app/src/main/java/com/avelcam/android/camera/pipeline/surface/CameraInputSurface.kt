package com.avelcam.android.camera.pipeline.surface

import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface
import java.util.concurrent.atomic.AtomicBoolean

internal interface CameraSurfaceOwnedSurface {
    val surface: CameraSurfaceRequestSurface
    fun release()
}

internal interface CameraSurfaceRequestSurface {
    fun resolveSurface(): Surface
    fun release()
}

internal class AndroidSurfaceRequestToken(
    private val surface: Surface
) : CameraSurfaceRequestSurface {
    private val isReleased = AtomicBoolean(false)

    override fun resolveSurface(): Surface = surface

    override fun release() {
        if (!isReleased.compareAndSet(false, true)) {
            return
        }
        surface.release()
    }
}

internal class CameraInputSurface internal constructor(
    val resolution: Size,
    val surfaceTexture: SurfaceTexture,
    override val surface: CameraSurfaceRequestSurface
) : CameraSurfaceOwnedSurface {
    private val isReleased = AtomicBoolean(false)

    init {
        require(resolution.width > 0 && resolution.height > 0) {
            "Surface resolution must be positive."
        }
    }

    internal fun isReleased(): Boolean = isReleased.get()

    override fun release() {
        if (!isReleased.compareAndSet(false, true)) {
            return
        }

        surface.release()
        surfaceTexture.release()
    }

    companion object {
        internal fun create(resolution: Size): CameraInputSurface {
            if (resolution.width <= 0 || resolution.height <= 0) {
                throw CameraInputSurfaceFailure.InvalidResolution(resolution.width, resolution.height)
            }

            val surfaceTexture = SurfaceTexture(false)
            surfaceTexture.setDefaultBufferSize(resolution.width, resolution.height)
            val surface = Surface(surfaceTexture)
            return CameraInputSurface(
                resolution = resolution,
                surfaceTexture = surfaceTexture,
                surface = AndroidSurfaceRequestToken(surface)
            )
        }
    }
}
