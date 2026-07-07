package com.now.nowbot.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Entity
@Table(name = "user_rank")
@IdClass(UserGlobalRankKey::class)
class UserGlobalRankLite(
    @Id
    @Column(name = "user_id", columnDefinition = "int8", nullable = false)
    var userID: Long = 0L,

    @Column(name = "rank", columnDefinition = "int8", nullable = false)
    var rank: Long = 0L,

    @Id
    @Column(name = "mode", columnDefinition = "int2", nullable = false)
    var mode: Byte = (-1).toByte(),

    @Id
    @Column(name = "date", columnDefinition = "date", nullable = false)
    var date: LocalDate = LocalDate.now(ZoneOffset.UTC),
) {
    companion object {
        /**
         * 通过一段时间的 rankLites，来构建一个虚拟 ranks
         *
         * List<UserGlobalRankLite> 必须是时间降序排列
         */
        fun List<UserGlobalRankLite>.parseToRanks(date: LocalDate = LocalDate.now(ZoneOffset.UTC)): List<Long> {
            val daysCount = 90
            val startDate = date.minusDays((daysCount - 1).toLong())

            // 1. 转为以 LocalDate 为 Key 的 Map，用于 O(1) 判断某天是否有精确数据
            val map = this.associate { it.date to it.rank }

            // 2. 利用已有的降序排列，快速找到 startDate 之前（更早）离它最近的一条数据作为左边界兜底
            // 因为是 desc 排序，第一个比 startDate 小的就是最近的
            val structuralBeforeEntry = this.firstOrNull { it.date.isBefore(startDate) }

            // 3. 找出在这个 90 天区间内的所有有效数据，并反转为按时间正序（ASC），方便双指针双向查找
            val intervalData = this.filter { !it.date.isBefore(startDate) && !it.date.isAfter(date) }
                .reversed()

            val result = ArrayList<Long>(daysCount)

            // 4. 遍历 90 天
            for (i in 0 until daysCount) {
                val currentDay = startDate.plusDays(i.toLong())
                val currentRank = map[currentDay]

                if (currentRank != null) {
                    // 情况 1: 当天有连续/有效数据
                    result.add(currentRank)
                } else {
                    // 当天无数据，利用有序的 intervalData 快速寻找前后的有效数据
                    // 寻找上一个有效数据：在区间内找最后一个比当前日期早的；找不到则用区间前的兜底
                    val lastEntry = intervalData.lastOrNull { it.date.isBefore(currentDay) } ?: structuralBeforeEntry

                    // 寻找下一个有效数据：在区间内找第一个比当前日期晚的
                    val nextEntry = intervalData.firstOrNull { it.date.isAfter(currentDay) }

                    when {
                        // 情况 2: 前后都有数据 -> 线性插值
                        lastEntry != null && nextEntry != null -> {
                            val daysBetweenLastAndNext = ChronoUnit.DAYS.between(
                                lastEntry.date, nextEntry.date
                            ).toDouble()

                            val daysFromLast = ChronoUnit.DAYS.between(
                                lastEntry.date, currentDay
                            ).toDouble()

                            val interpolatedRank = lastEntry.rank +
                                    ((nextEntry.rank - lastEntry.rank) * (daysFromLast / daysBetweenLastAndNext))

                            result.add(interpolatedRank.toLong())
                        }

                        // 情况 3: 有前数据，没后数据 -> 最近几天没数据，补最后一天有效数据
                        lastEntry != null && nextEntry == null -> {
                            result.add(lastEntry.rank)
                        }

                        // 情况 4: 没前数据 -> 90天前及之前没有任何有效数据，补 0
                        else -> {
                            result.add(0L)
                        }
                    }
                }
            }

            return result
        }
    }
}

data class UserGlobalRankKey(
    var userID: Long,
    var mode: Byte,
    var date: LocalDate,
) : Serializable {
    @Suppress("UNUSED")
    constructor() : this(0, 0, LocalDate.now())
}