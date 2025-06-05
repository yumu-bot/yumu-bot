package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.ScorePRService.Companion.getE5Param
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.sbApiService.SBScoreApiService
import com.now.nowbot.service.sbApiService.SBUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.Instruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service("SB_SCORE_PR")
class SBScorePRService(
    private val bindDao: BindDao,
    private val userApiService: SBUserApiService,
    private val scoreApiService: SBScoreApiService,

    private val osuBeatmapApiService: OsuBeatmapApiService,
    private val osuCalculateApiService: OsuCalculateApiService,

    private val imageService: ImageService,
): MessageService<ScorePRService.ScorePRParam> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<ScorePRService.ScorePRParam>
    ): Boolean {
        val matcher = Instruction.SB_SCORE_PR.matcher(messageText)
        if (!matcher.find()) return false

        val isPass =
            if (matcher.group("recent") != null) {
                false
            } else if (matcher.group("pass") != null) {
                true
            } else {
                log.error("成绩分类失败：")
                throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "成绩")
            }

        val mode = getMode(matcher).data!!
        val sb = bindDao.getSBBindFromQQ(event.sender.id, true)

        val user = userApiService.getUser(sb.userID) ?: throw GeneralTipsException(GeneralTipsException.Type.G_Null_Player, sb.username)
        val scores = if (isPass) {
            scoreApiService.getPassedScore(id = sb.userID, mode = mode, limit = 1)
        } else {
            scoreApiService.getRecentScore(id = sb.userID, mode = mode, limit = 1)
        }.mapIndexed { i, it -> i + 1 to it.toLazerScore() }.toMap()

        data.value = ScorePRService.ScorePRParam(user.toOsuUser(mode), scores, isPass = isPass)
        return true
    }

    override fun HandleMessage(event: MessageEvent, param: ScorePRService.ScorePRParam) {
        // 单成绩发送
        val score = param.scores.values.first()

        /*
        scoreApiService.asyncDownloadBackground(score, CoverType.LIST)
        scoreApiService.asyncDownloadBackground(score, CoverType.COVER)

         */

        val e5 = getE5Param(param.user, score, (if (param.isPass) "P" else "R"), osuBeatmapApiService, osuCalculateApiService)

        event.reply(imageService.getPanel(e5.toMap(), "E5"))
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SBScorePRService::class.java)
    }

}