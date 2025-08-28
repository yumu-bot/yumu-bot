package com.now.nowbot.service.messageServiceImpl

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
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.sbApiService.SBScoreApiService
import com.now.nowbot.service.sbApiService.SBUserApiService
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

@Service("SB_SCORE")
class SBScoreService(
    private val scoreApiService: SBScoreApiService,
    private val userApiService: SBUserApiService,

    private val beatmapApiService: OsuBeatmapApiService,
    private val osuCalculateApiService: OsuCalculateApiService,
    private val imageService: ImageService,
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

    override fun HandleMessage(event: MessageEvent, param: ScoreParam) {
        val message = getMessageChain(param)

        try {
            event.reply(message)
        } catch (e: Exception) {
            log.error("偏偏要上班谱面成绩：发送失败", e)
            throw IllegalStateException.Send("偏偏要上班谱面成绩")
        }
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

                user = CmdUtil.getSBUserWithoutRangeWithBackoff(event, matcher, inputMode, AtomicBoolean(true), messageText, "score").toOsuUser(mode)

                scores = scoreApiService.getBeatmapRecentScore(bid, mods, mode)
                    .filter { it.userID == user.userID }
                    .map { it.toLazerScore() }
            }
        } else {
            // 备用方法：先获取最近成绩，再获取谱面
            val recent: LazerScore

            val currentMode = CmdObject(inputMode.data)

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
                user = CmdUtil.getSBUserWithoutRangeWithBackoff(event, matcher, currentMode, AtomicBoolean(true), messageText, "score").toOsuUser(rx)

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

            /*
            if (!map.hasLeaderBoard) {
                throw NoSuchElementException.UnrankedBeatmapScore(map.previewName)
            }

             */

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
    private fun getMessageChain(param: ScoreParam): MessageChain {
        val image: ByteArray = if (param.scores.size > 1 && param.isMultipleScore) {
            // 实际上 SB 服内的成绩只能拿谱面最近 100 个和最高 100 个，事实上 SS 功能是失效的
            beatmapApiService.applyBeatmapExtendForSameScore(param.scores, param.map)
            // asyncDownloadBackground(param)

            val body = mapOf(
                "user" to param.user,

                "rank" to (1..(param.scores.size)).toList(),
                "score" to param.scores,
                "panel" to "SS"
            )

            imageService.getPanel(body, "A5")
        } else {
            val score = param.scores.first()

            val e5Param = ScorePRService.getE5Param(param.user, score, param.map, null, "S", beatmapApiService, osuCalculateApiService)

            // asyncDownloadBackground(param)

            imageService.getPanel(e5Param.toMap(), if (param.isShow) "E10" else "E5")
        }

        return QQMsgUtil.getImage(image)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SBScoreService::class.java)
    }

}