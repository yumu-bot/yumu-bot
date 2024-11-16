package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.json.Match
import com.now.nowbot.model.json.Match.MatchRound
import com.now.nowbot.model.multiplayer.MatchCalculate
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.MessageChain.MessageChainBuilder
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.MuRatingService.MuRatingParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.serviceException.MatchNowException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service("MATCH_NOW")
class MatchNowService(
        private val beatmapApiService: OsuBeatmapApiService,
        private val calculateApiService: OsuCalculateApiService,
        private val matchApiService: OsuMatchApiService,
        private val imageService: ImageService,
        private val muRatingService: MuRatingService,
) : MessageService<MuRatingParam>, TencentMessageService<MuRatingParam> {

    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: DataValue<MuRatingParam>,
    ): Boolean {
        val m = Instruction.MATCH_NOW.matcher(messageText)
        if (!m.find()) {
            return false
        }

        data.value = muRatingService.getMuRatingParam(m)
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: MuRatingParam) {
        val data = calculate(param, matchApiService, beatmapApiService, calculateApiService)

        val image: ByteArray
        try {
            image = imageService.getPanel(data, "F")
        } catch (e: Exception) {
            log.error("比赛结果：渲染失败")
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "比赛结果")
        }

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("比赛结果：发送失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "比赛结果")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): MuRatingParam? {
        val m = OfficialInstruction.MATCH_NOW.matcher(messageText)
        if (!m.find()) {
            return null
        }

        return muRatingService.getMuRatingParam(m)
    }

    override fun reply(event: MessageEvent, param: MuRatingParam): MessageChain? {
        val data = calculate(param, matchApiService, beatmapApiService, calculateApiService)

        return MessageChainBuilder().addImage(imageService.getPanel(data, "F")).build()
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MatchNowService::class.java)

        @JvmStatic
        @Throws(MatchNowException::class)
        fun calculate(
                param: MuRatingParam,
                matchApiService: OsuMatchApiService,
                beatmapApiService: OsuBeatmapApiService,
                calculateApiService: OsuCalculateApiService,
        ): MatchCalculate {
            val match: Match
            try {
                match = matchApiService.getMatchInfo(param.matchID.toLong(), 10)
            } catch (e: Exception) {
                throw MatchNowException(MatchNowException.Type.MN_Match_NotFound)
            }

            while (match.firstEventID != match.events.first().eventID) {
                val events = matchApiService.getMatchInfo(param.matchID.toLong(), 10).events
                if (events.isEmpty()) throw MatchNowException(MatchNowException.Type.MN_Match_Empty)
                match.events.addAll(0, events)
            }

            if (match.events.size - param.calParam.ignore - param.calParam.skip <= 0) {
                throw MatchNowException(MatchNowException.Type.MN_Match_OutOfBoundsError)
            }

            val c: MatchCalculate
            try {
                c = MatchCalculate(match, param.calParam, beatmapApiService, calculateApiService)

                // 如果只有一两个人，则不排序（slot 从小到大）
                val isSize2p =
                        c.rounds
                                .stream()
                                .filter { s: MatchRound -> s.scores!!.size > 2 }
                                .toList()
                                .isNotEmpty()

                for (r in c.rounds) {
                    val scoreList = r.scores ?: continue

                    if (isSize2p) {
                        r.scores =
                                scoreList
                                        .stream()
                                        .sorted(
                                                Comparator.comparingInt(Match.MatchScore::getScore)
                                                        .reversed()
                                        )
                                        .toList()
                    } else {
                        r.scores =
                                scoreList
                                        .stream()
                                        .sorted(
                                                Comparator.comparingInt { s: Match.MatchScore ->
                                                    s.playerStat.slot
                                                }
                                        )
                                        .toList()
                    }
                }
            } catch (e: Exception) {
                log.error("比赛结果：获取失败", e)
                throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Fetch, "比赛结果")
            }
            return c
        }
    }
}
