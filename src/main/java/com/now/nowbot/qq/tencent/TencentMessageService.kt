package com.now.nowbot.qq.tencent

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain

interface TencentMessageService<T> {
    // 等同于 isHandle, 如果为空，则不接受
    fun accept(event: MessageEvent, messageText: String): T?

    @Throws(Throwable::class)
    // 等同于 HandleMessage，需要返回一个消息链
    fun reply(event: MessageEvent, data: T): MessageChain?
}