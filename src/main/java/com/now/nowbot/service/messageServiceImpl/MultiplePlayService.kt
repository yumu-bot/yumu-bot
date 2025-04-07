package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue

//@Service("MP")
class MultiplePlayService : MessageService<String> {
    //TODO 这个干啥的？
    override fun isHandle(
        event: MessageEvent, messageText: String, data: DataValue<String>
    ): Boolean {
        return false
    }

    override fun HandleMessage(event: MessageEvent, data: String) {
    }
}
