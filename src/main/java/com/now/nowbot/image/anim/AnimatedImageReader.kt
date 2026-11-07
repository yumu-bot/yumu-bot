package com.now.nowbot.image.anim

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

object AnimatedImageReader {
    fun read(bytes: ByteArray): AnimatedImage {
        if (bytes.isEmpty()) throw IllegalArgumentException("图片数据为空")
        return when {
            isPng(bytes) -> {
                val apng = ApngReader.read(bytes)
                apng ?: staticImage(bytes, "PNG")
            }

            isGif(bytes) -> GifAnimatedReader.read(bytes)
            isWebp(bytes) -> WebpAnimatedReader.read(bytes)
            else -> staticImage(bytes, "图片")
        }
    }

    fun staticImage(bytes: ByteArray, name: String): AnimatedImage {
        val image = ImageIO.read(ByteArrayInputStream(bytes))
            ?: throw IllegalArgumentException("$name 无法被 ImageIO 解码")
        return AnimatedImage(
            width = image.width,
            height = image.height,
            frames = listOf(image),
            delaysMs = listOf(100),
            loopCount = 0,
            hasAlpha = hasTransparentPixel(image),
        )
    }

    private fun isPng(bytes: ByteArray): Boolean =
        bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() &&
            bytes[3] == 0x47.toByte() &&
            bytes[4] == 0x0D.toByte() &&
            bytes[5] == 0x0A.toByte() &&
            bytes[6] == 0x1A.toByte() &&
            bytes[7] == 0x0A.toByte()

    private fun isGif(bytes: ByteArray): Boolean =
        matches(bytes, 0, "GIF87a") || matches(bytes, 0, "GIF89a")

    private fun isWebp(bytes: ByteArray): Boolean =
        matches(bytes, 0, "RIFF") && matches(bytes, 8, "WEBP")

    private fun matches(bytes: ByteArray, offset: Int, value: String): Boolean {
        if (offset + value.length > bytes.size) return false
        for (i in value.indices) {
            if (bytes[offset + i] != value[i].code.toByte()) return false
        }
        return true
    }

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
