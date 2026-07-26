package com.avelcam.android.encoder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class H264NalUnitParserTest {
    @Test
    fun parsesThreeByteStartCode() {
        val sample = byteArrayOf(0, 0, 1, 0x67.toByte(), 1, 2, 3, 0, 0, 1, 0x68.toByte(), 4, 5)
        val parser = H264NalUnitParser()
        val types = parser.detectTypes(sample)
        assertTrue(types.contains(H264NalUnit.TYPE_SPS))
        assertTrue(types.contains(H264NalUnit.TYPE_PPS))
    }

    @Test
    fun parsesFourByteStartCode() {
        val sample = byteArrayOf(0, 0, 0, 1, 0x65.toByte(), 4, 0, 0, 0, 1, 0x06.toByte())
        val parser = H264NalUnitParser()
        val types = parser.detectTypes(sample)
        assertTrue(types.contains(H264NalUnit.TYPE_IDR))
        assertTrue(types.contains(H264NalUnit.TYPE_SEI))
    }

    @Test
    fun parsesMultipleNalUnits() {
        val sample = byteArrayOf(
            0, 0, 1, 0x67.toByte(), 1, 2, 0, 0, 1, 0x68.toByte(), 3, 4,
            0, 0, 1, 0x65.toByte(), 5, 6
        )
        val parser = H264NalUnitParser()
        val units = parser.extractNalUnits(sample)
        assertEquals(3, units.size)
    }

    @Test
    fun returnsEmptyForMalformed() {
        val sample = byteArrayOf(1, 2, 3, 4, 5)
        val parser = H264NalUnitParser()
        assertTrue(parser.detectTypes(sample).isEmpty())
    }

    @Test
    fun parsesAvccPayload() {
        val payload = byteArrayOf(0, 0, 0, 2, 0x67.toByte(), 0x00, 0, 0, 0, 1, 0x00)
        val parser = H264NalUnitParser()
        val types = parser.detectTypes(payload)
        assertEquals(1, types.size)
        assertTrue(types.contains(H264NalUnit.TYPE_SPS))
    }
}
