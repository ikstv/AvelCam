package com.avelcam.android.camera.pipeline.surface

import com.avelcam.android.camera.pipeline.GlFanoutEglContext

internal class CameraInputSurfaceFactoryOwner private constructor(
    val factory: ObservableCameraInputSurfaceFactory,
    private val closeAction: () -> Unit,
) : AutoCloseable {
    private var closed = false

    override fun close() {
        if (closed) return
        closed = true
        try {
            factory.clearSurface()
        } finally {
            closeAction()
        }
    }

    companion object {
        fun create(mode: CameraInputSurfaceMode): CameraInputSurfaceFactoryOwner {
            return when (mode) {
                CameraInputSurfaceMode.DEFAULT -> CameraInputSurfaceFactoryOwner(
                    factory = CameraInputSurfaceFactorySelector.create(CameraInputSurfaceMode.DEFAULT),
                    closeAction = {},
                )
                CameraInputSurfaceMode.EGL -> {
                    val eglContext = GlFanoutEglContext()
                    try {
                        CameraInputSurfaceFactoryOwner(
                            factory = CameraInputSurfaceFactorySelector.create(
                                mode = CameraInputSurfaceMode.EGL,
                                eglContext = eglContext,
                            ),
                            closeAction = eglContext::close,
                        )
                    } catch (error: Throwable) {
                        eglContext.close()
                        throw error
                    }
                }
            }
        }
    }
}
