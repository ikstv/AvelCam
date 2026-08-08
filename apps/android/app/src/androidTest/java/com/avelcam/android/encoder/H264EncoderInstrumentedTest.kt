package com.avelcam.android.encoder

import android.os.SystemClock
import android.util.Log
import android.view.Surface
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.avelcam.android.encoder.gl.SyntheticFrameRenderer
import com.avelcam.android.encoder.gl.SyntheticFrameSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

@RunWith(AndroidJUnit4::class)
class H264EncoderInstrumentedTest {
    companion object {
        private const val TAG = "AVELCAM_ENCODER_METRICS"
        private const val Cycles = 5
        private const val MinOutputSamples = 2
        private const val MaxFramesPerCycle = 20
        private const val SessionTimeoutMs = 8_000L
    }

    @Test
    fun syntheticEncoderSessionShouldStartAndProduceOutputAcrossFiveCycles() {
        Assume.assumeTrue("Physical H.264 AVC surface encoder required.", hasCompatibleEncoder())
        val config = EncoderConfig()

        val results = mutableListOf<SessionMetrics>()
        repeat(Cycles) { cycle ->
            results.add(runSingleSession(config, cycle + 1))
        }

        assertEquals("Expected exactly five encoder restart cycles", Cycles, results.size)
        Log.i(TAG, "cycleCount=${results.size}, allCyclesPass=true")
    }

    private fun runSingleSession(config: EncoderConfig, cycle: Int): SessionMetrics {
        val running = AtomicBoolean(true)
        val encodedUnits = Collections.synchronizedList(mutableListOf<EncodedAccessUnit>())
        val frameOutputCount = AtomicInteger(0)
        val errors = AtomicInteger(0)
        val codecConfigLatch = CountDownLatch(1)
        val sufficientFrameLatch = CountDownLatch(1)
        val firstFrameOutputWallClockUs = AtomicLong(-1L)
        val sessionStartWallClockUs = SystemClock.elapsedRealtimeNanos() / 1000L

        val sink = object : EncodedFrameSink {
            override fun onEncodedAccessUnit(accessUnit: EncodedAccessUnit) {
                if (!running.get()) return
                if (accessUnit.data.isNotEmpty()) {
                    encodedUnits.add(accessUnit)
                }
                if (accessUnit.isCodecConfig) {
                    codecConfigLatch.countDown()
                }
                if (!accessUnit.isCodecConfig && !accessUnit.endOfStream && accessUnit.presentationTimeUs >= 0L) {
                    if (firstFrameOutputWallClockUs.compareAndSet(-1L, SystemClock.elapsedRealtimeNanos() / 1000L)) {
                        // first-encoded-frame output timestamp captured for latency metric
                    }
                    val count = frameOutputCount.incrementAndGet()
                    if (count >= MinOutputSamples) {
                        sufficientFrameLatch.countDown()
                    }
                }
            }

            override fun onFrameSinkError(error: Throwable) {
                errors.incrementAndGet()
            }
        }

        val encoder = H264Encoder(config, sink)
        val startResult = encoder.start()
        if (startResult.isFailure) {
            fail(startResult.exceptionOrNull()?.message ?: "encoder start failed")
        }
        val selectedCodec = startResult.getOrNull()?.selectedCodec?.codecName
        val surface: Surface = requireNotNull(encoder.getInputSurface()) { "Input surface should be available." }
        val source = SyntheticFrameSource(
            renderer = SyntheticFrameRenderer(),
            width = config.width,
            height = config.height,
            frameRate = config.frameRate,
            sink = sink
        )

        try {
            source.start(surface, maxFrameCount = MaxFramesPerCycle)
            assertTrue(
                "Codec configuration output (CSD) not produced within timeout",
                codecConfigLatch.await(SessionTimeoutMs, TimeUnit.MILLISECONDS)
            )
            assertTrue(
                "Encoded frame outputs were not produced within timeout",
                sufficientFrameLatch.await(SessionTimeoutMs, TimeUnit.MILLISECONDS)
            )
        } finally {
            running.set(false)
            source.stop()
            val stopResult = encoder.stop()
            if (stopResult.isFailure) {
                fail(stopResult.exceptionOrNull()?.message ?: "encoder stop failed")
            }
            source.close()
            encoder.release()
        }

        val finalUnits = encodedUnits.toList()
        val frameUnits = finalUnits.filterNot { it.isCodecConfig }.filterNot { it.endOfStream }
        val outputPtsUs = frameUnits.map { it.presentationTimeUs }
        val firstPtsUs = outputPtsUs.firstOrNull()
        val lastPtsUs = outputPtsUs.lastOrNull()

        assertFalse("No encoded access units were produced", finalUnits.isEmpty())
        assertTrue(
            "Expected output format (codec-config)",
            finalUnits.any { it.isCodecConfig }
        )
        assertTrue("Expected keyframe unit", frameUnits.any { it.isKeyFrame })
        assertTrue("Expected at least two encoded frame samples", outputPtsUs.size >= MinOutputSamples)
        assertNotNull("First frame output PTS is required", firstPtsUs)
        assertNotNull("Last frame output PTS is required", lastPtsUs)
        outputPtsUs.forEach { pts -> assertTrue("Output PTS must be non-negative: $pts", pts >= 0L) }

        var ptsMonotonic = true
        var firstNonMonotonic = outputPtsUs.firstOrNull()
        for (index in 1 until outputPtsUs.size) {
            val previous = outputPtsUs[index - 1]
            val current = outputPtsUs[index]
            if (current < previous) {
                ptsMonotonic = false
                firstNonMonotonic = previous
                fail("Non-monotonic output PTS at index=$index: $previous -> $current")
            }
        }

        assertTrue(
            "Expected positive encoded output bytes",
            finalUnits.sumOf { it.data.size } > 0
        )
        assertEquals("Encoder should not report callback errors", 0, errors.get())
        assertEquals("Encoder should be released after cleanup", EncoderState.RELEASED, encoder.getState())

        val codecConfigUnits = finalUnits.count { it.isCodecConfig }.toLong()
        val encodedAccessUnits = finalUnits.size.toLong()
        val keyframes = frameUnits.count { it.isKeyFrame }.toLong()
        val encodedBytes = frameUnits.sumOf { it.data.size.toLong() }
        val durationUs = outputPtsUs.firstOrNull()?.let { first ->
            outputPtsUs.lastOrNull()?.let { last -> max(1L, last - first) }
        }
        val measuredOutputFps = if (outputPtsUs.size >= MinOutputSamples && durationUs != null && durationUs > 0L) {
            (outputPtsUs.size - 1).toDouble() * 1_000_000.0 / durationUs.toDouble()
        } else {
            0.0
        }
        val measuredAverageBitrateBps = if (durationUs != null && durationUs > 0L) {
            if (outputPtsUs.size >= MinOutputSamples) {
                encodedBytes * 8_000_000.0 / durationUs.toDouble()
            } else {
                0.0
            }
        } else {
            0.0
        }
        val firstOutputLatencyMs = if (firstFrameOutputWallClockUs.get() >= 0L) {
            (firstFrameOutputWallClockUs.get() - sessionStartWallClockUs).toDouble() / 1000.0
        } else {
            null
        }
        val submittedFrames = source.snapshot().submittedFrames.toLong()
        assertTrue("Expected at least one submitted source frame", submittedFrames > 0L)

        val metrics = SessionMetrics(
            cycle = cycle,
            codecName = selectedCodec,
            width = config.width,
            height = config.height,
            targetFps = config.frameRate,
            targetBitrateBps = config.bitrate,
            submittedFrames = submittedFrames,
            encodedAccessUnits = encodedAccessUnits,
            codecConfigUnits = codecConfigUnits,
            keyframes = keyframes,
            encodedBytes = encodedBytes,
            firstOutputLatencyMs = firstOutputLatencyMs,
            firstPtsUs = firstPtsUs ?: 0L,
            lastPtsUs = lastPtsUs ?: 0L,
            ptsSampleCount = outputPtsUs.size,
            ptsMonotonic = ptsMonotonic,
            measuredOutputFps = measuredOutputFps,
            measuredAverageBitrateBps = measuredAverageBitrateBps,
            errors = errors.get().toLong(),
            firstNonMonotonicPair = firstNonMonotonic?.let { "${it}" } ?: "n/a"
        )

        Log.i(TAG, metrics.toLogLine())
        return metrics
    }

