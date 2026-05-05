package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.Covers.Companion.CoverType
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.filter.ScoreFilter
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.NewbieRestrictService
import com.now.nowbot.service.messageServiceImpl.TodayBPService.TodayBPParam
import com.now.nowbot.service.messageServiceImpl.UUPRService.Companion.getUUScore
import com.now.nowbot.service.messageServiceImpl.UUPRService.Companion.getUUScores
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.sbApiService.SBScoreApiService
import com.now.nowbot.service.sbApiService.SBUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.BeatmapUtil
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.InstructionUtil.getMode
import com.now.nowbot.util.InstructionUtil.getSBUserWithRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.UserIDUtil
import com.now.nowbot.util.command.FLAG_ANY
import com.now.nowbot.util.command.FLAG_RANGE
import com.now.nowbot.util.command.REG_HYPHEN
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher
import kotlin.collections.isNotEmpty
import kotlin.collections.orEmpty

@Service("SB_TODAY_BP")
class SBTodayBPService(
    private val userApiService: SBUserApiService,
    private val scoreApiService: SBScoreApiService,

    private val osuBeatmapApiService: OsuBeatmapApiService,
    private val osuCalculateApiService: OsuCalculateApiService,
    private val osuScoreApiService: OsuScoreApiService,
    private val imageService: ImageService,
    private val restrict: NewbieRestrictService,

    ) : MessageService<TodayBPParam> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<TodayBPParam>
    ): Boolean {
        val matcher = Instruction.SB_TODAY_BP.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        val param = getParam(matcher, event)

        restrict.checkAsync(event, param.scores)

        data.value = param
        return true
    }

    override fun handleMessage(event: MessageEvent, param: TodayBPParam): ServiceCallStatistic? {
        // param.asyncImage()
        val image = param.getMessageChain()
        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("偏偏要上班今日最好成绩：发送失败", e)
            throw IllegalStateException.Send("偏偏要上班今日最好成绩")
        }

        val scores = param.scores.toList()

        return ServiceCallStatistic.builds(
            event,
            beatmapIDs = scores.map { it.second.beatmapID }.distinct(),
            userIDs = listOf(param.user.userID),
            modes = listOf(param.mode)
        )
    }
    private fun getParam(matcher: Matcher, event: MessageEvent): TodayBPParam {
        val any: String = matcher.group(FLAG_ANY) ?: ""
        val mode = getMode(matcher)
        val isMyself = AtomicBoolean()

        val conditions = DataUtil.getConditions(any, ScoreFilter.entries.map { it.regex })

        // 如果不加井号，则有时候范围会被匹配到这里来
        val rangeInConditions = conditions.lastOrNull().orEmpty()
        val hasRangeInConditions = rangeInConditions.isNotEmpty()
        val hasCondition = conditions.dropLast(1).any { it.isNotEmpty() }

        if (hasRangeInConditions.not() && hasCondition.not() && any.isNotBlank()) {
            throw IllegalArgumentException.WrongException.Cabbage()
        }

        val ranges = if (hasRangeInConditions) {
            rangeInConditions
        } else {
            matcher.group(FLAG_RANGE)?.split(REG_HYPHEN.toRegex())
        }

        val id = UserIDUtil.getSBUserIDWithRange(event, matcher, mode, isMyself, 999)
        id.setZeroDay()

        val user: OsuUser
        val bests: List<LazerScore>
        val dayStart: Int
        val dayEnd: Int

        if (id.data != null) {
            dayStart = ranges?.mapNotNull { it.toIntOrNull() }?.minByOrNull { it }?.coerceIn(1, 999)?.minus(1)
                ?: id.getDayStart()
            dayEnd = ranges?.mapNotNull { it.toIntOrNull() }?.maxByOrNull { it }?.coerceIn(1, 999) ?: id.getDayEnd()

            val async = AsyncMethodExecutor.awaitPair(
                { userApiService.getUser(id.data!!) },
                { scoreApiService.getBestScore(
                    id = id.data!!,
                    mode = mode.data ?: OsuMode.DEFAULT
                ) }
            )

            user = async.first?.toOsuUser(mode.data!!) ?: throw NoSuchElementException.Player(id.data!!.toString())
            bests = async.second.map { it.toLazerScore() }
        } else {
            val range = getSBUserWithRange(event, matcher, mode, isMyself)
            range.setZeroDay()

            user = range.data?.toOsuUser(mode.data)
                ?: throw NoSuchElementException.Player()

            dayStart = ranges?.mapNotNull { it.toIntOrNull() }?.minByOrNull { it }?.coerceIn(1, 999)?.minus(1)
                ?: range.getDayStart()
            dayEnd = ranges?.mapNotNull { it.toIntOrNull() }?.maxByOrNull { it }?.coerceIn(1, 999) ?: range.getDayEnd()

            bests = scoreApiService.getBestScore(
                id = user.userID,
                mode = mode.data
            ).map { it.toLazerScore() }
        }

        val isToday = (dayStart == 0 && dayEnd == 1)

        val laterDay = OffsetDateTime.now().minusDays(dayStart.toLong())
        val earlierDay = OffsetDateTime.now().minusDays(dayEnd.toLong())

        val dataMap = bests.mapIndexed { i, it ->
            return@mapIndexed if (it.endedTime.withOffsetSameInstant(ZoneOffset.ofHours(8)).isBefore(laterDay)
                && it.endedTime.withOffsetSameInstant(ZoneOffset.ofHours(8)).isAfter(earlierDay)) {
                i + 1 to it
            } else {
                null
            }
        }.filterNotNull().toMap()

        if (dataMap.isEmpty()) {
            if (isToday) {
                throw NoSuchElementException.TodayBestScore(user.username)
            } else {
                throw NoSuchElementException.PeriodBestScore(user.username)
            }
        }

        osuBeatmapApiService.applyBeatmapExtend(dataMap)

        val filteredScores = ScoreFilter.filterScores(dataMap, conditions)

        if (filteredScores.isEmpty()) {
            throw NoSuchElementException.TodayBestScoreFiltered(user.username)
        }

        return TodayBPParam(user, null, mode.data!!, dataMap, isToday)
    }

    /*
    fun TodayBPParam.asyncImage() {
        scoreApiService.asyncDownloadBackground(scores.values, listOf(CoverType.COVER, CoverType.LIST))
    }

     */

    fun TodayBPParam.getMessageChain(): MessageChain {
        return try {
            if (scores.size > 1) {

                val ranks = scores.keys
                val ss = scores.values

                AsyncMethodExecutor.awaitTriple(
                    { BeatmapUtil.applyBeatmapChanges(ss) },
                    { osuCalculateApiService.applyStarToScores(ss) },
                    { osuBeatmapApiService.applyBeatmapExtend(ss) }
                )

                val body = mapOf(
                    "user" to user,
                    "history_user" to historyUser,
                    "scores" to ss,
                    "rank" to ranks,
                    "panel" to "T"
                )

                MessageChain(imageService.getPanel(body, "A4"))
            } else {
                val pair = scores.toList().first()

                val score: LazerScore = pair.second
                score.ranking = pair.first

                val body = ScorePRService.getE5Param(user, null, score, "T", osuBeatmapApiService, osuCalculateApiService)

                MessageChain(imageService.getPanel(body, "E5"))
            }
        } catch (e: Exception) {
            log.error(e.message)
            return getUUMessageChain()
        }
    }

    private fun TodayBPParam.getUUMessageChain(): MessageChain {
        return if (scores.size > 1) {
            val list = scores.toList().take(5)
            val ss = list.map { it.second }

            AsyncMethodExecutor.awaitPair (
                { osuBeatmapApiService.applyBeatmapExtend(ss) },
                { osuCalculateApiService.applyPPToScores(ss) },
            )

            val covers = osuScoreApiService.getCovers(ss, CoverType.COVER)

            getUUScores(user, list, covers)
        } else {

            val s = scores.toList().first().second

            val cover = osuScoreApiService.getCover(s, CoverType.COVER)

            AsyncMethodExecutor.awaitPair (
                { osuBeatmapApiService.applyBeatmapExtend(s) },
                { osuCalculateApiService.applyPPToScore(s) },
            )

            getUUScore(user, s, cover)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SBTodayBPService::class.java)

    }
}