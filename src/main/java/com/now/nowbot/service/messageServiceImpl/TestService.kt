package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import org.springframework.stereotype.Service

@Service("TEST")
class TestService(private val maimaiApiService: MaimaiApiService) :
        MessageService<String> {
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: MessageService.DataValue<String>,
    ): Boolean {
        // if (messageText.contains("!test")) {
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
        val l = maimaiApiService.getMaimaiSongLibrary()

        for (e in l.values) {
            if (e.type == "SD" || e.charts.size < 4) continue

            if (e.star[3] !in 13.0..13.6) continue

            val notes = e.charts[3].notes
            val dx = 3 * (notes.tap + notes.touch + notes.hold + notes.slide + notes.break_)

            if (dx in 2907..3004) println(e.title)
        }
    }
}
