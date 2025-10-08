package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.ChunithmApiService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service

@Service("MAI_UPDATE")
class MaiUpdateService(private val maimaiApiService: MaimaiApiService, private val chunithmApiService: ChunithmApiService) : MessageService<Boolean> {

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<Boolean>
    ): Boolean {
        val matcher = Instruction.MAI_UPDATE.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        if (Permission.isSuperAdmin(event.sender.id)) {
            data.value = true
            return true
        } else return false
    }

    override fun handleMessage(event: MessageEvent, param: Boolean) {
        event.reply("正在尝试更新舞萌、中二数据！")
        maimaiApiService.updateMaimaiSongLibraryDatabase()
        maimaiApiService.updateMaimaiAliasLibraryDatabase()
        maimaiApiService.updateMaimaiRankLibraryDatabase()
        maimaiApiService.updateMaimaiFitLibraryDatabase()
        chunithmApiService.updateChunithmSongLibraryDatabase()
        chunithmApiService.updateChunithmAliasLibraryDatabase()
        event.reply("已尝试更新！")
    }
}
