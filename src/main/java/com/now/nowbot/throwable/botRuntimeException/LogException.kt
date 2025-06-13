package com.now.nowbot.throwable.botRuntimeException

class LogException : RuntimeException {
    var throwable: Throwable? = null

    constructor(msg: String?, throwable: Throwable?) : super(msg) {
        this.throwable = throwable
    }

    constructor(msg: String?) : super(msg)
}
