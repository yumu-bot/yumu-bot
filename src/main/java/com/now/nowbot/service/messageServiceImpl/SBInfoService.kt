package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service

@Service("SB_INFO")
class SBInfoService: MessageService<Boolean> {
    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<Boolean>): Boolean {
        val matcher = Instruction.SB_INFO.matcher(messageText)

        if (!matcher.find()) return false

        data.value = true
        return true
    }

    override fun HandleMessage(event: MessageEvent, param: Boolean) {
        if (param) {
            event.reply("SB_INFO 正在制作中...即将可用。")
        }
    }
}