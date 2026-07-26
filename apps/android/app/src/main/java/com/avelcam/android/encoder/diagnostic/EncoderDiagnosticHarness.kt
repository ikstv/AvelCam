package com.avelcam.android.encoder.diagnostic

import com.avelcam.android.encoder.EncoderConfig
import com.avelcam.android.encoder.EncodedAccessUnit
import com.avelcam.android.encoder.EncodedFrameSink
import com.avelcam.android.encoder.EncoderStatistics
import com.avelcam.android.encoder.H264Encoder
import com.avelcam.android.encoder.H264EncoderStartResult
import com.avelcam.android.encoder.gl.SyntheticFrameRenderer
import com.avelcam.android.encoder.gl.SyntheticFrameSource
import java.util.concurrent.atomic.AtomicReference

data class EncoderDiagnosticState(
    val running: Boolean = false,
    val selectedCodec: String = "unknown",
    val lastError: String? = null,
    val submittedFrames: Long = 0L,
    val encodedUnits: Long = 0L,
    val keyframes: Long = 0L,
    val codecConfigUnits: Long = 0L,
    val encodedBytes: Long = 0L,
    val firstOutputLatencyUs: Long? = null,
    val outputFps: Double = 0.0
)

class EncoderDiagnosticHarness(
    private val config: EncoderConfig = EncoderConfig()
) {
    private var encoder: H264Encoder? = null
    private var source: SyntheticFrameSource? = null
    private val state = AtomicReference(
        EncoderDiagnosticState(
            selectedCodec = "not started"
        )
    )

    fun start(): Result<H264EncoderStartResult> {
        if (state.get().running) {
            return Result.failure(IllegalStateException("Encoder already running."))
        }
        val sink = object : EncodedFrameSink {
            override fun onEncodedAccessUnit(accessUnit: EncodedAccessUnit) {
                updateFromSnapshot()
            }

            override fun onFrameSinkError(error: Throwable) {
                state.set(state.get().copy(lastError = error.message))
            }
        }
        val next = H264Encoder(config, sink)
        val started = next.start()
        if (started.isFailure) {
            return started
        }
        val startResult = started.getOrThrow()
        val surface = next.getInputSurface() ?: return Result.failure(IllegalStateException("No input surface."))
        val src = SyntheticFrameSource(
            renderer = SyntheticFrameRenderer(),
            width = config.width,
            height = config.height,
            frameRate = config.frameRate,
            sink = sink
        )
        src.start(surface)
        encoder = next
        source = src
        state.set(
            state.get().copy(
                running = true,
                selectedCodec = startResult.selectedCodec.codecName,
                lastError = null
            )
        )
        return Result.success(startResult)
    }

    fun stop(): Result<Unit> {
        return runCatching {
            source?.stop()
            encoder?.stop()
            source = null
            encoder = null
            state.set(state.get().copy(running = false))
        }
    }

    fun release(): Result<Unit> {
        return runCatching {
            source?.close()
            source = null
            encoder?.release()
            encoder = null
            state.set(EncoderDiagnosticState(selectedCodec = "not started"))
        }
    }

    fun getState(): EncoderDiagnosticState {
        updateFromSnapshot()
        return state.get()
    }

    private fun updateFromSnapshot() {
        val stats = encoder?.snapshotStatistics() ?: return
        val current = state.get()
        state.set(
            current.copy(
                running = current.running,
                submittedFrames = source?.snapshot()?.submittedFrames ?: 0L,
                encodedUnits = stats.encodedAccessUnits,
                keyframes = stats.keyframes,
                codecConfigUnits = stats.codecConfigUnits,
                encodedBytes = stats.encodedBytes,
                firstOutputLatencyUs = stats.firstOutputLatencyUs,
                outputFps = stats.outputFps
            )
        )
    }
}

