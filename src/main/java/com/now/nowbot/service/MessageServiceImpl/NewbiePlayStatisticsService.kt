package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.newbie.mapper.NewbieService
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Service

@Service("NIWBIE_PLAY_STATISTICS")
@ConditionalOnBean(NewbieService::class)
class NewbiePlayStatisticsService(
    val newbieService: NewbieService,
) : MessageService<Any?> {
    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<Any?>): Boolean {
        return false
    }

    override fun HandleMessage(event: MessageEvent, data: Any?) {

    }
}