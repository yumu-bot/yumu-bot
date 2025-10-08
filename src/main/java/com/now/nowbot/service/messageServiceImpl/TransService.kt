package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
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
    override fun handleMessage(event: MessageEvent, param: Matcher): ServiceCallStatistic? {
        val di = d1.indexOf(param.group("a"))
        if (di <= 0) throw TipsException("输入错误")

        val x = param.group("b").toInt()
        val sb = StringBuilder()

        if (di == 2 || di == 4 || di == 7 || di == 9 || di == 11) {
            sb.append("降").append(d1[di + 1])
        } else {
            sb.append(d1[di])
        }

        sb.append("大调").append('\n')
        for (j in c1) {
            if (12 < di + j) {
                sb.append(d1[di + j - 12]).append(x + 1).append(' ')
            } else {
                sb.append(d1[di + j]).append(x).append(' ')
            }
        }

        event.reply(sb.toString())
        return ServiceCallStatistic.building(event)
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
