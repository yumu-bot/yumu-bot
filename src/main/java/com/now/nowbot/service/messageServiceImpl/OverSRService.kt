package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.REG_NUMBER_DECIMAL
import org.springframework.stereotype.Service
import kotlin.math.roundToInt

@Service("OVER_SR") class OverSRService : MessageService<Double> {
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<Double>): Boolean {
        val m = Instruction.OVER_SR.matcher(messageText)
        if (m.find()) {
            val starStr = m.group("SR")

            if (starStr.matches(REG_NUMBER_DECIMAL.toRegex()).not()) {
                throw IllegalArgumentException.WrongException.Henan()
            }
            val star = starStr.toDoubleOrNull()
                ?: throw IllegalArgumentException.WrongException.StarRating()

            data.value = star
            return true
        } else return false
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: Double) {
        event.reply(getMessage(param))
    }

    @Throws(GeneralTipsException::class) private fun getMessage(star: Double): String {
        val message = StringBuilder()
        val silence: Int

        if (star < 5.7) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Success_NotOverRating)
        } else if (star < 20.0) { // 超 0.01 星加 10 分钟，6星 以上所有乘以二
            silence = if (star < 6.0) {
                ((star - 5.7) * 1000).roundToInt()
            } else {
                ((star - 5.7) * 2000).roundToInt()
            }
        } else {
            throw IllegalArgumentException.ExceedException.StarRating()
        }

        message.append("已超星，预计禁言：")
        message.append(getSilenceMessage(silence))

        return message.toString()
    }

    companion object {
        private fun getSilenceMessage(silence: Int): String {
            return if (silence >= 1440) {
                val day = silence / 1440
                val hour = (silence - (day * 1440)) / 60
                val minute = silence - (day * 1440) - (hour * 60)

                "${day}天${hour}时${minute}分"
            } else if (silence >= 60.0) {
                val hour = silence / 60
                val minute = silence - (hour * 60)

                "${hour}时${minute}分"
            } else {
                "${silence}分"
            }
        }
    }
}
