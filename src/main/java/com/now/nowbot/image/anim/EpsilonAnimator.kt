package com.now.nowbot.image.anim

import dev.matrixlab.webp4j.WebPCodec
import java.awt.AlphaComposite
import java.awt.image.BufferedImage

object EpsilonAnimator {
    private const val AVATAR_X = 70
    private const val AVATAR_Y = 40
    private const val AVATAR_SIZE = 320
    private const val QUALITY = 90

    fun compose(backgroundWebp: ByteArray, avatarBytes: ByteArray): ByteArray {
        if (!WebPCodec.isAvailable()) {
            throw IllegalStateException("当前平台没有可用的 libwebp native 库")
        }
        val background = WebPCodec.decodeImage(backgroundWebp)
        val avatar = AnimatedImageReader.read(avatarBytes)
        val frames = ArrayList<WebpFrameData>(avatar.frames.size)
        var hasAlpha = false
        for ((index, frame) in avatar.frames.withIndex()) {
            val resized = ImageResampler.fitInside(frame, AVATAR_SIZE, AVATAR_SIZE)
            val canvas = copyOf(background)
            val graphics = canvas.createGraphics()
            try {
                graphics.composite = AlphaComposite.SrcOver
                graphics.drawImage(resized, AVATAR_X, AVATAR_Y, null)
            } finally {
                graphics.dispose()
            }
            hasAlpha = hasAlpha || hasTransparentPixel(canvas)
            val delay = avatar.delaysMs.getOrElse(index) { 100 }.let { if (it <= 0) 100 else it }
            val encoded = WebpFrameEncoder.encodeLossy(canvas, QUALITY)
            frames += encoded.copy(durationMs = delay)
        }
        return RiffWebpMuxer.mux(
            width = background.width,
            height = background.height,
            frames = frames,
            loopCount = 0,
            hasAlpha = hasAlpha,
        )
    }

    private fun copyOf(source: BufferedImage): BufferedImage {
        val copy = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB)
        val graphics = copy.createGraphics()
        try {
            graphics.composite = AlphaComposite.Src
            graphics.drawImage(source, 0, 0, null)
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
