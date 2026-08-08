package com.avelcam.android.camera.pipeline.surface

import com.avelcam.android.camera.pipeline.GlFanoutEglContext

internal enum class CameraInputSurfaceMode {
    DEFAULT,
    EGL,
}

internal object CameraInputSurfaceFactorySelector {
    fun create(
        mode: CameraInputSurfaceMode,
        eglContext: GlFanoutEglContext? = null,
    ): ObservableCameraInputSurfaceFactory {
        return when (mode) {
            CameraInputSurfaceMode.DEFAULT -> ObservableCameraInputSurfaceFactory()
            CameraInputSurfaceMode.EGL -> {
                requireNotNull(eglContext) {
                    "EGL camera input surface mode requires a GlFanoutEglContext."
                }.createObservableCameraInputSurfaceFactory()
            }
        }
    }
}
