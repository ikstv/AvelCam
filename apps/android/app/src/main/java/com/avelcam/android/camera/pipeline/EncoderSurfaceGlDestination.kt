package com.avelcam.android.camera.pipeline

import android.opengl.GLES20
import android.view.Surface
import com.avelcam.android.encoder.gl.EglInputSurface
import java.util.concurrent.atomic.AtomicBoolean

class EncoderSurfaceGlDestination(
    override val spec: CameraGlFanoutOutputSpec,
    surface: Surface,
    createEglSurface: (Surface) -> EglInputSurface = { EglInputSurface(it) },
) : CameraGlFanoutDestination {

    init {
        require(spec.role == CameraGlFanoutOutputRole.ENCODER) {
            "EncoderSurfaceGlDestination requires ENCODER role."
        }
        require(spec.width > 0) { "Destination width must be > 0." }
        require(spec.height > 0) { "Destination height must be > 0." }
    }

    private val eglSurface = createEglSurface(surface)
    private val isReleased = AtomicBoolean(false)

    override fun render(frame: CameraGlFanoutFrame): CameraGlFanoutRenderResult {
        if (isReleased.get()) {
            return CameraGlFanoutRenderResult(
                role = spec.role,
                rendered = false,
                isFatal = true,
                message = "Destination released."
            )
        }

        return try {
            eglSurface.makeCurrent()
            GLES20.glViewport(0, 0, spec.width, spec.height)

            val red = ((frame.presentationTimestampNs % 1_000_000_000L).toFloat()) / 1_000_000_000f
            GLES20.glClearColor(red, 0.15f, 1f - red, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

            val accepted = eglSurface.swapBuffers(frame.presentationTimestampNs)
            CameraGlFanoutRenderResult(
                role = spec.role,
                rendered = accepted,
                message = if (!accepted) "EGL swapBuffers failed." else null
            )
        } catch (error: Throwable) {
            CameraGlFanoutRenderResult(
                role = spec.role,
                rendered = false,
                isFatal = true,
                message = error.message ?: "EncoderSurfaceGlDestination render failed."
            )
        }
    }

    override fun release() {
        if (!isReleased.compareAndSet(false, true)) {
            return
        }
        eglSurface.close()
    }
}
