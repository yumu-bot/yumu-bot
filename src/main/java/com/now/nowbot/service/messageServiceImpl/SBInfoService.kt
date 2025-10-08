package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.ppysb.SBUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.sbApiService.SBUserApiService
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service

@Service("SB_INFO")
class SBInfoService(private val sbUserApiService: SBUserApiService): MessageService<SBUser> {
    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<SBUser>): Boolean {
        val matcher = Instruction.SB_INFO.matcher(messageText)

        if (!matcher.find()) return false

        data.value = SBUser()
        return true
    }

    override fun handleMessage(event: MessageEvent, param: SBUser): ServiceCallStatistic? {
        event.reply("SB_INFO 正在制作中...即将可用。")

        return null
    }
}