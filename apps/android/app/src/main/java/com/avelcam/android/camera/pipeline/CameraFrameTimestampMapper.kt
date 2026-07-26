package com.avelcam.android.camera.pipeline

import kotlin.math.max

data class CameraTimestampSnapshot(
    val lastSourceTimestampNs: Long,
    val lastMappedTimestampNs: Long,
    val correctionCount: Int,
    val fallbackCount: Int
)

class CameraFrameTimestampMapper(
    private val fallbackTimestampSource: () -> Long = ::defaultFallbackTimestamp
) {
    private var lastSourceTimestampNs: Long = -1L
    private var lastMappedTimestampNs: Long = -1L
    private var correctionCount: Int = 0
    private var fallbackCount: Int = 0

    fun mapTimestamp(sourceTimestampNs: Long): Long {
        val mapped = when {
            sourceTimestampNs > 0L -> {
                if (lastMappedTimestampNs < 0L || sourceTimestampNs > lastMappedTimestampNs) {
                    sourceTimestampNs
                } else {
                    correctionCount++
                    max(lastMappedTimestampNs + 1L, sourceTimestampNs + 1L)
                }
            }

            else -> {
                fallbackCount++
                val fallback = max(fallbackTimestampSource(), lastMappedTimestampNs + 1L)
                if (fallback <= lastMappedTimestampNs) {
                    lastMappedTimestampNs + 1L
                } else {
                    fallback
                }
            }
        }

        lastSourceTimestampNs = sourceTimestampNs
        lastMappedTimestampNs = mapped
        return mapped
    }

    fun snapshot(): CameraTimestampSnapshot = CameraTimestampSnapshot(
        lastSourceTimestampNs = lastSourceTimestampNs,
        lastMappedTimestampNs = lastMappedTimestampNs,
        correctionCount = correctionCount,
        fallbackCount = fallbackCount
    )

    companion object {
        private fun defaultFallbackTimestamp(): Long = System.nanoTime()
    }
}

