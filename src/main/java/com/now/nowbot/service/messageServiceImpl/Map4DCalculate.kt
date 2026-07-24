package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.Companion.toOsuMode
import com.now.nowbot.model.osu.LazerMod.Companion.toLazerMods
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.Map4DCalculate.Map4DParam
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.util.BeatmapUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_ANY
import com.now.nowbot.util.command.FLAG_MOD
import com.now.nowbot.util.command.FLAG_MODE
import com.now.nowbot.util.command.FLAG_TYPE
import com.now.nowbot.util.command.REGEX_SPACE_MORE
import org.springframework.stereotype.Service

@Service("MAP_4D_CALCULATE")
class Map4DCalculate : MessageService<Map4DParam> {
    @JvmRecord data class Map4DParam(val type: String, val value: Float, val mods: List<LazerMod>, val mode: OsuMode)

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
                    matcher.group(FLAG_TYPE) ?: "ar",
                    matcher.group(FLAG_ANY).toFloatOrNull() ?: throw IllegalArgumentException.WrongException.DimensionValue(),
                    matcher.group(FLAG_MOD).toLazerMods(),
                    matcher.group(FLAG_MODE).toOsuMode(OsuMode.OSU),
                )
            return true
        }
        return false
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: Map4DParam): ServiceCallStatistic? {
        val (type, value, mods, mode) = param

        val message =
            when (type.trim().split(REGEX_SPACE_MORE).firstOrNull()) {
                "ar", "approach", "rate", "a", "r" -> {
                    val ar = BeatmapUtil.applyAR(value, mods)
                    val ms = BeatmapUtil.getMillisFromAR(ar)
                    String.format("AR: %.2f, 缩圈时间: %.2fms", ar, ms)
                }

                "od", "overall", "diff", "difficulty", "o", "d" -> {
                    // TODO 这里要赋予游戏模式，太鼓和下落式的 OD 不一样
                    val od = BeatmapUtil.applyOD(value, mods, mode)
                    val ms = BeatmapUtil.getMillisFromOD(od, mode)
                    String.format("OD: %.2f, 300 判定区间: %.2fms", od, ms)
                }

                "cs", "circle", "size", "c", "s" -> {
                    val cs = BeatmapUtil.applyCS(value, mods, mode)
                    String.format("CS: %.2f", cs)
                }

                "hp", "h", "p" -> {
                    val hp = BeatmapUtil.applyHP(value, mods)
                    String.format("HP: %.2f", hp)
                }

                else -> throw IllegalArgumentException.WrongException.Dimension()
            }

        event.replyAsync(message)

        return ServiceCallStatistic.building(event)
    }
}
