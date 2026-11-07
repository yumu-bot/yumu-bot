package com.now.nowbot.image.anim

import dev.matrixlab.webp4j.gif.GifDecoderJava
import java.awt.AlphaComposite
import java.awt.image.BufferedImage

object GifAnimatedReader {
    private const val MAX_FRAMES = 512
    private const val MAX_DIMENSION = 8_192
    private const val MAX_TOTAL_PIXELS = 64_000_000L

    fun read(bytes: ByteArray): AnimatedImage {
        val data = GifDecoderJava.decodeGif(bytes)
        val frames = data.frames
        if (frames.isEmpty()) throw IllegalArgumentException("GIF 没有帧")
        if (frames.size > MAX_FRAMES) throw IllegalArgumentException("GIF 帧数超出上限: ${frames.size}")

        val width = data.width.takeIf { it > 0 } ?: frames.first().image.width
        val height = data.height.takeIf { it > 0 } ?: frames.first().image.height
        if (width !in 1..MAX_DIMENSION || height !in 1..MAX_DIMENSION) {
            throw IllegalArgumentException("GIF 画布尺寸超出上限: ${width}x$height")
        }
        if (width.toLong() * height > MAX_TOTAL_PIXELS) {
            throw IllegalArgumentException("GIF 画布像素数超出上限")
        }

        val canvas = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val output = ArrayList<BufferedImage>(frames.size)
        val delays = ArrayList<Int>(frames.size)
        var hasAlpha = false
        for (frame in frames) {
            val image = frame.image
            if (image.width > width || image.height > height) {
                throw IllegalArgumentException("GIF 帧尺寸超出画布")
            }
            if (frame.leftOffset < 0 || frame.topOffset < 0) {
                throw IllegalArgumentException("GIF 帧偏移非法")
            }
            if (frame.leftOffset + image.width > width || frame.topOffset + image.height > height) {
                throw IllegalArgumentException("GIF 帧超出画布")
            }
            val before = if (frame.disposeMethod == 3) copyOf(canvas) else null
            val graphics = canvas.createGraphics()
            try {
                graphics.composite = AlphaComposite.SrcOver
                graphics.drawImage(image, frame.leftOffset, frame.topOffset, null)
            } finally {
                graphics.dispose()
            }
            val snapshot = copyOf(canvas)
            output += snapshot
            delays += if (frame.delayMs <= 0) 100 else frame.delayMs
            hasAlpha = hasAlpha || hasTransparentPixel(snapshot)
            when (frame.disposeMethod) {
                0, 1 -> Unit
                2 -> {
                    val clear = canvas.createGraphics()
                    try {
                        clear.composite = AlphaComposite.Clear
                        clear.fillRect(frame.leftOffset, frame.topOffset, image.width, image.height)
                    } finally {
                        clear.dispose()
                    }
                }
                3 -> {
                    val restore = before ?: throw IllegalArgumentException("GIF dispose=previous 缺少快照")
                    val graphicsRestore = canvas.createGraphics()
                    try {
                        graphicsRestore.composite = AlphaComposite.Src
                        graphicsRestore.drawImage(restore, 0, 0, null)
                    } finally {
                        graphicsRestore.dispose()
                    }
                }
                else -> throw IllegalArgumentException("GIF dispose 非法: ${frame.disposeMethod}")
            }
        }
        return AnimatedImage(
            width = width,
            height = height,
            frames = output,
            delaysMs = delays,
            loopCount = data.loopCount,
            hasAlpha = hasAlpha,
        )
    }

    private fun copyOf(image: BufferedImage): BufferedImage {
        val copy = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
        val graphics = copy.createGraphics()
        try {
            graphics.composite = AlphaComposite.Src
            graphics.drawImage(image, 0, 0, null)
        } finally {
            graphics.dispose()
        }
        return copy
    }

    private fun hasTransparentPixel(image: BufferedImage): Boolean {
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if ((image.getRGB(x, y) ushr 24) != 0xFF) return true
            }
        }
        return false
    }
}
