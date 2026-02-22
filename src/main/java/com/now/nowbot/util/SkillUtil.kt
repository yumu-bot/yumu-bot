package com.now.nowbot.util

import kotlin.math.pow
import kotlin.math.sqrt

/**
 *
 */
object SkillUtil {
    fun calculateEuclideanDistance(a: DoubleArray, b: DoubleArray): Double {
        require(a.size == 6 && b.size == 6) {
            "两条数据必须要六个维度：${a.size}，${b.size}"
        }

        val sum = (0..5).toList().sumOf { i ->
            val diff = a[i] - b[i]
            diff * diff
        }

        return sqrt(sum)
    }

    /**
     * 即星数，也用来给 K 面板算玩家的所有星
     */
    fun getMapSkillRating(skills: List<Double>): Double {
        val sorted = skills.take(6).sortedDescending()
        return (0.6 * sorted[1] + 0.4 * sorted[2] + 0.2 * sorted[3])
    }

    fun collectScoreSkills(skills: List<List<Double>>): List<Double> {
        return skills.map { skillList ->
            skillList.sortedDescending().foldIndexed(0.0) { i, acc, v ->
                val percent = if (i < FastPower095.MAX_EXP) {
                    FastPower095.pow(i)
                } else {
                    0.95.pow(i)
                }
                acc + (v * percent)
            } / DIVISOR
        }
    }

    // 用于求和并归一化
    private const val DIVISOR = 18.0 // (1 - (0.95).pow(100)) / 0.05 // 19.88158941559331949
}