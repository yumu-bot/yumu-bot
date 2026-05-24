package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.BPAnalysisService.BAParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.BeatmapUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.InstructionUtil
import com.now.nowbot.util.UserIDUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("BP_ANALYSIS_LEGACY")
class BPAnalysisLegacyService(
    private val scoreApiService: OsuScoreApiService,
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService,
    private val calculateApiService: OsuCalculateApiService,
    private val bindDao: BindDao,
    private val beatmapApiService: OsuBeatmapApiService
) : MessageService<BAParam> {

    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<BAParam>): Boolean {
        val matcher = Instruction.BP_ANALYSIS_LEGACY.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        data.value = getParam(event, matcher)

        return true
    }


    private fun getParam(event: MessageEvent, matcher: Matcher): BAParam {
        val isMyself = AtomicBoolean(false)
        val mode = InstructionUtil.getMode(matcher)
        val id = UserIDUtil.getUserIDWithoutRange(event, matcher, mode, isMyself)

        // 1. 使用 if 表达式并结合解构声明，集中处理数据的获取逻辑
        val (user, bests) = if (id != null) {
            val m = OsuMode.getMode(mode.data,
                bindDao.getBindModeFromID(id),
                bindDao.getGroupModeConfig(event)
            )

            if (m.isNotDefault()) {
                AsyncMethodExecutor.awaitPair(
                    { userApiService.getOsuUser(id, m) },
                    { scoreApiService.getBestScores(id, m) }
                )
            } else {
                val fetchedUser = userApiService.getOsuUser(id)
                val fetchedBests = scoreApiService.getBestScores(id, fetchedUser.currentOsuMode)
                fetchedUser to fetchedBests
            }
        } else {
            val fetchedUser = InstructionUtil.getUserWithoutRange(event, matcher, mode, isMyself)
            val fetchedBests = scoreApiService.getBestScores(fetchedUser.userID, fetchedUser.currentOsuMode)
            fetchedUser to fetchedBests
        }

        AsyncMethodExecutor.awaitTriple(
            { BeatmapUtil.applyBeatmapChanges(bests) },
            { calculateApiService.applyStarToScores(bests) },
            { beatmapApiService.applyBeatmapExtend(bests) }
        )

        return BAParam(user, bests, isMyself.get(), emptyList(), 1)
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