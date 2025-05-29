package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import java.lang.StringBuilder
import java.util.regex.Matcher

@Service("TRANS")
class TransService : MessageService<Matcher> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<Matcher>,
    ): Boolean {
        val m = Instruction.TRANS.matcher(messageText)
        if (m.find()) {
            data.value = m
            return true
        } else return false
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: Matcher) {
        val d_index = d1.indexOf(param.group("a"))
        if (d_index <= 0) throw TipsException("输入错误")

        val x = param.group("b").toInt()
        val sb = StringBuilder()

        if (d_index == 2 || d_index == 4 || d_index == 7 || d_index == 9 || d_index == 11) {
            sb.append("降").append(d1.get(d_index + 1))
        } else {
            sb.append(d1.get(d_index))
        }

        sb.append("大调").append('\n')
        for (j in c1) {
            if (12 < d_index + j) {
                sb.append(d1[d_index + j - 12]).append(x + 1).append(' ')
            } else {
                sb.append(d1[d_index + j]).append(x).append(' ')
            }
        }

        event.reply(sb.toString())
    }

    companion object {
        var c1 = intArrayOf(0, 2, 4, 5, 7, 9, 11)
        var d1 =
            mutableListOf(
                "null",
                "C",
                "C#",
                "D",
                "D#",
                "E",
                "F",
                "F#",
                "G",
                "G#",
                "A",
                "A#",
                "B",
            )
    }
}
