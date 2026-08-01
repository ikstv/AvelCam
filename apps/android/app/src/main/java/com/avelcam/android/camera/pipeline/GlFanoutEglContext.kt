package com.avelcam.android.camera.pipeline

import android.view.Surface
import com.avelcam.android.encoder.gl.EglCore
import com.avelcam.android.encoder.gl.EglInputSurface

internal class GlFanoutEglContext : AutoCloseable {
    private val eglCore = EglCore()

    fun createInputSurface(surface: Surface): EglInputSurface {
        return EglInputSurface(
            surface = surface,
            eglCore = eglCore,
            ownsEglCore = false,
        )
    }

    override fun close() {
        eglCore.release()
    }
}
