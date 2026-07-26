package com.avelcam.android.encoder

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.avelcam.android.encoder.gl.SyntheticFrameRenderer
import com.avelcam.android.encoder.gl.SyntheticFrameSource
import org.junit.Assert.fail
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class H264EncoderInstrumentedTest {
    @Test
    fun syntheticEncoderSessionShouldStartAndProduceOutput() {
        Assume.assumeTrue("Physical H.264 AVC surface encoder required.", hasCompatibleEncoder())

        val running = AtomicBoolean(false)
        val sink = object : EncodedFrameSink {
            override fun onEncodedAccessUnit(accessUnit: EncodedAccessUnit) {
                if (accessUnit.data.isNotEmpty()) {
                    running.set(true)
                }
            }
        }
        val encoder = H264Encoder(EncoderConfig(), sink)
        val startResult = encoder.start()
        if (startResult.isFailure) {
            fail(startResult.exceptionOrNull()?.message ?: "encoder start failed")
        }

        val surface = encoder.getInputSurface() ?: fail("Input surface should be available.")
        val source = SyntheticFrameSource(
            renderer = SyntheticFrameRenderer(),
            width = EncoderConfig().width,
            height = EncoderConfig().height,
            frameRate = EncoderConfig().frameRate,
            sink = sink
        )
        source.start(surface, maxFrameCount = 10)
        Thread.sleep(500L)
        source.stop()
        encoder.stop()
        source.close()
        encoder.release()
        if (!running.get()) {
            fail("No encoded access units were produced.")
        }
    }

    private fun hasCompatibleEncoder(): Boolean {
        return try {
            val selector = H264CodecSelector()
            selector.select(EncoderConfig())
            true
        } catch (_: Exception) {
            false
        }
    }
}

