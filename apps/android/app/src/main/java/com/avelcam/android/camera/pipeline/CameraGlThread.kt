package com.avelcam.android.camera.pipeline

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicBoolean

internal class CameraGlThread(
    name: String = "AvelCam-CameraGL",
) : AutoCloseable {
    private val thread = HandlerThread(name)
    private val started = AtomicBoolean(false)
    private val released = AtomicBoolean(false)
    private lateinit var handler: Handler

    fun <T> call(block: () -> T): T {
        check(!released.get()) { "Camera GL thread is released." }
        if (isOnGlThread()) return block()
        ensureStarted()
        val task = FutureTask(Callable(block))
        handler.post(task)
        return task.get()
    }

    fun post(block: () -> Unit): Boolean {
        if (released.get()) return false
        ensureStarted()
        return handler.post(block)
    }

    fun isOnGlThread(): Boolean = started.get() && Thread.currentThread() === thread

    fun handler(): Handler {
        ensureStarted()
        return handler
    }

    override fun close() {
        if (!released.compareAndSet(false, true)) return
        if (started.get()) {
            handler.post { thread.quitSafely() }
        }
    }

    private fun ensureStarted() {
        if (started.compareAndSet(false, true)) {
            thread.start()
            handler = Handler(thread.looper)
            Log.d(TAG, "Camera GL owner started: ${thread.name}")
        }
    }

    companion object {
        private const val TAG = "AvelCam-CameraGL"
    }
}
