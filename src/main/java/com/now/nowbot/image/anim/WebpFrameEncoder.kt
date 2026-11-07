package com.now.nowbot.image.anim

import dev.matrixlab.webp4j.WebPCodec
import java.awt.image.BufferedImage

object WebpFrameEncoder {
    fun encodeLossy(image: BufferedImage, quality: Int): WebpFrameData {
        if (!WebPCodec.isAvailable()) {
            throw IllegalStateException("当前平台没有可用的 libwebp native 库")
        }
        val bytes = WebPCodec.encodeImage(image, quality.toFloat(), false, false)
        return WebpFrameData(RiffWebpMuxer.extractFrameChunks(bytes), 0)
    }
}
