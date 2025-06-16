package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.CoverType
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.ScoreService.ScoreParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getBid
import com.now.nowbot.util.CmdUtil.getMod
import com.now.nowbot.util.CmdUtil.getMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("SCORE") class ScoreService(
    private val scoreApiService: OsuScoreApiService,
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService,
) : MessageService<ScoreParam>, TencentMessageService<ScoreParam> {

    data class ScoreParam(
        val user: OsuUser,
        val map: Beatmap,
        val scores: List<LazerScore>,
        val mode: OsuMode,
        val mods: List<LazerMod>,
        val isMultipleScore: Boolean,
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<ScoreParam>,
    ): Boolean {
        val m2 = Instruction.SCORES.matcher(messageText)
        val m = Instruction.SCORE.matcher(messageText)

        val isMultipleScore: Boolean

        val matcher: Matcher

        if (m2.find()) {
            matcher = m2
            isMultipleScore = true
        } else if (m.find()) {
            matcher = m
            isMultipleScore = false
        } else {
            return false
        }

        data.value = getParam(event, messageText, matcher, isMultipleScore)

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: ScoreParam) {
        val message = getMessageChain(param)

        try {
            event.reply(message)
        } catch (e: Exception) {
            log.error("谱面成绩：发送失败", e)
            throw IllegalStateException.Send("谱面成绩")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): ScoreParam? {
        val m2 = OfficialInstruction.SCORES.matcher(messageText)
        val m = OfficialInstruction.SCORE.matcher(messageText)

        val isMultipleScore: Boolean
        val matcher: Matcher

        if (m2.find()) {
            matcher = m2
            isMultipleScore = true
        } else if (m.find()) {
            matcher = m
            isMultipleScore = false
        } else {
            return null
        }
        return getParam(event, messageText, matcher, isMultipleScore)
    }

    override fun reply(event: MessageEvent, param: ScoreParam): MessageChain? {
        return getMessageChain(param)
    }

    private fun getParam(event: MessageEvent, messageText: String, matcher: Matcher, isMultipleScore: Boolean): ScoreParam {
        val bid = getBid(matcher)

        val inputMode = getMode(matcher)
        val map: Beatmap
        val user: OsuUser
        val scores: List<LazerScore>
        val mode: OsuMode

        if (bid != 0L) {

            val id = UserIDUtil.getUserIDWithoutRange(event, matcher, inputMode, AtomicBoolean(true))

            map = beatmapApiService.getBeatmap(bid)

            if (!map.hasLeaderBoard) {
                throw NoSuchElementException.UnrankedBeatmapScore(map.previewName)
            }

            if (id != null) {

                mode = OsuMode.getConvertableMode(inputMode.data, map.mode)

                val async = AsyncMethodExecutor.awaitPairWithCollectionSupplierExecute(
                    { userApiService.getOsuUser(id) },
                    { scoreApiService.getBeatMapScores(bid, id, mode) }
                )

                user = async.first
                scores = async.second.toList()
            } else {
                user = CmdUtil.getUserWithoutRangeWithBackoff(event, matcher, inputMode, AtomicBoolean(true), messageText, "score")

                mode = OsuMode.getConvertableMode(inputMode.data, map.mode)

                scores = scoreApiService.getBeatMapScores(bid, user.userID, mode)
            }
        } else {
            // 备用方法：先获取最近成绩，再获取谱面
            val recent: LazerScore

            val currentMode = CmdObject(inputMode.data)

            val id = UserIDUtil.getUserIDWithoutRange(event, matcher, currentMode, AtomicBoolean(true))

            if (id != null) {
                val async = AsyncMethodExecutor.awaitPairWithCollectionSupplierExecute(
                    { userApiService.getOsuUser(id) },
                    { scoreApiService.getRecentScore(id, currentMode.data!!, 0, 1) }
                )

                user = async.first
                recent = async.second.firstOrNull()
                    ?: throw NoSuchElementException.RecentScore(user.username, user.currentOsuMode)

            } else {
                user = CmdUtil.getUserWithoutRangeWithBackoff(event, matcher, currentMode, AtomicBoolean(true), messageText, "score")

                recent = scoreApiService.getRecentScore(user.userID, currentMode.data!!, 0, 1).firstOrNull()
                    ?: throw NoSuchElementException.RecentScore(user.username, user.currentOsuMode)
            }

            map = beatmapApiService.getBeatmap(recent.beatmapID)

            if (!map.hasLeaderBoard) {
                throw NoSuchElementException.UnrankedBeatmapScore(map.previewName)
            }

            mode = OsuMode.getConvertableMode(currentMode.data, map.mode)

            scores = scoreApiService.getBeatMapScores(recent.beatmapID, user.userID, mode)
        }

        if (scores.isEmpty()) {
            throw NoSuchElementException.BeatmapScore(map.previewName)
        }

        val mods = getMod(matcher)

        val filtered = scores.filter { score ->
            score.mods
                .map { it.acronym }
                .union(mods.map { it.acronym }).size == score.mods.size
        }

        if (filtered.isEmpty()) {
            throw NoSuchElementException.BeatmapScoreFiltered(map.previewName)
        }

        return ScoreParam(user, map, filtered, mode, mods, isMultipleScore)
    }

    private fun asyncDownloadBackground(param: ScoreParam) {
        scoreApiService.asyncDownloadBackground(param.scores, listOf(CoverType.COVER, CoverType.LIST))
    }

    private fun getMessageChain(param: ScoreParam): MessageChain {
        val image: ByteArray = if (param.scores.size > 1 && param.isMultipleScore) {
            beatmapApiService.applyBeatmapExtendForSameScore(param.scores, param.map)
            asyncDownloadBackground(param)

            val body = mapOf(
                "user" to param.user,

                "rank" to (1..(param.scores.size)).toList(),
                "score" to param.scores,
                "panel" to "SS"
            )

            imageService.getPanel(body, "A5")
        } else {
            val score = param.scores.first()

            val e5Param = ScorePRService.getE5Param(param.user, score, param.map, null, "S", beatmapApiService, calculateApiService)

            asyncDownloadBackground(param)

            imageService.getPanel(e5Param.toMap(), "E5")
        }

        return QQMsgUtil.getImage(image)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ScoreService::class.java)
    }
}
