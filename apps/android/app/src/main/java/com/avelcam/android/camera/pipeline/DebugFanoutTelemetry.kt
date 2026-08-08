package com.avelcam.android.camera.pipeline

import android.util.Log
import com.avelcam.android.BuildConfig
import com.avelcam.android.encoder.EncodedAccessUnit
import com.avelcam.android.encoder.EncodedFrameSink
import java.util.concurrent.atomic.AtomicLong

/** Debug-only, one-shot evidence for the physical EGL fan-out route. */
internal object DebugFanoutTelemetry : EncodedFrameSink {
    private const val TAG = "AvelCamFanoutDebug"
    private val surfaceTextureCallbacks = AtomicLong()
    private val surfaceTextureUpdates = AtomicLong()
    private val previewRenders = AtomicLong()
    private val encoderRenders = AtomicLong()
    private val encodedAccessUnits = AtomicLong()

    fun reset() {
        if (!BuildConfig.ENABLE_EGL_FANOUT_DEBUG) return
        surfaceTextureCallbacks.set(0)
        surfaceTextureUpdates.set(0)
        previewRenders.set(0)
        encoderRenders.set(0)
        encodedAccessUnits.set(0)
    }

    fun onSurfaceTextureCallback() = record(surfaceTextureCallbacks, "SurfaceTexture callback")

    fun onSurfaceTextureUpdated() = record(surfaceTextureUpdates, "SurfaceTexture updated")

    fun onPreviewRendered() = record(previewRenders, "Preview destination swap")

    fun onEncoderRendered() = record(encoderRenders, "Encoder destination swap")

    override fun onEncodedAccessUnit(accessUnit: EncodedAccessUnit) {
        record(encodedAccessUnits, "H.264 encoded access unit")
    }

    fun logSummary(reason: String) {
        if (!BuildConfig.ENABLE_EGL_FANOUT_DEBUG) return
        Log.d(
            TAG,
            "$reason: callbacks=${surfaceTextureCallbacks.get()}, updates=${surfaceTextureUpdates.get()}, " +
                "previewRenders=${previewRenders.get()}, encoderRenders=${encoderRenders.get()}, " +
                "encodedAccessUnits=${encodedAccessUnits.get()}",
        )
    }

    private fun record(counter: AtomicLong, event: String) {
        if (!BuildConfig.ENABLE_EGL_FANOUT_DEBUG) return
        if (counter.incrementAndGet() == 1L) {
            Log.d(TAG, "$event count=1 thread=${Thread.currentThread().name}")
        }
    }
}
