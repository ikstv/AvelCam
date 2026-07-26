package com.avelcam.android.encoder.gl

import android.view.Surface
import com.avelcam.android.encoder.EncodedFrameSink
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

data class SyntheticSourceSnapshot(
    val submittedFrames: Long,
    val isRunning: Boolean,
    val droppedFrames: Long
)

class SyntheticFrameSource(
    private val renderer: SyntheticFrameRenderer,
    private val width: Int,
    private val height: Int,
    private val frameRate: Int,
    private val sink: EncodedFrameSink,
) : AutoCloseable {
    private val running = AtomicBoolean(false)
    private var executor: ExecutorService? = null
    private var submittedFrames = 0L
    private var droppedFrames = 0L

    fun start(surface: Surface, maxFrameCount: Int? = null) {
        if (running.getAndSet(true)) return
        val loopExecutor = Executors.newSingleThreadExecutor()
        executor = loopExecutor
        loopExecutor.execute {
            EglInputSurface(surface).use { eglSurface ->
                val intervalNs = 1_000_000_000L / frameRate
                var nextFrameNs = System.nanoTime()
                var frameIndex = 0
                while (running.get() && (maxFrameCount == null || frameIndex < maxFrameCount)) {
                    eglSurface.makeCurrent()
                    renderer.render(frameIndex, width, height)
                    val framePtsNs = nextFrameNs
                    val frameAccepted = eglSurface.swapBuffers(framePtsNs)
                    if (!frameAccepted) {
                        droppedFrames++
                        sink.onFrameSinkError(IllegalStateException("Failed to swap EGL buffers."))
                    } else {
                        submittedFrames++
                    }

                    frameIndex++
                    nextFrameNs += intervalNs
                    val delayNs = nextFrameNs - System.nanoTime()
                    if (delayNs > 0L) {
                        try {
                            Thread.sleep(delayNs / 1_000_000L, (delayNs % 1_000_000L).toInt())
                        } catch (_: InterruptedException) {
                            Thread.currentThread().interrupt()
                            break
                        }
                    }
                }
            }
            renderer.close()
            running.set(false)
        }
    }

    fun stop() {
        running.set(false)
        executor?.shutdownNow()
        executor = null
    }

    fun snapshot(): SyntheticSourceSnapshot = SyntheticSourceSnapshot(
        submittedFrames = submittedFrames,
        isRunning = running.get(),
        droppedFrames = droppedFrames
    )

    override fun close() {
        stop()
        renderer.close()
    }
}

