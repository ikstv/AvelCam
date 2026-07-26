package com.avelcam.android.encoder

data class EncodedAccessUnit(
    val data: ByteArray,
    val presentationTimeUs: Long,
    val flags: Int,
    val isCodecConfig: Boolean,
    val isKeyFrame: Boolean,
    val endOfStream: Boolean,
    val nalUnitTypes: Set<Int>
)

