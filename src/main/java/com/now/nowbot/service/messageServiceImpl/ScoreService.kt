package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.Covers.Companion.CoverType
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
import com.now.nowbot.service.messageServiceImpl.UUPRService.Companion.getUUScore
import com.now.nowbot.service.messageServiceImpl.UUPRService.Companion.getUUScores
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.*
import com.now.nowbot.util.InstructionUtil.getBid
import com.now.nowbot.util.InstructionUtil.getMod
import com.now.nowbot.util.InstructionUtil.getMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("SCORE") class ScoreService(
    private val scoreApiService: OsuScoreApiService,
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService,
    private val dao: ServiceCallStatisticsDao
) : MessageService<ScoreParam>, TencentMessageService<ScoreParam> {

    data class ScoreParam(
        val user: OsuUser,
        val map: Beatmap,
        val scores: List<LazerScore>,
        val mode: OsuMode,
        val mods: List<LazerMod>,
        val isMultipleScore: Boolean,
        val isShow: Boolean = false,
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<ScoreParam>,
    ): Boolean {
        val m3 = Instruction.SCORE_SHOW.matcher(messageText)
        val m2 = Instruction.SCORES.matcher(messageText)
        val m = Instruction.SCORE.matcher(messageText)

        val isMultipleScore: Boolean
        val isShow: Boolean

        val matcher: Matcher

        if (m3.find()) {
            matcher = m3
            isMultipleScore = false
            isShow = true
        } else if (m2.find()) {
            matcher = m2
            isMultipleScore = true
            isShow = false
        } else if (m.find()) {
            matcher = m
            isMultipleScore = false
            isShow = false
        } else {
            return false
        }

        data.value = getParam(event, messageText, matcher, isMultipleScore, isShow)

        return true
    }

    override fun handleMessage(event: MessageEvent, param: ScoreParam): ServiceCallStatistic? {
        val message = param.getMessageChain()

        try {
            event.reply(message)
        } catch (e: Exception) {
            log.error("谱面成绩：发送失败", e)
            throw IllegalStateException.Send("谱面成绩")
        }
        val scores = param.scores.toList()

        return ServiceCallStatistic.builds(
            event,
            beatmapIDs = scores.map { it.beatmapID }.distinct(),
            userIDs = listOf(param.user.userID),
            modes = listOf(param.user.currentOsuMode),
        )
    }

    override fun accept(event: MessageEvent, messageText: String): ScoreParam? {
        val m3 = OfficialInstruction.SCORE_SHOW.matcher(messageText)
        val m2 = OfficialInstruction.SCORES.matcher(messageText)
        val m = OfficialInstruction.SCORE.matcher(messageText)

        val isMultipleScore: Boolean
        val isShow: Boolean

        val matcher: Matcher

        if (m3.find()) {
            matcher = m3
            isMultipleScore = false
            isShow = true
        } else if (m2.find()) {
            matcher = m2
            isMultipleScore = true
            isShow = false
        } else if (m.find()) {
            matcher = m
            isMultipleScore = false
            isShow = false
        } else {
            return null
        }
        return getParam(event, messageText, matcher, isMultipleScore, isShow)
    }

    override fun reply(event: MessageEvent, param: ScoreParam): MessageChain? {
        return param.getMessageChain()
    }

    private fun getParam(event: MessageEvent, messageText: String, matcher: Matcher, isMultipleScore: Boolean, isShow: Boolean): ScoreParam {
        val bid = getBid(matcher)

        val inputMode = getMode(matcher)
        var map: Beatmap
        val user: OsuUser
        val scores: List<LazerScore>
        val mode: OsuMode
        val mods: List<LazerMod>

        if (bid in 1L ..< 10000000L) {
            val id = UserIDUtil.getUserIDWithoutRange(event, matcher, inputMode, AtomicBoolean(true))

            map = beatmapApiService.getBeatmap(bid)

            if (!map.hasLeaderBoard) {
                throw NoSuchElementException.UnrankedBeatmapScore(map.previewName)
            }

            if (id != null) {
                mode = OsuMode.getConvertableMode(inputMode.data, map.mode)

                val async = AsyncMethodExecutor.awaitPairCallableExecute(
                    { userApiService.getOsuUser(id, mode) },
                    { scoreApiService.getBeatmapScores(bid, id, mode) }
                )

                user = async.first
                scores = async.second.toList()
            } else {
                mode = OsuMode.getConvertableMode(inputMode.data, map.mode)

                user = InstructionUtil.getUserWithoutRangeWithBackoff(event, matcher, InstructionObject(mode), AtomicBoolean(true), messageText, "score")

                scores = scoreApiService.getBeatmapScores(bid, user.userID, mode)
            }

            mods = getMod(matcher)
        } else if (bid >= 10000000L) {
            // 输入成绩 ID 的方法
            val score = scoreApiService.getScore(bid)

            val async = AsyncMethodExecutor.awaitPairCallableExecute(
                { userApiService.getOsuUser(score.userID, score.mode) },
                { beatmapApiService.getBeatmap(score.beatmapID) }
            )

            mods = score.mods
            mode = score.mode
            user = async.first
            map = async.second
            scores = listOf(score)
        } else {
            // 进阶备用方法：先获取之前大家使用的 bid，然后尝试获取最近成绩
            val beforeBeatmapID = dao.getLastBeatmapID(
                groupID = event.subject.id,
                name = "SCORE",
                from = LocalDateTime.now().minusMinutes(5L)
            ) ?: dao.getLastBeatmapID(
                groupID = event.subject.id,
                name = null,
                from = LocalDateTime.now().minusHours(24L)
            )

            val currentMode = InstructionObject(inputMode.data)

            if (beforeBeatmapID != null) {
                map = beatmapApiService.getBeatmap(beforeBeatmapID)

                if (!map.hasLeaderBoard) {
                    throw NoSuchElementException.UnrankedBeatmapScore(map.previewName)
                }

                val id = UserIDUtil.getUserIDWithoutRange(event, matcher, currentMode, AtomicBoolean(true))

                user = if (id != null) {
                    userApiService.getOsuUser(id, currentMode.data!!)
                } else {
                    InstructionUtil.getUserWithoutRangeWithBackoff(event, matcher, currentMode, AtomicBoolean(true), messageText, "score")
                }

                mode = OsuMode.getConvertableMode(currentMode.data, map.mode)

                scores = scoreApiService.getBeatmapScores(beforeBeatmapID, user.userID, mode)

                mods = getMod(matcher)

                if (scores.isEmpty()) {
                    throw NoSuchElementException.BeatmapScore(map.previewName)
                }

                val filtered = scores.filter { score ->
                    score.mods
                        .map { it.acronym }
                        .union(mods.map { it.acronym }).size == score.mods.size
                }

                if (filtered.isEmpty()) {
                    throw NoSuchElementException.BeatmapScoreFiltered(map.previewName)
                }

                return ScoreParam(user, map, filtered, currentMode.data!!, mods, isMultipleScore, isShow)
            }

            // 备用方法：先获取最近成绩，再获取谱面

            event.reply("没有获取到 24 小时内的参数。正在为您查询最近成绩所对应的谱面的在线成绩。").recallIn(60 * 1000L)

            val recent: LazerScore

            val id = UserIDUtil.getUserIDWithoutRange(event, matcher, currentMode, AtomicBoolean(true))

            if (id != null) {
                val async = AsyncMethodExecutor.awaitPairCallableExecute(
                    { userApiService.getOsuUser(id, currentMode.data!!) },
                    { scoreApiService.getRecentScore(id, currentMode.data!!, 0, 1) }
                )

                user = async.first
                recent = async.second.firstOrNull()
                    ?: throw NoSuchElementException.RecentScore(user.username, user.currentOsuMode)

            } else {
                user = InstructionUtil.getUserWithoutRangeWithBackoff(event, matcher, currentMode, AtomicBoolean(true), messageText, "score")

                recent = scoreApiService.getRecentScore(user.userID, currentMode.data!!, 0, 1).firstOrNull()
                    ?: throw NoSuchElementException.RecentScore(user.username, user.currentOsuMode)
            }

            map = beatmapApiService.getBeatmap(recent.beatmapID)

            if (!map.hasLeaderBoard) {
                throw NoSuchElementException.UnrankedBeatmapScore(map.previewName)
            }

            mode = OsuMode.getConvertableMode(currentMode.data, map.mode)

            scores = scoreApiService.getBeatmapScores(recent.beatmapID, user.userID, mode)

            mods = getMod(matcher)
        }

        if (scores.isEmpty()) {
            throw NoSuchElementException.BeatmapScore(map.previewName)
        }

        val filtered = scores.filter { score ->
            score.mods
                .map { it.acronym }
                .union(mods.map { it.acronym }).size == score.mods.size
        }

        if (filtered.isEmpty()) {
            throw NoSuchElementException.BeatmapScoreFiltered(map.previewName)
        }

        return ScoreParam(user, map, filtered, mode, mods, isMultipleScore, isShow)
    }

    private fun ScoreParam.asyncDownloadBackground() {
        scoreApiService.asyncDownloadBackgroundFromScores(map, listOf(CoverType.COVER, CoverType.LIST))
    }

    private fun ScoreParam.getMessageChain(): MessageChain {
        return try {
            if (scores.size > 1 && isMultipleScore) {
                beatmapApiService.applyBeatmapExtendForSameScore(scores, map)
                calculateApiService.applyStarToScores(scores)

                asyncDownloadBackground()

                val body = mapOf(
                    "user" to user,

                    "rank" to (1..(scores.size)).toList(),
                    "score" to scores,
                    "panel" to "SS",
                    "compact" to (scores.size >= 10)
                )

                MessageChain(imageService.getPanel(body, "A5"))
            } else {
                val score = scores.first()

                val e5Param = ScorePRService.getE5Param(user, null, score, map, null, "S", beatmapApiService, calculateApiService)

                asyncDownloadBackground()

                MessageChain(imageService.getPanel(e5Param.toMap(), if (isShow) "E10" else "E5"))
            }
        } catch (e: Exception) {
            log.error(e.message)
            return getUUMessageChain()
        }
    }

    private fun ScoreParam.getUUMessageChain(): MessageChain {
        return if (scores.size > 1) {
            val ss = scores.take(5)

            beatmapApiService.applyBeatmapExtendForSameScore(ss, map)

            val covers = scoreApiService.getCovers(ss, CoverType.COVER)

            val pairs = ss.mapIndexed { i, it -> i + 1 to it }

            getUUScores(user, pairs, covers)
        } else {

            val s = scores.first()

            val cover = scoreApiService.getCover(s, CoverType.COVER)

            beatmapApiService.applyBeatmapExtend(s, map)

            getUUScore(user, s, cover)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ScoreService::class.java)
    }
}
