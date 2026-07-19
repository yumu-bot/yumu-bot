package com.now.nowbot.util

@JvmInline
value class DataSize(val bytes: Long) {
    override fun toString(): String = "$bytes Bytes"

    val bytesInt get() = bytes.toInt()

    operator fun plus(other: DataSize): DataSize = DataSize(this.bytes + other.bytes)

    operator fun minus(other: DataSize): DataSize = DataSize(this.bytes - other.bytes)
}


inline val Long.bytes: DataSize get() = DataSize(this)
inline val Long.KB: DataSize get() = DataSize(this shl 10)
inline val Long.MB: DataSize get() = DataSize(this shl 20)
inline val Long.GB: DataSize get() = DataSize(this shl 30)
inline val Long.TB: DataSize get() = DataSize(this shl 40)

inline val Int.bytes: DataSize get() = this.toLong().bytes
inline val Int.KB: DataSize get() = this.toLong().KB
inline val Int.MB: DataSize get() = this.toLong().MB
inline val Int.GB: DataSize get() = this.toLong().GB
inline val Int.TB: DataSize get() = this.toLong().TB