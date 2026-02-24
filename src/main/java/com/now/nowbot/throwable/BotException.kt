package com.now.nowbot.throwable

interface BotException: Throwable {
    override val message: String?

    fun hasImage(): Boolean {
        return false
    }

    val image: ByteArray?
        get() = null
}
