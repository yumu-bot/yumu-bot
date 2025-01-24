package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service

@Service("TEST")
class TestService(private val maimaiApiService: MaimaiApiService) :
        MessageService<String> {
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: MessageService.DataValue<String>,
    ): Boolean {
        /*
        // if (messageText.contains("!test")) {
            if (false) {
            data.value = messageText
            return true
        } else {
            return false
        }

        val matcher = Instruction.MAI_SEARCH.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        data.value = ""
        return true
         */

        return false

    }

    override fun HandleMessage(event: MessageEvent, text: String) {
        val l = maimaiApiService.getMaimaiSongLibrary()

        for (e in l.values) {
            if (e.type == "DX" || e.charts.size < 5) continue

            if (e.star[4] < 14.0) continue

            val notes = e.charts[4].notes
            val dx = 3 * (notes.tap + notes.touch + notes.hold + notes.slide + notes.break_)

            if (dx >= 3600) println(e.title)
        }
    }
}
