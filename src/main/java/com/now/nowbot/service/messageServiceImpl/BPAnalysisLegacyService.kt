package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.BPAnalysisService.BAParam
import com.now.nowbot.service.messageServiceImpl.BPAnalysisService.Companion.getImage
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithoutRange
import com.now.nowbot.util.Instruction
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
    private val beatmapApiService: OsuBeatmapApiService
) : MessageService<BAParam> {

    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<BAParam>): Boolean {
        val matcher = Instruction.BP_ANALYSIS_LEGACY.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        val isMyself = AtomicBoolean(false)
        val mode = getMode(matcher)
        val user = getUserWithoutRange(event, matcher, mode, isMyself)
        val bpList = scoreApiService.getBestScores(user.userID, mode.data)
        data.value = BAParam(user, bpList, isMyself.get())

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: BAParam) {
        val image = param.getImage(
            1,
            calculateApiService,
            userApiService,
            imageService,
            scoreApiService,
            beatmapApiService
        )

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("最好成绩分析：发送失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "最好成绩分析")
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BPAnalysisLegacyService::class.java)
    }
}