package com.avelcam.android.camera.pipeline

import android.opengl.EGLSurface
import android.os.Handler
import android.view.Surface
import com.avelcam.android.camera.pipeline.surface.EglCameraInputSurfaceFactory
import com.avelcam.android.camera.pipeline.surface.ExternalOesTexture
import com.avelcam.android.camera.pipeline.surface.ObservableCameraInputSurfaceFactory
import com.avelcam.android.encoder.gl.EglCore
import com.avelcam.android.encoder.gl.EglInputSurface

internal class GlFanoutEglContext : AutoCloseable {
    private val glThread = CameraGlThread()
    private lateinit var eglCore: EglCore
    private lateinit var offscreenSurface: EGLSurface

    init {
        glThread.call {
            eglCore = EglCore()
            offscreenSurface = eglCore.createPbufferSurface()
        }
    }

    fun createInputSurface(surface: Surface): EglInputSurface {
        return glThread.call {
            EglInputSurface(surface, eglCore, false) { block -> glThread.call(block) }
        }
    }

    fun createCameraInputSurfaceFactory(): EglCameraInputSurfaceFactory {
        return EglCameraInputSurfaceFactory(
            externalTextureFactory = {
                runWithContext { ExternalOesTexture.create() }
            },
            glDispatch = { block -> glThread.call(block) },
        )
    }

    fun createObservableCameraInputSurfaceFactory(): ObservableCameraInputSurfaceFactory {
        return ObservableCameraInputSurfaceFactory(
            delegate = createCameraInputSurfaceFactory()
        )
    }

    fun <T> runWithContext(block: () -> T): T {
        return glThread.call {
            eglCore.makeCurrent(offscreenSurface)
            block()
        }
    }

    fun frameHandler(): Handler = glThread.handler()

    override fun close() {
        glThread.call {
            eglCore.clearCurrent()
            eglCore.destroySurface(offscreenSurface)
            eglCore.release()
        }
        glThread.close()
    }
}
