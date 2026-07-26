package com.avelcam.android.encoder

import kotlin.math.max

data class EncoderStatistics(
    val encoderStartTimeUs: Long,
    val submittedSyntheticFrames: Long,
    val encodedAccessUnits: Long,
    val codecConfigUnits: Long,
    val keyframes: Long,
    val regularFrames: Long,
    val encodedBytes: Long,
    val firstOutputLatencyUs: Long?,
    val lastPresentationTimestampUs: Long?,
    val averageEncodedBitrateBps: Double,
    val outputFps: Double,
    val errors: Long,
    val droppedSinkDeliveries: Long
) {
    companion object {
        val Empty = EncoderStatistics(
            encoderStartTimeUs = -1,
            submittedSyntheticFrames = 0,
            encodedAccessUnits = 0,
            codecConfigUnits = 0,
            keyframes = 0,
            regularFrames = 0,
            encodedBytes = 0,
            firstOutputLatencyUs = null,
            lastPresentationTimestampUs = null,
            averageEncodedBitrateBps = 0.0,
            outputFps = 0.0,
            errors = 0,
            droppedSinkDeliveries = 0
        )
    }
}

internal class EncoderStatisticsTracker {
    private val lock = Any()
    private var encoderStartTimeUs: Long = -1
    private var submittedSyntheticFrames: Long = 0
    private var encodedAccessUnits: Long = 0
    private var codecConfigUnits: Long = 0
    private var keyframes: Long = 0
    private var regularFrames: Long = 0
    private var encodedBytes: Long = 0
    private var firstOutputAtUs: Long = -1
    private var errors: Long = 0
    private var droppedSinkDeliveries: Long = 0
    private var firstOutputLatencyUs: Long? = null
    private var lastOutputUs: Long = -1
    private var firstOutputPresentationUs: Long = -1

    fun reset(startTimeUs: Long) {
        synchronized(lock) {
            encoderStartTimeUs = startTimeUs
            submittedSyntheticFrames = 0
            encodedAccessUnits = 0
            codecConfigUnits = 0
            keyframes = 0
            regularFrames = 0
            encodedBytes = 0
            firstOutputAtUs = -1
            errors = 0
            droppedSinkDeliveries = 0
            firstOutputLatencyUs = null
            lastOutputUs = -1
            firstOutputPresentationUs = -1
        }
    }

    fun recordSubmittedFrame() {
        synchronized(lock) {
            submittedSyntheticFrames++
        }
    }

    fun recordOutput(
        accessUnit: EncodedAccessUnit
    ) {
        synchronized(lock) {
            encodedAccessUnits++
            if (accessUnit.isCodecConfig) {
                codecConfigUnits++
            }
            if (accessUnit.isKeyFrame) {
                keyframes++
            } else {
                regularFrames++
            }
            encodedBytes += accessUnit.data.size.toLong()
            lastOutputUs = accessUnit.presentationTimeUs
            if (firstOutputPresentationUs == -1L) {
                firstOutputPresentationUs = accessUnit.presentationTimeUs
            }
            if (firstOutputAtUs < 0) {
                firstOutputAtUs = accessUnit.presentationTimeUs
            }
            if (firstOutputAtUs >= 0 && firstOutputLatencyUs == null) {
                firstOutputLatencyUs = accessUnit.presentationTimeUs - firstOutputAtUs
            }
        }
    }

    fun recordError() {
        synchronized(lock) {
            errors++
        }
    }

    fun recordDroppedDelivery() {
        synchronized(lock) {
            droppedSinkDeliveries++
        }
    }

    fun snapshot(currentWallClockUs: Long): EncoderStatistics {
        synchronized(lock) {
            val durationUs = max(1L, currentWallClockUs - encoderStartTimeUs)
            val bitrate = if (durationUs > 0) {
                encodedBytes * 8_000_000L / durationUs
            } else {
                0L
            }.toDouble()

            val elapsedFrames = max(1L, (lastOutputUs - firstOutputAtUs).coerceAtLeast(0L))
            val outputFps = encodedAccessUnits.toDouble() * 1_000_000.0 / elapsedFrames.toDouble()

            return EncoderStatistics(
                encoderStartTimeUs = encoderStartTimeUs,
                submittedSyntheticFrames = submittedSyntheticFrames,
                encodedAccessUnits = encodedAccessUnits,
                codecConfigUnits = codecConfigUnits,
                keyframes = keyframes,
                regularFrames = regularFrames,
                encodedBytes = encodedBytes,
                firstOutputLatencyUs = firstOutputLatencyUs,
                lastPresentationTimestampUs = if (lastOutputUs >= 0) lastOutputUs else null,
                averageEncodedBitrateBps = bitrate,
                outputFps = if (outputFps.isFinite()) outputFps else 0.0,
                errors = errors,
                droppedSinkDeliveries = droppedSinkDeliveries
            )
        }
    }
}

