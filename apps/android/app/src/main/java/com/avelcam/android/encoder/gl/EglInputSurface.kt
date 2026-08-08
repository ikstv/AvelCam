package com.avelcam.android.encoder.gl

import android.view.Surface

class EglInputSurface(
    surface: Surface,
    private val eglCore: EglCore = EglCore(),
    private val ownsEglCore: Boolean = true,
    private val dispatch: ((() -> Any?) -> Any?)? = null,
) : AutoCloseable {
    private val eglSurface = dispatchCall { eglCore.createWindowSurface(surface) }

    fun makeCurrent() {
        dispatchCall { eglCore.makeCurrent(eglSurface) }
    }

    fun <T> runOnEglThread(block: () -> T): T = dispatchCall(block)

    fun swapBuffers(presentationTimeNs: Long): Boolean {
        return dispatchCall {
            eglCore.setPresentationTime(eglSurface, presentationTimeNs)
            eglCore.swapBuffers(eglSurface)
        }
    }

    override fun close() {
        dispatchCall {
            eglCore.destroySurface(eglSurface)
            if (ownsEglCore) eglCore.release()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> dispatchCall(block: () -> T): T {
        val dispatcher = dispatch ?: return block()
        return dispatcher(block as () -> Any?) as T
    }
}
