package com.now.nowbot.image.anim

import java.awt.AlphaComposite
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import javax.imageio.ImageIO

/**
 * APNG chunk demux + per-frame PNG reconstruction. PNG decoding itself is delegated to ImageIO.
 */
object ApngReader {
    private val PNG_SIGNATURE = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    )
    private const val MAX_FRAMES = 512
    private const val MAX_DIMENSION = 8_192
    private const val MAX_TOTAL_PIXELS = 64_000_000L

    private data class Chunk(val type: String, val data: ByteArray)

    private data class FrameSpec(
        val width: Int,
        val height: Int,
        val x: Int,
        val y: Int,
        val delayMs: Int,
        val dispose: Int,
        val blend: Int,
        val payload: ByteArray,
    )

    fun read(bytes: ByteArray): AnimatedImage? {
        val chunks = parseChunks(bytes) ?: return null
        val ihdr = chunks.firstOrNull { it.type == "IHDR" }?.data
            ?: throw IllegalArgumentException("APNG 缺少 IHDR")
        if (ihdr.size < 13) throw IllegalArgumentException("APNG IHDR 长度非法")

        val canvasWidth = readInt(ihdr, 0)
        val canvasHeight = readInt(ihdr, 4)
        val bitDepth = ihdr[8].toInt() and 0xFF
        val colorType = ihdr[9].toInt() and 0xFF
        validateDimensions(canvasWidth, canvasHeight)

        val actl = chunks.firstOrNull { it.type == "acTL" }
            ?: return null
        if (actl.data.size < 8) throw IllegalArgumentException("APNG acTL 长度非法")
        val declaredFrames = readInt(actl.data, 0)
        val plays = readInt(actl.data, 4)
        if (declaredFrames !in 1..MAX_FRAMES) {
            throw IllegalArgumentException("APNG 帧数超出上限: $declaredFrames")
        }

        val firstIdat = chunks.indexOfFirst { it.type == "IDAT" }
        val firstFctl = chunks.indexOfFirst { it.type == "fcTL" }
        if (firstIdat < 0) throw IllegalArgumentException("APNG 缺少 IDAT")
        if (firstFctl < 0) throw IllegalArgumentException("APNG 缺少 fcTL")
        if (chunks.indexOfFirst { it.type == "IEND" } < 0) throw IllegalArgumentException("APNG 缺少 IEND")
        val idatIsFirstFrame = firstFctl < firstIdat

        val ancillary = chunks.filter {
            it.type !in setOf("IHDR", "IDAT", "acTL", "fcTL", "fdAT", "IEND")
        }

        var expectedSeq = 0
        val specs = mutableListOf<FrameSpec>()
        var current: MutableFrame? = null

        fun finishCurrent() {
            val frame = current ?: return
            if (frame.payload.size() == 0) throw IllegalArgumentException("APNG 帧缺少图像数据")
            if (specs.size >= MAX_FRAMES) throw IllegalArgumentException("APNG 帧数超出上限")
            validateFrame(frame, canvasWidth, canvasHeight)
            specs += FrameSpec(
                width = frame.width,
                height = frame.height,
                x = frame.x,
                y = frame.y,
                delayMs = delay(frame.delayNum, frame.delayDen),
                dispose = frame.dispose,
                blend = frame.blend,
                payload = frame.payload.toByteArray(),
            )
            current = null
        }

        for (chunk in chunks) {
            when (chunk.type) {
                "fcTL" -> {
                    if (chunk.data.size < 26) throw IllegalArgumentException("APNG fcTL 长度非法")
                    val seq = readInt(chunk.data, 0)
                    requireSequence(seq, expectedSeq)
                    expectedSeq++
                    finishCurrent()
                    current = MutableFrame(
                        width = readInt(chunk.data, 4),
                        height = readInt(chunk.data, 8),
                        x = readInt(chunk.data, 12),
                        y = readInt(chunk.data, 16),
                        delayNum = readShort(chunk.data, 20),
                        delayDen = readShort(chunk.data, 22),
                        dispose = chunk.data[24].toInt() and 0xFF,
                        blend = chunk.data[25].toInt() and 0xFF,
                    )
                }

                "fdAT" -> {
                    if (chunk.data.size < 4) throw IllegalArgumentException("APNG fdAT 长度非法")
                    val seq = readInt(chunk.data, 0)
                    requireSequence(seq, expectedSeq)
                    expectedSeq++
                    val frame = current ?: throw IllegalArgumentException("APNG fdAT 前缺少 fcTL")
                    frame.payload.write(chunk.data, 4, chunk.data.size - 4)
                }

                "IDAT" -> {
                    val frame = current
                    if (idatIsFirstFrame && specs.isEmpty() && frame != null) {
                        frame.payload.write(chunk.data)
                    }
                }
            }
        }
        finishCurrent()

        if (specs.size != declaredFrames) {
            throw IllegalArgumentException("APNG acTL 帧数与 fcTL 数不一致: $declaredFrames/${specs.size}")
        }
        if (idatIsFirstFrame) {
            val first = specs.firstOrNull() ?: throw IllegalArgumentException("APNG 没有可解码帧")
            if (first.width != canvasWidth || first.height != canvasHeight || first.x != 0 || first.y != 0) {
                throw IllegalArgumentException("APNG 首帧 IDAT 的 fcTL 必须是全画布")
            }
            if (first.payload.isEmpty()) throw IllegalArgumentException("APNG 首帧 IDAT 缺少数据")
        }

        val canvas = BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB)
        val frames = ArrayList<BufferedImage>(specs.size)
        val delays = ArrayList<Int>(specs.size)
        var hasAlpha = false
        for (spec in specs) {
            val frameImage = decodeFrame(spec, ihdr, bitDepth, colorType, ancillary)
            val before = if (spec.dispose == 2) copyOf(canvas) else null
            val graphics = canvas.createGraphics()
            try {
                graphics.composite = if (spec.blend == 0) {
                    AlphaComposite.Src
                } else {
                    AlphaComposite.SrcOver
                }
                graphics.drawImage(frameImage, spec.x, spec.y, null)
            } finally {
                graphics.dispose()
            }
            val output = copyOf(canvas)
            frames += output
            delays += spec.delayMs
            hasAlpha = hasAlpha || hasTransparentPixel(output)
            when (spec.dispose) {
                0 -> Unit
                1 -> {
                    val graphicsClear = canvas.createGraphics()
                    try {
                        graphicsClear.composite = AlphaComposite.Clear
                        graphicsClear.fillRect(spec.x, spec.y, spec.width, spec.height)
                    } finally {
                        graphicsClear.dispose()
                    }
                }
                2 -> {
                    val restore = before ?: throw IllegalArgumentException("APNG dispose=2 缺少快照")
                    val graphicsRestore = canvas.createGraphics()
                    try {
                        graphicsRestore.composite = AlphaComposite.Src
                        graphicsRestore.drawImage(restore, 0, 0, null)
                    } finally {
                        graphicsRestore.dispose()
                    }
                }
                else -> throw IllegalArgumentException("APNG dispose_op 非法: ${spec.dispose}")
            }
        }
        return AnimatedImage(
            width = canvasWidth,
            height = canvasHeight,
            frames = frames,
            delaysMs = delays,
            loopCount = plays,
            hasAlpha = hasAlpha,
        )
    }

    private data class MutableFrame(
        val width: Int,
        val height: Int,
        val x: Int,
        val y: Int,
        val delayNum: Int,
        val delayDen: Int,
        val dispose: Int,
        val blend: Int,
        val payload: ByteArrayOutputStream = ByteArrayOutputStream(),
    )

    private fun validateFrame(frame: MutableFrame, canvasWidth: Int, canvasHeight: Int) {
        if (frame.width <= 0 || frame.height <= 0) throw IllegalArgumentException("APNG 帧尺寸非法")
        if (frame.x < 0 || frame.y < 0) throw IllegalArgumentException("APNG 帧偏移非法")
        if (frame.x + frame.width > canvasWidth || frame.y + frame.height > canvasHeight) {
            throw IllegalArgumentException("APNG 帧超出画布")
        }
        if (frame.dispose !in 0..2) throw IllegalArgumentException("APNG dispose_op 非法: ${frame.dispose}")
        if (frame.blend !in 0..1) throw IllegalArgumentException("APNG blend_op 非法: ${frame.blend}")
        if (frame.width.toLong() * frame.height > MAX_TOTAL_PIXELS) {
            throw IllegalArgumentException("APNG 帧像素数超出上限")
        }
    }

    private fun decodeFrame(
        spec: FrameSpec,
        ihdr: ByteArray,
        bitDepth: Int,
        colorType: Int,
        ancillary: List<Chunk>,
    ): BufferedImage {
        val png = ByteArrayOutputStream()
        png.write(PNG_SIGNATURE)
        val frameIhdr = ihdr.copyOf()
        putInt(frameIhdr, 0, spec.width)
        putInt(frameIhdr, 4, spec.height)
        writeChunk(png, "IHDR", frameIhdr)
        for (chunk in ancillary) {
            writeChunk(png, chunk.type, chunk.data)
        }
        writeChunk(png, "IDAT", spec.payload)
        writeChunk(png, "IEND", ByteArray(0))
        val image = ImageIO.read(ByteArrayInputStream(png.toByteArray()))
            ?: throw IllegalArgumentException("APNG 帧无法被 ImageIO 解码")
        if (image.width != spec.width || image.height != spec.height) {
            throw IllegalArgumentException("APNG 帧解码尺寸不一致")
        }
        return image
    }

    private fun parseChunks(bytes: ByteArray): List<Chunk>? {
        if (bytes.size < PNG_SIGNATURE.size + 12) return null
        for (i in PNG_SIGNATURE.indices) {
            if (bytes[i] != PNG_SIGNATURE[i]) return null
        }
        var offset = PNG_SIGNATURE.size
        val chunks = ArrayList<Chunk>()
        var hasActl = false
        while (offset + 12 <= bytes.size) {
            val length = readInt(bytes, offset)
            if (length < 0 || offset + 12L + length > bytes.size) {
                throw IllegalArgumentException("APNG chunk 截断")
            }
            val type = String(bytes, offset + 4, 4, Charsets.US_ASCII)
            val dataStart = offset + 8
            val data = bytes.copyOfRange(dataStart, dataStart + length)
            val expectedCrc = readInt(bytes, dataStart + length)
            val crc = CRC32()
            crc.update(bytes, offset + 4, 4 + length)
            if (crc.value.toInt() != expectedCrc) {
                throw IllegalArgumentException("APNG chunk CRC 错误: $type")
            }
            chunks += Chunk(type, data)
            if (type == "acTL") {
                if (hasActl) throw IllegalArgumentException("APNG acTL 重复")
                hasActl = true
            }
            offset = dataStart + length + 4
        }
        if (offset != bytes.size) throw IllegalArgumentException("APNG chunk 截断")
        if (chunks.isEmpty() || chunks.first().type != "IHDR") return null
        return chunks
    }

    private fun writeChunk(out: ByteArrayOutputStream, type: String, data: ByteArray) {
        require(type.length == 4)
        out.write(byteArrayOf(type[0].code.toByte(), type[1].code.toByte(), type[2].code.toByte(), type[3].code.toByte()))
        writeInt(out, data.size)
        out.write(data)
        val crc = CRC32()
        crc.update(type.toByteArray(Charsets.US_ASCII))
        crc.update(data)
        writeInt(out, crc.value.toInt())
    }

    private fun readInt(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)

    private fun readShort(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)

    private fun putInt(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value ushr 24).toByte()
        bytes[offset + 1] = (value ushr 16).toByte()
        bytes[offset + 2] = (value ushr 8).toByte()
        bytes[offset + 3] = value.toByte()
    }

    private fun writeInt(out: ByteArrayOutputStream, value: Int) {
        out.write(value ushr 24)
        out.write(value ushr 16)
        out.write(value ushr 8)
        out.write(value)
    }

    private fun requireSequence(actual: Int, expected: Int) {
        if (actual != expected) {
            throw IllegalArgumentException("APNG 序列号乱序: $actual != $expected")
        }
    }

    private fun validateDimensions(width: Int, height: Int) {
        if (width !in 1..MAX_DIMENSION || height !in 1..MAX_DIMENSION) {
            throw IllegalArgumentException("APNG 画布尺寸超出上限: ${width}x$height")
        }
        if (width.toLong() * height > MAX_TOTAL_PIXELS) {
            throw IllegalArgumentException("APNG 画布像素数超出上限")
        }
    }

    private fun delay(num: Int, den: Int): Int {
        val denominator = if (den == 0) 100 else den
        val ms = Math.round(1000.0 * num / denominator).toInt()
        return if (ms <= 0) 100 else ms
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
