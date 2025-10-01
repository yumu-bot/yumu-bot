package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.filter.ScoreFilter
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.ppysb.SBUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.ScorePRService.Companion.getE5ParamForFilteredScore
import com.now.nowbot.service.messageServiceImpl.ScorePRService.ScorePRParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.sbApiService.SBScoreApiService
import com.now.nowbot.service.sbApiService.SBUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException

import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.command.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("SB_SCORE_PR")
class SBScorePRService(
    private val userApiService: SBUserApiService,
    private val scoreApiService: SBScoreApiService,
    private val osuBeatmapApiService: OsuBeatmapApiService,

    private val osuCalculateApiService: OsuCalculateApiService,
    private val imageService: ImageService,
): MessageService<ScorePRParam> {

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<ScorePRParam>
    ): Boolean {
        val matcher = Instruction.SB_SCORE_PR.matcher(messageText)
        if (!matcher.find()) return false

        val isMultiple = (matcher.group("s").isNullOrBlank().not() || matcher.group("es").isNullOrBlank().not())
        val isShow = matcher.group("w").isNullOrBlank().not()

        val isPass =
            if (matcher.group("recent") != null) {
                false
            } else if (matcher.group("pass") != null) {
                true
            } else {
                log.error("偏偏要上班最近成绩分类失败：")
                throw IllegalStateException.ClassCast("偏偏要上班最近成绩")
            }

        val param = getParam(event, messageText, matcher, isMultiple, isPass, isShow) ?: return false

        data.value = param
        return true
    }

    override fun HandleMessage(event: MessageEvent, param: ScorePRParam) {
        // param.asyncImage()
        val messageChain: MessageChain = param.getMessageChain()

        try {
            event.reply(messageChain)
        } catch (e: Exception) {
            log.error("偏偏要上班最好成绩：发送失败", e)
            throw IllegalStateException.Send("偏偏要上班最好成绩")
        }
    }

    /**
     * 封装主获取方法
     * 请在 matcher.find() 后使用
     */
    private fun getParam(event: MessageEvent, messageText: String, matcher: Matcher, isMultiple: Boolean, isPass: Boolean, isShow: Boolean): ScorePRParam? {
        val any: String = matcher.group("any") ?: ""

        // 避免指令冲突
        if (any.contains("&sb", ignoreCase = true)) return null

        val isRelax = if (any.isNotBlank()) {
            val rxMatcher = ScoreFilter.MOD.regex.toPattern().matcher(any)

            if (!rxMatcher.find()) {
                false
            } else if (rxMatcher.group("n").contains("([Rr][Xx])|([Rr]elax)".toRegex())) {
                true
            } else {
                false
            }
        } else {
            false
        }

        val isMyself = AtomicBoolean(true) // 处理 range
        val mode = getMode(matcher)

        val id = UserIDUtil.getSBUserIDWithRange(event, matcher, mode, isMyself)

        id.setZeroToRange100()

        val conditions = DataUtil.paramMatcher(any, ScoreFilter.entries.map { it.regex }, REG_RANGE.toRegex())

        // 如果不加井号，则有时候范围会被匹配到这里来
        val rangeInConditions = conditions.lastOrNull()?.firstOrNull()
        val hasRangeInConditions = (rangeInConditions.isNullOrEmpty().not())
        val hasCondition = conditions.dropLast(1).sumOf { it.size } > 0

        if (hasRangeInConditions.not() && hasCondition.not() && any.isNotBlank()) {
            throw IllegalArgumentException.WrongException.Cabbage()
        }

        val ranges = if (hasRangeInConditions) {
            rangeInConditions
        } else {
            matcher.group(FLAG_RANGE)
        }?.split(REG_HYPHEN.toRegex())

        val user: OsuUser
        val scores: Map<Int, LazerScore>

        // 高效的获取方式
        if (id.data != null) {
            val id2 = if (id.start != null) {
                id
            } else {
                val start = ranges?.firstOrNull()?.toIntOrNull()
                val end = if (ranges?.size == 2) {
                    ranges.last().toIntOrNull()
                } else {
                    null
                }

                CmdRange(id.data!!, start, end)
            }

            val rx = if (isRelax && mode.data!!.modeValue in 0..3) {
                OsuMode.getMode(mode.data!!.modeValue + 4.toByte())
            } else {
                mode.data!!
            }

            val async = AsyncMethodExecutor.awaitPairCallableExecute(
                { userApiService.getUser(id2.data!!)?.toOsuUser(rx) },
                { id2.getRecentsFromSBUserID(rx, isMultiple, hasCondition, isPass) }
            )

            user = async.first ?: throw NoSuchElementException.Player(id2.data!!.toString())
            scores = async.second
        } else {
            // 经典的获取方式

            val range = CmdUtil.getSBUserAndRangeWithBackoff(event, matcher, mode, isMyself, messageText, "re")
            range.setZeroToRange100()

            val range2 = if (range.start != null) {
                range
            } else {
                val start = ranges?.firstOrNull()?.toIntOrNull()
                val end = if (ranges?.size == 2) {
                    ranges.last().toIntOrNull()
                } else {
                    null
                }

                CmdRange(range.data!!, start, end)
            }

            val rx = if (isRelax && mode.data!!.modeValue in 0..3) {
                OsuMode.getMode(mode.data!!.modeValue + 4.toByte())
            } else {
                mode.data!!
            }

            user = range2.data!!.toOsuUser(rx)

            scores = range2.getRecentsFromSBUser(rx, isMultiple, hasCondition, isPass)
        }

        val filteredScores = ScoreFilter.filterScores(scores, conditions)

        if (filteredScores.isEmpty()) {
            if (isPass) {
                throw NoSuchElementException.PassedScoreFiltered(user.username, user.currentOsuMode)
            } else {
                throw NoSuchElementException.RecentScoreFiltered(user.username, user.currentOsuMode)
            }
        }

        return ScorePRParam(user, filteredScores, isPass, isShow)
    }


    private fun <T> CmdRange<T>.getOffsetAndLimit(
        isMultiple: Boolean,
        isSearch: Boolean = false,
    ): Pair<Int, Int> {
        val offset: Int
        val limit: Int

        if (isSearch && this.start == null) {
            offset = 0
            limit = 100
        } else if (isMultiple) {
            offset = getOffset(0, true)
            limit = getLimit(20, true)
        } else {
            offset = getOffset(0, false)
            limit = getLimit(1, false)
        }

        return offset to limit
    }

    private fun CmdRange<Long>.getRecentsFromSBUserID(
        mode: OsuMode,
        isMultiple: Boolean,
        isSearch: Boolean = false,
        isPass: Boolean = false,
    ): Map<Int, LazerScore> {
        val o = this.getOffsetAndLimit(isMultiple, isSearch)

        val offset: Int = o.first
        val limit: Int = o.second

        val scores = scoreApiService.getScore(
            id = this.data!!,
            mode = mode,
            offset = offset,
            limit = limit,
            includeFailed = !isPass
        ).map { it.toLazerScore() }

        // 检查查到的数据是否为空
        if (scores.isEmpty()) {
            if (isPass) {
                throw NoSuchElementException.RecentScore(this.data!!.toString(), mode)
            } else {
                throw NoSuchElementException.PassedScore(this.data!!.toString(), mode)
            }
        }

        osuCalculateApiService.applyStarToScores(scores)
        osuCalculateApiService.applyBeatMapChanges(scores)

        return scores.mapIndexed { index, score -> (index + offset + 1) to score }.toMap()
    }

    private fun CmdRange<SBUser>.getRecentsFromSBUser(
        mode: OsuMode,
        isMultiple: Boolean,
        isSearch: Boolean = false,
        isPass: Boolean = false,
    ): Map<Int, LazerScore> {

        val offset: Int
        val limit: Int

        if (isSearch && this.start == null) {
            offset = 0
            limit = 100
        } else if (isMultiple) {
            offset = getOffset(0, true)
            limit = getLimit(20, true)
        } else {
            offset = getOffset(0, false)
            limit = getLimit(1, false)
        }

        val scores = scoreApiService.getScore(
            id = data!!.userID,
            mode = mode,
            offset = offset,
            limit = limit,
            includeFailed = !isPass
        ).map { it.toLazerScore() }

        osuCalculateApiService.applyStarToScores(scores)
        osuCalculateApiService.applyBeatMapChanges(scores)

        // 检查查到的数据是否为空
        if (scores.isEmpty()) {
            if (isPass) {
                throw NoSuchElementException.RecentScore(data!!.username, mode)
            } else {
                throw NoSuchElementException.PassedScore(data!!.username, mode)
            }
        }

        return scores.mapIndexed { index, score -> (index + offset + 1) to score }.toMap()
    }

    /*
    private fun ScorePRParam.asyncImage() = run {
        osuScoreApiService.asyncDownloadBackground(scores.values, listOf(CoverType.COVER, CoverType.LIST))
    }

     */

    private fun ScorePRParam.getMessageChain(): MessageChain {
        if (scores.size > 1) {
            val ranks = scores.map { it.key }
            val scores = scores.map { it.value }

            val body = mapOf(
                "user" to user,
                "score" to scores,
                "rank" to ranks,
                "panel" to if (isPass) "PS" else "RS"
            )

            osuBeatmapApiService.applyBeatmapExtendFromDatabase(scores)

            osuCalculateApiService.applyPPToScores(scores)

            val image = imageService.getPanel(body, "A5")
            return MessageChain(image)
        } else {
            // 单成绩发送
            val pair = scores.toList().first()

            val score: LazerScore = pair.second
            score.ranking = pair.first

            val e5 = getE5ParamForFilteredScore(user, score, (if (isPass) "P" else "R"), osuBeatmapApiService, osuCalculateApiService)

            return MessageChain(imageService.getPanel(e5.toMap(), if (isShow) "E10" else "E5"))
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SBScorePRService::class.java)
    }

}