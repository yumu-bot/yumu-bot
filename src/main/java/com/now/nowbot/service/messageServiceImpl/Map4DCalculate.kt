package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.LazerMod
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.Map4DCalculate.Map4DParam
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_MOD
import org.springframework.stereotype.Service

@Service("MAP_4D_CALCULATE")
class Map4DCalculate : MessageService<Map4DParam> {
    @JvmRecord data class Map4DParam(val type: String, val value: Float, val mods: String?)

    @Throws(Throwable::class)
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: DataValue<Map4DParam>,
    ): Boolean {
        val message = event.rawMessage
        val matcher = Instruction.MAP_4D_CALCULATE.matcher(message)
        if (matcher.find()) {
            data.value =
                    Map4DParam(
                            matcher.group("type"),
                            matcher.group("value").toFloat(),
                            matcher.group(FLAG_MOD),
                    )
            return true
        }
        return false
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: Map4DParam) {
        val mod =
                if (param.mods == null) {
                    mutableListOf()
                } else {
                    LazerMod.getModsList(param.mods)
                }
        // 只针对 std 模式
        val message =
                when (param.type) {
                    "ar" -> {
                        val ar = DataUtil.applyAR(param.value, mod)
                        val ms = DataUtil.getMillisFromAR(ar)
                        String.format("AR: %.2f, 缩圈时间: %.2fms", ar, ms)
                    }
                    "od" -> {
                        val od = DataUtil.applyOD(param.value, mod)
                        val ms = DataUtil.getMillisFromOD(od)
                        String.format("OD: %.2f, 300 判定区间: %.2fms", od, ms)
                    }
                    "cs" -> {
                        val cs = DataUtil.applyCS(param.value, mod)
                        String.format("CS: %.2f", cs)
                    }
                    "hp" -> {
                        val hp = DataUtil.applyHP(param.value, mod)
                        String.format("HP: %.2f", hp)
                    }
                    else -> "Unexpected value: " + param.type
                }

        event.reply(message)
    }
}
