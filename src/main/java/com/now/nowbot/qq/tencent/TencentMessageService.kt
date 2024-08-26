package com.now.nowbot.qq.tencent

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain

interface TencentMessageService<T> {
    fun isHandle(event: MessageEvent, messageText: String): T?

    @Throws(Throwable::class)
    fun getReply(event: MessageEvent, data: T): MessageChain?
}