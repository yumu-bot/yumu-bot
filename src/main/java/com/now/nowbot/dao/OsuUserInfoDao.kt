package com.now.nowbot.dao

import com.now.nowbot.mapper.OsuUserInfoPercentilesLiteRepository
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.osu.Statistics
import com.now.nowbot.cache.PercentileCacheProvider
import com.now.nowbot.entity.UserGlobalRankLite
import com.now.nowbot.entity.UserGlobalRankLite.Companion.parseToRanks
import com.now.nowbot.entity.UserInfoLite
import com.now.nowbot.entity.UserInfoLite.Companion.updateFrom
import com.now.nowbot.entity.UserRankPercentLite
import com.now.nowbot.entity.UserRankPercentLite.Companion.updateFrom
import com.now.nowbot.entity.UserStatisticsLite
import com.now.nowbot.entity.UserStatisticsLite.Companion.updateFrom
import com.now.nowbot.mapper.UserGlobalRankRepository
import com.now.nowbot.mapper.UserInfoRepository
import com.now.nowbot.mapper.UserRankPercentRepository
import com.now.nowbot.mapper.UserStatisticsRepository
import com.now.nowbot.model.enums.OsuMode.Companion.toOsuMode
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

@Component
class OsuUserInfoDao(
    private val percentileRepository: OsuUserInfoPercentilesLiteRepository,

    private val userInfoRepository: UserInfoRepository,
    private val userStatisticsRepository: UserStatisticsRepository,
    private val userGlobalRankRepository: UserGlobalRankRepository,
    private val userRankPercentRepository: UserRankPercentRepository,

    private val percentileCacheProvider: PercentileCacheProvider,
) {
    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Transactional
    fun percentilesDailyUpsert(): Int {
        log.info("正在更新玩家百分比")
        entityManager.clear()

        val now = LocalDate.now(ZoneOffset.UTC)

        val infos = userInfoRepository.getLatestBetween(now.minusDays(1), now)

        val statMap = userStatisticsRepository.getLatestBetween(now.minusDays(1), now)
            .associateBy { it.userID to it.mode }

        val exists = infos.groupBy({ it.mode }, { it.userID })

        val globalMap = exists.flatMap { (mode, users) ->
            userGlobalRankRepository.getLatest(users, mode)
        }.associate { (it.userID to it.mode) to it.rank }

        val countryMap = exists.flatMap { (mode, users) ->
            userRankPercentRepository.getLatest(users, mode)
        }.associate { (it.userID to it.mode) to it.countryRank }

        log.info("已经获取到 ${infos.size} 条数据，正在更新")

        entityManager.clear()

        infos.forEachIndexed { index, info ->
            val key = info.userID to info.mode

            val stat = statMap[key] ?: return@forEachIndexed
            val global = globalMap[key]
            val country = countryMap[key]

            upsertPercentile(info, stat, global, country)

            // 每处理 1000 条数据，强制同步并清空一次缓存
            if (index % 1000 == 0 && index > 0) {
                entityManager.flush()
                entityManager.clear()
            }
        }

        log.info("更新完成。")

        return infos.size
    }

    fun upsertPercentile(info: UserInfoLite, stat: UserStatisticsLite, global: Long?, country: Long?) {
        percentileRepository.upsert(
            userID = info.userID,
            mode = info.mode,
            updatedAt = LocalDateTime.now(),
            globalRank = global,
            countryRank = country,
            level = stat.levelCurrent * 100 + stat.levelProgress,
            rankCountScore = 3 * (stat.countSS + stat.countSSH) + 2 * (stat.countSH + stat.countS) + stat.countA,
            playCount = stat.playCount,
            totalHits = stat.totalHits,
            playTime = stat.playTime,
            rankedScore = stat.rankedScore,
            totalScore = stat.totalScore,
            beatmapPlaycount = info.beatmapPlaycount,
            replaysWatched = stat.replaysWatched,
            maximumCombo = stat.maxCombo,
            achievementsCount = info.achievementsCount
        )
    }

    fun getPercentiles(user: OsuUser, mode: OsuMode): Map<String, Double> {
        // sb 服的模式转换
        val modeValue: Byte = when(mode.modeValue) {
            4.toByte(), 8.toByte() -> 0.toByte()
            5.toByte() -> 1.toByte()
            6.toByte() -> 2.toByte()
            (-1).toByte() -> user.currentOsuMode.modeValue
            else -> mode.modeValue
        }

        val stats = percentileCacheProvider.cachedData[modeValue]
            ?: return emptyMap()

        val stat = user.statistics
        val global = if (user.globalRank <= 0) Long.MAX_VALUE else user.globalRank
        val country = if (user.countryRank <= 0) Long.MAX_VALUE else user.countryRank
        val rankCountScore = 3 * ((stat?.countSS ?: 0) + (stat?.countSSH ?: 0)) + 2 * ((stat?.countSH ?: 0) + (stat?.countS ?: 0)) + (stat?.countA ?: 0)
        val level = user.levelCurrent * 100 + user.levelProgress

        return mapOf(
            "global_rank" to calculatePercentile(stats.globalRanks, global, false),
            "country_rank" to calculatePercentile(stats.countryRanks, country, false),
            "level" to calculatePercentile(stats.levels, level, true),
            "rank_count_score" to calculatePercentile(stats.rankCountScores, rankCountScore, true),
            "play_count" to calculatePercentile(stats.playCounts, user.playCount, true),
            "total_hits" to calculatePercentile(stats.totalHits, user.totalHits, true),
            "play_time" to calculatePercentile(stats.playTimes, user.playTime, true),
            "ranked_score" to calculatePercentile(stats.rankedScores, stat?.rankedScore, true),
            "total_score" to calculatePercentile(stats.totalScores, stat?.totalScore, true),
            "beatmap_playcount" to calculatePercentile(stats.beatmapPlaycounts, user.beatmapPlaycount, true),
            "replays_watched" to calculatePercentile(stats.replaysWatcheds, stat?.replaysWatchedByOthers, true),
            "maximum_combo" to calculatePercentile(stats.maximumCombos, stat?.maxCombo, true),
            "achievements_count" to calculatePercentile(stats.achievementCounts, user.userAchievementsCount, true)
        )
    }

    fun calculatePercentile(longs: LongArray, value: Long?, higherIsBetter: Boolean): Double {
        if (value == null) return 0.0
        return calculatePercentileCore(
            longs.size, value,
            { longs.binarySearch(value) },
            { longs[it] },
            higherIsBetter
        )
    }

    fun calculatePercentile(ints: IntArray, value: Int?, higherIsBetter: Boolean): Double {
        if (value == null) return 0.0
        return calculatePercentileCore(
            ints.size, value.toLong(),
            { ints.binarySearch(value) },
            { ints[it].toLong() },
            higherIsBetter
        )
    }

    private fun calculatePercentileCore(
        size: Int,
        value: Long,
        binarySearchOp: () -> Int,
        getAtIndex: (Int) -> Long,
        higherIsBetter: Boolean
    ): Double {
        if (size == 0) return 0.0

        val index = binarySearchOp()
        val count = if (higherIsBetter) {
            // 数值越大越好：小于等于该值
            if (index >= 0) {
                var lastIndex = index
                while (lastIndex + 1 < size && getAtIndex(lastIndex + 1) == value) lastIndex++
                lastIndex + 1
            } else {
                -index - 1
            }
        } else {
            // 数值越小越好：大于等于该值
            if (index >= 0) {
                var firstIndex = index
                while (firstIndex - 1 >= 0 && getAtIndex(firstIndex - 1) == value) firstIndex--
                size - firstIndex
            } else {
                size - (-index - 1)
            }
        }
        return count.toDouble() / size
    }

    fun upsertUserTodayAsync(user: OsuUser) {
        Thread.startVirtualThread {
            runCatching {
                upsertUserToday(user)
            }.onFailure { e ->
                log.info("玩家数据存储：存储失败：", e)
            }
        }
    }

    private fun upsertUserToday(user: OsuUser) {
        upsertPercent(user)
        upsertGlobalRank(user)
        upsertUserInfo(user)
        upsertUserStatistics(user)
    }

    fun saveUsersTodayAsync(users: List<MicroUser>) {
        Thread.startVirtualThread {
            runCatching {
                saveUsersToday(users)
            }.onFailure { e ->
                log.info("玩家数据存储：批量存储失败：", e)
            }
        }
    }

    /**
     * 这里的 users 必须是 isVariant = true 后获取的
     */
    private fun saveUsersToday(users: List<MicroUser>) {
        val countryRanks = userRankPercentRepository.getLatestCountryRanks(users.map { it.userID }.toSet())
            .associate { (it.userID to it.mode) to it.countryRank }

        users.flatMap { user ->
            val rulesets = user.rulesets ?: return@flatMap emptyList()
            buildList {
                rulesets.osu?.let { add(Triple(user.userID, OsuMode.OSU, it)) }
                rulesets.taiko?.let { add(Triple(user.userID, OsuMode.TAIKO, it)) }
                rulesets.fruits?.let { add(Triple(user.userID, OsuMode.CATCH, it)) }
                rulesets.mania?.let { add(Triple(user.userID, OsuMode.MANIA, it)) }
            }
        }.groupBy { it.second }.forEach { (mode, triples) ->
            // 组装 Statistics 批量数据
            val statInputs = triples.map { (userID, _, stat) -> Pair(userID, stat) }

            batchUpsertUserStatistics(mode, statInputs)

            // 组装 Percent 批量数据
            val percentInputs = triples.map { (userID, _, stat) ->
                val country = countryRanks[userID to mode.modeValue] ?: 0L

                Triple(userID, stat, country)
            }

            batchUpsertPercent(mode, percentInputs)
        }
    }

    fun getHistoryUser(user: OsuUser, duration: Duration = 1.days): OsuUser? {
        val today = LocalDate.now(ZoneOffset.UTC)
        val to = today.minusDays(duration.inWholeDays)
        val from = today.minusYears(1)

        val info = userInfoRepository.getLatestBetween(user.userID, user.currentOsuMode.modeValue, from, to)
        val stats = userStatisticsRepository.getLatestBetween(user.userID, user.currentOsuMode.modeValue, from, to)
        val rank = userGlobalRankRepository.getBetween(user.userID, user.currentOsuMode.modeValue, to.minusDays(90), to)
        val percent = userRankPercentRepository.getLatestBetween(user.userID, user.currentOsuMode.modeValue, from, to)

        return fromArchive(info, stats, rank, percent)
    }

    fun getClosestFromTarget(userID: Long, mode: OsuMode, target: Double): OsuUser? {
        val stats = userStatisticsRepository.getClosestFromTarget(userID, mode.modeValue, target) ?: return null

        val targetDate = stats.updatedAt

        val info = userInfoRepository.getClosestFromDateRange(userID, mode.modeValue, targetDate)
        val rank = userGlobalRankRepository.getBetween(userID, mode.modeValue, targetDate.minusDays(90), targetDate)
        val percent = userRankPercentRepository.getClosestFromDate(userID, mode.modeValue, targetDate)

        return fromArchive(info, stats, rank, percent)
    }


    fun getPP(userID: Long, mode: OsuMode): Float? {
        return userStatisticsRepository.getLatest(userID, mode.modeValue)?.pp
    }

    fun getPlayCountsFromUserIDBeforeToday(userID: Long): List<Long> {
        val today = LocalDate.now(ZoneOffset.UTC)
        val to = today.minusDays(1)
        val from = today.minusYears(1)

        val playCounts = userStatisticsRepository.getLatestPlayCounts(userID, from, to).associate { it.mode to it.playCount }

        return List(4) { i -> i.toByte() }.map { playCounts[it] ?: 0L }
    }
    fun getLatestPlayCountsBatchBetween(userIDs: List<Long>, from: LocalDate, to: LocalDate): List<UserStatisticsLite> {
        return userStatisticsRepository.getLatestBatchBetween(userIDs, from, to)
    }

    // Yumu 0.8.2 玩家信息优化

    private fun upsertGlobalRank(user: OsuUser) {
        val history = user.rankHistory?.data ?: return
        val mode = user.currentOsuMode.modeValue
        val today = LocalDate.now(ZoneOffset.UTC)

        val delta = (history.filter { it > 0 }.size - 1)

        if (delta < 0L) return

        val totalDays = history.size
        val incomingMap: Map<LocalDate, Long> = history.mapIndexed { index, rank ->
            val daysAgo = (totalDays - 1) - index
            val date = today.minusDays(daysAgo.toLong())
            date to rank
        }.filter { it.second > 0L }.toMap()

        if (incomingMap.isEmpty()) return

        // 2. 找出这批数据的实际日期边界
        val target = incomingMap.keys
        val startDate = target.minOrNull()!!
        val endDate = target.maxOrNull()!!

        // 3. 从数据库捞出这段时间内【已经存在】的日期集合
        val exist = userGlobalRankRepository.getDateBetween(user.userID, mode, startDate, endDate).toSet()

        val insert = target.minus(exist)

        if (insert.isNotEmpty()) {
            // 无需更新数据
            val entities = insert.map { date ->
                UserGlobalRankLite(
                    userID = user.userID,
                    mode = mode,
                    date = date,
                    rank = incomingMap[date]!!
                )
            }

            userGlobalRankRepository.saveAll(entities)
        }
    }

    private fun upsertPercent(userID: Long, mode: OsuMode, statistics: Statistics, countryRank: Long = 0L) {
        val today = LocalDate.now(ZoneOffset.UTC)
        val latest = userRankPercentRepository.getLatest(userID, mode.modeValue)

        if (latest != null) {
            // 无需更新数据
            if (latest.countryRank >= countryRank && countryRank >= 0L) {

                if (latest.date.isBefore(today)) {
                    // 更新时间
                    userRankPercentRepository.upsert(latest.updateFrom(userID, mode, countryRank, statistics))
                }

                return
            }

            val entity = latest.updateFrom(userID, mode, countryRank, statistics).apply {
                this.date = today
            }

            userRankPercentRepository.upsert(entity)
        } else {
            // 新增
            val entity = UserRankPercentLite().updateFrom(userID, mode, countryRank, statistics).apply {
                this.date = today
            }

            userRankPercentRepository.upsert(entity)
        }
    }

    private fun upsertPercent(user: OsuUser) {
        val today = LocalDate.now(ZoneOffset.UTC)
        val latest = userRankPercentRepository.getLatest(user.userID, user.currentOsuMode.modeValue)

        val countryRank = user.countryRank

        if (latest != null) {
            // 无需更新数据
            if (latest.countryRank >= countryRank && countryRank >= 0L) {

                if (latest.date.isBefore(today)) {
                    // 更新时间
                    userRankPercentRepository.upsert(latest.updateFrom(user) ?: return)
                }

                return
            }

            val entity = latest.updateFrom(user)?.apply {
                this.date = today
            } ?: return

            userRankPercentRepository.upsert(entity)
        } else {
            // 新增
            val entity = UserInfoLite().updateFrom(user).apply {
                this.createdAt = today
                this.updatedAt = today
            }

            userInfoRepository.upsert(entity)
        }
    }

    private fun batchUpsertPercent(mode: OsuMode, inputs: List<Triple<Long, Statistics, Long>>) {
        if (inputs.isEmpty()) return

        val today = LocalDate.now(ZoneOffset.UTC)
        val userIDs = inputs.map { it.first }.toSet()

        val latestMap = userRankPercentRepository.getLatestBatch(userIDs, mode.modeValue)
            .associateBy { it.userID }

        val entitiesToUpsert = mutableListOf<UserRankPercentLite>()

        for (input in inputs) {
            val userID = input.first
            val statistics = input.second
            val countryRank = input.third

            val latest = latestMap[userID]

            if (latest != null) {
                if (latest.countryRank >= countryRank && countryRank >= 0L) {
                    if (latest.date.isBefore(today)) {
                        val entity = latest.updateFrom(userID, mode, countryRank, statistics).apply {
                            this.date = today
                        }
                        entitiesToUpsert.add(entity)
                    }
                    continue
                }

                val entity = latest.updateFrom(userID, mode, countryRank, statistics).apply {
                    this.date = today
                }
                entitiesToUpsert.add(entity)
            } else {
                val entity = UserRankPercentLite().updateFrom(userID, mode, countryRank, statistics).apply {
                    this.date = today
                }
                entitiesToUpsert.add(entity)
            }
        }

        if (entitiesToUpsert.isNotEmpty()) {
            userRankPercentRepository.saveAll(entitiesToUpsert)
        }
    }

    private fun upsertUserInfo(user: OsuUser) {
        val today = LocalDate.now(ZoneOffset.UTC)
        val latest = userInfoRepository.getLatest(user.userID, user.currentOsuMode.modeValue)

        val achievementsCount = user.userAchievementsCount

        if (latest != null) {
            // 无需更新数据
            if (latest.achievementsCount >= achievementsCount && achievementsCount >= 0L) {

                if (latest.updatedAt.isBefore(today)) {
                    // 更新时间
                    userInfoRepository.update(latest.id!!, today)
                }

                return
            }

            val entity = latest.updateFrom(user).apply {
                this.updatedAt = today
            }

            userInfoRepository.upsert(entity)
        } else {
            // 新增
            val entity = UserInfoLite().updateFrom(user).apply {
                this.createdAt = today
                this.updatedAt = today
            }

            userInfoRepository.upsert(entity)
        }
    }

    private fun upsertUserStatistics(userID: Long, mode: OsuMode, statistics: Statistics) {
        val today = LocalDate.now(ZoneOffset.UTC)
        val latest = userStatisticsRepository.getLatest(userID, mode.modeValue)

        val totalHits = statistics.totalHits ?: 0L

        if (latest != null) {
            // 无需更新数据
            if (latest.totalHits >= totalHits && totalHits >= 0L) {

                if (latest.updatedAt.isBefore(today)) {
                    // 更新时间
                    userStatisticsRepository.update(latest.id!!, today)
                }

                return
            }

            val entity = latest.updateFrom(userID, mode, statistics).apply {
                this.updatedAt = today
            }

            userStatisticsRepository.upsert(entity)
        } else {
            // 新增
            val entity = UserStatisticsLite().updateFrom(userID, mode, statistics).apply {
                this.createdAt = today
                this.updatedAt = today
            }

            userStatisticsRepository.upsert(entity)
        }
    }

    private fun upsertUserStatistics(user: OsuUser) {
        val today = LocalDate.now(ZoneOffset.UTC)
        val latest = userStatisticsRepository.getLatest(user.userID, user.currentOsuMode.modeValue)

        val newTotalHits = user.totalHits

        if (latest != null) {
            // 无需更新数据
            if (latest.totalHits >= newTotalHits && newTotalHits >= 0L) {

                if (latest.updatedAt.isBefore(today)) {
                    // 更新时间
                    userStatisticsRepository.update(latest.id!!, today)
                }

                return
            }

            val entity = latest.updateFrom(user)!!.apply {
                this.updatedAt = today
            }

            userStatisticsRepository.upsert(entity)
        } else {
            // 新增
            val entity = UserStatisticsLite().updateFrom(user)!!.apply {
                this.createdAt = today
                this.updatedAt = today
            }

            userStatisticsRepository.save(entity)
        }
    }


    private fun batchUpsertUserStatistics(mode: OsuMode, inputs: List<Pair<Long, Statistics>>) {
        if (inputs.isEmpty()) return

        val today = LocalDate.now(ZoneOffset.UTC)
        val userIDs = inputs.map { it.first }.toSet()

        // 💡 优化核心 1：一次性查出这批用户在该模式下的最新记录，转成 Map 供内存对比
        // 假设你的实体类里有 userID 属性
        val latestMap = userStatisticsRepository.getLatestBatch(userIDs, mode.modeValue)
            .associateBy { it.userID }

        // 用于存放需要执行完整更新或新增的实体
        val entitiesToUpsert = mutableListOf<UserStatisticsLite>()
        // 用于存放仅需要更新 updatedAt 的主键 ID
        val idsToUpdateDate = mutableListOf<Long>()

        // 开始遍历处理
        for (input in inputs) {
            val userID = input.first
            val statistics = input.second
            val totalHits = statistics.totalHits ?: 0L

            val latest = latestMap[userID]

            if (latest != null) {
                // 对应原逻辑：无需更新数据，只看要不要更新时间
                if (latest.totalHits >= totalHits && totalHits >= 0L) {
                    if (latest.updatedAt.isBefore(today)) {
                        latest.id?.let { idsToUpdateDate.add(it) }
                    }
                    continue
                }

                val entity = latest.updateFrom(userID, mode, statistics).apply {
                    this.updatedAt = today
                }
                entitiesToUpsert.add(entity)
            } else {
                // 对应原逻辑：新增
                val entity = UserStatisticsLite().updateFrom(userID, mode, statistics).apply {
                    this.createdAt = today
                    this.updatedAt = today
                }
                entitiesToUpsert.add(entity)
            }
        }

        // 💡 优化核心 2：批量写入数据库，将几百次 I/O 合并为最多 2 次
        if (idsToUpdateDate.isNotEmpty()) {
            userStatisticsRepository.batchUpdateTime(idsToUpdateDate, today)
        }

        if (entitiesToUpsert.isNotEmpty()) {
            userStatisticsRepository.saveAll(entitiesToUpsert) // 或者调用你原本的批量 upsert
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OsuUserInfoDao::class.java)

        private fun fromArchive(
            info: UserInfoLite?,
            stats: UserStatisticsLite?,
            rank: List<UserGlobalRankLite>?,
            percent: UserRankPercentLite?
        ): OsuUser? {
            if (info == null) {
                return null
            }

            val ranks = rank?.parseToRanks()

            val latestRank = ranks?.lastOrNull()

            return OsuUser(info.userID).apply {
                this.beatmapPlaycount = info.beatmapPlaycount
                this.userAchievementsCount = info.achievementsCount
                this.currentOsuMode = info.mode.toOsuMode()

                if (!ranks.isNullOrEmpty()) {
                    this.rankHistory = OsuUser.RankHistory(info.mode.toOsuMode().shortName, ranks)
                }

                if (stats != null) {
                    this.statistics = Statistics().apply {
                        this.rankedScore = stats.rankedScore
                        this.totalScore = stats.totalScore
                        this.totalHits = stats.totalHits
                        this.playTime = stats.playTime
                        this.playCount = stats.playCount
                        this.pp = stats.pp.toDouble()
                        this.accuracy = stats.accuracy.toDouble()
                        this.countSSH = stats.countSSH
                        this.countSS = stats.countSS
                        this.countSH = stats.countSH
                        this.countS = stats.countS
                        this.countA = stats.countA
                        this.replaysWatchedByOthers = stats.replaysWatched
                        this.maxCombo = stats.maxCombo
                        this.levelCurrent = stats.levelCurrent
                        this.levelProgress = stats.levelProgress

                        if (percent != null) {
                            this.globalRankPercent = percent.globalRankPercent
                            this.countryRank = percent.countryRank
                        }

                        if (latestRank != null) {
                            this.globalRank = latestRank
                        }
                    }
                } else if (percent != null) {
                    this.statistics = Statistics().apply {
                        this.globalRankPercent = percent.globalRankPercent
                        this.countryRank = percent.countryRank

                        if (latestRank != null) {
                            this.globalRank = latestRank
                        }
                    }
                } else if (latestRank != null) {
                    this.statistics = Statistics().apply {
                        this.globalRank = latestRank
                    }
                }
            }
        }
    }
}
