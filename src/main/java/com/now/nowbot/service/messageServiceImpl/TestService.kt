package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import org.springframework.stereotype.Service

//@Service("TEST")
class TestService(val userApiService: OsuUserApiService) : MessageService<String> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<String>
    ): Boolean {
        return false
        //data.value = messageText
        //return true
    }

    override fun HandleMessage(event: MessageEvent, param: String) {
        event.reply(userApiService.isPlayerExist(param).toString())
    }

}
