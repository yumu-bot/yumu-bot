package com.now.nowbot.service

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent

interface MessageService<T> {
    @Throws(Throwable::class) fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<T>
    ): Boolean

    @Throws(Throwable::class) fun handleMessage(event: MessageEvent, param: T): ServiceCallStatistic?

    class DataValue<T> {
        var value: T? = null
    }
}
