package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.now.nowbot.dao.OsuUserInfoDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.InfoLogStatistics
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.InfoService.InfoParam
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.*
import com.now.nowbot.util.InstructionUtil.getMode
import com.now.nowbot.util.InstructionUtil.getUserWithoutRange
import com.now.nowbot.util.command.FLAG_DAY
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.Year
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher
import kotlin.math.absoluteValue

@Service("INFO")
class InfoService(
    private val userApiService: OsuUserApiService,
    private val scoreApiService: OsuScoreApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val infoDao: OsuUserInfoDao,
    private val imageService: ImageService,
) : MessageService<InfoParam>, TencentMessageService<InfoParam> {

    data class InfoParam(
        val user: OsuUser,
        val bests: List<LazerScore>,
        val mode: OsuMode,
        val historyUser: OsuUser?,
        val isMyself: Boolean,
        val version: Int = 2,
        val percentiles: Map<String, Double> = mapOf(),
    ) {
        fun toMap(): Map<String, Any?> {
            user.setEstimatedPP(bests)

            when(this.version) {
                3 -> {
                    val day: Long = if (historyUser != null) {
                        val stat = historyUser.statistics

                        if (stat is InfoLogStatistics) {
                            ChronoUnit.DAYS.between(stat.logTime.toLocalDate(), LocalDate.now())
                        } else {
                            0
                        }
                    } else {
                        0
                    }

                    return mapOf(
                        "user" to user,
                        "bests" to bests.take(6),
                        "best_arr" to BestsArray(bests),
                        "playcount_arr" to PlaycountsArray(user.monthlyPlaycounts),
                        "ranking_arr" to RankingArray(user.rankHistory?.data ?: listOf()),
                        "highest_rank" to HighestRanking(user.highestRank, user.rankHistory, user.globalRank),
                        "percentiles" to percentiles,
                        "history_day" to day,
                        "history_user" to historyUser,
                        "bonus_pp" to DataUtil.getBonusPP(bests, user.pp)
                    )
                }

                // 新面板 (v5.0) 的数据
                2 -> {
                    val out = mutableMapOf<String, Any>()

                    val scores = if (bests.size >= 6) {
                        bests.take(6)
                    } else {
                        bests
                    }

                    out["user"] = user
                    out["mode"] = mode
                    out["scores"] = scores
                    out["best_time"] = getBestTimes(bests)

                    if (historyUser != null) {
                        val stat = historyUser.statistics

                        if (stat is InfoLogStatistics) {
                            out["history_day"] = ChronoUnit.DAYS.between(stat.logTime.toLocalDate(), LocalDate.now())
                        }

                        out["history_user"] = historyUser
                    }

                    out["panel"] = "D2"

                    return out
                }

                // 老面板 (v3.6) 的数据
                else -> {
                    val out = mutableMapOf<String, Any>()

                    out["user"] = user
                    out["mode"] = mode
                    out["bp-times"] = getBestTimes(bests)
                    out["bonus_pp"] = DataUtil.getBonusPP(bests, user.pp)

                    if (historyUser != null) {
                        val stat = historyUser.statistics

                        if (stat is InfoLogStatistics) {
                            out["day"] = ChronoUnit.DAYS.between(stat.logTime.toLocalDate(), LocalDate.now())
                        }

                        out["historyUser"] = historyUser
                    }

                    out["panel"] = "D"

                    return out
                }
            }
        }
    }

    @Throws(TipsException::class)
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<InfoParam>): Boolean {
        val matcher = Instruction.INFO.matcher(messageText)
        val matcher2 = Instruction.WAIFU_INFO.matcher(messageText)
        val matcher3 = Instruction.LEGACY_INFO.matcher(messageText)
        val matcher4 = Instruction.TEST_INFO.matcher(messageText)

        if (matcher.find()) {
            data.value = getParam(event, matcher, 3)
            return data.value != null
        } else if (matcher2.find()) {
            data.value = getParam(event, matcher2, 2)
            return data.value != null
        } else if (matcher3.find()) {
            data.value = getParam(event, matcher3, 1)
            return data.value != null
        } else if (matcher4.find()) {
            event.reply("""
                TEST INFO 已完成任务并下线。感谢您的支持。
                功能等同于现在的 INFO。
                
                要查看老版本的 TEST INFO，可以输入 !IW。
                要查看更老版本的 INFO，可以输入 !IL。
                """.trimIndent())
            return false
        }

        return false
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: InfoParam): ServiceCallStatistic? {
        val message = param.getMessageChain()
        try {
            event.reply(message)
        } catch (e: Exception) {
            log.error("玩家信息：发送失败", e)
            throw IllegalStateException.Send("玩家信息")
        }

        return ServiceCallStatistic.build(event, userID = param.user.userID, mode = param.user.currentOsuMode)
    }

    override fun accept(event: MessageEvent, messageText: String): InfoParam? {
        val matcher = OfficialInstruction.INFO.matcher(messageText)

        if (!matcher.find()) return null
        return getParam(event, matcher, 3)
    }

    override fun reply(event: MessageEvent, param: InfoParam): MessageChain = param.getMessageChain()

    private fun getParam(event: MessageEvent, matcher: Matcher, version: Int = 1): InfoParam? {
        val isMyself = AtomicBoolean(false)

        val mode = getMode(matcher)
        val user: OsuUser
        val bests: List<LazerScore>

        val id = UserIDUtil.getUserIDWithoutRange(event, matcher, mode, isMyself)

        if (id != null) {
            val async = AsyncMethodExecutor.awaitPairCallableExecute(
                { userApiService.getOsuUser(id, mode.data!!) },
                { scoreApiService.getBestScores(id, mode.data!!) }
            )

            user = async.first
            bests = async.second
        } else {
            user = try {
                getUserWithoutRange(event, matcher, mode, isMyself)
            } catch (e: BindException) {
                if (InstructionUtil.isAvoidance(event.textMessage.trim(), "info")) {
                    log.debug("指令退避：I 退避成功")
                    return null
                } else {
                    throw e
                }
            }

            bests = scoreApiService.getBestScores(user.userID, mode.data!!)
        }

        AsyncMethodExecutor.awaitPairCallableExecute(
            { calculateApiService.applyBeatmapChanges(bests.take(6)) },
            { calculateApiService.applyStarToScores(bests.take(6)) }
        )

        val day = (matcher.group(FLAG_DAY) ?: "").toLongOrNull() ?: 1L

        val historyUser = infoDao.getHistoryUser(user, day)

        val currentMode = OsuMode.getMode(mode.data!!, user.currentOsuMode)

        val percentiles = infoDao.getPercentiles(user, user.currentOsuMode)

        return InfoParam(user, bests, currentMode, historyUser, isMyself.get(), version, percentiles)
    }

    private fun InfoParam.getMessageChain(): MessageChain {
        val name: String

        when(version) {
            1 -> {
                name = "D"
            }

            2 -> {
                name = "D2"
                calculateApiService.applyStarToScores(bests.take(6))
            }

            3 -> {
                name = "D3"
                calculateApiService.applyStarToScores(bests.take(6))
            }

            else -> {
                name = "D"
            }
        }

        return try {
            MessageChain(imageService.getPanel(this.toMap(), name))
        } catch (_: NetworkException) {
            log.info("玩家信息：渲染失败")

            val avatar = userApiService.getAvatarByte(user)

            // 变化不大就不去拿了
            val h = if (historyUser == null || (historyUser.pp - user.pp).absoluteValue <= 0.5) {
                null
            } else {
                historyUser
            }

            UUIService.getUUInfo(user, avatar, h)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(InfoService::class.java)

        private fun getBestTimes(bests: List<LazerScore>): IntArray {
            val times: List<OffsetDateTime> = bests.map { it.endedTime.withOffsetSameLocal(ZoneOffset.ofHours(8)) }
            val now = LocalDate.now()

            val timeArray = IntArray(90)

            times.forEach { time ->
                val day = (now.toEpochDay() - time.toLocalDate().toEpochDay()).toInt()
                if (day in 0..89) {
                    timeArray[89 - day]++
                }
            }

            return timeArray
        }

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
        internal data class BestsArray(
            private val bests: List<LazerScore>
        ) {
            val count: List<Int>
            val max: Int
            val time: String
            val week0: String
            val week4: String
            val week8: String

            private val formatter = DateTimeFormatter.ofPattern("M-d")

            init {
                // 获取本周最后一天（周日）
                val today = LocalDate.now()
                val endOfWeek = today.with(DayOfWeek.SUNDAY)

                val bestsCount = countLast90DaysFromEndOfWeek(bests, endOfWeek)

                count = bestsCount.map { it.value }

                val maxEntry = bestsCount.toList().maxByOrNull { it.second }

                max = maxEntry?.second ?: 0

                time = if (max > 0) {
                    maxEntry?.first?.format(formatter) ?: "-"
                } else {
                    "-"
                }

                week0 = endOfWeek.format(formatter)
                week4 = endOfWeek.minusWeeks(4).format(formatter)
                week8 = endOfWeek.minusWeeks(8).format(formatter)
            }

            private fun countLast90DaysFromEndOfWeek(
                scores: List<LazerScore>,
                endOfWeek: LocalDate = LocalDate.now().with(DayOfWeek.SUNDAY)
            ): Map<LocalDate, Int> {

                // 计算 90天 前的日期
                val startDate = endOfWeek.minusDays(90)

                val groupedData = scores
                    .filter { score ->
                        val localDate = score.endedTime
                            .atZoneSameInstant(ZoneOffset.ofHours(8))
                            .toLocalDate()

                        localDate.isAfter(startDate) && localDate.isBefore(endOfWeek.plusDays(1))
                    }
                    .groupBy { obj ->
                        obj.endedTime.atZoneSameInstant(ZoneOffset.UTC).toLocalDate()
                    }
                    .mapValues { (_, values) -> values.size }


                // 创建包含所有日期的结果映射，没有数据的日期填充0
                val result = mutableMapOf<LocalDate, Int>()
                var currentDate = startDate

                while (currentDate <= endOfWeek) {
                    result[currentDate] = groupedData[currentDate] ?: 0
                    currentDate = currentDate.plusDays(1)
                }

                return result
            }
        }


        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
        internal data class PlaycountsArray(
            private val monthlies: List<OsuUser.UserMonthly>
        ) {
            val count: List<Int>
            val max: Int
            val time: String
            val year0: String
            val year1: String
            val year2: String
            val year3: String
            val quarter: Int

            private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            private val formatter2 = DateTimeFormatter.ofPattern("yyyy-M")
            private val formatter3 = DateTimeFormatter.ofPattern("yy")

            init {
                val today = LocalDate.now()
                val thisYear = Year.from(today)

                quarter = (today.monthValue - 1) / 3 + 1

                val playcountsCount = countLastThreeAndQuarterYearFromEndOfYear(monthlies, thisYear, quarter)

                count = playcountsCount.map { it.value }

                val maxEntry = playcountsCount.toList().maxByOrNull { it.second }

                max = maxEntry?.second ?: 0
                time = if (max > 0) {
                    maxEntry?.first?.format(formatter2) ?: "-"
                } else {
                    "-"
                }

                year0 = thisYear.format(formatter3)
                year1 = thisYear.minusYears(1).format(formatter3)
                year2 = thisYear.minusYears(2).format(formatter3)
                year3 = thisYear.minusYears(3).format(formatter3)

            }

            private fun countLastThreeAndQuarterYearFromEndOfYear(
                monthlies: List<OsuUser.UserMonthly>,
                thisYear: Year,
                quarter: Int = 1,
            ): Map<YearMonth, Int> {
                require(quarter in 1..4) {
                    "季度不满足要求"
                }

                val latestYearMonth = thisYear.atMonth(12)

                val months = monthlies.associate {
                    YearMonth.parse(it.startDate, formatter) to it.count
                }

                // 计算 三年又四分之一年 前的日期，3年前的10月开始计时
                val startDate = thisYear.atMonth(quarter * 3)
                    .minusYears(3)
                    .minusMonths(12 - 10)

                // 创建包含所有日期的结果映射，没有数据的日期填充0
                val result = mutableMapOf<YearMonth, Int>()
                var currentYearMonth = startDate

                while (currentYearMonth <= latestYearMonth) {
                    result[currentYearMonth] = months[currentYearMonth] ?: 0
                    currentYearMonth = currentYearMonth.plusMonths(1)
                }

                return result
            }
        }

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
        internal data class RankingArray(
            val ranking: List<Long>
        ) {
            val statistics: RankingStatistics

            init {
                statistics = getRankingStatistics()
            }
            /**
             * 获取排名上升（数值下降）的区间信息，并过滤掉太短的区间
             * @param minIntervalLength 最小区间长度（包含的索引数量）
             */
            private fun getRankingImprovementIntervals(minIntervalLength: Int = 1): List<RankingInterval> {
                val intervals = mutableListOf<RankingInterval>()

                // 确保列表有90个元素，不足的用0填充
                val paddedRanking = ranking.toMutableList()
                while (paddedRanking.size < 90) {
                    paddedRanking.add(0, 0L)
                }

                var startIndex = -1
                var currentImprovement = 0L
                var lastValidRank: Long? = null

                for (i in 0 until paddedRanking.size) {
                    val currentRank = paddedRanking[i]

                    if (currentRank == 0L) {
                        // 遇到0，结束当前区间
                        if (startIndex != -1) {
                            val endIndex = i - 1
                            val intervalLength = endIndex - startIndex + 1
                            if (intervalLength >= minIntervalLength) {
                                intervals.add(RankingInterval(startIndex, endIndex, currentImprovement, intervalLength))
                            }
                            startIndex = -1
                            currentImprovement = 0L
                        }
                        lastValidRank = null
                        continue
                    }

                    // 检查是否排名上升（数值下降）
                    if (lastValidRank != null && currentRank < lastValidRank) {
                        // 排名上升：当前排名比前一个有效排名更高（数值更小）
                        if (startIndex == -1) {
                            // 开始新的上升区间，起始位置是前一个有效点
                            startIndex = i - 1
                        }
                        currentImprovement += (lastValidRank - currentRank)
                    } else if (startIndex != -1) {
                        // 排名没有上升或持平/下降，结束当前区间
                        val endIndex = i - 1
                        val intervalLength = endIndex - startIndex
                        if (intervalLength >= minIntervalLength) {
                            intervals.add(RankingInterval(startIndex, endIndex, currentImprovement, intervalLength))
                        }
                        startIndex = -1
                        currentImprovement = 0L
                    }

                    lastValidRank = currentRank
                }

                // 处理最后一个区间（如果数据以上升结束）
                if (startIndex != -1) {
                    val endIndex = paddedRanking.size - 1
                    val intervalLength = endIndex - startIndex
                    if (intervalLength >= minIntervalLength) {
                        intervals.add(RankingInterval(startIndex, endIndex, currentImprovement, intervalLength))
                    }
                }

                return intervals
            }

            /**
             * 获取所有统计信息，包含区间过滤
             */
            private fun getRankingStatistics(minIntervalLength: Int = 1): RankingStatistics {
                val intervals = getRankingImprovementIntervals(minIntervalLength)
                val (bestRanking, worstRanking) = getRankingExtremes()

                return RankingStatistics(
                    intervals = intervals,
                    top = bestRanking,
                    bottom = worstRanking,
                    improvement = intervals.sumOf { it.improvement },
                    intervalMinLength = minIntervalLength
                )
            }

            // 其他方法保持不变...
            private fun getRankingExtremes(): Pair<Long, Long> {
                val nonZeroRankings = ranking.filter { it > 0L }
                return if (nonZeroRankings.isEmpty()) {
                    Pair(0L, 0L)
                } else {
                    Pair(nonZeroRankings.min(), nonZeroRankings.max())
                }
            }
        }

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
        internal data class RankingInterval(
            val start: Int,
            val end: Int,
            val improvement: Long,
            val length: Int  // 新增：区间长度
        )

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
        internal data class RankingStatistics(
            val intervals: List<RankingInterval>,
            val top: Long,
            val bottom: Long,
            val improvement: Long,
            private val intervalMinLength: Int = 0  // 新增：使用的最小区间长度
        )

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
        internal data class HighestRanking(
            private val highestRank: OsuUser.HighestRank?,
            private val ranks: OsuUser.RankHistory?,
            private val globalRank: Long,
        ) {
            val rank: Long
            val time: OffsetDateTime

            init {
                if (highestRank != null && highestRank.updatedAt != null) {
                    rank = highestRank.rank
                    time = highestRank.updatedAt
                } else if (ranks != null && ranks.data.isNotEmpty()) {

                    // 确保列表有90个元素，不足的用0填充
                    val paddedRanking = ranks.data.toMutableList()
                    while (paddedRanking.size < 90) {
                        paddedRanking.add(0, 0)
                    }

                    rank = paddedRanking.minOrNull() ?: -1

                    val index = paddedRanking.indexOf(rank)

                    time = OffsetDateTime.now().minusDays(89L - index.toLong())
                } else {
                    rank = if (globalRank > 0) globalRank else -1L
                    time = OffsetDateTime.now()
                }
            }
        }
    }

}