    private fun hasCompatibleEncoder(): Boolean =
        try {
            val selector = H264CodecSelector()
            selector.select(EncoderConfig())
            true
        } catch (_: Exception) {
            false
        }

    private data class SessionMetrics(
        val cycle: Int,
        val codecName: String?,
        val width: Int,
        val height: Int,
        val targetFps: Int,
        val targetBitrateBps: Int,
        val submittedFrames: Long,
        val encodedAccessUnits: Long,
        val codecConfigUnits: Long,
        val keyframes: Long,
        val encodedBytes: Long,
        val firstOutputLatencyMs: Double?,
        val firstPtsUs: Long,
        val lastPtsUs: Long,
        val ptsSampleCount: Int,
        val ptsMonotonic: Boolean,
        val measuredOutputFps: Double,
        val measuredAverageBitrateBps: Double,
        val errors: Long,
        val firstNonMonotonicPair: String,
    ) {
        fun toLogLine(): String {
            return buildString {
                append("cycle=$cycle codecName=$codecName width=$width height=$height targetFps=$targetFps ")
                append("targetBitrateBps=$targetBitrateBps submittedFrames=$submittedFrames encodedAccessUnits=$encodedAccessUnits ")
                append("codecConfigUnits=$codecConfigUnits keyframes=$keyframes encodedBytes=$encodedBytes ")
                append("firstOutputLatencyMs=${firstOutputLatencyMs?.let { String.format(Locale.US, "%.2f", it) } ?: "na"} ")
                append("firstPtsUs=$firstPtsUs lastPtsUs=$lastPtsUs ptsSampleCount=$ptsSampleCount ptsMonotonic=$ptsMonotonic ")
                append(
                    "firstNonMonotonicPair=$firstNonMonotonicPair "
                )
                append("measuredOutputFps=${String.format(Locale.US, "%.3f", measuredOutputFps)} ")
                append("measuredAverageBitrateBps=${String.format(Locale.US, "%.2f", measuredAverageBitrateBps)} ")
                append("errors=$errors")
            }
        }
    }
}

