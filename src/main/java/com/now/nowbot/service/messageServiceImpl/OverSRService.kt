package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.NewbieRestrictService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.REGEX_NUMBER_DECIMAL
import org.springframework.stereotype.Service
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

@Service("OVER_SR") class OverSRService : MessageService<Double> {
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<Double>): Boolean {
        val m = Instruction.OVER_SR.matcher(messageText)
        if (m.find()) {
            val starStr: String? = m.group("SR")

            if (starStr.isNullOrBlank() || starStr.trim().matches(REGEX_NUMBER_DECIMAL).not()) {
                throw IllegalArgumentException.WrongException.Henan()
            }

            val star = starStr.toDoubleOrNull()
                ?: throw IllegalArgumentException.WrongException.StarRating()

            data.value = star
            return true
        } else return false
    }

    @Throws(Throwable::class) override fun handleMessage(event: MessageEvent, param: Double): ServiceCallStatistic? {
        event.replyAsync(getMessage(param))
        return ServiceCallStatistic.building(event) {
            setParam(mapOf(
                "star" to param.toFloat().toString()
            ))
        }
    }

     private fun getMessage(star: Double): String {
         val message = StringBuilder()
         val silence: Duration = NewbieRestrictService.getSilence(star)

         if (silence == Duration.ZERO) {
             return "未超星，可以在新人群正常讨论。"
         } else if (star > 30.0) {
             throw IllegalArgumentException.ExceedException.StarRating()
         } else if (silence >= 30.days) {
             return "看起来对方不在乎这条规定。"
         }

         message.append("已超星，预计禁言：")
         message.append(NewbieRestrictService.getTime(silence))

        return message.toString()
    }
}
