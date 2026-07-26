package com.avelcam.android.encoder

import android.media.MediaCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncoderStatisticsTest {
    @Test
    fun countsAccessUnitsAndBytes() {
        val tracker = EncoderStatisticsTracker()
        tracker.reset(1_000L)
        tracker.recordOutput(
            EncodedAccessUnit(
                data = byteArrayOf(1, 2, 3),
                presentationTimeUs = 1_500L,
                flags = MediaCodec.BUFFER_FLAG_KEY_FRAME,
                isCodecConfig = false,
                isKeyFrame = true,
                endOfStream = false,
                nalUnitTypes = emptySet()
            )
        )
        tracker.recordOutput(
            EncodedAccessUnit(
                data = byteArrayOf(4, 5),
                presentationTimeUs = 2_000L,
                flags = MediaCodec.BUFFER_FLAG_CODEC_CONFIG,
                isCodecConfig = true,
                isKeyFrame = false,
                endOfStream = false,
                nalUnitTypes = emptySet()
            )
        )
        val snapshot = tracker.snapshot(3_000L)
        assertEquals(2L, snapshot.encodedAccessUnits)
        assertEquals(1L, snapshot.keyframes)
        assertEquals(1L, snapshot.codecConfigUnits)
        assertEquals(5L, snapshot.encodedBytes)
        assertTrue(snapshot.outputFps >= 0.0)
    }

    @Test
    fun averageBitrateCalculationIsSafeWithZeroDuration() {
        val tracker = EncoderStatisticsTracker()
        tracker.reset(1_000L)
        val snapshot = tracker.snapshot(1_000L)
        assertEquals(0.0, snapshot.averageEncodedBitrateBps, 0.0001)
    }
}

