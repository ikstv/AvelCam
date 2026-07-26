package com.avelcam.android.encoder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EncoderConfigTest {
    @Test
    fun defaultConfigIsValid() {
        EncoderConfig().validate()
    }

    @Test
    fun rejectZeroWidth() {
        assertThrows(IllegalArgumentException::class.java) {
            EncoderConfig(width = 0).validate()
        }
    }

    @Test
    fun rejectOddHeight() {
        assertThrows(IllegalArgumentException::class.java) {
            EncoderConfig(height = 719).validate()
        }
    }

    @Test
    fun rejectZeroFrameRate() {
        assertThrows(IllegalArgumentException::class.java) {
            EncoderConfig(frameRate = 0).validate()
        }
    }

    @Test
    fun rejectNegativeBitrate() {
        assertThrows(IllegalArgumentException::class.java) {
            EncoderConfig(bitrate = -10).validate()
        }
    }

    @Test
    fun rejectNegativeIFrameInterval() {
        assertThrows(IllegalArgumentException::class.java) {
            EncoderConfig(iFrameIntervalSeconds = -1).validate()
        }
    }
}

