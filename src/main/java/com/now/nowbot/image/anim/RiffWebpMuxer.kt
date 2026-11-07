package com.now.nowbot.image.anim

import java.io.ByteArrayOutputStream

data class WebpFrameData(val chunks: List<WebpChunk>, val durationMs: Int)

data class WebpChunk(val type: String, val data: ByteArray)

object RiffWebpMuxer {
    private const val MAX_DIMENSION = 1 shl 24
    private const val MAX_DURATION = 1 shl 24
    private const val MAX_LOOP = 1 shl 16

    fun extractFrameChunks(webp: ByteArray): List<WebpChunk> {
        if (!matches(webp, 0, "RIFF") || !matches(webp, 8, "WEBP")) {
            throw IllegalArgumentException("不是 WebP RIFF 文件")
        }
        val chunks = ArrayList<WebpChunk>()
        var offset = 12
        while (offset + 8 <= webp.size) {
            val size = readIntLe(webp, offset + 4)
            if (size < 0 || offset + 8L + size > webp.size) {
                throw IllegalArgumentException("WebP chunk 截断")
            }
            val type = String(webp, offset, 4, Charsets.US_ASCII)
            val data = webp.copyOfRange(offset + 8, offset + 8 + size)
            if (type == "ALPH" || type == "VP8 " || type == "VP8L") {
                chunks += WebpChunk(type, data)
            }
            offset += 8 + size + (size and 1)
        }
        if (chunks.none { it.type == "VP8 " || it.type == "VP8L" }) {
            throw IllegalArgumentException("WebP 缺少 VP8/VP8L 图像数据")
        }
        return chunks
    }

    fun mux(
        width: Int,
        height: Int,
        frames: List<WebpFrameData>,
        loopCount: Int,
        hasAlpha: Boolean,
    ): ByteArray {
        if (width !in 1..MAX_DIMENSION || height !in 1..MAX_DIMENSION) {
            throw IllegalArgumentException("WebP 画布尺寸非法: ${width}x$height")
        }
        if (width.toLong() * height > 0xFFFFFFFFL) {
            throw IllegalArgumentException("WebP 画布像素数超出容器上限")
        }
        if (frames.isEmpty()) throw IllegalArgumentException("WebP 动画没有帧")
        if (loopCount !in 0 until MAX_LOOP) throw IllegalArgumentException("WebP loop 超出 uint16")
        val hasAlphaChunk = hasAlpha || frames.any { frame -> frame.chunks.any { it.type == "ALPH" } }

        val out = ByteArrayOutputStream()
        out.write("RIFF".toByteArray(Charsets.US_ASCII))
        writeIntLe(out, 0)
        out.write("WEBP".toByteArray(Charsets.US_ASCII))

        val vp8x = ByteArrayOutputStream()
        vp8x.write(0x02 or if (hasAlphaChunk) 0x10 else 0)
        vp8x.write(0)
        vp8x.write(0)
        vp8x.write(0)
        writeUInt24(vp8x, width - 1)
        writeUInt24(vp8x, height - 1)
        writeChunk(out, "VP8X", vp8x.toByteArray())

        val anim = ByteArrayOutputStream()
        anim.write(255)
        anim.write(255)
        anim.write(255)
        anim.write(255)
        anim.write(loopCount and 0xFF)
        anim.write((loopCount ushr 8) and 0xFF)
        writeChunk(out, "ANIM", anim.toByteArray())

        for (frame in frames) {
            if (frame.durationMs !in 0 until MAX_DURATION) {
                throw IllegalArgumentException("WebP 帧时长超出 uint24: ${frame.durationMs}")
            }
            val payload = ByteArrayOutputStream()
            writeUInt24(payload, 0)
            writeUInt24(payload, 0)
            writeUInt24(payload, width - 1)
            writeUInt24(payload, height - 1)
            writeUInt24(payload, frame.durationMs)
            payload.write(0x03)
            for (chunk in frame.chunks) {
                writeChunk(payload, chunk.type, chunk.data)
            }
            writeChunk(out, "ANMF", payload.toByteArray())
        }

        val result = out.toByteArray()
        putIntLe(result, 4, result.size - 8)
        return result
    }

    private fun writeChunk(out: ByteArrayOutputStream, type: String, data: ByteArray) {
        require(type.length == 4)
        out.write(type.toByteArray(Charsets.US_ASCII))
        writeIntLe(out, data.size)
        out.write(data)
        if ((data.size and 1) != 0) out.write(0)
    }

    private fun writeUInt24(out: ByteArrayOutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
        out.write((value ushr 16) and 0xFF)
    }

    private fun writeIntLe(out: ByteArrayOutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
        out.write((value ushr 16) and 0xFF)
        out.write((value ushr 24) and 0xFF)
    }

    private fun putIntLe(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = value.toByte()
        bytes[offset + 1] = (value ushr 8).toByte()
        bytes[offset + 2] = (value ushr 16).toByte()
        bytes[offset + 3] = (value ushr 24).toByte()
    }

    private fun readIntLe(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    private fun matches(bytes: ByteArray, offset: Int, value: String): Boolean {
        if (offset + value.length > bytes.size) return false
        for (i in value.indices) {
            if (bytes[offset + i] != value[i].code.toByte()) return false
        }
        return true
    }
}
