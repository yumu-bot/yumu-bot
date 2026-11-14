package com.now.nowbot.service.qqImageApiService

import org.springframework.http.MediaType

/**
 * 图片格式识别与尺寸读取。
 *
 * 素材上传接口要求「文件名扩展名」和「contentType」对得上，而消息里的图片可能是
 * 内存字节、本地文件，也可能是一段没有扩展名的字节流，所以这里统一按文件头判断格式；
 * 顺便读出宽高，供 Markdown 的 `#宽px #高px` 尺寸提示使用。
 */
object QQImageFormat {
    /**
     * 识别结果。
     *
     * @param fileName 带扩展名的文件名, 用于上传时推断 contentType
     * @param contentType 图片 MIME 类型
     */
    data class ImageType(val fileName: String, val contentType: String)

    /**
     * 按文件头识别图片格式, 不支持时返回 null。
     */
    fun detect(bytes: ByteArray): ImageType? {
        fun startsWith(vararg signature: Int) =
            bytes.size >= signature.size && signature.indices.all { bytes[it] == signature[it].toByte() }

        fun ascii(offset: Int, length: Int) =
            if (bytes.size >= offset + length) String(bytes, offset, length, Charsets.US_ASCII) else ""

        return when {
            startsWith(0x89, 0x50, 0x4E, 0x47) -> ImageType("image.png", MediaType.IMAGE_PNG_VALUE)
            startsWith(0xFF, 0xD8, 0xFF) -> ImageType("image.jpg", MediaType.IMAGE_JPEG_VALUE)
            ascii(0, 4) == "GIF8" -> ImageType("image.gif", "image/gif")
            ascii(0, 4) == "RIFF" && ascii(8, 4) == "WEBP" -> ImageType("image.webp", "image/webp")
            else -> null
        }
    }

    /**
     * 读图片宽高, 读不出来返回 null。
     */
    fun size(bytes: ByteArray): Pair<Int, Int>? {
        fun int32be(offset: Int) = ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)

        fun int16le(offset: Int) = (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

        return when {
            // PNG: 8 字节签名, 4 字节长度, "IHDR", 然后宽高各 4 字节大端
            bytes.size >= 24 && (bytes[0].toInt() and 0xFF) == 0x89 &&
                String(bytes, 1, 3, Charsets.US_ASCII) == "PNG" -> int32be(16) to int32be(20)

            // GIF: 逻辑屏幕宽高, 小端
            bytes.size >= 10 && String(bytes, 0, 4, Charsets.US_ASCII) == "GIF8" -> int16le(6) to int16le(8)

            // JPEG: 扫描 SOFn 段
            bytes.size >= 4 && (bytes[0].toInt() and 0xFF) == 0xFF && (bytes[1].toInt() and 0xFF) == 0xD8 ->
                jpegSize(bytes)

            else -> null
        }
    }

    private fun jpegSize(bytes: ByteArray): Pair<Int, Int>? {
        var offset = 2
        while (offset + 9 < bytes.size) {
            if ((bytes[offset].toInt() and 0xFF) != 0xFF) {
                offset++
                continue
            }
            val marker = bytes[offset + 1].toInt() and 0xFF
            // SOF0..SOF15, 排除 DHT(C4) / JPG(C8) / DAC(CC)
            if (marker in 0xC0..0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC) {
                val height = ((bytes[offset + 5].toInt() and 0xFF) shl 8) or (bytes[offset + 6].toInt() and 0xFF)
                val width = ((bytes[offset + 7].toInt() and 0xFF) shl 8) or (bytes[offset + 8].toInt() and 0xFF)
                return if (width > 0 && height > 0) width to height else null
            }
            val length = ((bytes[offset + 2].toInt() and 0xFF) shl 8) or (bytes[offset + 3].toInt() and 0xFF)
            if (length < 2) return null
            offset += 2 + length
        }
        return null
    }
}
