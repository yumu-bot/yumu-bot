package com.now.nowbot.image.util

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerStatistics

object Rankings {
    private val DEFAULT_BOUNDARY = listOf(10.0, 8.0, 6.5, 5.3, 4.0, 2.8, 2.0, 0.1)
    private val DEFAULT_RANKS = listOf("EX", "SS", "S+", "S", "A", "B", "C", "D")

    fun getRankFromValue(
        value: Double,
        boundary: List<Double> = DEFAULT_BOUNDARY,
        ranks: List<String> = DEFAULT_RANKS,
    ): String {
        val thresholds = if (boundary.size < 8) DEFAULT_BOUNDARY else boundary
        val grades = if (ranks.size < 8) DEFAULT_RANKS else ranks
        for (index in grades.indices) {
            val threshold = thresholds.getOrNull(index) ?: continue
            if (value >= threshold) return grades[index]
        }
        return "F"
    }

    fun rankSS2X(rank: String = "SS"): String {
        return when (rank) {
            "SS" -> "X"
            "SSH" -> "XH"
            else -> rank
        }
    }

    fun getOsuDXRatingStar(stat: LazerStatistics, statMax: LazerStatistics, mode: String = "o"): Int {
        var dx = 0.0
        var dxMax = 0.0

        when (mode) {
            "o" -> {
                dx = stat.great * 3.0 + stat.ok + stat.meh * 0.5
                dxMax = statMax.great * 3.0
            }

            "t" -> {
                dx = stat.great * 3.0 + stat.ok * 1.5
                dxMax = statMax.great * 3.0
            }

            "c" -> {
                dx = stat.smallTickHit * 3.0 + stat.largeTickHit * 2.0 + stat.great
                dxMax = statMax.smallTickHit * 3.0 + statMax.largeTickHit * 2.0 + statMax.great
            }

            "m" -> {
                dx = stat.perfect * 3.0 + stat.great * 2.0 + stat.good
                dxMax = statMax.perfect * 3.0
            }
        }

        if (dxMax == 0.0) return 0
        val div = dx / dxMax

        return when (mode) {
            "o", "t", "c" -> when {
                div >= 0.9975 -> 5
                div >= 0.995 -> 4
                div >= 0.99 -> 3
                div >= 0.97 -> 2
                div >= 0.95 -> 1
                else -> 0
            }

            "m" -> when {
                div >= 0.97 -> 5
                div >= 0.95 -> 4
                div >= 0.93 -> 3
                div >= 0.9 -> 2
                div >= 0.85 -> 1
                else -> 0
            }

            else -> 0
        }
    }

    fun getOsuDXRatingStar(stat: LazerStatistics, statMax: LazerStatistics, mode: OsuMode): Int {
        return getOsuDXRatingStar(stat, statMax, osuModeKey(mode))
    }

    private fun osuModeKey(mode: OsuMode): String {
        return when (mode.safeModeValue.toInt()) {
            0 -> "o"
            1 -> "t"
            2 -> "c"
            else -> "m"
        }
    }
}
