package com.now.nowbot.image.markdown

internal data class PngSize(val width: Int, val height: Int)

internal object MarkdownPng {
    private val SIGNATURE = byteArrayOf(
        0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(),
        0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte(),
    )

    fun size(bytes: ByteArray): PngSize {
        require(bytes.size >= 24) { "PNG 数据过短" }
        for (index in SIGNATURE.indices) {
            require(bytes[index] == SIGNATURE[index]) { "不是有效的 PNG 数据" }
        }
        require(
            bytes[12] == 'I'.code.toByte() &&
                bytes[13] == 'H'.code.toByte() &&
                bytes[14] == 'D'.code.toByte() &&
                bytes[15] == 'R'.code.toByte()
        ) { "PNG 缺少 IHDR" }
        val width = readInt(bytes, 16)
        val height = readInt(bytes, 20)
        require(width > 0 && height > 0) { "PNG 尺寸无效: ${width}x$height" }
        return PngSize(width, height)
    }

    private fun readInt(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
    }
}
