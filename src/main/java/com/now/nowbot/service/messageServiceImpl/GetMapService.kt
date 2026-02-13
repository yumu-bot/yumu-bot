package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.impl.CalculateApiImpl

import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.InstructionUtil
import com.now.nowbot.util.command.FLAG_ID
import com.now.nowbot.util.command.FLAG_MOD
import com.now.nowbot.util.command.REG_SEPERATOR_NO_SPACE
import org.springframework.stereotype.Service
import java.util.regex.Matcher
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.text.split
import kotlin.text.trim

@Service("GET_MAP")
class GetMapService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
) : MessageService<GetMapService.GetMapParam> {

    abstract class GetMapParam

    data class PoolParam(
        val beatmapID: Long,
        val mod: String?
    ): GetMapParam()

    data class NewbieMapParam(
        val beatmapID: Long,
        val mode: OsuMode,
        val mods: List<LazerMod>
    ): GetMapParam()

    data class NewbiePlayerParam(
        val user: OsuUser,
        val mode: OsuMode,
    ): GetMapParam()

    private fun getPoolParam(matcher: Matcher): PoolParam {
        val bid = matcher.group(FLAG_ID).toLongOrNull() ?: throw IllegalArgumentException.WrongException.BeatmapID()
        val mod = matcher.group(FLAG_MOD)

        return PoolParam(bid, mod)
    }

    private fun getNewbieMapParam(matcher: Matcher): NewbieMapParam {
        val mode = InstructionUtil.getMode(matcher)
        val bid = matcher.group(FLAG_ID).toLongOrNull() ?: throw IllegalArgumentException.WrongException.BeatmapID()
        val mod = InstructionUtil.getMod(matcher)

        return NewbieMapParam(bid, mode.data!!, mod)
    }

    private fun getNewbiePlayerParam(event: MessageEvent, matcher: Matcher): NewbiePlayerParam {

        val mode = InstructionUtil.getMode(matcher)
        val user = InstructionUtil.getUserWithoutRange(event, matcher, mode)

        return NewbiePlayerParam(user, mode.data!!)
    }

    private fun NewbiePlayerParam.getNewbiePlayerComponent(): String {
        return """
            <Player 
              id="${user.userID}"
              name="${user.username}"
              country="${user.countryRank}"
              global="${user.globalRank}"
              from="${user.countryCode}"
              accuracy="${"%.2f".format(user.accuracy).toDouble()}"
              level="${user.levelCurrent}"
              progress="${user.levelProgress}"
              performance="${user.pp.roundToInt()}"
            />
        """.trimIndent()
    }

    private fun NewbieMapParam.getNewbieMapComponent(): String {
        val b = try {
            beatmapApiService.getBeatmap(beatmapID)
        } catch (_: NetworkException.BeatmapException.NotFound) {
            try {
                val s = beatmapApiService.getBeatmapset(beatmapID)

                s.getTopDiff()!!
            } catch (e: NetworkException.BeatmapException) {
                throw e
            }
        }

        calculateApiService.applyStarToBeatmap(b, OsuMode.getConvertableMode(mode, b.mode), mods)

        return """
            <Beatmap
            bid="${b.beatmapID}"
            sid="${b.beatmapsetID}"
            preview="${b.previewName}"
            star="${"%.2f".format(b.starRating)}"
            max="${b.maxCombo}"
            />
        """.trimIndent()

    }

    private fun PoolParam.getMapPoolText(): String {
        val b = beatmapApiService.getBeatmap(beatmapID)
        val sb = StringBuilder()

        sb.append(beatmapID).append(',')
        if (b.beatmapset != null) {
            sb.append(b.beatmapset!!.artistUnicode).append(' ').append('-').append(' ')
            sb.append(b.beatmapset!!.titleUnicode).append(' ')
            sb.append('(').append(b.beatmapset!!.creator).append(')').append(' ')
        }
        sb.append('[').append(b.difficultyName).append(']').append(',')

        if (mod == null || mod.trim {it <= ' '} .isEmpty()) {
            sb.append(String.format("%.2f", b.starRating)).append(',')
                .append(String.format("%d", b.bpm.roundToInt())).append(',')
                .append(String.format("%d", floor((b.totalLength / 60.0)).roundToInt()))
                .append(':')
                .append(String.format("%02d", (b.totalLength % 60f).roundToInt()))
                .append(',')
            sb.append(b.maxCombo).append(',')
                .append(b.cs).append(',')
                .append(b.ar).append(',')
                .append(b.od)

            return sb.toString()
        }

        val mods = LazerMod.getModsList(mod
            .split(REG_SEPERATOR_NO_SPACE.toRegex())
            .dropLastWhile { it.isEmpty() }
        )

        val a = beatmapApiService.getAttributes(beatmapID, b.mode, mods)
        val newTotalLength = CalculateApiImpl.applyLength(b.totalLength, mods).toFloat()

        sb.append(String.format("%.2f", a.starRating)).append(',')
            .append(String.format("%d", CalculateApiImpl.applyBPM(b.bpm, mods).roundToInt())).append(',')
            .append(String.format("%d", floor((newTotalLength / 60.0)).roundToInt()))
            .append(':')
            .append(String.format("%02d", (newTotalLength % 60.0).roundToInt()))
            .append(',')
        sb.append(a.maxCombo).append(',')
            .append(String.format("%.2f", CalculateApiImpl.applyCS(b.cs!!, mods))).append(',')
            .append(String.format("%.2f", CalculateApiImpl.applyAR(b.ar!!, mods))).append(',')
            .append(String.format("%.2f", CalculateApiImpl.applyOD(b.od!!, mods, b.mode)))

        return sb.toString()
    }
    
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<GetMapParam>): Boolean {
        val m = Instruction.GET_MAP.matcher(messageText)
        val m2 = Instruction.GET_NEWBIE_MAP.matcher(messageText)
        val m3 = Instruction.GET_NEWBIE_PLAYER.matcher(messageText)

        if (m.find()) {
            data.value = getPoolParam(m)
            return true
        } else if (m2.find()) {
            data.value = getNewbieMapParam(m2)
            return true
        } else if (m3.find()) {
            data.value = getNewbiePlayerParam(event, m3)
            return true
        }

        return false
    }

    override fun handleMessage(event: MessageEvent, param: GetMapParam): ServiceCallStatistic? {
        when(param) {
            is PoolParam -> event.reply(param.getMapPoolText())
            is NewbieMapParam -> event.reply(param.getNewbieMapComponent())
            is NewbiePlayerParam -> event.reply(param.getNewbiePlayerComponent())
        }

        return null
    }
}
