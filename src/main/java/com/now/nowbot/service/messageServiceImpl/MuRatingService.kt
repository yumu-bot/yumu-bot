package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.multiplayer.Match
import com.now.nowbot.model.multiplayer.MatchRating
import com.now.nowbot.model.multiplayer.MatchRating.Companion.insertMicroUserToScores
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
import java.util.*
import java.util.regex.Matcher

@Service("MU_RATING") class MuRatingService(
    private val matchApiService: OsuMatchApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService,
) : MessageService<MuRatingService.MuRatingPanelParam>, TencentMessageService<MuRatingService.MuRatingPanelParam> {

    @JvmRecord data class MuRatingPanelParam(
        @JvmField val match: Match,
        @JvmField val param: MatchRating.RatingParam,
        @JvmField val isUU: Boolean = false
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<MuRatingPanelParam>,
    ): Boolean {
        val matcher = Instruction.MU_RATING.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        data.value = getMuRatingParam(matcher, matchApiService)
        return true
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: MuRatingPanelParam) {
        val mr : MatchRating

        try {
            mr = MatchRating(param.match, param.param, beatmapApiService, calculateApiService)
            mr.calculate()
        } catch (e: MRAException) {
            throw e
        } catch (e: Exception) {
            log.error("木斗力：数据计算失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Calculate, "木斗力")
        }

        if (param.isUU) {
            mr.insertMicroUserToScores()
            val str = parseCSA(mr)

            try {
                event.reply(str).recallIn(60000)
            } catch (e: Exception) {
                log.error("木斗力文字版：发送失败", e)
                throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "木斗力")
            }
        } else {
            val image: ByteArray = imageService.getPanel(mr, "C")

            try {
                event.reply(image)
            } catch (e: Exception) {
                log.error("木斗力：发送失败", e)
                throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "木斗力")
            }
        }
    }

    override fun accept(event: MessageEvent, messageText: String): MuRatingPanelParam? {
        val matcher = OfficialInstruction.MU_RATING.matcher(messageText)

        if (!matcher.find()) {
            return null
        }

        return getMuRatingParam(matcher, matchApiService)
    }

    override fun reply(event: MessageEvent, param: MuRatingPanelParam): MessageChain? {
        val mr: MatchRating = calculate(param, beatmapApiService, calculateApiService)
        mr.calculate()
        return MessageChainBuilder().addImage(imageService.getPanel(mr, "C")).build()
    }

    private fun parseCSA(mr: MatchRating): String {
        val data = mr.playerDataList

        // 结果数据
        val sb = StringBuilder()
        sb.append(mr.match.statistics.name).append("\n").append(mr.teamPointMap["red"]).append(" : ")
            .append(mr.teamPointMap["blue"]).append("\n").append("mp").append(mr.match.statistics.matchID).append(" ")
            .append(mr.isTeamVs).append("\n")

        for (p in data) {
            sb.append(
                String.format(
                    "#%d [%.2f] %s (%s)",
                    p.ranking,
                    p.mra,
                    p.player.userName,
                    p.team!!.uppercase(Locale.getDefault()),
                )
            ).append(" ").append(
                    String.format(
                        "%dW-%dL %d%% (%.2fM) [%.2f] [%s | %s]",
                        p.win,
                        p.lose,
                        Math.round(p.win.toDouble() * 100 / (p.win + p.lose)),
                        p.total / 1000000.0,
                        p.rws * 100.0,
                        p.playerClass!!.name,
                        p.playerClass!!.nameCN,
                    )
                ).append("\n\n")
        }
        return sb.toString()
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MuRatingService::class.java)

        @NonNull @Throws(MRAException::class) private fun getEasyMultiplier(matcher: Matcher): Double {
            val easyStr = matcher.group("easy") ?: ""
            var easy = 1.0

            if (easyStr.isNotBlank()) {
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

        @NonNull private fun getIntegers(matcher: Matcher): List<Int> {
            val removeStrArr = matcher.group("remove")
            val remove: MutableList<Int> = mutableListOf()

            if (!removeStrArr.isNullOrBlank()) {
                val split = removeStrArr.split("[\\s,，\\-|:]+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (s in split) {
                    var r: Int
                    try {
                        r = s.toInt()
                        remove.add(r)
                    } catch (ignored: NumberFormatException) {
                    }
                }
            }
            return remove
        }
        /**
         * 提取通用方法：将消息匹配变成 MRA 参数
         *
         * @param matcher 消息匹配
         * @return MRA 参数
         * @throws MRAException 错误
         */
        @Throws(MRAException::class)
        @JvmStatic
        fun getMuRatingParam(matcher: Matcher, matchApiService: OsuMatchApiService): MuRatingPanelParam {
            val matchID: Long
            val matchIDStr = matcher.group("matchid")

            if (matchIDStr.isNullOrBlank()) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_MatchID)
            }

            try {
                matchID = matchIDStr.toLong()
            } catch (e: NumberFormatException) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Exceed_Param)
            }

            val skipStr = matcher.group("skip")
            val ignoreStr = matcher.group("ignore")

            val skip = skipStr?.toInt() ?: 0
            val ignore = ignoreStr?.toInt() ?: 0
            val failed = matcher.group("failed") == null || !matcher.group("failed").equals("f", ignoreCase = true)
            val rematch = matcher.group("rematch") == null || !matcher.group("rematch").equals("r", ignoreCase = true)

            val remove = getIntegers(matcher)
            val easy = getEasyMultiplier(matcher)
            val isUU = matcher.namedGroups().containsKey("uu") && matcher.group("uu") != null

            val match: Match
            try {
                match = matchApiService.getMatchInfo(matchID, 10)
            } catch (e: Exception) {
                throw MRAException(MRAException.Type.RATING_Match_NotFound)
            }

            while (match.firstEventID != match.events.first().eventID) {
                val events = matchApiService.getMatchInfo(matchID, 10).events
                if (events.isEmpty()) throw MRAException(MRAException.Type.RATING_Round_Empty)
                match.events.addAll(0, events)
            }

            return MuRatingPanelParam(match, MatchRating.RatingParam(skip, ignore, remove, easy, failed, rematch), isUU)
        }

        @Throws(MRAException::class) fun calculate(
            matchID: Long,
            skip: Int,
            ignore: Int,
            remove: List<Int>?,
            easy: Double,
            failed: Boolean,
            rematch: Boolean,
            matchApiService: OsuMatchApiService,
            beatmapApiService: OsuBeatmapApiService,
            calculateApiService: OsuCalculateApiService,
        ): MatchRating {
            val match: Match
            try {
                match = matchApiService.getMatchInfo(matchID, 10)
            } catch (e: Exception) {
                throw MRAException(MRAException.Type.RATING_Match_NotFound)
            }

            while (match.firstEventID != match.events.first().eventID) {
                val events = matchApiService.getMatchInfo(matchID, 10).events
                if (events.isEmpty()) throw MRAException(MRAException.Type.RATING_Round_Empty)
                match.events.addAll(0, events)
            }

            val param = MuRatingPanelParam(match, MatchRating.RatingParam(skip, ignore, remove, easy, failed, rematch), false)

            return calculate(param, beatmapApiService, calculateApiService)
        }

        @JvmStatic @Throws(MRAException::class)
        fun calculate(
            panel: MuRatingPanelParam,
            beatmapApiService: OsuBeatmapApiService,
            calculateApiService: OsuCalculateApiService,
        ): MatchRating {
            if (panel.param.skip < 0) throw MRAException(MRAException.Type.RATING_Parameter_SkipError)
            if (panel.param.ignore < 0) throw MRAException(MRAException.Type.RATING_Parameter_SkipEndError)

            val mr = MatchRating(
                panel.match, panel.param, beatmapApiService, calculateApiService
            )

            mr.calculate()

            return mr
        }
    }
}
