package com.now.nowbot.service

import com.now.nowbot.qq.event.MessageEvent
import org.springframework.lang.NonNull

interface MessageService<T> {
    @Throws(Throwable::class) fun isHandle(
        @NonNull event: MessageEvent,
        @NonNull messageText: String,
        @NonNull data: DataValue<T>
    ): Boolean

    @Throws(Throwable::class) fun handleMessage(@NonNull event: MessageEvent, @NonNull param: T)

    class DataValue<T> {
        var value: T? = null
    }

    /*
    interface MessageService<T> {
    fun isHandle(
        event: MessageEvent, messageText: String
    ): T?

    fun handleMessage(event: MessageEvent, param: T): MessageWithStat?

    data class MessageWithStat(
        val messageChain: MessageChain?,
        val serviceHeritage: ServiceHeritage?,
    )
}
     */
}
