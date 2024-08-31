package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.model.JsonData.BeatMap
import com.now.nowbot.model.JsonData.Match
import com.now.nowbot.model.JsonData.MicroUser
import com.now.nowbot.model.enums.OsuMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.multiplayer.MatchCalculate
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.MessageServiceImpl.MatchMapService.MatchMapParam
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import com.now.nowbot.service.OsuApiService.OsuMatchApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.yumu.core.extensions.isNotNull
import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service("MATCH_MAP")
class MatchMapService(
        private var imageService: ImageService? = null,
        private var beatmapApiService: OsuBeatmapApiService? = null,
        private var matchApiService: OsuMatchApiService? = null
) : MessageService<MatchMapParam>, TencentMessageService<MatchMapParam> {

    @JvmRecord
    data class MatchMapParam(
            val match: Match,
            val position: Int,
    )

    @JvmRecord
    data class PanelE7Param(
            @JvmField val match: MatchCalculate,
            @JvmField val mode: OsuMode,
            @JvmField val mods: MutableList<String>,
            @JvmField val players: MutableList<MicroUser>,
            @JvmField val beatmap: BeatMap,
            @JvmField val density: IntArray,
            @JvmField val original: Map<String, Any>,
    )

    @Throws(Throwable::class)
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: DataValue<MatchMapParam>
    ): Boolean {
        // 这个只能通过父服务 MatchListenerService 调用得到
        // return false

        val matcher = Instruction.TEST_MATCH_START.matcher(messageText)

        if (!matcher.find()) return false

        val match = matchApiService!!.getMatchInfo(matcher.group("id").toLong(), 10)
        val position =
                matcher.group("round").let {
                    if (it.isNullOrEmpty()) {
                        1
                    } else {
                        it.toInt()
                    }
                }

        data.value = MatchMapParam(match, position)

        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: MatchMapParam) {
        val from = event.subject
        val e7Param = getPanelE7Param(param, beatmapApiService)
        var image = byteArrayOf();

        try {
            image = imageService!!.getPanelE7(e7Param)
        } catch (e: Exception) {
            log.error("比赛谱面信息：渲染失败: ", e)
            from.sendMessage(
                    GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "比赛谱面信息")
                            .message)
        }

        try {
            from.sendImage(image)
        } catch (e: Exception) {
            log.error("比赛谱面信息：发送失败: ", e)
            from.sendMessage(
                    GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "比赛谱面信息")
                            .message)
        }

        /*
               if (param.bid == 0L) return

               val beatMap = beatmapApiService!!.getBeatMapInfo(param.bid)

               // 标准化 combo
               var combo: Int

               val maxCombo = beatMap.maxCombo
               combo =
                       if (Objects.nonNull(maxCombo)) {
                           maxCombo
                       } else {
                           99999
                       }

               // 只有转谱才能赋予游戏模式
               val mode: OsuMode
               val beatMapMode = OsuMode.getMode(beatMap.mode)

               mode =
                       if (beatMapMode != OsuMode.OSU && OsuMode.isDefaultOrNull(param.mode)) {
                           beatMapMode
                       } else {
                           param.mode
                       }

               val expected = Expected(mode, 1.0, combo, 0, OsuMod.getModsAbbrList(param.mod))

               try {
                   val image = imageService!!.getPanelE3(param.match, beatMap, expected)
                   from.sendImage(image)
               } catch (e: Exception) {
                   log.error("比赛谱面信息：发送失败: ", e)
                   from.sendMessage(
                           GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "比赛谱面信息")
                                   .message)
               }

        */
    }

    override fun accept(event: MessageEvent, messageText: String): MatchMapParam? {
        return null
    }

    override fun reply(event: MessageEvent, param: MatchMapParam): MessageChain? {
        return null
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MatchMapService::class.java)

        @Throws(Throwable::class)
        @JvmStatic
        fun getPanelE7Param(
                param: MatchMapParam,
                beatmapApiService: OsuBeatmapApiService?
        ): PanelE7Param {
            val calculate = MatchCalculate(param.match, beatmapApiService)

            if (calculate.rounds.isNullOrEmpty() || param.position > calculate.rounds.size)
                    throw GeneralTipsException(GeneralTipsException.Type.G_Null_MatchRound)

            val round = calculate.rounds[param.position - 1]

            var eventID: Long = 0L
            for (e in param.match.events) {
                if (e.round.isNotNull() && e.round.roundID == round.roundID) {
                    eventID = e.eventID
                }
            }

            val beatmap = beatmapApiService!!.getBeatMapInfo(round.beatMapID)

            // 只有转谱才能赋予游戏模式
            val mode: OsuMode
            val beatMapMode = OsuMode.getMode(beatmap.mode)

            mode =
                    if (beatMapMode != OsuMode.OSU ||
                            OsuMode.isDefaultOrNull(OsuMode.getMode(round.mode))) {
                        beatMapMode
                    } else {
                        OsuMode.getMode(round.mode)
                    }

            val original = DataUtil.getOriginal(beatmap)

            beatmapApiService.applySRAndPP(
                    beatmap, mode, OsuMod.getModsValueFromAbbrList(round.mods))

            val density = beatmapApiService.getBeatmapObjectGrouping26(beatmap)

            val players = DataUtil.getPlayersBeforeRoundStart(param.match, eventID)

            return PanelE7Param(calculate, mode, round.mods, players, beatmap, density, original)
        }
    }
}
