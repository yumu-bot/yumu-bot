package com.now.nowbot.model.skill

import com.now.nowbot.model.beatmapParse.OsuFile
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.throwable.TipsException
import kotlin.math.E
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow

sealed class Skill6 {

    companion object {
        operator fun invoke(file: OsuFile, mode: OsuMode, clockRate: Double = 1.0): Skill6 {
            return when(mode) {
                OsuMode.MANIA -> SkillMania6(
                    file.getMania()
                        .apply {
                            this.clockRate = clockRate
                        }
                )
                else -> throw TipsException("仅 mania 可用！")
            }
        }
    }

    abstract val skills: List<Double>

    abstract val bases: List<Double>

    abstract val names: List<String>

    abstract val abbreviates: List<String>

    abstract val rating: Double

    protected val frac16 = 1000 / 48.0
    protected val frac12 = 1000 / 36.0
    protected val frac8 = 1000 / 24.0
    protected val frac6 = 1000 / 18.0
    protected val frac4 = 1000 / 12.0
    protected val frac3 = 1000 / 9.0
    protected val frac2 = 1000 / 6.0
    protected val frac1 = 1000 / 3.0

    protected val calculateUnit = 5000

    /**
     * 获取 y = 1/x 的值。这个图像在 x = delta_time = standard_time 处，y = 1。
     * 这个算法可以用来计算 jack。
     * @param this 时间差。
     * @param standard 标准时间差。时间差等于标准时间时，y 取到 1。
     * @param max 最大时间。截断时间太长，而算出的极小值。
     * @param min 最小时间。避免算出过大的值。如果设为 0，则会出现很大的值。
     * @return 难度
     */
    protected fun Int.inverse(
        standard: Double = frac2,
        max: Double = frac1,
        min: Double = frac16
    ): Double {
        val x = abs(this)
        return if (x <= min) {
            standard / min
        } else if (x <= max) {
            standard / x
        } else {
            0.0
        }
    }

    /**
     * 获取 y = 1-(e^2x) 的值。这个图像在 x = 2 的时候即逼近了 1。
     * 这个算法可以用来计算 LN
     * @param this 时间。
     * @param standard 标准时间。时间差 = 2 倍标准时间时，y 接近最大值 1。
     */
    protected fun Int.approach(standard: Double): Double {
        return 1.0 - exp(-2.0 * abs(this / standard))
    }

    /**
     * 获取 y = (ex/e^x)^2 的值。这个图像在 x = 时间/标准时间 = 1 处，y = 最大值 1。这个超越函数的性质是，x ∈ (0, 1) 时，y 从 0 递增到 1，随后逐渐递减为 0。
     *
     * @param this 时间。
     * @param standard 标准时间。时间差 = 标准时间时，y 取到最大值 1。
     * @param max 最大时间。截断时间太长，而算出的极小值。推荐大约是标准时间的 3 倍。
     */
    protected fun Int.exponent(standard: Double = frac8, max: Double = frac3) : Double {
        if (standard <= 0.0 || this.toDouble() !in 0.0..max) {
            return 0.0
        }

        val x = abs(this / standard)

        return (E * x / exp(x)).pow(2.0)
    }

    /**
     * 使用二维范数，来合并多个参数
     * 这个算法可以用来求最终值
     * @param norm: 如果是二，就是二维范数。如果是一，则等于对大家求和。如果是无穷大，则就是对大家最大值。
     * @param scale: 一般取 0.6-0.8，来贴合实际情况。
     */
    protected fun List<Double>.norm2(norm : Int = 2, scale: Double = 0.6): Double {
        return this.sumOf { it.pow(norm) }
            .pow(1.0 / norm) * scale
    }

    /**
     * 从大到小加权求和。
     * 如果是 2 个值，则是 7:3，如果是 3 个值，则是 6:3:1。
     */
    protected fun List<Double>.sortAndSum() : Double {
        val sorted = this.sortedDescending()

        return when(sorted.size) {
            1 -> sorted[0]
            2 -> 0.7 * sorted[0] + 0.3 * sorted[1]
            3 -> 0.6 * sorted[0] + 0.3 * sorted[1] + 0.1 * sorted[2]
            4 -> 0.4 * sorted[0] + 0.3 * sorted[1] + 0.2 * sorted[2] + 0.1 * sorted[2]
            else -> sorted[0]
        }
    }


    /**
     * 降序衰减聚合函数
     * 这个算法能用来对组内的数据求和
     * Σ (Strain\[i] * decay^i)
     * @param decay 如果是 0.95，则能容纳 top 143 个元素，如果是 0.85，则只能容纳 43 个。
     */
    protected fun List<Double>.aggregate(decay: Double = 0.85): Double {
        if (this.isEmpty()) {
            return 0.0
        }
        val sorted = this.filter { it > 0 }.sortedDescending()

        var total = 0.0
        var weight = 1.0

        for (s in sorted) {
            total += s * weight
            weight *= decay
            if (weight < 0.001) break
        }

        return total
    }

    /**
     * 评估算法，将一系列值按 y = a(cx)^b + d 来化成一个值。x 是求和算法算出来的值。
     */
    protected fun Double.eval(a: Double = 1.0, b: Double = 1.0, c: Double = 1.0, d: Double = 0.0): Double {
        return a * (c * this).pow(b) + d
    }

}