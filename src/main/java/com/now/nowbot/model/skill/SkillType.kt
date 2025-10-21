package com.now.nowbot.model.skill

import com.now.nowbot.util.SkillUtil
import kotlin.math.*

class SkillType {

    enum class ManiaType(
        val chinese: String,
        private val rice: Double,
        private val stamina: Double,
        private val speed: Double,
        private val ln: Double,
        private val coordination: Double,
        private val precision: Double,
        private val sv: Double = 1.0,
        private val star: Double,
    ) {
        DEFAULT("未知", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),

        // 834370 anemoi
        EASY_JUMP_TRILL("简单对拍图", 4.03, 3.15, 4.16, 1.39, 3.1, 0.0, 0.0, 4.3),

        // 770127 暴徒
        HARD_JUMP_TRILL("困难对拍图", 6.84, 4.15, 7.74, 1.62, 2.26, 2.57, 0.0, 7.12),

        // 1388200 石英
        JACK("叠键图", 6.59, 0.0, 1.83, 0.91, 7.83, 5.01, 0.0, 5.61),

        // 3261950 电蝴蝶
        HARD_JACK("困难叠键图", 8.38, 4.6, 8.86, 0.0, 1.2, 0.0, 0.0, 7.99),

        // 4123096 Duvet
        SPEED_STREAM("速切图", 5.57, 3.82, 4.67, 1.79, 3.35, 3.79, 0.0, 5.5),

        // 1938170 Cyber Inductance (Speed Up Ver.)
        STAMINA_RICE("耐力图", 6.16, 3.87, 6.31, 1.0, 2.29, 2.05, 0.0, 6.01),

        // 4376016 your new home
        EASY_COORDINATE("简单协调图", 0.28, 1.5, 0.91, 1.63, 1.16, 0.91, 0.0, 1.63),

        // 559657 VERSUS
        HARD_RELEASE("困难放手图", 0.46, 4.39, 3.63, 6.18, 4.63, 4.26, 0.0, 5.96),

        // 4499579 月光
        HARD_COORDINATE("困难协调图", 1.7, 4.37, 2.41, 3.57, 7.04, 2.14, 0.0, 5.89),

        // 2995459 猫中毒
        SHORT_LN("短面图", 0.47, 3.89, 2.93, 5.32, 4.32, 2.95, 0.0, 5.18),

        // 2351412 Labyrinth
        IRREGULAR_LN("不规则面图", 3.0, 4.66, 4.33, 5.66, 4.79, 3.56, 0.0, 5.81),

        // 701570  The Quick Brown Fox - CRT
        SINGLE_LINE("单轨图", 3.0, 2.04, 3.5, 0.86, 0.0, 0.0, 0.0, 3.14),

        // 4208754 星河一天
        OVERESTIMATED_SR("撑星图", 4.66, 7.54, 6.04, 7.54, 7.1, 6.19, 0.0, 8.37),

        // 3281146 暗黑舞踏会
        UNDERESTIMATED_SR("压星图", 8.41, 5.64, 7.65, 2.72, 3.11, 6.01, 0.0, 8.42),

        // 3866394 洄映的漩流 DUMP
        HARD_STREAM("困难切换图", 7.35, 3.67, 5.66, 0.0, 2.15, 4.49, 0.0, 6.74),

        // 1009933 大钱
        RICE("米图", 5.17, 3.11, 5.19, 0.39, 1.74, 2.29, 0.0, 5.08),

        // 4035790 大浪淘沙
        MODERN("现代图", 2.09, 4.7, 2.73, 3.72, 3.62, 2.04, 0.0, 4.57),

        // 4487439 重霄
        AWMRONE("狙\uD83D\uDC4C", 9.42, 10.53, 9.63, 10.35, 11.97, 8.96, 0.0, 12.65),

        // 1148050 re: end of a dream
        EASY_HYBRID("简单综合图", 5.07, 3.82, 5.09, 2.43, 1.92, 2.3, 0.0, 5.19),

        // 4675450 Aite
        HARD_HYBRID("困难综合图", 4.74, 4.62, 4.97, 3.78, 3.53, 1.48, 0.0, 5.39),

        // 4518831 咏奏妖华 -明镜止水-
        EASY_STREAM("简单切换图", 3.66, 2.65, 2.64, 0.0, 0.7, 1.31, 0.0, 3.32),

        // 1230814 星が降らない街
        BURST("爆发图", 5.5, 5.27, 4.36, 3.76, 4.06, 4.43, 0.0, 5.86),

        // 4778171 feel my soul (TV Size)
        EASY_RELEASE("简单放手图", 0.53, 2.23, 1.12, 1.75, 2.2, 0.0, 0.0, 2.26),

        // 3525373 feel my rhythm
        NORMAL_RELEASE("普通放手图", 1.43, 3.96, 2.22, 4.22, 3.45, 2.04, 0.0, 4.31),

        ;

        fun getValues(): List<Double> = listOf(rice, ln, coordination, precision, speed, stamina)
    }

    companion object {
        // 获取谱面的 ppm3 类型
        fun getType(m: Skill): Map<ManiaType, Double> {
            if (m is SkillMania) {
                val typeMap = mutableMapOf<ManiaType, Double>()

                val mapList = getStandardizedList(
                    m.values.take(6).map {it.toDouble()}
                ) // SV 不参与计算
                for (e in ManiaType.entries) {
                    val typeList = getStandardizedList(
                        e.getValues()
                    )

                    val distance = SkillUtil.calculateEuclideanDistance(mapList.toDoubleArray(), typeList.toDoubleArray())

                    typeMap[e] = 1.0 / (1.0 + 1.0/9.0 * distance.pow(2.226))
                    /*
                    var similarity = 1.0

                    similarity *= getDifference(m.star.toDouble(), e.star)

                    val typeList = getStandardizedList(
                        listOf(e.rice, e.ln, e.coordination, e.precision, e.speed, e.stamina)
                    )

                    for (i in 0..5) {
                        val std = getStandardDeviation(mapList[i] - typeList[i])

                        similarity *= std
                    }


                    typeMap[e] = similarity

                     */
                }

                /*
                for (v in typeMap.toList().sortedByDescending { (_, value) -> value }.take(5)) {
                    println("${v.first.chinese}: ${(v.second * 1000).toInt() / 10.0}%")
                }

                 */

                return typeMap.toList().sortedByDescending { (_, value) -> value }.toMap()
            } else {
                return mapOf(ManiaType.DEFAULT to 0.0)
            }
        }

        // 获取 Z 分数标准化后的数组
        private fun getStandardizedList(list: List<Double>?): List<Double> {
            if (list.isNullOrEmpty()) return emptyList()

            val average = list.average()

            val deviation = sqrt(list.map { (it - average) * (it - average) }.average())

            if (deviation <= 1e-4) return list

            return list.map { (it - average) / deviation }

            /*

            val sum = list.sum()
            val count = list.size
            val average = sum / count

            val deviation = sqrt(list.sumOf { (it - average).pow(2) } / count)

            if (deviation == 0.0) return list

            return list.map { (it - average) / deviation }

             */
        }

        // 获取正态分布密度，均值 0，标准差 sqrt(2 * PI)
        private fun getStandardDeviation(x: Double): Double {
            return exp(-(x.pow(2.0) / 2.0))
        }

        //
        private fun getDifference(input: Double, base: Double): Double {
            return if (base != 0.0) {
                1.0 - abs(input - base).coerceIn(0.0, 1.0) / base
            } else {
                1.0
            }
        }
    }
}
