package com.avelcam.android.encoder.gl

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.EGLExt
import android.view.Surface
import java.util.Arrays

class EglCore(
    private val sharedContext: EGLContext = EGL14.EGL_NO_CONTEXT,
) {
    private val eglDisplay: EGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    private val config: EGLConfig
    private val eglContext: android.opengl.EGLContext
    private val version = IntArray(2)
    private val nConfigs = IntArray(1)

    init {
        val configAttribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        EGL14.eglInitialize(eglDisplay, version, 0, version, 1)
        EGL14.eglChooseConfig(
            eglDisplay,
            configAttribs,
            0,
            configs,
            0,
            configs.size,
            nConfigs,
            0
        )
        if (nConfigs[0] != 1 || configs[0] == null) {
            throw IllegalStateException("Expected one EGL config, got ${Arrays.toString(nConfigs)}.")
        }
        config = configs[0] ?: throw IllegalStateException("Failed to choose EGL config.")

        val ctxAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        eglContext = EGL14.eglCreateContext(
            eglDisplay,
            config,
            sharedContext,
            ctxAttribs,
            0
        )
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            throw IllegalStateException("Failed to create EGL context.")
        }
    }

    fun getEglContext(): EGLContext = eglContext

    fun createWindowSurface(surface: Surface): android.opengl.EGLSurface {
        return EGL14.eglCreateWindowSurface(eglDisplay, config, surface, intArrayOf(EGL14.EGL_NONE), 0)
    }

    fun makeCurrent(eglSurface: android.opengl.EGLSurface) {
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
    }

    fun swapBuffers(eglSurface: android.opengl.EGLSurface): Boolean {
        return EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    fun setPresentationTime(eglSurface: android.opengl.EGLSurface, presentationTimeNs: Long): Boolean {
        return EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, presentationTimeNs)
    }

    fun destroySurface(eglSurface: android.opengl.EGLSurface) {
        EGL14.eglDestroySurface(eglDisplay, eglSurface)
    }

    fun release() {
        EGL14.eglMakeCurrent(
            eglDisplay,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT
        )
        EGL14.eglDestroyContext(eglDisplay, eglContext)
        EGL14.eglTerminate(eglDisplay)
    }
}
