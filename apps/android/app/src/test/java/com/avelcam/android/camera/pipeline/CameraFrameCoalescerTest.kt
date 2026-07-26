
package com.avelcam.android.camera.pipeline

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraFrameCoalescerTest {
    @Test
    fun firstCallbackSchedulesSingleTask() {
        var scheduled = 0
        val coalescer = CameraFrameCoalescer { scheduled++ }

        coalescer.onFrameAvailable()

        assertEquals(1L, coalescer.snapshot().renderTasksScheduled)
        assertEquals(1L, coalescer.snapshot().callbacksReceived)
        assertEquals(1, scheduled)
    }

    @Test
    fun repeatedCallbacksAreCoalesced() {
        var scheduled = 0
        val coalescer = CameraFrameCoalescer { scheduled++ }

        coalescer.onFrameAvailable()
        coalescer.onFrameAvailable()
        coalescer.onFrameAvailable()

        val stats = coalescer.snapshot()
        assertEquals(3L, stats.callbacksReceived)
        assertEquals(1L, stats.renderTasksScheduled)
        assertEquals(2L, stats.callbacksCoalesced)
        assertEquals(1, scheduled)
    }

    @Test
    fun frameDuringRenderSchedulesNextRender() {
        var scheduled = 0
        val coalescer = CameraFrameCoalescer { scheduled++ }

        coalescer.onFrameAvailable()
        coalescer.onRenderCompleted()

        coalescer.onFrameAvailable()
        val stats = coalescer.snapshot()

        assertEquals(1L, stats.renderCompletions)
        assertEquals(2L, stats.renderTasksScheduled)
    }

    @Test
    fun noUnboundedScheduling() {
        var scheduled = 0
        val coalescer = CameraFrameCoalescer { scheduled++ }

        repeat(1000) { coalescer.onFrameAvailable() }
        coalescer.onRenderCompleted()

        assertEquals(2L, coalescer.snapshot().renderTasksScheduled)
    }

    @Test
    fun releaseIgnoresNewCallbacks() {
        var scheduled = 0
        val coalescer = CameraFrameCoalescer { scheduled++ }

        coalescer.onFrameAvailable()
        coalescer.release()
        coalescer.onFrameAvailable()
        coalescer.onRenderCompleted()

        val stats = coalescer.snapshot()
        assertEquals(1L, stats.renderTasksScheduled)
        assertEquals(1L, stats.callbacksIgnoredAfterRelease)
    }

    @Test
    fun completionAfterReleaseDoesNotReschedule() {
        var scheduled = 0
        val coalescer = CameraFrameCoalescer { scheduled++ }

        coalescer.onFrameAvailable()
        coalescer.onFrameAvailable()
        coalescer.release()
        coalescer.onRenderCompleted()

        assertEquals(1, scheduled)
        assertEquals(0L, coalescer.snapshot().callbacksIgnoredAfterRelease)
    }
}
