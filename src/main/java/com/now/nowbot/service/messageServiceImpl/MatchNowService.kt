package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.match.MatchRating
import com.now.nowbot.model.match.MatchRating.Companion.applyDTMod
import com.now.nowbot.model.match.MatchRating.Companion.insertMicroUserToScores
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.MessageChain.MessageChainBuilder
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.MuRatingService.MuRatingPanelParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuMatchApiService

import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service("MATCH_NOW") class MatchNowService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val matchApiService: OsuMatchApiService,
    private val imageService: ImageService,
) : MessageService<MuRatingPanelParam>, TencentMessageService<MuRatingPanelParam> {

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<MuRatingPanelParam>,
    ): Boolean {
        val m = Instruction.MATCH_NOW.matcher(messageText)
        if (!m.find()) {
            return false
        }

        data.value = MuRatingService.getMuRatingParam(m, matchApiService)
        return true
    }

    @Throws(Throwable::class) override fun handleMessage(event: MessageEvent, param: MuRatingPanelParam) {
        val data = calculate(param, beatmapApiService, calculateApiService)

        val image: ByteArray = imageService.getPanel(data, "F")

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("比赛结果：发送失败", e)
            throw IllegalStateException.Send("比赛结果")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): MuRatingPanelParam? {
        val m = OfficialInstruction.MATCH_NOW.matcher(messageText)
        if (!m.find()) {
            return null
        }

        return MuRatingService.getMuRatingParam(m, matchApiService)
    }

    override fun reply(event: MessageEvent, param: MuRatingPanelParam): MessageChain? {
        val data = calculate(param, beatmapApiService, calculateApiService)

        return MessageChainBuilder().addImage(imageService.getPanel(data, "F")).build()
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MatchNowService::class.java)

        fun calculate(
            panel: MuRatingPanelParam,
            beatmapApiService: OsuBeatmapApiService,
            calculateApiService: OsuCalculateApiService,
        ): MatchRating {

            if (panel.match.events.size - panel.param.ignore - panel.param.skip <= 0) {
                throw TipsException("输入的参数已经过滤掉了所有对局！")
            }

            val mr: MatchRating

            try {
                mr = MatchRating(
                    panel.match, panel.param, beatmapApiService, calculateApiService
                )
                mr.calculate()
                mr.insertMicroUserToScores()
                mr.applyDTMod()

                // 如果只有一两个人，则不排序（slot 从小到大）
                val isSize2p = mr.rounds.any { it.scores.size > 2 }

                for (ro in mr.rounds) {
                    if (ro.scores.isEmpty()) continue

                    if (isSize2p) {
                        ro.scores = ro.scores.sortedByDescending { it.score }
                    } else {
                        ro.scores = ro.scores.sortedBy { it.playerStat!!.slot }
                    }
                }
            } catch (e: Exception) {
                log.error("比赛结果：获取失败", e)
                throw IllegalStateException.Fetch("比赛结果")
            }
            return mr
        }
    }
}
