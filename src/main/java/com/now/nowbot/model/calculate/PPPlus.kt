package com.now.nowbot.model.calculate

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import kotlin.math.pow

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
class PPPlus {

    data class Stats(
        val aim: Double = 0.0,
        @field:JsonProperty("jumpAim") val jumpAim: Double = 0.0,
        @field:JsonProperty("flowAim") val flowAim: Double = 0.0,
        val precision: Double = 0.0,
        val speed: Double = 0.0,
        val stamina: Double = 0.0,
        val accuracy: Double = 0.0,
        val total: Double = 0.0
    )

    data class AdvancedStats(
        val index: List<Double>,
        val general: Double,
        val advanced: Double,
        val sum: Double,
        val approval: Double
    )

    var accuracy: Double? = null
    var combo: Int? = null
    var difficulty: Stats? = null
    var performance: Stats? = null

    val skill: Stats
        get () {
            val d = difficulty ?: return Stats()

            return Stats(
                calculateSkillValue(d.aim),
                calculateSkillValue(d.jumpAim),
                calculateSkillValue(d.flowAim),
                calculateSkillValue(d.precision),
                calculateSkillValue(d.speed),
                calculateSkillValue(d.stamina),
                calculateSkillValue(d.accuracy),
                calculateSkillValue(d.total)
            )
        }

    var advancedStats: AdvancedStats? = null

    override fun toString(): String {
        return "PPPlus(accuracy=$accuracy, combo=$combo, difficulty=$difficulty, performance=$performance, skill=$skill, advancedStats=$advancedStats)"
    }

    companion object {
        // 计算出 legacy PP+ 显示的接近 1000 的值
        fun calculateSkillValue(difficultyValue: Double): Double {
            return difficultyValue.pow(3.0) * 3.9
        }

        val maxStats: Stats
            get() = Stats(
                99999.0,
                99999.0,
                99999.0,
                99999.0,
                99999.0,
                99999.0,
                99999.0,
                99999.0
            )
    }
}