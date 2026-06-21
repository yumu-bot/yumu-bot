package com.now.nowbot.util

import java.nio.ByteBuffer
import java.nio.ByteOrder

object WebpUtil {

    /**
     * 将普通静态 WebP 的 ByteArray 极速重构为带 Canvas 尺寸声明的单帧 AWebP
     * 无需图像解码，纯字节级拼装，耗时接近 0
     */
    fun extendWebpHeader(src: ByteArray): ByteArray {
        if (src.size < 30) return src

        // 1. 校验基础 WebP 报头
        if (src[0] != 'R'.code.toByte() || src[1] != 'I'.code.toByte() ||
            src[2] != 'F'.code.toByte() || src[3] != 'F'.code.toByte() ||
            src[8] != 'W'.code.toByte() || src[9] != 'E'.code.toByte() ||
            src[10] != 'B'.code.toByte() || src[11] != 'P'.code.toByte()) {
            return src // 不是合法的 WebP，原样返回
        }

        // 2. 读取原始图片的长宽
        val fourCC = String(src, 12, 4)
        val buffer = ByteBuffer.wrap(src).order(ByteOrder.LITTLE_ENDIAN)
        var width: Int
        var height: Int

        when (fourCC) {
            "VP8 " -> {
                width = buffer.getShort(26).toInt() and 0x3FFF
                height = buffer.getShort(28).toInt() and 0x3FFF
            }
            "VP8L" -> {
                val b1 = src[21].toInt() and 0xFF
                val b2 = src[22].toInt() and 0xFF
                val b3 = src[23].toInt() and 0xFF
                val b4 = src[24].toInt() and 0xFF
                width = 1 + (((b2 and 0x3F) shl 8) or b1)
                height = 1 + (((b4 and 0xF) shl 10) or (b3 shl 2) or ((b2 and 0xC0) shr 6))
            }
            "VP8X" -> {
                // 已经是扩展格式了，直接返回无需处理
                return src
            }
            else -> return src
        }

        if (width <= 0 || height <= 0) return src

        // 3. 构建全新的 VP8X 扩展文件头 (共 38 字节)
        // 包含: RIFF头(12B) + VP8X块(18B) + ANIM全局动画控制块(8B)
        val newHeader = ByteArray(38)
        val outBuf = ByteBuffer.wrap(newHeader).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF Header
        outBuf.put("RIFF".toByteArray())
        outBuf.putInt(src.size - 8 + 26) // 全局文件大小修正：原大小 + 新增块大小
        outBuf.put("WEBP".toByteArray())

        // VP8X Chunk (18 字节)
        outBuf.put("VP8X".toByteArray())
        outBuf.putInt(10) // VP8X 数据区长度固定为 10 字节

        // 核心：设置 Flags。第 1 字节的第 2 位(0x02)代表 ANIMation 动图标记
        outBuf.put(0x02.toByte())
        outBuf.put(0x00.toByte())
        outBuf.put(0x00.toByte())
        outBuf.put(0x00.toByte())

        // 写入 Canvas 实际长宽（WebP 规范规定要 -1）
        val wMinus1 = width - 1
        outBuf.put((wMinus1 and 0xFF).toByte())
        outBuf.put(((wMinus1 shr 8) and 0xFF).toByte())
        outBuf.put((0).toByte())

        val hMinus1 = height - 1
        outBuf.put((hMinus1 and 0xFF).toByte())
        outBuf.put(((hMinus1 shr 8) and 0xFF).toByte())
        outBuf.put((0).toByte())

        // ANIM Chunk (8 字节，告诉腾讯这是个 1 帧的动画容器)
        outBuf.put("ANIM".toByteArray())
        outBuf.putInt(6) // ANIM 数据长度为 6
        outBuf.putInt(0) // 背景颜色 RGBA
        outBuf.putShort(0) // 循环次数 (0代表无限循环，强制触发QQ的自适应气泡)

        // 4. 将原始数据的图像内容（剔除原 RIFF 头 12 字节）拼接在新头后面
        val result = ByteArray(newHeader.size + (src.size - 12))
        System.arraycopy(newHeader, 0, result, 0, newHeader.size)
        System.arraycopy(src, 12, result, newHeader.size, src.size - 12)

        return result
    }
}