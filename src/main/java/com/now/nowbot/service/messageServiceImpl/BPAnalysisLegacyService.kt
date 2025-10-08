package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.BPAnalysisService.BAParam
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithoutRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.UserIDUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean

@Service("BP_ANALYSIS_LEGACY")
class BPAnalysisLegacyService(
    private val scoreApiService: OsuScoreApiService,
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService,
    private val calculateApiService: OsuCalculateApiService,
) : MessageService<BAParam> {

    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<BAParam>): Boolean {
        val matcher = Instruction.BP_ANALYSIS_LEGACY.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        val isMyself = AtomicBoolean(false)
        val mode = getMode(matcher)

        val user: OsuUser
        val bests: List<LazerScore>

        val id = UserIDUtil.getUserIDWithoutRange(event, matcher, mode, isMyself)

        if (id != null) {
            val async = AsyncMethodExecutor.awaitPairCallableExecute(
                { userApiService.getOsuUser(id, mode.data!!) },
                {
                    val ss = scoreApiService.getBestScores(id, mode.data!!)

                    calculateApiService.applyBeatMapChanges(ss)
                    calculateApiService.applyStarToScores(ss)

                    ss
                }
            )

            user = async.first
            bests = async.second.toList()
        } else {
            user = getUserWithoutRange(event, matcher, mode, isMyself)
            bests = scoreApiService.getBestScores(user.userID, mode.data)

            calculateApiService.applyBeatMapChanges(bests)
            calculateApiService.applyStarToScores(bests)
        }

        val mapperIDs = bests.flatMap { it.beatmap.mapperIDs }.toSet()

        val mappers = userApiService.getUsers(mapperIDs)

        data.value = BAParam(user, bests, isMyself.get(), mappers, 1)

        return true
    }

    override fun handleMessage(event: MessageEvent, param: BAParam): ServiceCallStatistic? {
        val image = imageService.getPanel(param.toMap(), "J")

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("最好成绩分析：发送失败", e)
            throw IllegalStateException.Send("最好成绩分析")
        }

        return ServiceCallStatistic.build(event, userID = param.user.userID, mode = param.user.currentOsuMode)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BPAnalysisLegacyService::class.java)
    }
}