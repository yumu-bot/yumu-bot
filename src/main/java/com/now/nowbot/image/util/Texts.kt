package com.now.nowbot.image.util

import java.util.concurrent.ThreadLocalRandom

object Texts {
    private const val RANDOM_CHARS = "ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz2345678"

    fun isBlank(value: Any?): Boolean {
        if (value == null) return true
        return value.toString().isEmpty()
    }

    fun isNotBlank(value: Any?): Boolean = !isBlank(value)

    fun isEmpty(value: Any?): Boolean {
        if (value == null) return true
        return value.toString().isEmpty()
    }

    fun isNotEmpty(value: Any?): Boolean = !isEmpty(value)

    fun isAscii(value: Any?): Boolean {
        if (value is Number) return true
        val text = value?.toString() ?: return false
        if (text.isEmpty()) return false
        for (c in text) {
            if (c.code > 382) return false
        }
        return true
    }

    fun isNumber(value: Any?): Boolean = Numbers.isNumber(value)

    fun isNotNumber(value: Any?): Boolean = !isNumber(value)

    fun random(length: Int = 6): String {
        if (length <= 0) return ""
        val random = ThreadLocalRandom.current()
        return buildString(length) {
            repeat(length) { append(RANDOM_CHARS[random.nextInt(RANDOM_CHARS.length)]) }
        }
    }

    fun padSeconds(value: Double): String = value.toInt().toString().padStart(2, '0')
}
