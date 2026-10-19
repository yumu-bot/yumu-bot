package com.now.nowbot.dao

import com.now.nowbot.mapper.OsuUserInfoPercentilesLiteRepository
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.osu.Statistics
import com.now.nowbot.cache.PercentileCacheProvider
import com.now.nowbot.entity.UserGlobalRankLite
import com.now.nowbot.entity.UserGlobalRankLite.Companion.parseToRankArray
import com.now.nowbot.entity.UserInfoLite
import com.now.nowbot.entity.UserInfoLite.Companion.updateFrom
import com.now.nowbot.entity.UserRankPercentLite
import com.now.nowbot.entity.UserStatisticsLite
import com.now.nowbot.entity.UserStatisticsLite.Companion.updateFrom
import com.now.nowbot.mapper.UserGlobalRankRepository
import com.now.nowbot.mapper.UserInfoRepository
import com.now.nowbot.mapper.UserRankPercentRepository
import com.now.nowbot.mapper.UserStatisticsRepository
import com.now.nowbot.model.calculate.InfoLogStatistics
import com.now.nowbot.model.enums.OsuMode.Companion.toOsuMode
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.abs
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
    transactionManager: PlatformTransactionManager,
) {
    @PersistenceContext
    lateinit var entityManager: EntityManager

    private val transactionTemplate = TransactionTemplate(transactionManager)
    private val percentilesLock = ReentrantLock()
    private val userLocks = ConcurrentHashMap<Long, UserLock>()

    private class UserLock {
        val lock = ReentrantLock()
        var references = 0
    }

    private fun acquireUserLock(userID: Long): UserLock {
        return userLocks.compute(userID) { _, old ->
            (old ?: UserLock()).also { it.references++ }
        }!!
    }

    private fun releaseUserLock(userID: Long, lock: UserLock) {
        userLocks.computeIfPresent(userID) { _, current ->
            if (current !== lock) {
                current
            } else {
                current.references--
                if (current.references == 0) null else current
            }
        }
    }

    private inline fun <T> withUserLock(userID: Long, block: () -> T): T {
        val lock = acquireUserLock(userID)
        lock.lock.lock()
        try {
            return block()
        } finally {
            lock.lock.unlock()
            releaseUserLock(userID, lock)
        }
    }

    private fun <T> withUserLocks(userIDs: Collection<Long>, block: () -> T): T {
        val locks = userIDs.distinct().sorted().map { it to acquireUserLock(it) }
        try {
            locks.forEach { (_, lock) -> lock.lock.lock() }
            return block()
        } finally {
            locks.asReversed().forEach { (_, lock) -> lock.lock.unlock() }
            locks.forEach { (userID, lock) -> releaseUserLock(userID, lock) }
        }
    }

    fun percentilesDailyUpsert(): Int {
        if (!percentilesLock.tryLock()) {
            log.warn("玩家百分比更新任务已在运行，跳过本次触发")
            return 0
        }

        try {
            log.info("正在更新玩家百分比")
            val now = LocalDate.now(ZoneOffset.UTC)

            // 以统计表为准：批量写入路径只更新 user_statistics，不会写 user_info。
            val statsList = userStatisticsRepository.getLatestBetween(now.minusDays(1), now)
            if (statsList.isEmpty()) {
                log.info("没有需要更新百分比的玩家数据。")
                return 0
            }

            val infoMap = userInfoRepository.getLatestBetween(now.minusDays(1), now)
                .associateBy { it.userID to it.mode }

            val exists = statsList.groupBy({ it.mode }, { it.userID })

            val globalMap = exists.flatMap { (mode, users) ->
                userGlobalRankRepository.getLatest(users, mode)
            }.associate { (it.userID to it.mode) to it.rank }

            val countryMap = exists.flatMap { (mode, users) ->
                userRankPercentRepository.getLatest(users, mode)
            }.associate { (it.userID to it.mode) to it.countryRank }

            log.info("已经获取到 ${statsList.size} 条数据，正在更新")

            var updated = 0
            statsList.chunked(1000).forEach { chunk ->
                runCatching {
                    transactionTemplate.executeWithoutResult {
                        entityManager.clear()
                        chunk.forEach { stat ->
                            val key = stat.userID to stat.mode
                            upsertPercentile(infoMap[key], stat, globalMap[key], countryMap[key])
                        }
                        entityManager.flush()
                    }
                }.onSuccess {
                    updated += chunk.size
                }.onFailure { e ->
                    // 每个批次使用独立事务，单批失败不会影响其他玩家。
                    log.error("更新玩家百分比失败，本批 ${chunk.size} 条已跳过", e)
                }
            }

            runCatching {
                percentileRepository.deleteOlderThan(LocalDateTime.now().minusDays(PERCENTILE_RETENTION_DAYS))
            }.onFailure { e ->
                log.warn("清理过期玩家百分比失败", e)
            }

            // 等所有事务提交后再刷新缓存，避免 05:25 的定时任务读到旧数据。
            runCatching {
                percentileCacheProvider.refreshCache()
            }.onFailure { e ->
                log.error("刷新玩家百分比缓存失败", e)
            }

            log.info("更新完成，共更新 $updated 条玩家百分比数据。")
            return updated
        } finally {
            percentilesLock.unlock()
        }
    }

    fun upsertPercentile(info: UserInfoLite?, stat: UserStatisticsLite, global: Long?, country: Long?) {
        percentileRepository.upsert(
            userID = stat.userID,
            mode = stat.mode,
            updatedAt = LocalDateTime.now(),
            // 0 不是合法名次，传 null 让 COALESCE 保留旧值。
            globalRank = global?.takeIf { it > 0 },
            countryRank = country?.takeIf { it > 0 },
            level = stat.levelCurrent * 100 + stat.levelProgress,
            rankCountScore = 3 * (stat.countSS + stat.countSSH) + 2 * (stat.countSH + stat.countS) + stat.countA,
            playCount = stat.playCount,
            totalHits = stat.totalHits,
            playTime = stat.playTime,
            rankedScore = stat.rankedScore,
            totalScore = stat.totalScore,
            beatmapPlaycount = info?.beatmapPlaycount ?: 0,
            replaysWatched = stat.replaysWatched,
            maximumCombo = stat.maxCombo,
            achievementsCount = info?.achievementsCount ?: 0
        )
    }

    fun getPercentiles(user: OsuUser, mode: OsuMode): Map<String, Double> {
        // sb 服的模式转换，复用 OsuMode 自己的映射规则（含 mania 7k -> mania）。
        val modeValue: Byte = if (mode == OsuMode.DEFAULT) {
            user.mode.safeModeValue
        } else {
            mode.safeModeValue
        }

        val stats = percentileCacheProvider.cachedData.get()[modeValue]
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
                withUserLock(user.userID) {
                    transactionTemplate.executeWithoutResult {
                        upsertUserToday(user)
                    }
                }
            }.onFailure { e ->
                log.error("玩家数据存储：存储失败：", e)
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
                log.error("玩家数据存储：批量存储失败：", e)
            }
        }
    }

    /**
     * 这里的 users 必须是 isVariant = true 后获取的
     */
    fun saveUsersToday(users: List<MicroUser>) {
        if (users.isEmpty()) return

        withUserLocks(users.map { it.userID }) {
            transactionTemplate.executeWithoutResult {
                users.flatMap { user ->
                    val rulesets = user.rulesets ?: return@flatMap emptyList()

                    buildList {
                        rulesets.osu?.let { add(Triple(user.userID, OsuMode.OSU, it)) }
                        rulesets.taiko?.let { add(Triple(user.userID, OsuMode.TAIKO, it)) }
                        rulesets.fruits?.let { add(Triple(user.userID, OsuMode.CATCH, it)) }
                        rulesets.mania?.let { add(Triple(user.userID, OsuMode.MANIA, it)) }
                    }
                }
                    .filter { (_, _, stat) ->
                        (stat.playCount ?: 0L) > 0L && (stat.playTime ?: 0L) > 0L
                    }
                    .groupBy { it.second }.forEach { (mode, triples) ->
                        val stats = triples.map { (userID, _, stat) -> Pair(userID, stat) }

                        batchUpsertUserStatistics(mode, stats)
                        batchUpsertGlobalRank(mode, stats)

                        // 批量接口的统计对象里已经有最新的国家排名，不能再用旧表里的值。
                        val percents = triples.map { (userID, _, stat) ->
                            Triple(userID, stat, stat.countryRank ?: 0L)
                        }

                        batchUpsertPercent(mode, percents)
                    }
            }
        }
    }

    fun getHistoryUser(user: OsuUser, duration: Duration = 1.days): OsuUser? {
        val today = LocalDate.now(ZoneOffset.UTC)

        val target = today.minusDays(duration.inWholeDays)
        val mode = user.mode.modeValue

        val info = userInfoRepository.getLatestValid(user.userID, mode, target)
        val stats = userStatisticsRepository.getLatestValid(user.userID, mode, target)
        val ranks = userGlobalRankRepository.getBetween(user.userID, mode, target.minusDays(89), target)
        val closest = userGlobalRankRepository.getClosestFromDate(user.userID, mode, target)
        val percent = userRankPercentRepository.getLatest(user.userID, mode, target)

        return fromArchive(info, stats, ranks, percent, closest?.rank, target)
    }

    fun getClosestFromTarget(userID: Long, mode: OsuMode, targetPP: Double): OsuUser? {
        val stats = userStatisticsRepository.getClosestFromTarget(userID, mode.modeValue, targetPP) ?: return null

        // createdAt 才是这条数据真正开始生效的日期；updatedAt 可能只是覆盖到今天。
        val targetDate = stats.createdAt

        val info = userInfoRepository.getClosestFromDate(userID, mode.modeValue, targetDate)
        val ranks = userGlobalRankRepository.getBetween(userID, mode.modeValue, targetDate.minusDays(89), targetDate)
        val closest = userGlobalRankRepository.getClosestFromDate(userID, mode.modeValue, targetDate)
        val percent = userRankPercentRepository.getClosestFromDate(userID, mode.modeValue, targetDate)

        return fromArchive(info, stats, ranks, percent, closest?.rank, targetDate)
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

//    fun getLatestBatchBetween(userIDs: List<Long>, from: LocalDate, to: LocalDate): List<UserStatisticsLite> {
//        return userStatisticsRepository.getLatestBatchBetween(userIDs, from, to)
//    }
//
//    fun getLatestBatchFrom(userIDs: List<Long>, from: LocalDate): List<UserStatisticsLite> {
//        return userStatisticsRepository.getLatestBatchFrom(userIDs, from)
//    }

    /**
     * 优先获取等于目标天的数据，其次，获取从新到老的第一条数据
     */
    fun getTargetOrLatestBatch(userIDs: Collection<Long>, target: LocalDate): List<UserStatisticsLite> {
        return userStatisticsRepository.getTargetOrLatestBatch(userIDs, target)
    }
    // Yumu 0.8.2 玩家信息优化

    private fun upsertGlobalRank(user: OsuUser) {
        val history = user.rankHistory?.data ?: return
        val mode = user.mode.modeValue
        val today = LocalDate.now(ZoneOffset.UTC)

        val totalDays = history.size
        val incomingMap: Map<LocalDate, Long> = history.mapIndexed { index, rank ->
            val daysAgo = (totalDays - 1) - index
            val date = today.minusDays(daysAgo.toLong())
            date to rank
        }.filter { it.second > 0L }.toMap()

        if (incomingMap.isEmpty()) return

        val targetDates = incomingMap.keys
        val startDate = targetDates.min()
        val endDate = targetDates.max()

        val existingEntities = userGlobalRankRepository.getBetween(user.userID, mode, startDate, endDate)
            .associateBy { it.date }

        val entitiesToSave = ArrayList<UserGlobalRankLite>(incomingMap.size)

        for ((date, rank) in incomingMap) {
            val existing = existingEntities[date]
            if (existing == null) {
                entitiesToSave.add(
                    UserGlobalRankLite(
                        userID = user.userID,
                        mode = mode,
                        date = date,
                        rank = rank
                    )
                )
            } else if (existing.rank != rank) {
                existing.rank = rank
                entitiesToSave.add(existing)
            }
        }

        if (entitiesToSave.isNotEmpty()) {
            userGlobalRankRepository.saveAll(entitiesToSave)
        }
    }

    /**
     * 批量接口只返回 statistics，不返回 rankHistory；这里把 statistics.globalRank 补成 user_rank 的当日快照，
     * 否则只走批量路径的玩家永远没有全球排名，百分比表里的 global_rank 也会一直是 NULL。
     */
    private fun batchUpsertGlobalRank(mode: OsuMode, inputs: List<Pair<Long, Statistics>>) {
        val today = LocalDate.now(ZoneOffset.UTC)
        val incoming = inputs.mapNotNull { (userID, statistics) ->
            val rank = statistics.globalRank?.takeIf { it > 0 } ?: return@mapNotNull null
            userID to rank
        }.toMap()

        if (incoming.isEmpty()) return

        val latestMap = userGlobalRankRepository.getLatest(incoming.keys, mode.modeValue)
            .associateBy { it.userID }

        val entitiesToSave = mutableListOf<UserGlobalRankLite>()

        incoming.forEach { (userID, rank) ->
            val latest = latestMap[userID]
            when {
                latest == null || latest.date.isBefore(today) -> {
                    entitiesToSave.add(
                        UserGlobalRankLite(
                            userID = userID,
                            rank = rank,
                            mode = mode.modeValue,
                            date = today,
                        )
                    )
                }

                latest.date == today && latest.rank != rank -> {
                    latest.rank = rank
                    entitiesToSave.add(latest)
                }
            }
        }

        if (entitiesToSave.isNotEmpty()) {
            userGlobalRankRepository.saveAll(entitiesToSave)
        }
    }

    private fun upsertPercentSnapshot(
        userID: Long,
        mode: OsuMode,
        countryRank: Long,
        globalRankPercent: Double?,
        latest: UserRankPercentLite? = userRankPercentRepository.getLatest(userID, mode.modeValue),
    ) {
        val today = LocalDate.now(ZoneOffset.UTC)

        // 0 不是有效的国家排名，沿用上一条非 0 数据，避免把有效名次覆盖成 0。
        val resolvedCountryRank = if (countryRank > 0) {
            countryRank
        } else {
            latest?.countryRank?.takeIf { it > 0 } ?: 0L
        }
        val resolvedGlobalPercent = globalRankPercent ?: latest?.globalRankPercent ?: 1.0

        if (latest != null && latest.date == today) {
            // 今天已有快照：只更新今天的行，保留原来的 created 语义。
            latest.countryRank = resolvedCountryRank
            latest.globalRankPercent = resolvedGlobalPercent
            userRankPercentRepository.upsert(latest)
        } else {
            // 每天一行；跨天时新建实体，绝不修改旧行的 date。
            val entity = UserRankPercentLite(
                userID = userID,
                globalRankPercent = resolvedGlobalPercent,
                countryRank = resolvedCountryRank,
                mode = mode.modeValue,
                date = today,
            )
            userRankPercentRepository.upsert(entity)
        }
    }

    private fun upsertPercent(userID: Long, mode: OsuMode, statistics: Statistics, countryRank: Long = 0L) {
        upsertPercentSnapshot(userID, mode, countryRank, statistics.globalRankPercent)
    }

    private fun upsertPercent(user: OsuUser) {
        val statistics = user.statistics ?: return
        upsertPercentSnapshot(user.userID, user.mode, user.countryRank, statistics.globalRankPercent)
    }

    private fun batchUpsertPercent(mode: OsuMode, inputs: List<Triple<Long, Statistics, Long>>) {
        if (inputs.isEmpty()) return

        val latestMap = userRankPercentRepository.getLatestBatch(inputs.map { it.first }.toSet(), mode.modeValue)
            .associateBy { it.userID }

        // 同一个 userID 只处理一次。
        inputs.associateBy { it.first }.forEach { (userID, input) ->
            val (_, statistics, countryRank) = input
            upsertPercentSnapshot(
                userID = userID,
                mode = mode,
                countryRank = countryRank,
                globalRankPercent = statistics.globalRankPercent,
                latest = latestMap[userID],
            )
        }
    }

    private fun upsertUserInfo(user: OsuUser) {
        val today = LocalDate.now(ZoneOffset.UTC)
        val latest = userInfoRepository.getLatest(user.userID, user.mode.modeValue)

        if (latest == null) {
            val entity = UserInfoLite().updateFrom(user).apply {
                this.createdAt = today
                this.updatedAt = today
            }
            userInfoRepository.save(entity)
            return
        }

        val changed = user.beatmapPlaycount > latest.beatmapPlaycount ||
                user.userAchievementsCount > latest.achievementsCount

        when {
            latest.updatedAt == today -> {
                if (changed) {
                    latest.updateFrom(user)
                    latest.updatedAt = today
                    userInfoRepository.upsert(latest)
                }
            }

            latest.updatedAt.isBefore(today) -> {
                if (changed) {
                    closeUserInfo(latest, today)
                    val entity = UserInfoLite().updateFrom(user).apply {
                        this.createdAt = today
                        this.updatedAt = today
                    }
                    userInfoRepository.save(entity)
                } else {
                    userInfoRepository.update(latest.id!!, today)
                }
            }
        }
    }

    private fun closeUserInfo(latest: UserInfoLite, today: LocalDate) {
        val yesterday = today.minusDays(1)
        if (latest.updatedAt.isBefore(yesterday) && !latest.createdAt.isAfter(yesterday)) {
            // 旧快照的覆盖范围最多补到昨天，今天由新快照接上，避免日期断层。
            userInfoRepository.update(latest.id!!, yesterday)
        }
    }

    private fun upsertUserStatistics(userID: Long, mode: OsuMode, statistics: Statistics) {
        upsertStatisticsSnapshot(userID, mode, statistics)
    }

    private fun upsertUserStatistics(user: OsuUser) {
        val statistics = user.statistics ?: return
        if ((statistics.totalHits ?: 0L) == 0L) return
        if (user.mode.isDefault) return

        upsertStatisticsSnapshot(user.userID, user.mode, statistics)
    }

    private fun upsertStatisticsSnapshot(userID: Long, mode: OsuMode, statistics: Statistics) {
        val today = LocalDate.now(ZoneOffset.UTC)
        val latest = userStatisticsRepository.getLatest(userID, mode.modeValue)

        if (latest == null) {
            val entity = UserStatisticsLite().updateFrom(userID, mode, statistics).apply {
                this.createdAt = today
                this.updatedAt = today
            }
            userStatisticsRepository.save(entity)
            return
        }

        val changed = statisticsChanged(latest, statistics)

        when {
            latest.updatedAt == today -> {
                if (changed) {
                    latest.updateFrom(userID, mode, statistics)
                    latest.updatedAt = today
                    userStatisticsRepository.upsert(latest)
                }
            }

            latest.updatedAt.isBefore(today) -> {
                if (changed) {
                    closeUserStatistics(latest, today)
                    val entity = UserStatisticsLite().updateFrom(userID, mode, statistics).apply {
                        this.createdAt = today
                        this.updatedAt = today
                    }
                    userStatisticsRepository.save(entity)
                } else {
                    userStatisticsRepository.update(latest.id!!, today)
                }
            }
        }
    }

    private fun closeUserStatistics(latest: UserStatisticsLite, today: LocalDate) {
        val yesterday = today.minusDays(1)
        if (latest.updatedAt.isBefore(yesterday) && !latest.createdAt.isAfter(yesterday)) {
            userStatisticsRepository.update(latest.id!!, yesterday)
        }
    }

    /**
     * 单条记录的变更判定。只比较能可靠判断“确实推进了”的字段，避免把批量接口缺失的字段当成 0 写回。
     */
    private fun statisticsChanged(latest: UserStatisticsLite, statistics: Statistics): Boolean {
        fun increased(old: Long, value: Long?): Boolean = value != null && value > old
        fun changed(old: Float, value: Double?): Boolean =
            value != null && value > 0.0 && abs(value - old) > 1e-4

        return increased(latest.totalHits, statistics.totalHits) ||
                increased(latest.playCount, statistics.playCount) ||
                increased(latest.playTime, statistics.playTime) ||
                increased(latest.rankedScore, statistics.rankedScore) ||
                increased(latest.totalScore, statistics.totalScore) ||
                changed(latest.pp, statistics.pp) ||
                changed(latest.accuracy, statistics.accuracy) ||
                statistics.countSSH > latest.countSSH ||
                statistics.countSS > latest.countSS ||
                statistics.countSH > latest.countSH ||
                statistics.countS > latest.countS ||
                statistics.countA > latest.countA ||
                statistics.replaysWatchedByOthers > latest.replaysWatched ||
                statistics.maxCombo > latest.maxCombo ||
                statistics.levelCurrent > latest.levelCurrent ||
                (statistics.levelCurrent == latest.levelCurrent && statistics.levelProgress > latest.levelProgress)
    }

    private fun batchUpsertUserStatistics(mode: OsuMode, inputs: List<Pair<Long, Statistics>>) {
        if (inputs.isEmpty() || mode.modeValue !in 0..3) return

        val today = LocalDate.now(ZoneOffset.UTC)
        val yesterday = today.minusDays(1)
        val distinctInputs = inputs.associateBy({ it.first }, { it.second })
        val userIDs = distinctInputs.keys

        val latestMap = userStatisticsRepository.getLatestBatch(userIDs, mode.modeValue)
            .associateBy { it.userID }

        val entitiesToSave = mutableListOf<UserStatisticsLite>()
        val idsToClose = mutableListOf<Long>()
        val idsToTouch = mutableListOf<Long>()

        distinctInputs.forEach { (userID, statistics) ->
            val latest = latestMap[userID]

            when {
                latest == null -> {
                    entitiesToSave.add(
                        UserStatisticsLite().apply {
                            updateFrom(userID, mode, statistics)
                            createdAt = today
                            updatedAt = today
                        }
                    )
                }

                latest.updatedAt == today -> {
                    if (statisticsChanged(latest, statistics)) {
                        latest.updateFrom(userID, mode, statistics)
                        latest.updatedAt = today
                        entitiesToSave.add(latest)
                    }
                }

                latest.updatedAt.isBefore(today) -> {
                    if (statisticsChanged(latest, statistics)) {
                        // 旧快照补到昨天，新快照从今天开始。
                        latest.id?.let { idsToClose.add(it) }
                        entitiesToSave.add(
                            UserStatisticsLite().apply {
                                updateFrom(userID, mode, statistics)
                                createdAt = today
                                updatedAt = today
                            }
                        )
                    } else {
                        // 数据没有变化，只把旧记录的覆盖范围延长到今天。
                        latest.id?.let { idsToTouch.add(it) }
                    }
                }
            }
        }

        if (entitiesToSave.isNotEmpty()) {
            userStatisticsRepository.saveAll(entitiesToSave)
        }

        if (idsToClose.isNotEmpty()) {
            userStatisticsRepository.updateBatchUpdatedAt(idsToClose, yesterday)
        }

        if (idsToTouch.isNotEmpty()) {
            userStatisticsRepository.updateBatchUpdatedAt(idsToTouch, today)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OsuUserInfoDao::class.java)
        private const val PERCENTILE_RETENTION_DAYS = 7L

        private fun fromArchive(
            info: UserInfoLite?,
            stats: UserStatisticsLite?,
            ranks: List<UserGlobalRankLite>?,
            percent: UserRankPercentLite?,
            closestRank: Long? = null,
            targetDate: LocalDate = LocalDate.now(ZoneOffset.UTC)
        ): OsuUser? {
            // 批量路径可能只写了 statistics，没有 user_info；此时仍然应该能还原历史数据。
            val userID = info?.userID ?: stats?.userID ?: percent?.userID ?: return null
            val mode = info?.mode ?: stats?.mode ?: percent?.mode ?: return null

            val rankArray = ranks?.parseToRankArray(targetDate)
            val latestRank = rankArray?.lastOrNull()
            val shortName = mode.toOsuMode().shortName

            return OsuUser(userID).apply {
                this.beatmapPlaycount = info?.beatmapPlaycount ?: 0
                this.userAchievementsCount = info?.achievementsCount ?: 0
                this.mode = mode.toOsuMode()

                if (!rankArray.isNullOrEmpty()) {
                    this.rankHistory = OsuUser.RankHistory(shortName, rankArray)
                }

                if (stats != null) {
                    this.statistics = InfoLogStatistics().apply {
                        // createdAt 才是快照开始日期；updatedAt 可能只是“数据仍有效至今”。
                        this.logTime = stats.createdAt.atStartOfDay()

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
                        } else if (closestRank != null) {
                            this.globalRank = closestRank
                        }
                    }
                } else if (percent != null) {
                    this.statistics = Statistics().apply {
                        this.globalRankPercent = percent.globalRankPercent
                        this.countryRank = percent.countryRank

                        if (latestRank != null) {
                            this.globalRank = latestRank
                        } else if (closestRank != null) {
                            this.globalRank = closestRank
                        }
                    }
                } else if (latestRank != null) {
                    this.statistics = Statistics().apply {
                        this.globalRank = latestRank
                    }
                } else if (closestRank != null) {
                    this.statistics = Statistics().apply {
                        this.globalRank = closestRank
                    }
                }
            }
        }
    }
}
