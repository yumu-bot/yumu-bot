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
    ) {
        fun toMap(): Map<String, Any> {
            when(this.version) {
                3 -> {
                    return mapOf(
                        "user" to user,
                        "best_arr" to BestsArray(bests),
                        "playcount_arr" to PlaycountsArray(user.monthlyPlaycounts),
                        "ranking_arr" to RankingArray(user.rankHistory?.data ?: listOf()),
                        "highest_rank" to HighestRanking(user.highestRank, user.rankHistory)
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
                    out["bonus_pp"] = getBonus(bests, user)

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
        val matcher2 = Instruction.INFO2.matcher(messageText)

        if (matcher.find()) {
            data.value = getParam(event, matcher, 1)
            return data.value != null
        } else if (matcher2.find()) {
            data.value = getParam(event, matcher2, 2)
            return data.value != null
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
        return getParam(event, matcher, 2)
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
            bests = async.second.toList()
        } else {
            user = try {
                getUserWithoutRange(event, matcher, getMode(matcher), isMyself)
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
            { calculateApiService.applyBeatMapChanges(bests.take(6)) },
            { calculateApiService.applyStarToScores(bests.take(6)) }
        )

        val day = (matcher.group(FLAG_DAY) ?: "").toLongOrNull() ?: 1L

        val historyUser =
            infoDao.getLastFrom(
                user.userID,
                user.currentOsuMode,
                LocalDate.now().minusDays(day)
            )?.let { OsuUserInfoDao.fromArchive(it) }

        val currentMode = OsuMode.getMode(mode.data!!, user.currentOsuMode)

        return InfoParam(user, bests, currentMode, historyUser, isMyself.get(), version)
    }

    private fun InfoParam.getMessageChain(): MessageChain {
        if (this.version == 2) {
            try {
                calculateApiService.applyStarToScores(bests.take(5))
            } catch (_: Exception) {
                log.info("玩家信息：获取新谱面星数失败")
            }
        }

        val name = when(version) {
            1 -> "D"
            else -> "D2"
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

        private fun getBonus(bests: List<LazerScore>, user: OsuUser): Double {
            return if (bests.isNotEmpty()) {
                DataUtil.getBonusPP(user.pp, bests.map { it.pp })
            } else {
                0.0
            }
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

            private val formatter = DateTimeFormatter.ofPattern("MM-dd")

            init {
                // 获取本周最后一天（周日）
                val today = LocalDate.now()
                val endOfWeek = today.with(DayOfWeek.SUNDAY)

                val bestsCount = countLast90DaysFromEndOfWeek(bests, endOfWeek)

                count = bestsCount.map { it.value }

                val maxEntry = bestsCount.toList().maxByOrNull { it.second }

                max = maxEntry?.second ?: 0
                time = maxEntry?.first?.format(formatter) ?: "-"

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

            private val formatter = DateTimeFormatter.ofPattern("yy-MM-dd")
            private val formatter2 = DateTimeFormatter.ofPattern("yyyy-MM")
            private val formatter3 = DateTimeFormatter.ofPattern("yy")

            init {
                val today = LocalDate.now()
                val thisYear = Year.from(today)

                val playcountsCount = countLastThreeAndQuarterYearFromEndOfYear(monthlies, thisYear)

                count = playcountsCount.map { it.value }

                val maxEntry = playcountsCount.toList().maxByOrNull { it.second }

                max = maxEntry?.second ?: 0
                time = maxEntry?.first?.format(formatter2) ?: "-"

                year0 = thisYear.format(formatter3)
                year1 = thisYear.minusYears(1).format(formatter3)
                year2 = thisYear.minusYears(2).format(formatter3)
                year3 = thisYear.minusYears(3).format(formatter3)

            }

            private fun countLastThreeAndQuarterYearFromEndOfYear(
                monthlies: List<OsuUser.UserMonthly>,
                thisYear: Year
            ): Map<YearMonth, Int> {
                val latestYearMonth = thisYear.atMonth(12)

                val months = monthlies.associate {
                    YearMonth.parse(it.startDate, formatter) to it.count
                }

                // 计算 三年又四分之一年 前的日期，3年前的10月开始计时
                val startDate = thisYear.atMonth(12)
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
             * @param minIntervalLength 最小区间长度（包含的索引数量），默认4表示至少4个索引变化
             */
            fun getRankingImprovementIntervals(minIntervalLength: Int = 4): List<RankingInterval> {
                val intervals = mutableListOf<RankingInterval>()

                // 确保列表有90个元素，不足的用0填充
                val paddedRanking = ranking.toMutableList()
                while (paddedRanking.size < 90) {
                    paddedRanking.add(0, 0L)
                }

                var startIndex = -1
                var currentImprovement = 0L

                for (i in 1 until paddedRanking.size) {
                    val prevRank = paddedRanking[i - 1]
                    val currentRank = paddedRanking[i]

                    // 跳过包含0的情况
                    if (prevRank == 0L || currentRank == 0L) {
                        // 如果正在记录区间，结束当前区间
                        if (startIndex != -1) {
                            val intervalLength = (i - 1) - startIndex + 1
                            // 只添加长度满足要求的区间
                            if (intervalLength >= minIntervalLength) {
                                intervals.add(RankingInterval(startIndex, i - 1, currentImprovement, intervalLength))
                            }
                            startIndex = -1
                            currentImprovement = 0L
                        }
                        continue
                    }

                    // 检查是否是排名上升（数值下降）
                    if (currentRank < prevRank) {
                        // 排名上升：从45名上升到40名（数值从45降到40）
                        if (startIndex == -1) {
                            // 开始新的上升区间
                            startIndex = i - 1
                        }
                        currentImprovement += (prevRank - currentRank) // 计算提升的位数
                    } else {
                        // 排名下降或持平，结束当前上升区间
                        if (startIndex != -1) {
                            val intervalLength = (i - 1) - startIndex + 1
                            // 只添加长度满足要求的区间
                            if (intervalLength >= minIntervalLength) {
                                intervals.add(RankingInterval(startIndex, i - 1, currentImprovement, intervalLength))
                            }
                            startIndex = -1
                            currentImprovement = 0L
                        }
                    }
                }

                // 处理最后一个区间
                if (startIndex != -1) {
                    val intervalLength = paddedRanking.size - 1 - startIndex + 1
                    if (intervalLength >= minIntervalLength) {
                        intervals.add(RankingInterval(startIndex, paddedRanking.size - 1, currentImprovement, intervalLength))
                    }
                }

                return intervals
            }

            /**
             * 获取所有统计信息，包含区间过滤
             */
            fun getRankingStatistics(minIntervalLength: Int = 4): RankingStatistics {
                val intervals = getRankingImprovementIntervals(minIntervalLength)
                val (bestRanking, worstRanking) = getRankingExtremes()

                return RankingStatistics(
                    intervals = intervals,
                    top = bestRanking,
                    bottom = worstRanking,
                    count = intervals.sumOf { it.improvement },
                    intervalMinLength = minIntervalLength
                )
            }

            // 其他方法保持不变...
            fun getRankingExtremes(): Pair<Long, Long> {
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
            val count: Long,
            private val intervalMinLength: Int = 0  // 新增：使用的最小区间长度
        )

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
        internal data class HighestRanking(
            private val highestRank: OsuUser.HighestRank?,
            private val ranks: OsuUser.RankHistory?
        ) {
            val rank: Long
            val time: OffsetDateTime

            init {
                if (highestRank != null && highestRank.updatedAt != null) {
                    rank = highestRank.rank
                    time = highestRank.updatedAt
                } else if (ranks != null) {

                    // 确保列表有90个元素，不足的用0填充
                    val paddedRanking = ranks.data.toMutableList()
                    while (paddedRanking.size < 90) {
                        paddedRanking.add(0, Long.MAX_VALUE)
                    }

                    rank = paddedRanking.minOrNull() ?: -1

                    val index = paddedRanking.indexOf(rank)

                    time = OffsetDateTime.now().minusDays(89L - index.toLong())
                } else {
                    rank = -1
                    time = OffsetDateTime.now()
                }
            }
        }
    }

}
