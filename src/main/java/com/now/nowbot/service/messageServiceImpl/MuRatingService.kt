package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.json.Match
import com.now.nowbot.model.multiplayer.MatchCalculate
import com.now.nowbot.model.multiplayer.MatchCalculate.CalculateParam
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.MessageChain.MessageChainBuilder
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.serviceException.MRAException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.lang.NonNull
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.util.*
import java.util.regex.Matcher

@Service("MU_RATING")
class MuRatingService(
        private val matchApiService: OsuMatchApiService,
        private val beatmapApiService: OsuBeatmapApiService,
        private val calculateApiService: OsuCalculateApiService,
        private val imageService: ImageService,
) : MessageService<MuRatingService.MuRatingParam>, TencentMessageService<MuRatingService.MuRatingParam> {

    @JvmRecord
    data class MuRatingParam(@JvmField val matchID: Int,
                             @JvmField val calParam: CalculateParam,
                             @JvmField val isUU: Boolean = false
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<MuRatingParam>,
    ): Boolean {
        val matcher = Instruction.MU_RATING.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        data.value = getMuRatingParam(matcher)
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: MuRatingParam) {
        val c: MatchCalculate

        try {
            c = calculate(param, matchApiService, beatmapApiService, calculateApiService)
        } catch (e: MRAException) {
            throw e
        } catch (e: Exception) {
            log.error("木斗力：数据计算失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Calculate, "木斗力")
        }

        if (param.isUU) {
            val str = parseCSA(c)

            try {
                event.reply(str).recallIn(60000)
            } catch (e: Exception) {
                log.error("木斗力文字版：发送失败", e)
                throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "木斗力")
            }
        } else {
            val image: ByteArray
            try {
                image = imageService.getPanel(c, "C")
            } catch (e: Exception) {
                log.error("木斗力：渲染失败", e)
                throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "木斗力")
            }

            try {
                event.reply(image)
            } catch (e: Exception) {
                log.error("木斗力：发送失败", e)
                throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "木斗力")
            }
        }
    }

    override fun accept(event: MessageEvent, messageText: String): MuRatingParam? {
        val matcher = OfficialInstruction.MU_RATING.matcher(messageText)

        if (!matcher.find()) {
            return null
        }

        return getMuRatingParam(matcher)
    }

    override fun reply(event: MessageEvent, param: MuRatingParam): MessageChain? {
        val c: MatchCalculate = calculate(param, matchApiService, beatmapApiService, calculateApiService)
        return MessageChainBuilder().addImage(imageService.getPanel(c, "C")).build()
    }

    /**
     * 提取通用方法：将消息匹配变成 MRA 参数
     *
     * @param matcher 消息匹配
     * @return MRA 参数
     * @throws MRAException 错误
     */
    @Throws(MRAException::class)
    fun getMuRatingParam(matcher: Matcher): MuRatingParam {
        val matchID: Int
        val matchIDStr = matcher.group("matchid")

        if (matchIDStr.isNullOrBlank()) throw GeneralTipsException(GeneralTipsException.Type.G_Null_MatchID)

        try {
            matchID = matchIDStr.toInt()
        } catch (e: NumberFormatException) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Exceed_Param)
        }

        val skipStr = matcher.group("skip")
        val ignoreStr = matcher.group("ignore")

        val skip = skipStr?.toInt() ?: 0
        val ignore = ignoreStr?.toInt() ?: 0
        val failed =
                matcher.group("failed") == null ||
                        !matcher.group("failed").equals("f", ignoreCase = true)
        val rematch =
                matcher.group("rematch") == null ||
                        !matcher.group("rematch").equals("r", ignoreCase = true)

        val remove = getIntegers(matcher)
        val easy = getEasyMultiplier(matcher)
        val isUU = matcher.namedGroups().containsKey("uu") && matcher.group("uu") != null

        return MuRatingParam(matchID, CalculateParam(skip, ignore, remove, easy, failed, rematch), isUU)
    }

    private fun parseCSA(c: MatchCalculate): String {
        val data = c.matchData

        // 结果数据
        val sb = StringBuilder()
        sb.append(c.match.matchStat.name)
                .append("\n")
                .append(data.getTeamPointMap()["red"])
                .append(" : ")
                .append(data.getTeamPointMap()["blue"])
                .append("\n")
                .append("mp")
                .append(c.match.matchStat.matchID)
                .append(" ")
                .append(data.isTeamVs)
                .append("\n")

        for (p in data.getPlayerDataMap().values.stream().toList()) {
            sb.append(
                            String.format(
                                    "#%d [%.2f] %s (%s)",
                                    p.ranking,
                                    p.mra,
                                    p.player.userName,
                                    p.team.uppercase(Locale.getDefault()),
                            )
                    )
                    .append(" ")
                    .append(
                            String.format(
                                    "%dW-%dL %d%% (%.2fM) [%.2f] [%s | %s]",
                                    p.win,
                                    p.lose,
                                    Math.round(p.win.toDouble() * 100 / (p.win + p.lose)),
                                    p.total / 1000000.0,
                                    p.rws * 100.0,
                                    p.playerClass.name,
                                    p.playerClass.nameCN,
                            )
                    )
                    .append("\n\n")
        }
        return sb.toString()
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MuRatingService::class.java)

        @NonNull
        @Throws(MRAException::class)
        private fun getEasyMultiplier(matcher: Matcher): Double {
            val easyStr = matcher.group("easy")
            var easy = 1.0

            if (StringUtils.hasText(easyStr)) {
                try {
                    easy = easyStr.toDouble()
                } catch (e: NullPointerException) {
                    throw MRAException(MRAException.Type.RATING_Parameter_EasyError)
                } catch (e: NumberFormatException) {
                    throw MRAException(MRAException.Type.RATING_Parameter_EasyError)
                }
            }

            if (easy > 10.0) throw MRAException(MRAException.Type.RATING_Parameter_EasyTooLarge)
            if (easy < 0.0) throw MRAException(MRAException.Type.RATING_Parameter_EasyTooSmall)
            return easy
        }

        @NonNull
        private fun getIntegers(matcher: Matcher): List<Int> {
            val removeStrArr = matcher.group("remove")
            val remove: MutableList<Int> = mutableListOf()

            if (! removeStrArr.isNullOrBlank()) {
                val split =
                        removeStrArr
                                .split("[\\s,，\\-|:]+".toRegex())
                                .dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                for (s in split) {
                    var r: Int
                    try {
                        r = s.toInt()
                        remove.add(r)
                    } catch (ignored: NumberFormatException) {}
                }
            }
            return remove
        }

        @Throws(MRAException::class)
        fun calculate(
                matchID: Int,
                skip: Int,
                ignore: Int,
                remove: List<Int>?,
                easy: Double,
                failed: Boolean,
                rematch: Boolean,
                matchApiService: OsuMatchApiService,
                beatmapApiService: OsuBeatmapApiService,
                calculateApiService: OsuCalculateApiService,
        ): MatchCalculate {
            val param = MuRatingParam(matchID, CalculateParam(skip, ignore, remove, easy, failed, rematch))

            return calculate(param, matchApiService, beatmapApiService, calculateApiService)
        }

        @JvmStatic
        @Throws(MRAException::class)
        fun calculate(
            param: MuRatingParam,
            matchApiService: OsuMatchApiService,
            beatmapApiService: OsuBeatmapApiService,
            calculateApiService: OsuCalculateApiService,
        ): MatchCalculate {
            if (param.calParam.skip < 0)
                    throw MRAException(MRAException.Type.RATING_Parameter_SkipError)
            if (param.calParam.ignore < 0)
                    throw MRAException(MRAException.Type.RATING_Parameter_SkipEndError)

            val match: Match
            try {
                match = matchApiService.getMatchInfo(param.matchID.toLong(), 10)
            } catch (e: Exception) {
                throw MRAException(MRAException.Type.RATING_Match_NotFound)
            }

            while (match.firstEventID != match.events.first().eventID) {
                val events = matchApiService.getMatchInfo(param.matchID.toLong(), 10).events
                if (events.isEmpty()) throw MRAException(MRAException.Type.RATING_Round_Empty)
                match.events.addAll(0, events)
            }

            return MatchCalculate(match, param.calParam, beatmapApiService, calculateApiService)
        }
    }
}
