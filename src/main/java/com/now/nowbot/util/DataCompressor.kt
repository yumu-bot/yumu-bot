package com.now.nowbot.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID

object IntArrayCompressor {

    /**
     * 将 IntArray 压缩为字节数组（使用差值 + Variant 编码）
     */
    fun intArrayToByteArray(ints: IntArray): ByteArray {
        if (ints.isEmpty()) return byteArrayOf()

        val out = ByteArrayOutputStream(ints.size * 2)

        // 存储第一个值
        writeVariant(ints[0], out)

        // 存储差值
        var last = ints[0]
        for (i in 1 until ints.size) {
            val delta = ints[i] - last
            writeVariant(delta, out)
            last = ints[i]
        }
        return out.toByteArray()
    }

    /**
     * 从字节数组解压为 IntArray
     */
    fun byteArrayToIntArray(bytes: ByteArray): IntArray {
        if (bytes.isEmpty()) return intArrayOf()

        val `in` = ByteArrayInputStream(bytes)
        var capacity = 256
        var result = IntArray(capacity)
        var size = 0

        // 读取第一个值
        var last = readVariant(`in`)
        result[size++] = last

        // 读取差值并还原
        while (`in`.available() > 0) {
            val delta = readVariant(`in`)
            last += delta

            if (size == capacity) {
                capacity *= 2
                result = result.copyOf(capacity)
            }
            result[size++] = last
        }

        return if (size == result.size) result else result.copyOf(size)
    }

    private fun writeVariant(value: Int, out: ByteArrayOutputStream) {
        var v = value
        while (v and -0x80 != 0) {
            out.write((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        out.write(v and 0x7F)
    }

    private fun readVariant(`in`: ByteArrayInputStream): Int {
        var value = 0
        var shift = 0
        while (true) {
            val b = `in`.read()
            if (b == -1) break
            value = value or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0) break
            shift += 7
        }
        return value
    }
}

object UUIDConverter {
    fun md5ToUUID(md5: String): UUID {
        require(md5.length == 32) { "MD5 length must be 32" }

        // 拼接成标准 UUID 格式: 8-4-4-4-12
        val uuidStr = "${md5.substring(0, 8)}-${md5.substring(8, 12)}-${md5.substring(12, 16)}-${md5.substring(16, 20)}-${md5.substring(20)}"
        return UUID.fromString(uuidStr)
    }

    /**
     * 将 UUID 对象还原为 32 位纯 MD5 字符串
     */
    fun uuidToMD5(uuid: UUID): String {
        return uuid.toString().replace("-", "")
    }
}