package com.now.nowbot.image.resource

import java.security.SecureRandom
import java.util.Base64

internal object RenderResourceId {
    private const val BYTE_LENGTH = 24
    private val PATTERN = Regex("[A-Za-z0-9_-]{32}")
    private val random = SecureRandom()

    fun next(): String {
        val bytes = ByteArray(BYTE_LENGTH)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun isValid(value: String): Boolean = PATTERN.matches(value)
}
