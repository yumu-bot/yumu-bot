package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.sbApiService.SBUserApiService
import org.springframework.stereotype.Service

@Service("SB_BIND")
class SBBindService(
    private val userApiService: SBUserApiService,
    private val bindDao: BindDao,
    ) : MessageService<SBBindService.BindParam> {

    data class BindParam(
        val qq: Long,
        val id: Long?,
        val name: String?,
        val at: Boolean,
        val unbind: Boolean,
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<BindParam>
    ): Boolean {
        return false
    }

    override fun HandleMessage(event: MessageEvent, param: BindParam) {

    }
}