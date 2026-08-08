package com.avelcam.android.encoder

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build

data class H264CodecSelection(
    val codecName: String,
    val canonicalName: String?,
    val isHardwareAccelerated: Boolean,
    val isSoftwareOnly: Boolean,
    val isVendor: Boolean,
    val supportedProfiles: Set<Int>,
    val supportedLevels: Set<Int>,
    val supportedBitrateModes: Set<Int>,
    val widthRangeMin: Int,
    val widthRangeMax: Int,
    val heightRangeMin: Int,
    val heightRangeMax: Int,
    val reason: String
)

class H264CodecSelector {
    fun select(config: EncoderConfig): H264CodecSelection {
        var bestMatch: H264CodecSelection? = null
        val list = MediaCodecList(MediaCodecList.ALL_CODECS)

        for (info in list.codecInfos) {
            if (!info.isEncoder) continue
            if (!info.supportedTypes.contains(config.mimeType)) continue
            val caps = info.getCapabilitiesForType(config.mimeType)
            val videoCaps = caps.videoCapabilities ?: continue
            if (!videoCaps.isSizeSupported(config.width, config.height)) continue
            if (!caps.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_AdaptivePlayback)) {
                // optional check; ignore when unsupported
            }
            val supportsSurface = caps.colorFormats.contains(config.colorFormat)
            if (!supportsSurface) continue
            if (config.frameRate > 0 && !videoCaps.areSizeAndRateSupported(
                    config.width,
                    config.height,
                    config.frameRate.toDouble()
                )
            ) {
                continue
            }

            val bitrateModes = mutableSetOf<Int>()
            val bitrateCaps = caps.encoderCapabilities ?: null
            if (bitrateCaps != null) {
                val supported = listOf(
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR,
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR,
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ
                )
                for (mode in supported) {
                    if (bitrateCaps.isBitrateModeSupported(mode)) {
                        bitrateModes.add(mode)
                    }
                }
            }

            val profileLevels = supportedProfileLevelPairs(info, config.mimeType)
            val isHardwareAccelerated = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info.isHardwareAccelerated
            val isSoftwareOnly = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info.isSoftwareOnly
            val isVendor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info.isVendor
            val candidate = H264CodecSelection(
                codecName = info.name,
                canonicalName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) info.canonicalName else null,
                isHardwareAccelerated = isHardwareAccelerated,
                isSoftwareOnly = isSoftwareOnly,
                isVendor = isVendor,
                supportedProfiles = profileLevels.first.toSet(),
                supportedLevels = profileLevels.second.toSet(),
                supportedBitrateModes = bitrateModes,
                widthRangeMin = if (videoCaps.widthAlignment > 0) videoCaps.widthAlignment else config.width,
                widthRangeMax = if (videoCaps.widthAlignment > 0) Int.MAX_VALUE else config.width,
                heightRangeMin = if (videoCaps.heightAlignment > 0) videoCaps.heightAlignment else config.height,
                heightRangeMax = if (videoCaps.heightAlignment > 0) Int.MAX_VALUE else config.height,
                reason = "Compatible encoder for ${config.width}x${config.height} ${config.mimeType}"
            )

            bestMatch = when {
                bestMatch == null -> candidate
                candidate.isHardwareAccelerated && !bestMatch!!.isHardwareAccelerated -> candidate
                candidate.isHardwareAccelerated == bestMatch!!.isHardwareAccelerated &&
                    candidate.supportedBitrateModes.contains(config.bitrateMode) &&
                    !bestMatch!!.supportedBitrateModes.contains(config.bitrateMode) -> candidate
                else -> bestMatch
            }
        }

        return bestMatch ?: throw IllegalStateException("No compatible H.264 encoder found.")
    }

    private fun supportedProfileLevelPairs(
        info: MediaCodecInfo,
        mime: String
    ): Pair<List<Int>, List<Int>> {
        val profiles = mutableListOf<Int>()
        val levels = mutableListOf<Int>()
        try {
            val caps = info.getCapabilitiesForType(mime)
            for (entry in caps.profileLevels) {
                profiles.add(entry.profile)
                levels.add(entry.level)
            }
        } catch (_: Exception) {
        }
        return profiles.toList() to levels.toList()
    }
}
