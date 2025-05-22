package com.now.nowbot.throwable

open class TipsRuntimeException : RuntimeException, BotException {
    final override var image: ByteArray? = null

    constructor(msg: String?) : super(msg)

    constructor(image: ByteArray?) {
        this.image = image
    }

    override fun hasImage(): Boolean {
        return image != null
    }
}
