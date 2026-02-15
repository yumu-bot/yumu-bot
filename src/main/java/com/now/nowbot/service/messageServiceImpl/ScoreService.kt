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
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher
import kotlin.time.DurationUnit
import kotlin.time.toDuration

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
        val isCompact: Boolean = false,
        val isRecordCallStatistics: Boolean = true,
    )

    internal data class ScoreData(
        val user: OsuUser,
        val map: Beatmap,
        val scores: List<LazerScore>,
        val mode: OsuMode,
        val mods: List<LazerMod>? = null
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

        data.value = getParam(event, messageText, matcher, isMultipleScore, isShow, isCompact = false)

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

        return if (param.isRecordCallStatistics) {
            ServiceCallStatistic.builds(
                event,
                beatmapIDs = scores.map { it.beatmapID }.distinct(),
                userIDs = listOf(param.user.userID),
                modes = listOf(param.user.currentOsuMode),
            )
        } else {
            // 部分情况下不记录 bids
            ServiceCallStatistic.builds(
                event,
                userIDs = listOf(param.user.userID),
                modes = listOf(param.user.currentOsuMode),
            )
        }
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
        return getParam(event, messageText, matcher, isMultipleScore, isShow, isCompact = true)
    }

    override fun reply(event: MessageEvent, param: ScoreParam): MessageChain? {
        return param.getMessageChain()
    }

    private fun getParam(event: MessageEvent, messageText: String, matcher: Matcher, isMultipleScore: Boolean, isShow: Boolean, isCompact: Boolean): ScoreParam {
        val number = getBid(matcher)
        val inputMode = getMode(matcher)
        val mods: List<LazerMod> = getMod(matcher)

        val isMyself = AtomicBoolean(true)

        val userID: Long? = UserIDUtil.getUserIDWithoutRange(event, matcher, inputMode, isMyself)

        val isRecordCallStatistics = AtomicBoolean(true)

        val data: ScoreData = when {
            number in 1L ..< 1000_0000L -> {
                getFromBeatmapID(number, userID, inputMode, event, messageText, matcher)
            }

            number >= 1000_0000L -> {
                getFromScoreID(number)
            }

            else -> {
                // 进阶备用方法：先获取之前大家使用的 bid，然后尝试获取最近成绩
                val beforeBeatmapID = dao.getLastBeatmapID(
                    groupID = event.subject.contactID,
                    name = "SCORE",
                    from = LocalDateTime.now().minusMinutes(2L)
                ) ?: dao.getLastBeatmapID(
                    groupID = event.subject.contactID,
                    name = null,
                    from = LocalDateTime.now().minusHours(24L)
                )

                if (beforeBeatmapID != null) {
                    val d = getFromBefore(beforeBeatmapID, userID, inputMode, event, messageText, matcher)

                    if (d.scores.isNotEmpty()) {
                        d
                    } else {
                        isRecordCallStatistics.set(false)
                        getFromSearch(d.user, d.map, d.mode, d.mods ?: mods, event)
                    }
                } else {
                    getFromRecentScore(userID, inputMode, event, messageText, matcher)
                }
            }
        }

        if (data.scores.isEmpty()) {
            throw NoSuchElementException.BeatmapScore(data.map.previewName)
        }

        val m = data.mods?.ifEmpty { mods } ?: mods

        val preSelectAcronymSet = m.map { it.acronym }.toSet()

        val filtered = data.scores.filter { score ->
            val scoreAcronymSet = score.mods.map { it.acronym }.toSet()

            scoreAcronymSet.containsAll(preSelectAcronymSet)
        }

        if (filtered.isEmpty()) {
            throw NoSuchElementException.BeatmapScoreFiltered(data.map.previewName)
        }

        return ScoreParam(data.user, data.map, filtered, data.mode, m,
            isMultipleScore, isShow, isCompact, isRecordCallStatistics.get())
    }

    private fun getFromBeatmapID(beatmapID: Long, userID: Long?, inputMode: InstructionObject<OsuMode>, event: MessageEvent, messageText: String, matcher: Matcher): ScoreData {
        val map = beatmapApiService.getBeatmap(beatmapID)
        val mode: OsuMode
        val user: OsuUser
        val scores: List<LazerScore>

        if (!map.hasLeaderBoard) {
            throw NoSuchElementException.UnrankedBeatmapScore(map.previewName)
        }

        if (userID != null) {
            mode = OsuMode.getConvertableMode(inputMode.data, map.mode)

            val async = AsyncMethodExecutor.awaitPairCallableExecute(
                { userApiService.getOsuUser(userID, mode) },
                { scoreApiService.getBeatmapScores(beatmapID, userID, mode) }
            )

            user = async.first
            scores = async.second.toList()
        } else {
            mode = OsuMode.getConvertableMode(inputMode.data, map.mode)

            user = InstructionUtil.getUserWithoutRangeWithBackoff(event, matcher, InstructionObject(mode), AtomicBoolean(true), messageText, "score")

            scores = scoreApiService.getBeatmapScores(beatmapID, user.userID, mode)
        }

        return ScoreData(user, map, scores, mode)
    }

    private fun getFromScoreID(scoreID: Long
    ) : ScoreData {
        val score = scoreApiService.getScore(scoreID)

        val async = AsyncMethodExecutor.awaitPairCallableExecute(
            { userApiService.getOsuUser(score.userID, score.mode) },
            { beatmapApiService.getBeatmap(score.beatmapID) }
        )

        val user = async.first
        val map = async.second
        val scores = listOf(score)
        val mode = score.mode
        val mods = score.mods

        return ScoreData(user, map, scores, mode, mods)
    }

    // 备用方法：先获取最近成绩，再获取谱面
    private fun getFromRecentScore(userID: Long?, inputMode: InstructionObject<OsuMode>, event: MessageEvent, messageText: String, matcher: Matcher): ScoreData {
        event.reply("""
            没有获取到 24 小时内的可用谱面。
            正在查询您最近成绩所属的谱面上的最好成绩。
        """.trimIndent()).recallIn(60 * 1000)

        val recent: LazerScore
        val user: OsuUser

        if (userID != null) {
            val async = AsyncMethodExecutor.awaitPairCallableExecute(
                { userApiService.getOsuUser(userID, inputMode.data ?: OsuMode.DEFAULT) },
                { scoreApiService.getRecentScore(userID, inputMode.data, 0, 1) }
            )

            user = async.first
            recent = async.second.firstOrNull()
                ?: throw NoSuchElementException.RecentScore(user.username, user.currentOsuMode)

        } else {
            user = InstructionUtil.getUserWithoutRangeWithBackoff(event, matcher, inputMode, AtomicBoolean(true), messageText, "score")

            recent = scoreApiService.getRecentScore(user.userID, inputMode.data!!, 0, 1).firstOrNull()
                ?: throw NoSuchElementException.RecentScore(user.username, user.currentOsuMode)
        }

        val map: Beatmap = beatmapApiService.getBeatmap(recent.beatmapID)

        if (!map.hasLeaderBoard) {
            throw NoSuchElementException.UnrankedBeatmapScore(map.previewName)
        }

        val mode = OsuMode.getConvertableMode(inputMode.data, map.mode)

        val scores = scoreApiService.getBeatmapScores(recent.beatmapID, user.userID, mode)

        return ScoreData(user, map, scores, mode)
    }

    private fun getFromBefore(beforeBeatmapID: Long, userID: Long?, inputMode: InstructionObject<OsuMode>, event: MessageEvent, messageText: String, matcher: Matcher): ScoreData {
        val map = beatmapApiService.getBeatmap(beforeBeatmapID)

        if (!map.hasLeaderBoard) {
            throw NoSuchElementException.UnrankedBeatmapScore(map.previewName)
        }

        val mode = OsuMode.getConvertableMode(inputMode.data, map.mode)

        val user: OsuUser
        val scores: List<LazerScore>

        if (userID != null) {
            val async = AsyncMethodExecutor.awaitPairCallableExecute(
                { userApiService.getOsuUser(userID, inputMode.data!!)},
                { scoreApiService.getBeatmapScores(beforeBeatmapID, userID, mode)}
            )

            user = async.first
            scores = async.second
        } else {
            user = InstructionUtil.getUserWithoutRangeWithBackoff(event, matcher, inputMode, AtomicBoolean(true), messageText, "score")
            scores = scoreApiService.getBeatmapScores(beforeBeatmapID, user.userID, mode)
        }

        return ScoreData(user, map, scores, mode)
    }

    private fun getFromSearch(user: OsuUser, map: Beatmap, mode: OsuMode, mods: List<LazerMod>, event: MessageEvent): ScoreData {

        val receipt = event.reply("""
            没有获取到您在这个难度上的成绩。
            正在查询此谱面中，其他难度上相比最好的成绩。
            这可能需要一段时间。
        """.trimIndent())

        receipt.recallIn(60 * 1000)

        val set = beatmapApiService.getBeatmapset(map.beatmapsetID)

        val maps = (set.beatmaps ?: listOf()).dropWhile { it.beatmapID == map.beatmapID }

        if (maps.size >= 16) {
            receipt.recall()
            event.reply("""
                检测到此谱面含有大量难度。
                因此，只会尝试查询主难度往下 16 个难度内的成绩。
            """.trimIndent()).recallIn(60 * 1000)
        }

        val beatmaps = maps.sortedByDescending { it.starRating }
            .take(16)

        val scores = mutableListOf<LazerScore>()

        var count = 0

        for (b4 in beatmaps.chunked(4)) {
            val works = b4.map {
                Callable {
                    try {
                        scoreApiService.getBeatmapScore(it.beatmapID, user.userID, mode, mods)
                    } catch (_: Exception) {
                        null
                    }
                }
            }

            val b4Scores = AsyncMethodExecutor
                .awaitListCallableExecute(works)
                .mapNotNull { it?.score }

            count += b4.size

            if (b4Scores.isNotEmpty()) {
                scores.addAll(b4Scores)
                break
            } else if (count < beatmaps.size) {
                // 避免大量查询卡爆
                Thread.sleep(3.toDuration(DurationUnit.SECONDS).inWholeMilliseconds)
            }
        }

        // 成绩筛选机制：取 pp x 星数 最好的
        val better = scores.maxByOrNull { it.pp * it.beatmap.starRating }
            ?: throw NoSuchElementException.BeatmapScore(map.beatmapset!!.previewName)

        val beatmap = better.beatmap.apply { this.beatmapset = map.beatmapset }

        return ScoreData(user, beatmap, listOf(better), mode)
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
                    "compact" to ((isCompact && scores.size >= 10) || scores.size > 100)
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
