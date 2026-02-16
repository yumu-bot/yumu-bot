package com.now.nowbot.model.skill

import com.now.nowbot.model.beatmapParse.OsuFile
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.throwable.TipsException
import kotlin.math.*

abstract class Skill {
    abstract val values: List<Float>

    abstract val bases: List<Float>

    abstract val names: List<String>

    abstract val abbreviates: List<String>

    abstract val star: Float

    protected val frac16 = 1000f/48f
    protected val frac12 = 1000f/36f
    protected val frac8 = 1000f/24f
    protected val frac6 = 1000f/18f
    protected val frac4 = 1000f/12f
    protected val frac3 = 1000f/9f
    protected val frac2 = 1000f/6f
    protected val frac1 = 1000f/3f

    protected val calculateUnit = 2500 // 计算元，2500 毫秒

    /**
     * 获取 y = 1/x 的值。这个图像在 x = delta_time = standard_time 处，y = 1。
     * 这个算法可以用来计算 jack。
     * @param time 时间。
     * @param standard 标准时间。时间差 = 标准时间，y 取到 1。
     * @param max 最大时间。截断时间太长，而算出的极小值。
     * @param min 最小时间。避免算出过大的值。如果设为 0，则会出现很大的值。
     * @return 难度
     */
    protected fun inverse(
        time: Float,
        standard: Float = frac2,
        max: Float = frac1,
        min: Float = frac16
    ): Float {
        val x = abs(time)
        return if (x < min) {
            1f * standard / min
        } else if (x < max) {
            1f * standard / x
        } else {
            0f
        }
    }

    /**
     * 获取 y = 1-(e^2x) 的值。这个图像在 x = 2 的时候即逼近了 1。
     * @param time 时间。
     * @param standard 标准时间。时间差 = 2 倍标准时间时，y 接近最大值 1。
     */
    protected fun approach(time: Float, standard: Float): Float {
        return 1f - exp(-2f * abs(time / standard))
    }


    /**
     * 获取 y = (ex/e^x)^2 的值。这个图像在 x = 时间/标准时间 = 1 处，y = 最大值 1。这个超越函数的性质是，x ∈ (0, 1) 时，y 从 0 递增到 1，随后逐渐递减为 0。
     *
     * @param time 时间。
     * @param standard 标准时间。时间差 = 标准时间时，y 取到最大值 1。
     * @param max 最大时间。截断时间太长，而算出的极小值。推荐大约是标准时间的 3 倍。
     */
    protected fun exponent(time: Float, standard: Float = frac8, max: Float = frac3) : Float {
        if (standard <= 0f || time <= 0f || time > max) return 0f

        val x = abs(time * 1f / standard)

        return (Math.E * x / exp(x)).pow(2.0).toFloat()
    }

    /**
     * 求和算法：按最大排到最小 0.95^n，逐个求值
     */
    protected fun sum(values: List<Float>?): Float {
        if (values.isNullOrEmpty()) return 0f

        return values
            .filter { it > 0f }
            .sortedDescending()
            .mapIndexed { index, it -> it * 0.95.pow(index) }
            .sum().toFloat()

        /*

        val nonZero = values.filter { it > 1e-4 }

        if (nonZero.isEmpty()) return 0f

        val threeQuarter = nonZero.sorted()[nonZero.size * 3 / 4]
        val bonus = log10(nonZero.size / 60f + 1) + 1

        return (0.8f * threeQuarter + 0.2f * nonZero.max()) * bonus

         */
    }

    /**
     * 评估算法，将一系列值按 y = a(cx)^b + d 来化成一个值。x 是求和算法算出来的值。
     */
    protected fun eval(values: List<Float>?, a: Float = 1f, b: Float = 1f, c: Float = 1f, d: Float = 0f): Float {
        val x = sum(values)
        return a * (c * x).pow(b) + d
    }

    companion object {
        fun getInstance(file: OsuFile, mode: OsuMode, clockRate: Double = 1.0): Skill {
            return when(mode) {
                OsuMode.MANIA -> {
                    val f = file.getMania()
                    f.clockRate = clockRate

                    SkillMania(f)
                }
                else -> throw TipsException("仅 mania 可用！")
            }
        }
    }
}