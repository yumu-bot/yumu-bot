package com.now.nowbot.model.mapminus

import com.now.nowbot.model.mapminus.impl.PPMinus3ManiaImpl
import kotlin.math.*

class PPMinus3Type {

    enum class ManiaType(
            var chinese: String,
            var star: Double,
            var rice: Double,
            var ln: Double,
            var coordination: Double,
            var precision: Double,
            var speed: Double,
            var stamina: Double,
            var sv: Double,
    ) {
        // 834370 anemoi
        EASY_JUMP_TRILL("简单对拍图", 4.22, 3.5, 3.06, 3.78, 0.0, 4.55, 3.98, 0.0),

        // 770127 暴徒
        HARD_JUMP_TRILL("困难对拍图", 6.81, 6.62, 3.88, 2.59, 4.21, 9.71, 5.84, 0.0),

        // 1388200 石英
        JACK("叠键图", 5.61, 6.59, 0.0, 1.83, 0.91, 7.83, 5.01, 0.0),

        // 3261950 电蝴蝶
        VIBRATE("抖图", 8.72, 11.03, 0.0, 1.29, 0.0, 11.81, 7.43, 0.0),

        // 4123096 Duvet
        SPEED_STREAM("速切图", 5.37, 4.39, 2.94, 2.54, 4.73, 5.77, 5.27, 0.0),

        // 1938170 Cyber Inductance (Speed Up Ver.)
        STAMINA_RICE("耐力/技巧图", 5.98, 5.11, 2.33, 2.3, 3.84, 6.73, 6.25, 0.0),

        // 4376016 your new home
        EASY_RELEASE("简单放手图", 3.45, 1.09, 3.81, 5.88, 1.67, 1.55, 1.12, 0.0),

        // 559657 VERSUS
        HARD_RELEASE("困难放手图", 6.39, 1.67, 6.54, 6.24, 5.91, 4.04, 5.85, 0.0),

        // 4499579 月光
        COORDINATE("协调图", 5.76, 1.67, 5.15, 11.2, 2.12, 2.86, 4.35, 0.0),

        // 2995459 猫中毒
        SHORT_LN("短面图", 6.48, 1.41, 6.82, 5.22, 11.61, 3.61, 4.1, 0.0),

        // 2351412 Labyrinth
        IRREGULAR_LN("不规则面图", 5.42, 2.77, 6.98, 3.33, 4.78, 4.27, 4.88, 0.0),

        // 701570  The Quick Brown Fox - CRT
        SINGLE_LINE("单轨图", 4.17, 5.23, 2.19, 0.0, 0.0, 7.34, 1.77, 0.0),

        // 4208754 星河一天
        OVERESTIMATED_SR("撑星图", 8.65, 4.51, 8.33, 6.34, 9.04, 5.9, 7.83, 0.0),

        // 3281146 暗黑舞踏会
        UNDERESTIMATED_SR("压星图", 8.09, 7.25, 4.66, 3.27, 6.67, 8.55, 7.94, 0.0),

        // 3866394 洄映的漩流
        DUMP("塞爆图", 6.07, 6.19, 0.0, 2.35, 5.37, 6.42, 6.78, 0.0),

        // 1009933 大钱
        CLASSIC("经典老图", 5.42, 5.02, 2.21, 1.87, 5.65, 6.22, 4.73, 0.0),

        // 4035790 大浪淘沙
        MODERN("现代图", 5.18, 2.39, 5.68, 4.86, 3.56, 3.17, 4.75, 0.0),

        // 4487439 重霄
        AWMRONE("狙\uD83D\uDC4C", 12.08, 7.71, 11.12, 10.82, 9.1, 9.49, 10.96, 0.0);
    }

    companion object {
        // 获取谱面的 ppm3 类型
        fun getType(m: PPMinus3): Map<String, Double> {
            if (m is PPMinus3ManiaImpl) {
                val typeMap = mutableMapOf<String, Double>()

                val mapList = getStandardizedList(
                    m.values.subList(0, 6)
                )
                // SV 不参与计算

                for(e in ManiaType.entries) {
                    var similarity = 1.0

                    similarity *= getDifference(m.sr, e.star)

                    val typeList = getStandardizedList(
                        listOf(e.rice, e.ln, e.coordination, e.precision, e.speed, e.stamina)
                    )

                    for(i in 0..5) {
                        similarity *= getStandardDeviation(mapList[i] - typeList[i])
                    }

                    typeMap[e.chinese] = similarity.pow(1.0 / 6.0)
                }

                return typeMap.toList().sortedBy { (_, value) -> value }.reversed().toMap()
            } else {
                return mapOf("未知" to -100.0)
            }
        }

        // 获取 Z 分数标准化后的数组
        private fun getStandardizedList(list: List<Double>?) : List<Double> {
            if (list.isNullOrEmpty()) return emptyList()

            val sum = list.stream().reduce { a, b -> a + b}.orElse(0.0)
            val count = list.size
            val average = sum / count

            val deviation = sqrt(list.stream().map{ (it - average).pow(2) }
                .reduce {a, b -> a + b}.orElse(0.0) / count)

            return list.stream().map {(it - average) / deviation}.toList()
        }

        // 获取正态分布密度，均值 0，标准差 sqrt(2 * PI)
        private fun getStandardDeviation(x: Double) : Double {
            return exp(- (x.pow(2.0) / 2.0))
        }

        //
        private fun getDifference(input: Double, base: Double) : Double {
            return if (base != 0.0) {
                1.0 - clamp(double = (input - base) / base)
            } else {
                1.0
            }
        }

        // 0-0.99 限制值
        private fun clamp(double: Double) : Double {
            return max(min(abs(double), 0.99), 0.0)
        }
    }
}
