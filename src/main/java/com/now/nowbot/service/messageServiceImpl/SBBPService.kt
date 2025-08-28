package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.ScoreFilter
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.ppysb.SBUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.BPService.BPParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.sbApiService.SBScoreApiService
import com.now.nowbot.service.sbApiService.SBUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getSBUserAndRangeWithBackoff
import com.now.nowbot.util.command.FLAG_RANGE
import com.now.nowbot.util.command.REG_EQUAL
import com.now.nowbot.util.command.REG_HYPHEN
import com.now.nowbot.util.command.REG_RANGE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("SB_BP")
class SBBPService(
    private val userApiService: SBUserApiService,
    private val scoreApiService: SBScoreApiService,

    private val osuCalculateApiService: OsuCalculateApiService,
    private val osuBeatmapApiService: OsuBeatmapApiService,
    private val imageService: ImageService,
) : MessageService<BPParam> {
    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<BPParam>): Boolean {
        val matcher = Instruction.SB_BP.matcher(messageText)
        if (!matcher.find()) return false

        val isMultiple = matcher.group("s").isNullOrBlank().not()
        val isShow = matcher.group("w").isNullOrBlank().not()

        val param = getParam(event, messageText, matcher, isMultiple, isShow) ?: return false

        data.value = param
        return true
    }

    override fun HandleMessage(event: MessageEvent, param: BPParam) {
        // param.asyncImage()

        val image: ByteArray = param.getImage()

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("偏偏要上班最好成绩：发送失败", e)
            throw IllegalStateException.Send("偏偏要上班最好成绩")
        }
    }


    /**
     * 封装主获取方法
     * 请在 matcher.find() 后使用
     */
    private fun getParam(event: MessageEvent, messageText: String, matcher: Matcher, isMultiple: Boolean, isShow: Boolean): BPParam? {
        val any: String? = matcher.group("any")

        // 避免指令冲突
        if (any?.contains("&sb", ignoreCase = true) == true) return null

        val isRelax = if (any != null) {
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

        val conditions = DataUtil.paramMatcher(any, ScoreFilter.entries.map { it.regex }, "$REG_EQUAL|$REG_RANGE".toRegex())

        // 如果不加井号，则有时候范围会被匹配到这里来
        val rangeInConditions = conditions.lastOrNull()?.firstOrNull()
        val hasRangeInConditions = (rangeInConditions.isNullOrEmpty().not())
        val hasCondition = conditions.dropLast(1).sumOf { it.size } > 0

        if (hasRangeInConditions.not() && hasCondition.not() && any.isNullOrBlank().not()) {
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
                { id2.getBestsFromUserID(rx, isMultiple, hasCondition) }
            )

            user = async.first ?: throw NoSuchElementException.Player(id2.data!!.toString())
            scores = async.second
        } else {
            // 经典的获取方式

            val range = getSBUserAndRangeWithBackoff(event, matcher, mode, isMyself, messageText, "bp")
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

            scores = range2.getBestsFromSBUser(rx, isMultiple, hasCondition)
        }

        val filteredScores = ScoreFilter.filterScores(scores, conditions)

        if (filteredScores.isEmpty()) {
            throw NoSuchElementException.BestScoreFiltered(user.username)
        }

        return BPParam(user, filteredScores, isShow)
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

    private fun CmdRange<Long>.getBestsFromUserID(
        mode: OsuMode,
        isMultiple: Boolean,
        isSearch: Boolean = false,
    ): Map<Int, LazerScore> {
        val o = this.getOffsetAndLimit(isMultiple, isSearch)

        val offset: Int = o.first
        val limit: Int = o.second

        val scores = scoreApiService.getBestScore(
            id = this.data!!,
            mode = mode,
            offset = offset,
            limit = limit
        ).map { it.toLazerScore() }

        // 检查查到的数据是否为空
        if (scores.isEmpty()) {
            throw NoSuchElementException.BestScoreWithMode(this.data!!.toString(), mode)
        }

        osuCalculateApiService.applyStarToScores(scores)
        osuCalculateApiService.applyBeatMapChanges(scores)

        return scores.mapIndexed { index, score -> (index + offset + 1) to score }.toMap()
    }

    private fun CmdRange<SBUser>.getBestsFromSBUser(
        mode: OsuMode,
        isMultiple: Boolean,
        isSearch: Boolean = false,
    ): Map<Int, LazerScore> {
        val o = this.getOffsetAndLimit(isMultiple, isSearch)

        val offset: Int = o.first
        val limit: Int = o.second

        val scores = scoreApiService.getBestScore(
            id = data!!.userID,
            mode = mode,
            offset = offset,
            limit = limit
        ).map { it.toLazerScore() }

        // 检查查到的数据是否为空
        if (scores.isEmpty()) {
            throw NoSuchElementException.BestScoreWithMode(this.data!!.username, mode)
        }

        osuCalculateApiService.applyStarToScores(scores)
        osuCalculateApiService.applyBeatMapChanges(scores)

        return scores.mapIndexed { index, score -> (index + offset + 1) to score }.toMap()
    }

    /*
    private fun BPParam.asyncImage() = run {
        scoreApiService.asyncDownloadBackground(scores.values, listOf(CoverType.COVER, CoverType.LIST))
    }

     */

    private fun BPParam.getImage(): ByteArray =
        if (scores.size > 1) {
            val ranks = scores.map { it.key }
            val scores = scores.map { it.value }

            osuBeatmapApiService.applyBeatmapExtendFromDatabase(scores)

            val body = mapOf(
                "user" to user,
                "scores" to scores,
                "rank" to ranks,
                "panel" to "BS"
            )

            imageService.getPanel(body, "A4")
        } else {
            val pair = scores.toList().first()

            val score: LazerScore = pair.second
            score.ranking = pair.first

            val e5Param = ScorePRService.getE5ParamForFilteredScore(user, score, "B", osuBeatmapApiService, osuCalculateApiService)

            imageService.getPanel(e5Param.toMap(), if (isShow) "E10" else "E5")
        }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SBBPService::class.java)

    }
}