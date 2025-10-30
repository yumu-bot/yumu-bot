package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.Covers.Companion.CoverType
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.ScoreService.ScoreParam
import com.now.nowbot.service.messageServiceImpl.UUPRService.Companion.getUUScore
import com.now.nowbot.service.messageServiceImpl.UUPRService.Companion.getUUScores
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.sbApiService.SBScoreApiService
import com.now.nowbot.service.sbApiService.SBUserApiService
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

@Service("SB_SCORE")
class SBScoreService(
    private val scoreApiService: SBScoreApiService,
    private val userApiService: SBUserApiService,

    private val beatmapApiService: OsuBeatmapApiService,
    private val osuCalculateApiService: OsuCalculateApiService,
    private val osuScoreApiService: OsuScoreApiService,
    private val imageService: ImageService,
    private val dao: ServiceCallStatisticsDao,
    ): MessageService<ScoreParam> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<ScoreParam>
    ): Boolean {
        val m3 = Instruction.SB_SCORE_SHOW.matcher(messageText)
        val m2 = Instruction.SB_SCORES.matcher(messageText)
        val m = Instruction.SB_SCORE.matcher(messageText)

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
            log.error("偏偏要上班谱面成绩：发送失败", e)
            throw IllegalStateException.Send("偏偏要上班谱面成绩")
        }

        val scores = param.scores.toList()

        return ServiceCallStatistic.builds(
            event,
            beatmapIDs = scores.map { it.beatmapID }.distinct(),
            userIDs = listOf(param.user.userID),
            modes = listOf(param.user.currentOsuMode),
        )
    }

    private fun getParam(event: MessageEvent, messageText: String, matcher: Matcher, isMultipleScore: Boolean, isShow: Boolean): ScoreParam {
        val bid = getBid(matcher)

        val inputMode = getMode(matcher)
        val mods = getMod(matcher)
        val map: Beatmap
        val user: OsuUser
        val scores: List<LazerScore>
        val mode: OsuMode

        if (bid != 0L) {

            val id = UserIDUtil.getSBUserIDWithoutRange(event, matcher, inputMode, AtomicBoolean(true))

            map = beatmapApiService.getBeatmap(bid)

            /*
            if (!map.hasLeaderBoard) {
                throw NoSuchElementException.UnrankedBeatmapScore(map.previewName)
            }

             */

            val rx = if (LazerMod.hasMod(mods, LazerMod.Relax) && inputMode.data!!.modeValue in 0..3) {
                OsuMode.getMode(inputMode.data!!.modeValue + 4.toByte())
            } else {
                inputMode.data!!
            }

            if (id != null) {
                mode = OsuMode.getConvertableMode(rx, map.mode)

                val async = AsyncMethodExecutor.awaitPairCallableExecute(
                    { userApiService.getUser(id) },
                    { scoreApiService.getBeatmapRecentScore(bid, mods, mode) }
                )

                user = async.first?.toOsuUser(mode) ?: throw NoSuchElementException.Player(id.toString())
                scores = async.second
                    .filter { it.userID == id }
                    .map { it.toLazerScore() }
            } else {
                mode = OsuMode.getConvertableMode(rx, map.mode)

                user = InstructionUtil.getSBUserWithoutRangeWithBackoff(event, matcher, inputMode, AtomicBoolean(true), messageText, "score").toOsuUser(mode)

                scores = scoreApiService.getBeatmapRecentScore(bid, mods, mode)
                    .filter { it.userID == user.userID }
                    .map { it.toLazerScore() }
            }
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

                val rx = if (LazerMod.hasMod(mods, LazerMod.Relax) && currentMode.data!!.modeValue in 0..3) {
                    OsuMode.getMode(currentMode.data!!.modeValue + 4.toByte())
                } else {
                    currentMode.data!!
                }

                if (id != null) {

                    user = userApiService.getUser(id)?.toOsuUser(rx) ?: throw NoSuchElementException.Player(id.toString())

                } else {
                    user = InstructionUtil.getSBUserWithoutRangeWithBackoff(event, matcher, currentMode, AtomicBoolean(true), messageText, "score").toOsuUser(rx)
                }

                mode = OsuMode.getConvertableMode(rx, map.mode)

                scores = scoreApiService.getBeatmapRecentScore(bid, mods, mode)
                    .filter { it.userID == user.userID }
                    .map { it.toLazerScore() }

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
            val recent: LazerScore

            event.reply("没有获取到 24 小时内的参数。正在为您查询最近成绩所对应的谱面的在线成绩。").recallIn(60 * 1000L)

            val id = UserIDUtil.getUserIDWithoutRange(event, matcher, currentMode, AtomicBoolean(true))

            val rx = if (LazerMod.hasMod(mods, LazerMod.Relax) && currentMode.data!!.modeValue in 0..3) {
                OsuMode.getMode(currentMode.data!!.modeValue + 4.toByte())
            } else {
                currentMode.data!!
            }

            if (id != null) {
                val async = AsyncMethodExecutor.awaitPairCallableExecute(
                    { userApiService.getUser(id) },
                    { scoreApiService.getRecentScore(
                        id = id,
                        mods = mods,
                        mode = rx,
                        offset = 0,
                        limit = 1
                    ) }
                )

                user = async.first?.toOsuUser(rx) ?: throw NoSuchElementException.Player(id.toString())
                recent = async.second.firstOrNull()?.toLazerScore()
                    ?: throw NoSuchElementException.RecentScore(user.username, user.currentOsuMode)
            } else {
                user = InstructionUtil.getSBUserWithoutRangeWithBackoff(event, matcher, currentMode, AtomicBoolean(true), messageText, "score").toOsuUser(rx)

                recent = scoreApiService.getRecentScore(
                    id = user.userID,
                    mods = mods,
                    mode = rx,
                    offset = 0,
                    limit = 1
                ).firstOrNull()?.toLazerScore()
                    ?: throw NoSuchElementException.RecentScore(user.username, user.currentOsuMode)
            }

            map = beatmapApiService.getBeatmap(recent.beatmapID)

            mode = OsuMode.getConvertableMode(rx, map.mode)

            scores = scoreApiService.getBeatmapRecentScore(bid, mods, mode)
                .filter { it.userID == user.userID }
                .map { it.toLazerScore() }
        }

        if (scores.isEmpty()) {
            throw NoSuchElementException.BeatmapRecentScore(map.previewName)
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
    private fun ScoreParam.getMessageChain(): MessageChain {
        return try {
            if (scores.size > 1 && isMultipleScore) {
                // 实际上 SB 服内的成绩只能拿谱面最近 100 个和最高 100 个，事实上 SS 功能是失效的
                beatmapApiService.applyBeatmapExtendForSameScore(scores, map)
                // asyncDownloadBackground(param)

                val body = mapOf(
                    "user" to user,

                    "rank" to (1..(scores.size)).toList(),
                    "score" to scores,
                    "panel" to "SS"
                )

                MessageChain(imageService.getPanel(body, "A5"))
            } else {
                val score = scores.first()

                val e5Param = ScorePRService.getE5Param(
                    user,
                    score,
                    map,
                    null,
                    "S",
                    beatmapApiService,
                    osuCalculateApiService
                )

                // asyncDownloadBackground(param)

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

            val covers = osuScoreApiService.getCovers(ss, CoverType.COVER)

            val pairs = ss.mapIndexed { i, it -> i + 1 to it }

            getUUScores(user, pairs, covers)
        } else {

            val s = scores.first()

            val cover = osuScoreApiService.getCover(s, CoverType.COVER)

            beatmapApiService.applyBeatmapExtend(s, map)

            getUUScore(user, s, cover)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SBScoreService::class.java)
    }

}