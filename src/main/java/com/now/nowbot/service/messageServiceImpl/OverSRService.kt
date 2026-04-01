package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.NewbieRestrictService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.REG_NUMBER_DECIMAL
import org.springframework.stereotype.Service
import kotlin.math.roundToLong

@Service("OVER_SR") class OverSRService : MessageService<Double> {
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<Double>): Boolean {
        val m = Instruction.OVER_SR.matcher(messageText)
        if (m.find()) {
            val starStr: String? = m.group("SR")

            if (starStr.isNullOrBlank() || starStr.matches(REG_NUMBER_DECIMAL.toRegex()).not()) {
                throw IllegalArgumentException.WrongException.Henan()
            }

            val star = starStr.toDoubleOrNull()
                ?: throw IllegalArgumentException.WrongException.StarRating()

            data.value = star
            return true
        } else return false
    }

    @Throws(Throwable::class) override fun handleMessage(event: MessageEvent, param: Double): ServiceCallStatistic? {
        event.reply(getMessage(param))
        return ServiceCallStatistic.building(event) {
            setParam(mapOf(
                "star" to param.toFloat().toString()
            ))
        }
    }

     private fun getMessage(star: Double): String {
        val message = StringBuilder()
         val silence: Long

        if (star < 5.7) {
            throw IllegalArgumentException("未超星。")
        } else if (star < 20.0) { // 超 0.01 星加 10 分钟，6星 以上所有乘以二
            silence = if (star < 6.0) {
                ((star - 5.7) * 1000).roundToLong()
            } else {
                ((star - 5.7) * 2000).roundToLong()
            }
        } else {
            throw IllegalArgumentException.ExceedException.StarRating()
        }

        message.append("已超星，预计禁言：")
         message.append(NewbieRestrictService.getTime(silence))

        return message.toString()
    }
}
