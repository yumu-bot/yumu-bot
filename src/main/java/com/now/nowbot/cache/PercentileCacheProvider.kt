package com.now.nowbot.cache

import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.now.nowbot.mapper.OsuUserInfoPercentilesLiteRepository
import org.springframework.stereotype.Component

@Component
class PercentileCacheProvider(
    private val percentileRepository: OsuUserInfoPercentilesLiteRepository,
) {
    val cachedData = java.util.concurrent.atomic.AtomicReference<Map<Byte, ModeStats>>(emptyMap())

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

    @CanIgnoreReturnValue
    fun refreshCache(): Map<Byte, Int> {
        val all = percentileRepository.findAll()

        // 按模式分组处理
        val newData = all.groupBy { it.mode }
            .toSortedMap()
            .mapValues { (_, records) ->

            // 使用 List 替代 TreeSet，保留所有数据
            val globalRankList = ArrayList<Long>(records.size)
            val countryRankList = ArrayList<Long>(records.size)
            val levelList = ArrayList<Int>(records.size)
            val rankCountScoreList = ArrayList<Int>(records.size)
            val playCountList = ArrayList<Long>(records.size)
            val totalHitList = ArrayList<Long>(records.size)
            val playTimeList = ArrayList<Long>(records.size)
            val rankedScoreList = ArrayList<Long>(records.size)
            val totalScoreList = ArrayList<Long>(records.size)
            val beatmapPlaycountList = ArrayList<Int>(records.size)
            val replaysWatchedList = ArrayList<Int>(records.size)
            val maximumComboList = ArrayList<Int>(records.size)
            val achievementCountList = ArrayList<Int>(records.size)

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
        cachedData.set(newData)

        return newData.mapValues { (_, stats) ->
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