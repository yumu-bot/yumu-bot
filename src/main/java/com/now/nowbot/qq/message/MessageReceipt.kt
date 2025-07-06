package com.now.nowbot.qq.message

import com.now.nowbot.qq.contact.Contact

abstract class MessageReceipt {
    abstract fun recall()
    abstract fun recallIn(time: Long)
    abstract fun reply(): ReplyMessage?
    abstract fun getTarget(): Contact?
}

class NoneMessageReceipt : MessageReceipt() {
    override fun recall() {
    }

    override fun recallIn(time: Long) {
    }

    override fun reply(): ReplyMessage? {
        return null
    }

    override fun getTarget(): Contact? {
        return null
    }
}
