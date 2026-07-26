package com.avelcam.android.encoder

import android.media.MediaCodecInfo
import android.media.MediaFormat
import kotlin.math.max

data class EncoderConfig(
    val mimeType: String = MediaFormat.MIMETYPE_VIDEO_AVC,
    val width: Int = 1280,
    val height: Int = 720,
    val frameRate: Int = 30,
    val bitrate: Int = 4_000_000,
    val iFrameIntervalSeconds: Int = 1,
    val repeatPreviousFrameAfterUs: Long = -1L,
    val bitrateMode: Int = MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR,
    val profile: Int = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
    val level: Int = MediaCodecInfo.CodecProfileLevel.AVCLevel3,
    val colorFormat: Int = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
) {
    fun validate() {
        require(width > 0) { "Encoder width must be positive." }
        require(height > 0) { "Encoder height must be positive." }
        require(width % 2 == 0) { "Encoder width must be even." }
        require(height % 2 == 0) { "Encoder height must be even." }
        require(frameRate > 0) { "Frame rate must be positive." }
        require(bitrate > 0) { "Bitrate must be positive." }
        require(iFrameIntervalSeconds >= 0) { "I-frame interval must be zero or positive." }
        require(repeatPreviousFrameAfterUs >= 0L || repeatPreviousFrameAfterUs == -1L) {
            "repeatPreviousFrameAfterUs must be >= 0 or -1 to disable."
        }
        require(mimeType.isNotBlank()) { "MIME type is required." }
        require(colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface) {
            "Only Surface input is supported in this phase."
        }
    }

    val frameIntervalUs: Long get() = max(1L, 1_000_000L / frameRate.toLong())
}

