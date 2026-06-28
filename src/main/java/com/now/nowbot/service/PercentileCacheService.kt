package com.now.nowbot.service

import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.now.nowbot.mapper.OsuUserInfoPercentilesLiteRepository
import org.springframework.stereotype.Service

@Service
class PercentileCacheService(
    private val percentileRepository: OsuUserInfoPercentilesLiteRepository,
) {
    // 使用只读的 Map 存储各模式的统计数据，确保线程安全
    @Volatile
    final var cachedData: Map<Byte, ModeStats> = emptyMap()

    // 将统计数据封装到一个数据类中
    class ModeStats(
        val globalRanks: LongArray,
        val countryRanks: LongArray,
        val levels: IntArray,
        val rankCountScores: IntArray,
        val playCounts: LongArray,
        val totalHits: LongArray,
        val playTimes: LongArray,
        val rankedScores: LongArray,
        val totalScores: LongArray,
        val beatmapPlaycounts: IntArray,
        val replaysWatcheds: IntArray,
        val maximumCombos: IntArray,
        val achievementCounts: IntArray,
    )

    // 每 30 分钟后台刷新一次数据
    @CanIgnoreReturnValue
    fun refreshCache(): Map<Byte, Int> {
        val all = percentileRepository.findAll()

        // 按模式分组处理
        val newData = all.groupBy { it.mode }.mapValues { (_, records) ->

            // 使用 List 替代 TreeSet，保留所有数据
            val globalRankList = mutableListOf<Long>()
            val countryRankList = mutableListOf<Long>()
            val levelList = mutableListOf<Int>()
            val rankCountScoreList = mutableListOf<Int>()
            val playCountList = mutableListOf<Long>()
            val totalHitList = mutableListOf<Long>()
            val playTimeList = mutableListOf<Long>()
            val rankedScoreList = mutableListOf<Long>()
            val totalScoreList = mutableListOf<Long>()
            val beatmapPlaycountList = mutableListOf<Int>()
            val replaysWatchedList = mutableListOf<Int>()
            val maximumComboList = mutableListOf<Int>()
            val achievementCountList = mutableListOf<Int>()

            records.forEach {
                if (it.globalRank != null && it.globalRank!! > 0) {
                    globalRankList.add(it.globalRank!!)
                }
                if (it.countryRank != null && it.countryRank!! > 0) {
                    countryRankList.add(it.countryRank!!)
                }
                if (it.level > 0) levelList.add(it.level)
                if (it.rankCountScore > 0) rankCountScoreList.add(it.rankCountScore)
                if (it.playCount > 0) playCountList.add(it.playCount)
                if (it.totalHits > 0) totalHitList.add(it.totalHits)
                if (it.playTime > 0) playTimeList.add(it.playTime)
                if (it.rankedScore > 0) rankedScoreList.add(it.rankedScore)
                if (it.totalScore > 0) totalScoreList.add(it.totalScore)
                if (it.replaysWatched > 0) replaysWatchedList.add(it.replaysWatched)
                if (it.maximumCombo > 0) maximumComboList.add(it.maximumCombo)

                if (it.beatmapPlaycount > 0) beatmapPlaycountList.add(it.beatmapPlaycount)
                if (it.achievementsCount > 0) achievementCountList.add(it.achievementsCount)
            }

            ModeStats(
                globalRanks = globalRankList.apply { sort() }.toLongArray(),
                countryRanks = countryRankList.apply { sort() }.toLongArray(),
                levels = levelList.apply { sort() }.toIntArray(),
                rankCountScores = rankCountScoreList.apply { sort() }.toIntArray(),
                playCounts = playCountList.apply { sort() }.toLongArray(),
                totalHits = totalHitList.apply { sort() }.toLongArray(),
                playTimes = playTimeList.apply { sort() }.toLongArray(),
                rankedScores = rankedScoreList.apply { sort() }.toLongArray(),
                totalScores = totalScoreList.apply { sort() }.toLongArray(),
                beatmapPlaycounts = beatmapPlaycountList.apply { sort() }.toIntArray(),
                replaysWatcheds = replaysWatchedList.apply { sort() }.toIntArray(),
                maximumCombos = maximumComboList.apply { sort() }.toIntArray(),
                achievementCounts = achievementCountList.apply { sort() }.toIntArray()
            )
        }

        // 替换旧缓存（原子操作）
        cachedData = newData

        return cachedData.mapValues { (_, stats) ->
            stats.globalRanks.size +
                    stats.countryRanks.size +
                    stats.levels.size +
                    stats.rankCountScores.size +
                    stats.playCounts.size +
                    stats.totalHits.size +
                    stats.playTimes.size +
                    stats.rankedScores.size +
                    stats.totalScores.size +
                    stats.beatmapPlaycounts.size +
                    stats.replaysWatcheds.size +
                    stats.maximumCombos.size +
                    stats.achievementCounts.size
        }
    }
}