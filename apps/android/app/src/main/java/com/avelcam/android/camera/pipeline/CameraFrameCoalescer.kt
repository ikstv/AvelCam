package com.avelcam.android.camera.pipeline

data class CameraFrameCoalescerStats(
    val callbacksReceived: Long,
    val renderTasksScheduled: Long,
    val callbacksCoalesced: Long,
    val renderCompletions: Long,
    val callbacksIgnoredAfterRelease: Long
)

class CameraFrameCoalescer(
    private val scheduleRender: () -> Unit
) {
    private val lock = Any()
    private var released = false
    private var renderScheduled = false
    private var hasPendingAgain = false
    private var callbacksReceived = 0L
    private var renderTasksScheduled = 0L
    private var callbacksCoalesced = 0L
    private var renderCompletions = 0L
    private var callbacksIgnoredAfterRelease = 0L

    fun onFrameAvailable() {
        val shouldSchedule = synchronized(lock) {
            callbacksReceived++

            if (released) {
                callbacksIgnoredAfterRelease++
                return
            }

            if (renderScheduled) {
                callbacksCoalesced++
                hasPendingAgain = true
                false
            } else {
                renderScheduled = true
                renderTasksScheduled++
                true
            }
        }

        if (shouldSchedule) {
            scheduleRender()
        }
    }

    fun onRenderCompleted() {
        val maybeScheduleNext = synchronized(lock) {
            renderCompletions++

            if (released) {
                renderScheduled = false
                hasPendingAgain = false
                false
            } else if (hasPendingAgain) {
                callbacksCoalesced++
                hasPendingAgain = false
                renderTasksScheduled++
                true
            } else {
                renderScheduled = false
                false
            }
        }

        if (maybeScheduleNext) {
            scheduleRender()
        }
    }

    fun release() {
        synchronized(lock) {
            released = true
            renderScheduled = false
            hasPendingAgain = false
        }
    }

    fun snapshot(): CameraFrameCoalescerStats {
        synchronized(lock) {
            return CameraFrameCoalescerStats(
                callbacksReceived = callbacksReceived,
                renderTasksScheduled = renderTasksScheduled,
                callbacksCoalesced = callbacksCoalesced,
                renderCompletions = renderCompletions,
                callbacksIgnoredAfterRelease = callbacksIgnoredAfterRelease
            )
        }
    }
}

