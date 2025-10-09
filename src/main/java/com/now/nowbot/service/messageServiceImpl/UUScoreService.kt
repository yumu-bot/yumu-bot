package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.CoverType
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.ScoreService.ScoreParam
import com.now.nowbot.service.messageServiceImpl.UUPRService.Companion.getUUScore
import com.now.nowbot.service.messageServiceImpl.UUPRService.Companion.getUUScores
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
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
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("UU_SCORE") class UUScoreService(
    private val scoreApiService: OsuScoreApiService,
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val dao: ServiceCallStatisticsDao
) : MessageService<ScoreParam>, TencentMessageService<ScoreParam> {

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<ScoreParam>,
    ): Boolean {
        val matcher = Instruction.UU_SCORE.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        data.value = getParam(event, messageText, matcher)

        return true
    }

    override fun handleMessage(event: MessageEvent, param: ScoreParam): ServiceCallStatistic? {
        val message = param.getUUMessageChain()

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
        val matcher = OfficialInstruction.UU_SCORE.matcher(messageText)

        if (!matcher.find()) return null

        return getParam(event, messageText, matcher)
    }

    override fun reply(event: MessageEvent, param: ScoreParam): MessageChain? {
        return param.getUUMessageChain()
    }

    private fun getParam(event: MessageEvent, messageText: String, matcher: Matcher): ScoreParam {
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

                user = CmdUtil.getUserWithoutRangeWithBackoff(event, matcher, CmdObject(mode), AtomicBoolean(true), messageText, "score")

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

            val currentMode = CmdObject(inputMode.data)

            if (beforeBeatmapID != null) {
                map = beatmapApiService.getBeatmap(beforeBeatmapID)

                if (!map.hasLeaderBoard) {
                    throw NoSuchElementException.UnrankedBeatmapScore(map.previewName)
                }

                val id = UserIDUtil.getUserIDWithoutRange(event, matcher, currentMode, AtomicBoolean(true))

                user = if (id != null) {
                    userApiService.getOsuUser(id, currentMode.data!!)
                } else {
                    CmdUtil.getUserWithoutRangeWithBackoff(event, matcher, currentMode, AtomicBoolean(true), messageText, "score")
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

                return ScoreParam(user, map, filtered, currentMode.data!!, mods,
                    isMultipleScore = false,
                    isShow = false
                )
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
                user = CmdUtil.getUserWithoutRangeWithBackoff(event, matcher, currentMode, AtomicBoolean(true), messageText, "score")

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

        return ScoreParam(user, map, filtered, mode, mods, isMultipleScore = false, isShow = false)
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

            beatmapApiService.applyBeatmapExtend(s, map)

            val cover = scoreApiService.getCover(s, CoverType.COVER)

            getUUScore(user, s, cover)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ScoreService::class.java)
    }
}
