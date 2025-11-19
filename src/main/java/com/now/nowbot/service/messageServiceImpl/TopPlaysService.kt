package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_MODE
import com.now.nowbot.util.command.FLAG_PAGE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.regex.Matcher

@Service("TOP_PLAYS")
class TopPlaysService(
    private val userApiService: OsuUserApiService,
    private val bindDao: BindDao,
    private val imageService: ImageService,
    private val calculateApiService: OsuCalculateApiService,
): MessageService<TopPlaysService.TopPlaysParam> {

    data class TopPlaysParam(
        val page: Int = 1,
        val mode: OsuMode = OsuMode.OSU
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<TopPlaysParam>
    ): Boolean {
        val matcher = Instruction.TOP_PLAYS.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        data.value = getParam(event, matcher)

        return true
    }

    override fun handleMessage(
        event: MessageEvent,
        param: TopPlaysParam
    ): ServiceCallStatistic? {
        val message = param.getMessageChain()

        try {
            event.reply(message)
        } catch (e: Exception) {
            log.error("顶流成绩：发送失败", e)
            throw IllegalStateException.Send("顶流成绩")
        }

        return ServiceCallStatistic.building(event) {
            setParam(
                mapOf(
                    "page" to param.page,
                    "mods" to listOf(param.mode.modeValue),
                )
            )
        }
    }


    private fun getParam(event: MessageEvent, matcher: Matcher): TopPlaysParam {
        val page = matcher.group(FLAG_PAGE)?.toIntOrNull()?.coerceIn(1, 20) ?: 1

        val user = bindDao.getBindFromQQOrNull(event.sender.id)

        val mode = OsuMode.getMode(
            OsuMode.getMode(matcher.group(FLAG_MODE)),
            user?.mode,
            bindDao.getGroupModeConfig(event)
        )

        val nonDefaultMode = if (mode == OsuMode.DEFAULT) OsuMode.OSU else mode

        return TopPlaysParam(page, nonDefaultMode)
    }

    private fun TopPlaysParam.getMessageChain(): MessageChain {

        val top = userApiService.getTopPlays((this.page + 1) / 2, this.mode)

        val isFirstHalf = this.page % 2 == 1

        if (top?.scores.isNullOrEmpty()) {
            throw NoSuchElementException.Score()
        }

        val firstScoreRank = if (isFirstHalf) {
            top.firstScoreRank
        } else {
            top.firstScoreRank + 50
        }

        val scores = if (isFirstHalf) {
            top.scores.take(50)
        } else {
            top.scores.drop(50)
        }

        calculateApiService.applyStarToScores(scores)


        if (scores.isEmpty()) {
            throw NoSuchElementException.Score()
        }

        val image = imageService.getPanel(mapOf(
            "first_score_rank" to firstScoreRank,
            "scores" to scores,
            "page" to this.page,
            "max_page" to 20
        ), "A15")

        return MessageChain(image)
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(TopPlaysService::class.java)
    }
}