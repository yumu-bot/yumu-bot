package com.now.nowbot.image.anim

import dev.matrixlab.webp4j.WebPCodec
import java.awt.image.BufferedImage

object WebpAnimatedReader {
    private const val MAX_FRAMES = 512
    private const val MAX_DIMENSION = 8_192
    private const val MAX_TOTAL_PIXELS = 64_000_000L

    fun read(bytes: ByteArray): AnimatedImage {
        if (!WebPCodec.isAvailable()) {
            throw IllegalStateException("当前平台没有可用的 libwebp native 库")
        }
        if (isAnimated(bytes)) {
            val data = WebPCodec.decodeAnimatedWebP(bytes)
            val frames = data.frames.map { it.image }
            if (frames.isEmpty()) throw IllegalArgumentException("WebP 动画没有帧")
            if (frames.size > MAX_FRAMES) throw IllegalArgumentException("WebP 帧数超出上限: ${frames.size}")
            val width = data.canvasWidth
            val height = data.canvasHeight
            validate(width, height)
            val delays = data.delays.map { if (it <= 0) 100 else it }
            return AnimatedImage(
                width = width,
                height = height,
                frames = frames,
                delaysMs = delays,
                loopCount = data.loopCount,
                hasAlpha = frames.any { hasTransparentPixel(it) },
            )
        }

        val image = WebPCodec.decodeImage(bytes)
        validate(image.width, image.height)
        return AnimatedImage(
            width = image.width,
            height = image.height,
            frames = listOf(image),
            delaysMs = listOf(100),
            loopCount = 0,
            hasAlpha = hasTransparentPixel(image),
        )
    }

    private fun validate(width: Int, height: Int) {
        if (width !in 1..MAX_DIMENSION || height !in 1..MAX_DIMENSION) {
            throw IllegalArgumentException("WebP 画布尺寸超出上限: ${width}x$height")
        }
        if (width.toLong() * height > MAX_TOTAL_PIXELS) {
            throw IllegalArgumentException("WebP 画布像素数超出上限")
        }
    }

    private fun isAnimated(bytes: ByteArray): Boolean {
        if (bytes.size < 21) return false
        if (!matches(bytes, 0, "RIFF") || !matches(bytes, 8, "WEBP")) return false
        var offset = 12
        while (offset + 8 <= bytes.size) {
            val size = readIntLe(bytes, offset + 4)
            if (size < 0 || offset + 8L + size > bytes.size) return false
            val type = String(bytes, offset, 4, Charsets.US_ASCII)
            if (type == "ANIM" || type == "ANMF") return true
            if (type == "VP8X" && size >= 1 && (bytes[offset + 8].toInt() and 0x02) != 0) return true
            offset += 8 + size + (size and 1)
        }
        return false
    }

    private fun matches(bytes: ByteArray, offset: Int, value: String): Boolean {
        if (offset + value.length > bytes.size) return false
        for (i in value.indices) {
            if (bytes[offset + i] != value[i].code.toByte()) return false
        }
        return true
    }

    private fun readIntLe(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    private fun hasTransparentPixel(image: BufferedImage): Boolean {
        if (!image.colorModel.hasAlpha()) return false
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if ((image.getRGB(x, y) ushr 24) != 0xFF) return true
            }
        }
        return false
    }
}
