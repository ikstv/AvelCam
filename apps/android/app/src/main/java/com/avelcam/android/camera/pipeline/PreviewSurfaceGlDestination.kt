package com.avelcam.android.camera.pipeline

import android.opengl.GLES20
import android.view.Surface
import com.avelcam.android.encoder.gl.EglInputSurface
import java.util.concurrent.atomic.AtomicBoolean

class PreviewSurfaceGlDestination(
    override val spec: CameraGlFanoutOutputSpec,
    surface: Surface,
    createEglSurface: (Surface) -> EglInputSurface = { EglInputSurface(it) },
): CameraGlFanoutDestination {
    init {
        require(spec.role == CameraGlFanoutOutputRole.PREVIEW) {
            "PreviewSurfaceGlDestination requires PREVIEW role."
        }
        require(spec.width > 0) { "Destination width must be > 0." }
        require(spec.height > 0) { "Destination height must be > 0." }
    }

    private val eglSurface = createEglSurface(surface)
    private var previewRenderer: PreviewGlRenderer? = null
    private val isReleased = AtomicBoolean(false)

    override fun render(frame: CameraGlFanoutFrame): CameraGlFanoutRenderResult {
        return eglSurface.runOnEglThread { renderOnEglThread(frame) }
    }

    private fun renderOnEglThread(frame: CameraGlFanoutFrame): CameraGlFanoutRenderResult {
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
            val renderer = ensureRenderer()
            var isOesRendered = false
            var oesError: String? = null
            if (frame.sourceTextureId > 0) {
                val oesResult = runCatching {
                    renderer.render(
                        textureId = frame.sourceTextureId,
                        transform = frame.transformSnapshot,
                        width = spec.width,
                        height = spec.height,
                    )
                }
                isOesRendered = oesResult.getOrElse {
                    oesError = it.message
                    false
                }
            }

            if (!isOesRendered) {
                GLES20.glViewport(0, 0, spec.width, spec.height)
                GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            }
            val accepted = eglSurface.swapBuffers(frame.presentationTimestampNs)
            if (accepted) DebugFanoutTelemetry.onPreviewRendered()
            CameraGlFanoutRenderResult(
                role = spec.role,
                rendered = accepted,
                isFatal = isOesRendered.not() && frame.sourceTextureId > 0 && oesError != null,
                message = if (!accepted) {
                    "EGL swapBuffers failed."
                } else {
                    oesError?.let { "OES render fallback used: $it" }
                },
            )
        } catch (error: Throwable) {
            CameraGlFanoutRenderResult(
                role = spec.role,
                rendered = false,
                isFatal = true,
                message = error.message ?: "PreviewSurfaceGlDestination render failed."
            )
        }
    }

    override fun release() {
        if (!isReleased.compareAndSet(false, true)) {
            return
        }
        eglSurface.runOnEglThread {
            previewRenderer?.close()
            previewRenderer = null
            eglSurface.close()
        }
    }

    private fun ensureRenderer(): PreviewGlRenderer {
        return previewRenderer ?: PreviewGlRenderer().also { previewRenderer = it }
    }
}
