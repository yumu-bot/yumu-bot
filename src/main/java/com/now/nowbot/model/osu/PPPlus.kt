package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import kotlin.math.pow

@JsonInclude(JsonInclude.Include.NON_NULL) @JsonIgnoreProperties(ignoreUnknown = true)
class PPPlus {
    @JvmRecord
    data class Stats(
        @JvmField val aim: Double = 0.0,
        @JvmField @JsonProperty("jumpAim") val jumpAim: Double = 0.0,
        @JvmField @JsonProperty("flowAim") val flowAim: Double = 0.0,
        @JvmField val precision: Double = 0.0,
        @JvmField val speed: Double = 0.0,
        @JvmField val stamina: Double = 0.0,
        @JvmField val accuracy: Double = 0.0,
        @JvmField val total: Double = 0.0
    )

    @JvmRecord
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
    var skill: Stats? = null
        get() = if (field == null && difficulty != null) {
            field = Stats(
                calculateSkillValue(difficulty!!.aim),
                calculateSkillValue(difficulty!!.jumpAim),
                calculateSkillValue(difficulty!!.flowAim),
                calculateSkillValue(difficulty!!.precision),
                calculateSkillValue(difficulty!!.speed),
                calculateSkillValue(difficulty!!.stamina),
                calculateSkillValue(difficulty!!.accuracy),
                calculateSkillValue(difficulty!!.total)
            )

            field
        } else Stats()

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
