package com.now.nowbot.util

@JvmInline
value class DataSize(val bytes: Long) {
    override fun toString(): String = "$bytes Bytes"

    val bytesInt get() = bytes.toInt()

    operator fun plus(other: DataSize): DataSize = DataSize(this.bytes + other.bytes)

    operator fun minus(other: DataSize): DataSize = DataSize(this.bytes - other.bytes)
}

inline val Int.bytes: DataSize get() = DataSize(this.toLong())
inline val Int.KB: DataSize get() = DataSize(this.toLong() shl 10)
inline val Int.MB: DataSize get() = DataSize(this.toLong() shl 20)
inline val Int.GB: DataSize get() = DataSize(this.toLong() shl 30)

inline val Long.bytes: DataSize get() = DataSize(this)
inline val Long.KB: DataSize get() = DataSize(this shl 10)
inline val Long.MB: DataSize get() = DataSize(this shl 20)
inline val Long.GB: DataSize get() = DataSize(this shl 30)