package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.MaiDao
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import org.springframework.stereotype.Service

@Service("TEST")
class TestService(private val maimaiApiService: MaimaiApiService, private val maiDao: MaiDao) : MessageService<String> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<String>
    ): Boolean {
        //if (messageText.contains("!test")) {
        if (false) {
            data.value = messageText
            return true
        } else {
            return false
        }
        /*

        val matcher = Instruction.MAI_SCORE.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        data.value = matcher.group("name")
        return true
         */

    }

    override fun HandleMessage(event: MessageEvent, text: String) {
        maimaiApiService.updateMaimaiSongLibraryDatabase()
    }
}
