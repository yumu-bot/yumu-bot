package com.now.nowbot.service.messageServiceImpl

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
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithoutRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.UserIDUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
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
            val deferred = scope.async {
                userApiService.getOsuUser(id, mode.data!!)
            }

            val deferred2 = scope.async {
                val ss = scoreApiService.getBestScores(id, mode.data!!, 0, 200)

                calculateApiService.applyBeatMapChanges(ss)
                calculateApiService.applyStarToScores(ss)

                ss
            }

            runBlocking {
                user = deferred.await()
                bests = deferred2.await()
            }
        } else {
            user = getUserWithoutRange(event, matcher, mode, isMyself)
            bests = scoreApiService.getBestScores(user.userID, mode.data, 0, 200)

            calculateApiService.applyBeatMapChanges(bests)
            calculateApiService.applyStarToScores(bests)
        }

        val mappers = userApiService.getUsers(bests.map { it.beatmap.mapperID }.toSet())

        data.value = BAParam(user, bests, isMyself.get(), mappers, 1)

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: BAParam) {
        val image = imageService.getPanel(param.toMap(), "J")

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("最好成绩分析：发送失败", e)
            throw IllegalStateException.Send("最好成绩分析")
        }
    }

    companion object {
        private val scope = CoroutineScope(Dispatchers.IO.limitedParallelism(4))
        private val log: Logger = LoggerFactory.getLogger(BPAnalysisLegacyService::class.java)
    }
}