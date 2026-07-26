package com.avelcam.android.encoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.Surface
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference

data class H264EncoderStartResult(
    val state: EncoderState,
    val selectedCodec: H264CodecSelection,
    val codecConfigData: Map<Int, ByteArray?>
)

class H264Encoder(
    private val config: EncoderConfig,
    private val sink: EncodedFrameSink,
    private val parser: H264NalUnitParser = H264NalUnitParser()
) {
    private val stateReference = AtomicReference(EncoderState.IDLE)
    private var mediaCodec: MediaCodec? = null
    private var mediaCodecCallback: MediaCodec.Callback? = null
    private var codecThread: HandlerThread? = null
    private var selectedCodec: H264CodecSelection? = null
    private var inputSurface: Surface? = null
    private val codecSelector = H264CodecSelector()
    private val statistics = EncoderStatisticsTracker()
    private var codecConfigData: Map<Int, ByteArray?> = emptyMap()
    private var startedUs: Long = -1

    fun getState(): EncoderState = stateReference.get()
    fun getInputSurface(): Surface? = inputSurface
    fun getSelectedCodec(): H264CodecSelection? = selectedCodec
    fun getConfigData(): Map<Int, ByteArray?> = codecConfigData

    fun start(): Result<H264EncoderStartResult> {
        if (!stateReference.compareAndSet(EncoderState.IDLE, EncoderState.CONFIGURING) &&
            !stateReference.compareAndSet(EncoderState.STOPPED, EncoderState.CONFIGURING)
        ) {
            return Result.failure(IllegalStateException("Invalid state for start: ${getState()}"))
        }
        return runCatching {
            config.validate()
            val selector = codecSelector.select(config)
            selectedCodec = selector

            val codec = MediaCodec.createByCodecName(selector.codecName)
            mediaCodec = codec
            mediaCodecCallback = buildCallback()

            val format = buildMediaFormat(selector)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = codec.createInputSurface()

            val callback = mediaCodecCallback ?: throw IllegalStateException("Callback not configured.")
            val thread = HandlerThread("AvelCam-H264Encoder")
            codecThread = thread
            thread.start()
            codec.setCallback(callback, Handler(thread.looper))
            codec.start()

            startedUs = SystemClock.elapsedRealtimeNanos() / 1000
            statistics.reset(startedUs)
            stateReference.set(EncoderState.RUNNING)
            H264EncoderStartResult(
                state = stateReference.get(),
                selectedCodec = selector,
                codecConfigData = codecConfigData
            )
        }.onFailure {
            stateReference.set(EncoderState.ERROR)
            cleanupCodec()
            throw it
        }
    }

    fun stop(): Result<Unit> {
        val current = getState()
        if (current != EncoderState.RUNNING && current != EncoderState.CONFIGURED && current != EncoderState.ERROR) {
            if (current == EncoderState.STOPPED || current == EncoderState.RELEASED) {
                return Result.success(Unit)
            }
            return Result.failure(IllegalStateException("Invalid state for stop: $current"))
        }
        return runCatching {
            stateReference.set(EncoderState.STOPPING)
            val codec = mediaCodec ?: throw IllegalStateException("Encoder not configured.")
            try {
                codec.signalEndOfInputStream()
            } catch (_: IllegalStateException) {
            }
            codec.stop()
            codec.release()
            inputSurface?.release()
            inputSurface = null
            mediaCodec = null
            mediaCodecCallback = null
            codecThread?.quitSafely()
            codecThread = null
            stateReference.set(EncoderState.STOPPED)
        }.onFailure {
            stateReference.set(EncoderState.ERROR)
        }
    }

    fun release(): Result<Unit> {
        return runCatching {
            stop().getOrThrow()
            inputSurface?.release()
            inputSurface = null
            mediaCodec = null
            mediaCodecCallback = null
            selectedCodec = null
            codecConfigData = emptyMap()
            codecThread = null
            stateReference.set(EncoderState.RELEASED)
        }
    }

    fun snapshotStatistics(): EncoderStatistics {
        val nowUs = SystemClock.elapsedRealtimeNanos() / 1000
        return statistics.snapshot(nowUs)
    }

    private fun buildMediaFormat(selection: H264CodecSelection): MediaFormat {
        val format = MediaFormat().apply {
            setString(MediaFormat.KEY_MIME, config.mimeType)
            setInteger(MediaFormat.KEY_WIDTH, config.width)
            setInteger(MediaFormat.KEY_HEIGHT, config.height)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaFormat.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, config.bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, config.frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, config.iFrameIntervalSeconds)
            setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, config.repeatPreviousFrameAfterUs)
        }
        if (selection.supportedProfiles.contains(config.profile)) {
            format.setInteger(MediaFormat.KEY_PROFILE, config.profile)
        }
        if (selection.supportedBitrateModes.contains(config.bitrateMode)) {
            format.setInteger(MediaFormat.KEY_BITRATE_MODE, config.bitrateMode)
        } else if (selection.supportedBitrateModes.contains(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)) {
            format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
        }
        return format
    }

    private fun buildCallback(): MediaCodec.Callback {
        return object : MediaCodec.Callback() {
            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                statistics.recordError()
                stateReference.set(EncoderState.ERROR)
                sink.onFrameSinkError(e)
            }

            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) = Unit

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                if (getState() == EncoderState.RELEASED) return
                stateReference.set(EncoderState.CONFIGURED)
                val newConfig = mutableMapOf<Int, ByteArray?>()
                var index = 0
                while (true) {
                    val key = "csd-$index"
                    val value = format.getByteBuffer(key) ?: break
                    newConfig[index] = value.toByteArray()
                    index++
                }
                codecConfigData = newConfig
            }

            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                if (getState() == EncoderState.STOPPING || getState() == EncoderState.STOPPED || getState() == EncoderState.RELEASED) {
                    codec.releaseOutputBuffer(index, false)
                    return
                }
                val buffer = codec.getOutputBuffer(index)
                if (buffer == null) {
                    codec.releaseOutputBuffer(index, false)
                    statistics.recordDroppedDelivery()
                    return
                }
                val output = ByteArray(info.size)
                buffer.position(info.offset)
                buffer.limit(info.offset + info.size)
                buffer.get(output)
                codec.releaseOutputBuffer(index, false)

                val isCodecConfig = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                val nalTypes = parser.detectTypes(output)
                val isKey = info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0 ||
                    nalTypes.contains(H264NalUnit.TYPE_IDR)

                val accessUnit = EncodedAccessUnit(
                    data = output,
                    presentationTimeUs = info.presentationTimeUs,
                    flags = info.flags,
                    isCodecConfig = isCodecConfig,
                    isKeyFrame = isKey,
                    endOfStream = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0,
                    nalUnitTypes = nalTypes
                )
                statistics.recordOutput(accessUnit)
                try {
                    sink.onEncodedAccessUnit(accessUnit)
                } catch (_: Exception) {
                    statistics.recordDroppedDelivery()
                }
            }
        }
    }

    private fun cleanupCodec() {
        try {
            mediaCodec?.release()
        } catch (_: Exception) {
        }
        try {
            inputSurface?.release()
        } catch (_: Exception) {
        }
        codecThread?.quitSafely()
        inputSurface = null
        mediaCodec = null
        mediaCodecCallback = null
        codecThread = null
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        val buffer = duplicate()
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        return data
    }
}
