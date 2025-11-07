package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import org.springframework.stereotype.Service

@Service("TEST")
class TestService() :
        MessageService<String> {
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: MessageService.DataValue<String>,
    ): Boolean {
        /*
        if (messageText.contains("!ymtest")) {
            data.value = messageText
            return true
        } else {
            return false
        }

         */

        return false

    }

    override fun handleMessage(event: MessageEvent, param: String): ServiceCallStatistic? {

        return null
    }
}
