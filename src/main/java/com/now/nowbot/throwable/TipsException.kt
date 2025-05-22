package com.now.nowbot.throwable

open class TipsException : Exception, BotException {
    override var message: String? = null
        get() {
            return if (field != null) {
                field!!
            } else {
                super.message!!
            }
        }
    final override var image: ByteArray? = null

    constructor()

    constructor(message: String?) {
        this.message = message
    }

    constructor(message: String, vararg args: Any?) {
        this.message = String.format(message, *args)
    }

    constructor(image: ByteArray?) {
        this.image = image
    }

    override fun hasImage(): Boolean {
        return image != null
    }
}
