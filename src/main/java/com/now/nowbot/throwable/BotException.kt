package com.now.nowbot.throwable

interface BotException {
    val message: String?

    fun hasImage(): Boolean {
        return false
    }

    val image: ByteArray?
        get() = null
}
