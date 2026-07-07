package com.now.nowbot.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID

@Entity
@Table(
    name = "beatmap_count",
)
class BeatmapCountLite(
    @Id
        @Column(name = "id")
    val beatmapID: Long,

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "hash", columnDefinition = "uuid")
    var hash: UUID? = null,

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "delta")
    var delta: ByteArray? = null,

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "density", columnDefinition = "int4[]")
    var density: IntArray? = null
) {

    fun readTimestamps(): IntArray? {
        return delta?.let { TimestampConverter.bytesToIntArray(it) }
    }

    fun writeTimestamps(ints: IntArray?) {
        this.delta = ints?.let { TimestampConverter.intArrayToBytes(it) }
    }

    fun readHash(): String? {
        return hash?.let { UUIDConverter.uuidToMD5(it) }
    }

    fun writeHash(hash: String?) {
        this.hash = hash?.let { UUIDConverter.md5ToUUID(it) }
    }

    interface TimeResult {
        val beatmapID: Long
        val delta: ByteArray
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

object TimestampConverter {


    /**
     * 将绝对时间戳数组转为【Variant 差值字节数组】
     */
    fun intArrayToBytes(ints: IntArray): ByteArray {
        if (ints.isEmpty()) return byteArrayOf()

        // 使用高效的字节流缓冲
        val out = ByteArrayOutputStream(ints.size * 2) // 预估平均每个物件 2 字节

        // 1. 第一个数存绝对值（作为基准）
        writeVariant(ints[0], out)

        // 2. 后续所有物件存与前一个物件的【差值】
        var last = ints[0]
        for (i in 1 until ints.size) {
            val current = ints[i]
            val delta = current - last
            writeVariant(delta, out)
            last = current
        }
        return out.toByteArray()
    }

    /**
     * 从【Variant 差值字节数组】还原为绝对时间戳 IntArray
     */
    fun bytesToIntArray(bytes: ByteArray): IntArray {
        if (bytes.isEmpty()) return intArrayOf()

        val `in` = ByteArrayInputStream(bytes)

        // 优化：为了避免 List<Int> 自动装箱带来的性能损耗
        // 我们采用一个动态扩容的临时 Int 数组
        var capacity = 256
        var result = IntArray(capacity)
        var size = 0

        // 1. 读取第一个绝对值
        var last = readVariant(`in`)
        result[size++] = last

        // 2. 循环读取后面的差值并还原
        while (`in`.available() > 0) {
            val delta = readVariant(`in`)
            last += delta // 差值累加，还原为绝对时间戳

            // 动态扩容
            if (size == capacity) {
                capacity *= 2
                result = result.copyOf(capacity)
            }
            result[size++] = last
        }

        // 裁剪到实际大小返回
        return if (size == result.size) result else result.copyOf(size)
    }

    /**
     * Variant 编码（7位压缩法）
     * 将一个 Int 根据大小写入 1 ~ 5 个字节
     */
    private fun writeVariant(value: Int, out: ByteArrayOutputStream) {
        var v = value
        // 只要高 25 位还有数据，就说明需要继续写下一个字节
        while (v and -0x80 != 0) {
            // 取低 7 位，并将最高位（第8位）设为 1，表示后面还有字节
            out.write((v and 0x7F) or 0x80)
            v = v ushr 7 // 右移 7 位
        }
        // 最后一个字节，最高位为 0，表示结束
        out.write(v and 0x7F)
    }

    /**
     * Variant 解码
     */
    private fun readVariant(`in`: ByteArrayInputStream): Int {
        var value = 0
        var shift = 0
        while (true) {
            val b = `in`.read()
            if (b == -1) break
            value = value or ((b and 0x7F) shl shift)
            // 如果最高位是 0，说明这个数字读完了
            if ((b and 0x80) == 0) break
            shift += 7
        }
        return value
    }
}