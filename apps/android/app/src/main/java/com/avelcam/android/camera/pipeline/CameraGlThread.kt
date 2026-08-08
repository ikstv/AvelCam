package com.avelcam.android.camera.pipeline

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicBoolean

internal class CameraGlThread(
    name: String = "AvelCam-CameraGL",
) : AutoCloseable {
    private val thread = HandlerThread(name)
    private val started = AtomicBoolean(false)
    private val released = AtomicBoolean(false)
    private val lifecycleLock = Any()
    private lateinit var handler: Handler

    fun <T> call(block: () -> T): T {
        if (isOnGlThread()) {
            check(!released.get()) { "Camera GL thread is released." }
            return block()
        }

        ensureStarted()
        val task = FutureTask(Callable(block))
        synchronized(lifecycleLock) {
            check(!released.get()) { "Camera GL thread is released." }
            check(handler.post(task)) { "Camera GL thread is shutting down." }
        }
        return try {
            task.get()
        } catch (error: ExecutionException) {
            throw error.cause ?: error
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IllegalStateException("Interrupted while waiting for the Camera GL thread.", error)
        }
    }

    fun post(block: () -> Unit): Boolean {
        ensureStarted()
        return synchronized(lifecycleLock) {
            !released.get() && handler.post(block)
        }
    }

    fun isOnGlThread(): Boolean = started.get() && Thread.currentThread() === thread

    fun handler(): Handler {
        ensureStarted()
        synchronized(lifecycleLock) {
            check(!released.get()) { "Camera GL thread is released." }
            return handler
        }
    }

    override fun close() {
        synchronized(lifecycleLock) {
            if (!released.compareAndSet(false, true)) return
            if (started.get()) {
                handler.post { thread.quitSafely() }
            }
        }
    }

    private fun ensureStarted() {
        synchronized(lifecycleLock) {
            check(!released.get()) { "Camera GL thread is released." }
            if (started.compareAndSet(false, true)) {
                thread.start()
                handler = Handler(thread.looper)
                Log.d(TAG, "Camera GL owner started: ${thread.name}")
            }
        }
    }

    companion object {
        private const val TAG = "AvelCam-CameraGL"
    }
}
