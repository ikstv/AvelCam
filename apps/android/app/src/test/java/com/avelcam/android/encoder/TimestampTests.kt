package com.avelcam.android.encoder

import org.junit.Assert.assertTrue
import org.junit.Test

class TimestampTests {
    @Test
    fun syntheticTimestampsAreMonotonic() {
        val config = EncoderConfig(frameRate = 30)
        val base = 100_000L
        val times = List(10) { index ->
            base + index * config.frameIntervalUs
        }
        var previous = times.first()
        times.drop(1).forEach { current ->
            assertTrue(current > previous)
            previous = current
        }
    }

    @Test
    fun monotonicSequenceIsBasedOnFrameRate() {
        val config = EncoderConfig(frameRate = 60)
        val expected = 1_000_000L / 60L
        assertTrue(config.frameIntervalUs == expected || config.frameIntervalUs == expected - 1)
    }
}

