package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.aop.ServiceLimit
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service

@Service("ECHO")
class EchoService : MessageService<String> {
    @Throws(Throwable::class)
    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<String>): Boolean {
        val m = Instruction.ECHO.matcher(messageText)
        if (!m.find()) {
            return false
        }
        data.value = m.group("any") ?: return false
        return true
    }

    @ServiceLimit(limit = 15000)
    @CheckPermission(isSuperAdmin = true)
    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: String): ServiceCallStatistic? {
        event.reply(param)
        return ServiceCallStatistic.building(event)
    }
}
