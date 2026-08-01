package com.avelcam.android.encoder.gl

import android.view.Surface

class EglInputSurface(
    surface: Surface,
    private val eglCore: EglCore = EglCore(),
    private val ownsEglCore: Boolean = true,
) : AutoCloseable {
    private val eglSurface = eglCore.createWindowSurface(surface)

    fun makeCurrent() {
        eglCore.makeCurrent(eglSurface)
    }

    fun swapBuffers(presentationTimeNs: Long): Boolean {
        eglCore.setPresentationTime(eglSurface, presentationTimeNs)
        return eglCore.swapBuffers(eglSurface)
    }

    override fun close() {
        eglCore.destroySurface(eglSurface)
        if (ownsEglCore) {
            eglCore.release()
        }
    }
}
