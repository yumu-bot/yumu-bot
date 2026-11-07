package com.now.nowbot.image.anim

import java.awt.AlphaComposite
import java.awt.image.BufferedImage
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Separable resampler matching sharp's default resize mapping:
 * downsample Lanczos3, upsample cubic (Catmull-Rom), premultiplied alpha.
 */
object ImageResampler {
    fun fitInside(source: BufferedImage, maxWidth: Int, maxHeight: Int): BufferedImage {
        if (maxWidth <= 0 || maxHeight <= 0) throw IllegalArgumentException("目标尺寸必须为正")
        val scale = min(maxWidth.toDouble() / source.width, maxHeight.toDouble() / source.height)
        val width = max(1, (source.width * scale).roundToInt())
        val height = max(1, (source.height * scale).roundToInt())
        return resize(source, width, height)
    }

    fun resize(source: BufferedImage, width: Int, height: Int): BufferedImage {
        if (width <= 0 || height <= 0) throw IllegalArgumentException("目标尺寸必须为正")
        if (source.width == width && source.height == height) return copyOf(source)

        val pixels = premultiplied(source)
        val horizontal = resampleHorizontal(pixels, source.width, source.height, width)
        val vertical = resampleVertical(horizontal, width, source.height, height)
        return toImage(vertical, width, height)
    }

    private fun premultiplied(source: BufferedImage): FloatArray {
        val argb = argb(source)
        val out = FloatArray(source.width * source.height * 4)
        for (i in 0 until source.width * source.height) {
            val color = argb.getRGB(i % source.width, i / source.width)
            val alpha = (color ushr 24) and 0xFF
            val factor = alpha / 255f
            out[i * 4] = ((color ushr 16) and 0xFF) * factor
            out[i * 4 + 1] = ((color ushr 8) and 0xFF) * factor
            out[i * 4 + 2] = (color and 0xFF) * factor
            out[i * 4 + 3] = alpha.toFloat()
        }
        return out
    }

    private fun resampleHorizontal(
        source: FloatArray,
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
    ): FloatArray {
        val out = FloatArray(targetWidth * sourceHeight * 4)
        val downscale = targetWidth < sourceWidth
        val radius = if (downscale) 3.0 else 2.0
        val filterScale = max(1.0, sourceWidth.toDouble() / targetWidth)
        val support = radius * filterScale
        for (y in 0 until sourceHeight) {
            for (x in 0 until targetWidth) {
                val center = (x + 0.5) * sourceWidth / targetWidth - 0.5
                val start = max(0, ceil(center - support).toInt())
                val end = min(sourceWidth - 1, floor(center + support).toInt())
                var totalWeight = 0.0
                val accum = DoubleArray(4)
                for (sx in start..end) {
                    val distance = (sx - center) / filterScale
                    val weight = if (downscale) lanczos3(distance) else catmullRom(distance)
                    if (weight == 0.0) continue
                    totalWeight += weight
                    val offset = (y * sourceWidth + sx) * 4
                    accum[0] += source[offset] * weight
                    accum[1] += source[offset + 1] * weight
                    accum[2] += source[offset + 2] * weight
                    accum[3] += source[offset + 3] * weight
                }
                if (totalWeight == 0.0) continue
                val offset = (y * targetWidth + x) * 4
                out[offset] = (accum[0] / totalWeight).toFloat()
                out[offset + 1] = (accum[1] / totalWeight).toFloat()
                out[offset + 2] = (accum[2] / totalWeight).toFloat()
                out[offset + 3] = (accum[3] / totalWeight).toFloat()
            }
        }
        return out
    }

    private fun resampleVertical(
        source: FloatArray,
        width: Int,
        sourceHeight: Int,
        targetHeight: Int,
    ): FloatArray {
        val out = FloatArray(width * targetHeight * 4)
        val downscale = targetHeight < sourceHeight
        val radius = if (downscale) 3.0 else 2.0
        val filterScale = max(1.0, sourceHeight.toDouble() / targetHeight)
        val support = radius * filterScale
        for (y in 0 until targetHeight) {
            val center = (y + 0.5) * sourceHeight / targetHeight - 0.5
            val start = max(0, ceil(center - support).toInt())
            val end = min(sourceHeight - 1, floor(center + support).toInt())
            for (x in 0 until width) {
                var totalWeight = 0.0
                val accum = DoubleArray(4)
                for (sy in start..end) {
                    val distance = (sy - center) / filterScale
                    val weight = if (downscale) lanczos3(distance) else catmullRom(distance)
                    if (weight == 0.0) continue
                    totalWeight += weight
                    val offset = (sy * width + x) * 4
                    accum[0] += source[offset] * weight
                    accum[1] += source[offset + 1] * weight
                    accum[2] += source[offset + 2] * weight
                    accum[3] += source[offset + 3] * weight
                }
                if (totalWeight == 0.0) continue
                val offset = (y * width + x) * 4
                out[offset] = (accum[0] / totalWeight).toFloat()
                out[offset + 1] = (accum[1] / totalWeight).toFloat()
                out[offset + 2] = (accum[2] / totalWeight).toFloat()
                out[offset + 3] = (accum[3] / totalWeight).toFloat()
            }
        }
        return out
    }

    private fun toImage(pixels: FloatArray, width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val argb = IntArray(width * height)
        for (i in argb.indices) {
            val alpha = pixels[i * 4 + 3].coerceIn(0f, 255f)
            val factor = if (alpha == 0f) 0f else 255f / alpha
            val red = (pixels[i * 4] * factor).coerceIn(0f, 255f).roundToInt()
            val green = (pixels[i * 4 + 1] * factor).coerceIn(0f, 255f).roundToInt()
            val blue = (pixels[i * 4 + 2] * factor).coerceIn(0f, 255f).roundToInt()
            argb[i] = (alpha.roundToInt() shl 24) or (red shl 16) or (green shl 8) or blue
        }
        image.setRGB(0, 0, width, height, argb, 0, width)
        return image
    }

    private fun lanczos3(x: Double): Double {
        val ax = kotlin.math.abs(x)
        if (ax >= 3.0) return 0.0
        if (ax < 1e-12) return 1.0
        val pix = Math.PI * x
        return (sin(pix) / pix) * (sin(pix / 3.0) / (pix / 3.0))
    }

    private fun catmullRom(x: Double): Double {
        val ax = kotlin.math.abs(x)
        if (ax >= 2.0) return 0.0
        if (ax < 1.0) return 1.5 * ax * ax * ax - 2.5 * ax * ax + 1.0
        return -0.5 * ax * ax * ax + 2.5 * ax * ax - 4.0 * ax + 2.0
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

    private fun argb(source: BufferedImage): BufferedImage {
        if (source.type == BufferedImage.TYPE_INT_ARGB) return source
        return copyOf(source)
    }
}
