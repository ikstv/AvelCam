package com.avelcam.android.encoder

class H264NalUnitParser {
    fun detectTypes(sample: ByteArray): Set<Int> {
        if (sample.isEmpty()) return emptySet()
        return extractNalUnits(sample).map { unit ->
            unit.firstOrNull()?.let { it.toInt() and 0x1F } ?: -1
        }.filter { it in 1..31 }.toSet()
    }

    fun extractNalUnits(sample: ByteArray): List<ByteArray> {
        if (sample.size < 5) return emptyList()
        return if (isAvccFormat(sample)) {
            parseAvcc(sample)
        } else {
            parseAnnexB(sample)
        }
    }

    private fun isAvccFormat(sample: ByteArray): Boolean {
        if (sample.size < 5) return false
        if (sample.size >= 3 &&
            sample[0] == 0.toByte() &&
            sample[1] == 0.toByte() &&
            sample[2] == 1.toByte()) {
            return false
        }
        if (sample.size >= 4 &&
            sample[0] == 0.toByte() &&
            sample[1] == 0.toByte() &&
            sample[2] == 0.toByte() &&
            sample[3] == 1.toByte()) {
            return false
        }
        val payloadSize = u32ToInt(sample, 0)
        return payloadSize > 0 && sample.size >= payloadSize + 4
    }

    private fun parseAvcc(sample: ByteArray): List<ByteArray> {
        val units = mutableListOf<ByteArray>()
        var offset = 0
        while (offset + 4 <= sample.size) {
            val length = u32ToInt(sample, offset)
            offset += 4
            if (length <= 0 || offset + length > sample.size) {
                return units
            }
            val payload = sample.copyOfRange(offset, offset + length)
            units.add(payload)
            offset += length
        }
        return units
    }

    private fun parseAnnexB(sample: ByteArray): List<ByteArray> {
        val units = mutableListOf<ByteArray>()
        val startCodes = mutableListOf<Int>()
        var i = 0
        while (i <= sample.size - 3) {
            val isThreeByte = sample[i] == 0.toByte() && sample[i + 1] == 0.toByte() && sample[i + 2] == 1.toByte()
            val isFourByte = i + 3 < sample.size &&
                sample[i] == 0.toByte() &&
                sample[i + 1] == 0.toByte() &&
                sample[i + 2] == 0.toByte() &&
                sample[i + 3] == 1.toByte()
            if (isThreeByte || isFourByte) {
                startCodes.add(i)
                i += if (isFourByte) 4 else 3
                continue
            }
            i++
        }
        if (startCodes.isEmpty()) return emptyList()

        for (idx in startCodes.indices) {
            val start = startCodes[idx]
            val startLength = if (start + 3 < sample.size &&
                sample[start] == 0.toByte() &&
                sample[start + 1] == 0.toByte() &&
                sample[start + 2] == 0.toByte() &&
                sample[start + 3] == 1.toByte()
            ) 4 else 3
            val end = if (idx + 1 < startCodes.size) startCodes[idx + 1] else sample.size
            val nalStart = start + startLength
            if (nalStart < end && end <= sample.size) {
                units.add(sample.copyOfRange(nalStart, end))
            }
        }
        return units
    }

    private fun u24ToInt(data: ByteArray, offset: Int): Int {
        if (offset + 3 >= data.size) return 0
        return ((data[offset].toInt() and 0xFF) shl 16) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            (data[offset + 2].toInt() and 0xFF)
    }

    private fun u32ToInt(data: ByteArray, offset: Int): Int {
        if (offset + 3 >= data.size) return 0
        return ((data[offset].toInt() and 0xFF) shl 24) or
            ((data[offset + 1].toInt() and 0xFF) shl 16) or
            ((data[offset + 2].toInt() and 0xFF) shl 8) or
            (data[offset + 3].toInt() and 0xFF)
    }
}
