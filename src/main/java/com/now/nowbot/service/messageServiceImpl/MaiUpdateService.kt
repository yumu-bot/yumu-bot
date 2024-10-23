package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service

@Service("MAI_UPDATE")
class MaiUpdateService(private val maimaiApiService: MaimaiApiService) : MessageService<Boolean> {

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<Boolean>
    ): Boolean {
        val matcher = Instruction.MAI_UPDATE.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        data.value = true
        return true
    }

    override fun HandleMessage(event: MessageEvent, update: Boolean) {
        if (Permission.isSuperAdmin(event.sender.id)) {
            event.reply("正在尝试更新！")
            maimaiApiService.updateMaimaiSongLibraryDatabase()
            maimaiApiService.updateMaimaiAliasLibraryDatabase()
            maimaiApiService.updateMaimaiRankLibraryDatabase()
            maimaiApiService.updateMaimaiFitLibraryDatabase()
            event.reply("已尝试更新！")
        }
    }
}
