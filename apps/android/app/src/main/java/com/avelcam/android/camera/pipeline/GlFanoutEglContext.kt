package com.avelcam.android.camera.pipeline

import android.opengl.EGLSurface
import android.view.Surface
import com.avelcam.android.camera.pipeline.surface.EglCameraInputSurfaceFactory
import com.avelcam.android.camera.pipeline.surface.ExternalOesTexture
import com.avelcam.android.encoder.gl.EglCore
import com.avelcam.android.encoder.gl.EglInputSurface

internal class GlFanoutEglContext : AutoCloseable {
    private val eglCore = EglCore()
    private val offscreenSurface: EGLSurface = eglCore.createPbufferSurface()

    fun createInputSurface(surface: Surface): EglInputSurface {
        return EglInputSurface(
            surface = surface,
            eglCore = eglCore,
            ownsEglCore = false,
        )
    }

    fun createCameraInputSurfaceFactory(): EglCameraInputSurfaceFactory {
        return EglCameraInputSurfaceFactory(
            externalTextureFactory = {
                runWithContext { ExternalOesTexture.create() }
            }
        )
    }

    fun <T> runWithContext(block: () -> T): T {
        eglCore.makeCurrent(offscreenSurface)
        return block()
    }

    override fun close() {
        eglCore.clearCurrent()
        eglCore.destroySurface(offscreenSurface)
        eglCore.release()
    }
}
