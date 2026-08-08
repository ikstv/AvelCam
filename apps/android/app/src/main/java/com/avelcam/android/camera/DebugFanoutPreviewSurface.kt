package com.avelcam.android.camera

import android.graphics.PixelFormat
import android.media.ImageReader
import android.util.Log
import android.view.Surface
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Debug-only off-screen consumer used to exercise the real preview GL destination. */
internal class DebugFanoutPreviewSurface : AutoCloseable {
    private val closed = AtomicBoolean(false)
    private val firstImageLogged = AtomicBoolean(false)
    private val framesReceived = AtomicLong(0)
    private val imageReader = ImageReader.newInstance(1280, 720, PixelFormat.RGBA_8888, 3)

    val surface: Surface = imageReader.surface

    init {
        imageReader.setOnImageAvailableListener({ reader ->
            reader.acquireLatestImage()?.use {
                framesReceived.incrementAndGet()
                if (firstImageLogged.compareAndSet(false, true)) {
                    Log.d(TAG, "Debug preview destination received its first GL frame.")
                }
            }
        }, null)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) imageReader.close()
    }

    companion object {
        private const val TAG = "AvelCamFanoutDebug"
    }
}
