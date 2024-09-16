package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.DivingFishApiService.MaimaiApiService
import com.now.nowbot.service.MessageService
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service

@Service("MAI_COVER")
@CheckPermission(isSuperAdmin = true)
class TestMaiCoverService(private val maimaiApiService: MaimaiApiService) :
    MessageService<TestMaiCoverService.MaiCoverParam> {

    data class MaiCoverParam(val songID: Long)

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<MaiCoverParam>,
    ): Boolean {
        val matcher = Instruction.MAI_COVER.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        val songID = matcher.group("sid")?.toLong() ?: 0L

        data.value = MaiCoverParam(songID)
        return true
    }

    override fun HandleMessage(event: MessageEvent, param: MaiCoverParam) {
        event.reply(maimaiApiService.getMaimaiCover(param.songID))
    }
}
