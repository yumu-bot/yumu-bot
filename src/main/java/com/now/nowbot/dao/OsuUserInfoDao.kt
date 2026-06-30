package com.now.nowbot.dao

import com.now.nowbot.entity.OsuUserInfoArchiveLite
import com.now.nowbot.entity.OsuUserInfoArchiveLite.InfoArchive
import com.now.nowbot.mapper.OsuUserInfoPercentilesLiteRepository
import com.now.nowbot.mapper.OsuUserInfoRepository
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.calculate.InfoLogStatistics
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.osu.Statistics
import com.now.nowbot.service.PercentileCacheService
import com.now.nowbot.util.JacksonUtil
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Component
class OsuUserInfoDao(
    private val infoRepository: OsuUserInfoRepository,
    private val percentileRepository: OsuUserInfoPercentilesLiteRepository,
    private val percentileCacheService: PercentileCacheService
) {
    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Transactional
    fun percentilesDailyUpsert(): Int {
        log.info("正在更新玩家百分比")
        entityManager.clear()

        val now = LocalDateTime.now()

        val users = infoRepository.getLatestBetween(now.minusDays(1), now)

        log.info("已经获取到 ${users.size} 条数据，正在更新")

        entityManager.clear()

        users.forEachIndexed { index, user ->
            upsert(user)

            // 每处理 1000 条数据，强制同步并清空一次缓存
            if (index % 1000 == 0 && index > 0) {
                entityManager.flush() // 确保 SQL 已经发给数据库
                entityManager.clear() // 释放内存中的对象引用
                // log.info("已处理 $index 条...")
            }
        }

        log.info("更新完成。")

        return users.size
    }

    fun upsert(info: OsuUserInfoArchiveLite) {
        percentileRepository.upsert(
            userID = info.userID,
            mode = info.mode.modeValue,
            updatedAt = LocalDateTime.now(),
            globalRank = info.globalRank,
            countryRank = info.countryRank,
            level = info.levelCurrent * 100 + info.levelProgress,
            rankCountScore = 3 * (info.countSS + info.countSSH) + 2 * (info.countSH + info.countS) + info.countA,
            playCount = info.playCount,
            totalHits = info.totalHits,
            playTime = info.playTime,
            rankedScore = info.rankedScore,
            totalScore = info.totalScore,
            beatmapPlaycount = info.beatmapPlaycount,
            replaysWatched = info.replaysWatched,
            maximumCombo = info.maximumCombo,
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

        val stats = percentileCacheService.cachedData[modeValue]
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

    fun saveUserTodayAsync(user: OsuUser, mode: OsuMode) {
        Thread.startVirtualThread {
            runCatching {
                saveUserToday(user, mode)
            }.onFailure { e ->
                log.info("玩家数据存储：存储失败：", e)
            }
        }
    }

    private fun saveUserToday(user: OsuUser, mode: OsuMode) {
        val now = LocalDateTime.now()
        val today = LocalDate.now()

        val last = getLastToday(user.userID, mode, today)

        if (last != null && user.playCount > 0) {

            // 今天内已经有数据，但是最新的数据发生了变化
            if (user.playCount != last.playCount) {
                infoRepository.removeBetween(user.userID, mode, LocalDateTime.of(today, LocalTime.MIN), now)
            } else {
                return
            }
        }

        val lite = fromModel(user, mode)
        infoRepository.saveAndFlush(lite)
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

    private fun saveUsersToday(users: List<MicroUser>) {
        val now = LocalDateTime.now().toLocalDate()

        users.flatMap {
            if (it.rulesets == null) return@flatMap emptyList<OsuUserInfoArchiveLite>()

            val crs: List<Array<Any>> = infoRepository.getLatestCountryRanks(it.userID)

            val rankMap = crs.associate { array ->
                val modeByte = (array[0] as Number).toByte()
                val countryRank = (array[1] as Number).toLong()

                modeByte to countryRank
            }

            val oc = rankMap[OsuMode.OSU.modeValue]
            val tc = rankMap[OsuMode.TAIKO.modeValue]
            val cc = rankMap[OsuMode.CATCH.modeValue]
            val mc = rankMap[OsuMode.MANIA.modeValue]

            val osu = fromStatistics(it.rulesets!!.osu, OsuMode.OSU, oc)
            if (osu != null) {
                osu.userID = it.userID
            }

            val taiko = fromStatistics(it.rulesets!!.taiko, OsuMode.TAIKO, tc)
            if (taiko != null) {
                taiko.userID = it.userID
            }

            val fruits = fromStatistics(it.rulesets!!.fruits, OsuMode.CATCH, cc)
            if (fruits != null) {
                fruits.userID = it.userID
            }

            val mania = fromStatistics(it.rulesets!!.mania, OsuMode.MANIA, mc)
            if (mania != null) {
                mania.userID = it.userID
            }

            return@flatMap listOf(osu, taiko, fruits, mania)
            }
            .filterNotNull()
            .forEach { user ->
                val today = getLastToday(user.userID, user.mode, now)

                if (today == null || (user.playCount != 0L && today.playCount != user.playCount)) {
                    infoRepository.save(user)
                }

            }
    }

    /**
     * 取这一天最后的数据
     *
     * @param date 这一天，不输入就是今天
     * @return 这一天最后的数据
     */
    private fun getLastToday(userID: Long, mode: OsuMode, date: LocalDate = LocalDate.now()): OsuUserInfoArchiveLite? {
        return infoRepository.getLatestBetween(userID, mode, LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX))
    }

    fun getHistoryUser(user: OsuUser, day: Long = 1): OsuUser? {
        return getLastFrom(
            user.userID,
            user.currentOsuMode,
            LocalDate.now().minusDays(day)
        )?.let { fromArchive(it) }
    }


    /**
     * 取 到那一天为止 最后的数据 (默认向前取一年)
     *
     * @param date 那一天
     */
    fun getLastFrom(userID: Long, mode: OsuMode, date: LocalDate): OsuUserInfoArchiveLite? {
        val time = LocalDateTime.of(date, LocalTime.MAX)
        return infoRepository.getLatestBetween(userID, mode, time.minusYears(1), time)
    }

    fun getLast(userID: Long, mode: OsuMode): OsuUserInfoArchiveLite? {
        return infoRepository.getLatest(userID, mode)
    }

    private fun getFromUserIDsYesterday(userIDs: List<Long>): List<InfoArchive> {
        val time = LocalDate.now().minusDays(1)
        return infoRepository.getFromUserIDs(userIDs, LocalDateTime.of(time, LocalTime.MIN), LocalDateTime.of(time, LocalTime.MAX))
    }

    fun getPlayCountsFromUserIDBeforeToday(userID: Long): List<Long> {

        val to = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.MAX)
        val from = to.minusYears(1)

        fun parseResults(arrays: List<Array<Any>>): List<Long> {
            // 1. 初始化一个长度为 4 的默认值列表 [0, 0, 0, 0]
            // 对应索引: 0:osu, 1:taiko, 2:catch, 3:mania
            val playCounts = mutableListOf(0L, 0L, 0L, 0L)

            arrays.forEach { row ->
                val count = (row[1] as Number).toLong()
                val modeIndex = (row[2] as Number).toInt()

                // 2. 根据数据库返回的 mode 值，精准填入对应的索引位置
                if (modeIndex in 0..3) {
                    playCounts[modeIndex] = count
                }
            }

            return playCounts
        }

        val arrays = infoRepository.getLatestPlayCounts(userID, from, to)
        return parseResults(arrays)
    }

    fun getClosestFromTarget(userID: Long, mode: OsuMode, target: Double): OsuUser? {
        val lite = infoRepository.getClosestFromTarget(userID, mode, target)

        return lite?.let { fromArchive(it) }
    }


    /*
    fun getPlayCountsFromUserIDBeforeToday(userID: Long): List<Long> {
        val time = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.MAX)
        val year = time.minusYears(1)

        val osu = infoRepository.getLatestBetween(userID, OsuMode.OSU, year, time)?.playCount ?: 0L
        val taiko = infoRepository.getLatestBetween(userID, OsuMode.TAIKO, year, time)?.playCount ?: 0L
        val catch = infoRepository.getLatestBetween(userID, OsuMode.CATCH, year, time)?.playCount ?: 0L
        val mania = infoRepository.getLatestBetween(userID, OsuMode.MANIA, year, time)?.playCount ?: 0L

        return listOf(osu, taiko, catch, mania)
    }

     */

    companion object {
        private val log = LoggerFactory.getLogger(OsuUserInfoDao::class.java)

        fun fromArchive(archive: OsuUserInfoArchiveLite): OsuUser {
            val user = OsuUser()
            user.mode = archive.mode.shortName
            user.id = archive.userID
            user.userAchievementsCount = archive.achievementsCount
            user.beatmapPlaycount = archive.beatmapPlaycount

            val statistics = InfoLogStatistics()
            statistics.countA = archive.countA
            statistics.countS = archive.countS
            statistics.countSS = archive.countSS
            statistics.countSH = archive.countSH
            statistics.countSSH = archive.countSSH

            statistics.globalRank = archive.globalRank
            statistics.globalRankPercent = archive.globalRankPercent
            statistics.countryRank = archive.countryRank
            statistics.totalScore = archive.totalScore
            statistics.totalHits = archive.totalHits
            statistics.rankedScore = archive.rankedScore
            statistics.accuracy = archive.accuracy
            statistics.playCount = archive.playCount
            statistics.playTime = archive.playTime
            statistics.levelCurrent = archive.levelCurrent
            statistics.levelProgress = archive.levelProgress
            statistics.maxCombo = archive.maximumCombo
            statistics.replaysWatchedByOthers = archive.replaysWatched
            statistics.pp = archive.pp

            statistics.logTime = archive.time

            user.statistics = statistics
            archive.rankHistory?.let {
                user.rankHistory = OsuUser.RankHistory(archive.mode.shortName,
                    JacksonUtil.parseObjectList(archive.rankHistory, Long::class.java)
                )
            }

            return user
        }

        fun fromModel(data: OsuUser, mode: OsuMode): OsuUserInfoArchiveLite {
            val archive = OsuUserInfoArchiveLite()

            archive.userID = data.userID
            archive.setLiteStatistics(data.statistics)

            archive.playCount = data.playCount
            archive.playTime = data.playTime
            data.rankHistory?.let { archive.rankHistory = it.data.toString() }
            archive.beatmapPlaycount = data.beatmapPlaycount
            archive.achievementsCount = data.userAchievementsCount

            // 过滤掉非法的游戏模式
            if (mode.isDefault()) {
                archive.mode = OsuMode.getMode(data.currentOsuMode.modeValue % 4)
            } else {
                archive.mode = OsuMode.getMode(mode.modeValue % 4)
            }

            archive.time = LocalDateTime.now()
            return archive
        }

        private fun OsuUserInfoArchiveLite.setLiteStatistics(statistics: Statistics?) {
            if (statistics == null) return

            this.globalRank = statistics.globalRank
            this.globalRankPercent = statistics.globalRankPercent
            this.countryRank = statistics.countryRank
            this.totalScore = statistics.totalScore ?: 0
            this.rankedScore = statistics.rankedScore ?: 0
            this.countA = statistics.countA
            this.countS = statistics.countS
            this.countSH = statistics.countSH
            this.countSS = statistics.countSS
            this.countSSH = statistics.countSSH

            this.accuracy = statistics.accuracy ?: 0.0
            this.pp = statistics.pp ?: 0.0
            this.levelCurrent = statistics.levelCurrent
            this.levelProgress = statistics.levelProgress
            this.isRanked = statistics.ranked ?: false
            this.maximumCombo = statistics.maxCombo
            this.replaysWatched = statistics.replaysWatchedByOthers
            this.totalHits = statistics.totalHits ?: 0
            this.playCount = statistics.playCount ?: 0
            this.playTime = statistics.playTime ?: 0
        }

        private fun fromStatistics(statistics: Statistics?, mode: OsuMode, countryRank: Long? = null): OsuUserInfoArchiveLite? {
            if (statistics == null) return null

            return OsuUserInfoArchiveLite().apply {
                this.mode = mode
                this.time = LocalDateTime.now()
                this.setLiteStatistics(statistics)

                countryRank?.let { this.countryRank = countryRank }
            }
        }
    }
}
