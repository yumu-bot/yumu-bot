package com.now.nowbot.dao

import com.now.nowbot.entity.OsuUserInfoArchiveLite
import com.now.nowbot.entity.OsuUserInfoArchiveLite.InfoArchive
import com.now.nowbot.mapper.OsuUserInfoPercentilesLiteRepository
import com.now.nowbot.mapper.OsuUserInfoRepository
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.InfoLogStatistics
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.osu.Statistics
import com.now.nowbot.util.JacksonUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.NavigableSet
import java.util.TreeSet

@Component
class OsuUserInfoDao(
    private val infoRepository: OsuUserInfoRepository,
    private val percentileRepository: OsuUserInfoPercentilesLiteRepository
) {

    fun percentilesDailyUpsert(): Int {
        log.info("正在更新玩家百分比")

        val now = LocalDateTime.now()

        val users = infoRepository.getLastBetween(now.minusDays(1), now)

        log.info("已经获取到 ${users.size} 条数据，正在更新")

        users.forEach { upsert(it) }

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
        val globalRankSet = TreeSet<Long>()
        val countryRankSet = TreeSet<Long>()
        val levelSet = TreeSet<Int>()
        val rankCountScoreSet = TreeSet<Int>()
        val playCountSet = TreeSet<Long>()
        val totalHitSet = TreeSet<Long>()
        val playTimeSet = TreeSet<Long>()
        val rankedScoreSet = TreeSet<Long>()
        val totalScoreSet = TreeSet<Long>()
        val beatmapPlaycountSet = TreeSet<Int>()
        val replaysWatchedSet = TreeSet<Int>()
        val maximumComboSet = TreeSet<Int>()
        val achievementCountSet = TreeSet<Int>()

        val all = percentileRepository.findAll()

        all.asSequence().forEach {
            if (it.mode == mode.modeValue) {
                if (it.globalRank != null && it.globalRank!! > 0) {
                    globalRankSet.add(it.globalRank!!)
                }

                if (it.countryRank != null && it.countryRank!! > 0) {
                    countryRankSet.add(it.countryRank!!)
                }

                if (it.level > 0) levelSet.add(it.level)

                if (it.rankCountScore > 0) rankCountScoreSet.add(it.rankCountScore)

                if (it.playCount > 0) playCountSet.add(it.playCount)

                if (it.totalHits > 0) totalHitSet.add(it.totalHits)

                if (it.playTime > 0) playTimeSet.add(it.playTime)

                if (it.rankedScore > 0) rankedScoreSet.add(it.rankedScore)

                if (it.totalScore > 0) totalScoreSet.add(it.totalScore)

                if (it.replaysWatched > 0) replaysWatchedSet.add(it.replaysWatched)

                if (it.maximumCombo > 0) maximumComboSet.add(it.maximumCombo)
            }

            if (it.beatmapPlaycount > 0) beatmapPlaycountSet.add(it.beatmapPlaycount)

            if (it.achievementsCount > 0) achievementCountSet.add(it.achievementsCount)
        }

        val stat = user.statistics

        val global = if (user.globalRank <= 0) {
            Long.MAX_VALUE
        } else {
            user.globalRank
        }

        val country = if (user.countryRank <= 0) {
            Long.MAX_VALUE
        } else {
            user.countryRank
        }

        val rankCountScore = 3 * ((stat?.countSS ?: 0) + (stat?.countSSH ?: 0)) + 2 * ((stat?.countSH ?: 0) + (stat?.countS ?: 0)) + (stat?.countA ?: 0)

        val level = user.levelCurrent * 100 + user.levelProgress
        
        // 3. 计算每个指标的百分位数
        return mapOf(
            // 对于排名指标：排名越好（数字越小），百分位数越高
            "global_rank" to calculatePercentileForNavigableSet(globalRankSet, global, false),
            "country_rank" to calculatePercentileForNavigableSet(countryRankSet, country, false),
            "level" to calculatePercentileForNavigableSet(levelSet, level, true),

            // 对于数值指标：数值越大，百分位数越高
            "rank_count_score" to calculatePercentileForNavigableSet(rankCountScoreSet, rankCountScore, true),
            "play_count" to calculatePercentileForNavigableSet(playCountSet, user.playCount, true),
            "total_hits" to calculatePercentileForNavigableSet(totalHitSet, user.totalHits, true),
            "play_time" to calculatePercentileForNavigableSet(playTimeSet, user.playTime, true),
            "ranked_score" to calculatePercentileForNavigableSet(rankedScoreSet, stat?.rankedScore, true),
            "total_score" to calculatePercentileForNavigableSet(totalScoreSet, stat?.totalScore, true),
            "beatmap_playcount" to calculatePercentileForNavigableSet(beatmapPlaycountSet, user.beatmapPlaycount, true),
            "replays_watched" to calculatePercentileForNavigableSet(replaysWatchedSet, stat?.replaysWatchedByOthers, true),
            "maximum_combo" to calculatePercentileForNavigableSet(maximumComboSet, stat?.maxCombo, true),
            "achievements_count" to calculatePercentileForNavigableSet(achievementCountSet, user.userAchievementsCount, true)
        )
    }

    private fun calculatePercentileForNavigableSet(
        sortedSet: NavigableSet<Int>,
        value: Int?,
        higherIsBetter: Boolean = true
    ): Double {
        if (value == null || sortedSet.isEmpty()) return 0.0

        return if (higherIsBetter) {
            // 对于数值越大越好的指标
            val headSet = sortedSet.headSet(value, true)
            headSet.size.toDouble() / sortedSet.size
        } else {
            // 对于数值越小越好的指标（排名）
            val tailSet = sortedSet.tailSet(value, true)
            tailSet.size.toDouble() / sortedSet.size
        }
    }

    private fun calculatePercentileForNavigableSet(
        sortedSet: NavigableSet<Long>,
        value: Long?,
        higherIsBetter: Boolean = true
    ): Double {
        if (value == null || sortedSet.isEmpty()) return 0.0

        return if (higherIsBetter) {
            // 对于数值越大越好的指标
            val headSet = sortedSet.headSet(value, true)
            headSet.size.toDouble() / sortedSet.size
        } else {
            // 对于数值越小越好的指标（排名）
            val tailSet = sortedSet.tailSet(value, true)
            tailSet.size.toDouble() / sortedSet.size
        }
    }

    fun saveUserToday(user: OsuUser, mode: OsuMode) {
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
        infoRepository.save(lite)
    }

    fun saveUsersToday(users: List<MicroUser>) {
        val now = LocalDateTime.now().toLocalDate()

        users.flatMap {
            if (it.rulesets == null) return@flatMap emptyList<OsuUserInfoArchiveLite>()

            val oc = infoRepository.getLastCountryRank(it.userID, OsuMode.OSU)
            val tc = infoRepository.getLastCountryRank(it.userID, OsuMode.TAIKO)
            val cc = infoRepository.getLastCountryRank(it.userID, OsuMode.CATCH)
            val mc = infoRepository.getLastCountryRank(it.userID, OsuMode.MANIA)

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

                if (today != null && user.playCount > 0 && today.playCount == user.playCount) {
                    return
                }

                infoRepository.save(user)
            }
    }

    /**
     * 取这一天最后的数据
     *
     * @param date 这一天，不输入就是今天
     * @return 这一天最后的数据
     */
    private fun getLastToday(userID: Long, mode: OsuMode, date: LocalDate = LocalDate.now()): OsuUserInfoArchiveLite? {
        return infoRepository.getLastBetween(userID, mode, LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX))
    }

    /**
     * 取 到那一天为止 最后的数据 (默认向前取一年)
     *
     * @param date 那一天
     */
    fun getLastFrom(userID: Long, mode: OsuMode, date: LocalDate): OsuUserInfoArchiveLite? {
        val time = LocalDateTime.of(date, LocalTime.MAX)
        return infoRepository.getLastBetween(userID, mode, time.minusYears(1), time)
    }

    fun getLast(userID: Long, mode: OsuMode): OsuUserInfoArchiveLite? {
        return infoRepository.getLast(userID, mode)
    }

    fun getFromYesterday(userIDs: List<Long>): List<InfoArchive> {
        return getFromUserIDsYesterday(userIDs)
    }

    private fun getFromUserIDsYesterday(userIDs: List<Long>): List<InfoArchive> {
        val time = LocalDate.now().minusDays(1)
        return infoRepository.getFromUserIDs(userIDs, LocalDateTime.of(time, LocalTime.MIN), LocalDateTime.of(time, LocalTime.MAX))
    }

    companion object {
        private val log = LoggerFactory.getLogger(OsuUserInfoDao::class.java)

        fun fromArchive(archive: OsuUserInfoArchiveLite): OsuUser {
            val user = OsuUser()
            user.mode = archive.mode.shortName
            user.id = archive.userID

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
                    JacksonUtil.parseObjectList(archive.rankHistory, Long::class.java) ?: listOf())
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
