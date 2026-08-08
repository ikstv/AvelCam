
package com.avelcam.android.camera.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraFrameTimestampMapperTest {
    @Test
    fun increasingTimestampsPassThrough() {
        val mapper = CameraFrameTimestampMapper { 0L }
        val first = mapper.mapTimestamp(1000L)
        val second = mapper.mapTimestamp(2000L)
        val third = mapper.mapTimestamp(3000L)

        assertEquals(1000L, first)
        assertEquals(2000L, second)
        assertEquals(3000L, third)
        assertEquals(0, mapper.snapshot().correctionCount)
        assertEquals(0L, mapper.snapshot().fallbackCount.toLong())
    }

    @Test
    fun duplicateTimestampCorrected() {
        val mapper = CameraFrameTimestampMapper { 100L }
        mapper.mapTimestamp(1000L)
        val fixed = mapper.mapTimestamp(1000L)

        assertEquals(1001L, fixed)
        assertEquals(1, mapper.snapshot().correctionCount)
    }

    @Test
    fun decreasingTimestampCorrected() {
        val mapper = CameraFrameTimestampMapper { 100L }
        mapper.mapTimestamp(2000L)
        val fixed = mapper.mapTimestamp(1000L)

        assertTrue(fixed > 2000L)
        assertEquals(1, mapper.snapshot().correctionCount)
    }

    @Test
    fun zeroTimestampUsesFallback() {
        var fallback = 10_000L
        val mapper = CameraFrameTimestampMapper { fallback.apply { fallback += 10L } }

        mapper.mapTimestamp(100L)
        val fallbackValue = mapper.mapTimestamp(0L)

        assertTrue(fallbackValue > 100L)
        assertEquals(1, mapper.snapshot().fallbackCount)
    }

    @Test
    fun negativeTimestampUsesFallback() {
        val mapper = CameraFrameTimestampMapper { 42_000L }

        mapper.mapTimestamp(100L)
        val fallbackValue = mapper.mapTimestamp(-1L)

        assertEquals(42_000L, fallbackValue)
        assertEquals(1, mapper.snapshot().fallbackCount)
    }

    @Test
    fun outputAlwaysStrictlyIncreasing() {
        val mapper = CameraFrameTimestampMapper { 5_000L }
        var previous = mapper.mapTimestamp(100L)
        previous = mapper.mapTimestamp(50L)
        previous = mapper.mapTimestamp(50L)
        previous = mapper.mapTimestamp(0L)
        val next = mapper.mapTimestamp(120L)

        assertTrue(next > previous)
    }

    @Test
    fun deterministicFallbackCanBeInjected() {
        var seed = 0L
        val mapper = CameraFrameTimestampMapper { seed += 1_000L; seed }

        val first = mapper.mapTimestamp(0L)
        val second = mapper.mapTimestamp(0L)

        assertEquals(1000L, first)
        assertEquals(2000L, second)
    }
}
