package com.now.nowbot.model.calculate

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.LazerStatistics
import kotlinx.serialization.Serializable
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

sealed interface CalculatePerformance

sealed interface FullCalculatePerformance: CalculatePerformance {
    val fullPP: Double?
    val perfectPP: Double?
}

object EmptyPerformance: CalculatePerformance

object EmptyFullPerformance: FullCalculatePerformance {
    override val fullPP: Double? = null
    override val perfectPP: Double? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
class CosuResponse(
    val difficulty: CosuDifficulty,
    val performance: CosuPerformance? = null,
)

@Serializable
class CosuDifficulty(
    @field:JsonProperty("star_rating")
    val starRating: Double,

    @field:JsonProperty("max_combo")
    val maxCombo: Int,

    @field:JsonProperty("aim_difficulty")
    val aimDifficulty: Double,

    @field:JsonProperty("aim_difficult_slider_count")
    val aimDifficultSliderCount: Double,

    @field:JsonProperty("speed_difficulty")
    val speedDifficulty: Double,

    @field:JsonProperty("speed_note_count")
    val speedNoteCount: Double,

    @field:JsonProperty("slider_factor")
    val sliderFactor: Double,

    @field:JsonProperty("aim_top_weighted_slider_factor")
    val aimTopWeightedSliderFactor: Double,

    @field:JsonProperty("speed_top_weighted_slider_factor")
    val speedTopWeightedSliderFactor: Double,

    @field:JsonProperty("aim_difficult_strain_count")
    val aimDifficultStrainCount: Double,

    @field:JsonProperty("speed_difficult_strain_count")
    val speedDifficultStrainCount: Double,

    @field:JsonProperty("nested_score_per_object")
    val nestedScorePerObject: Double,

    @field:JsonProperty("legacy_score_base_multiplier")
    val legacyScoreBaseMultiplier: Double,

    @field:JsonProperty("maximum_legacy_combo_score")
    val maximumLegacyComboScore: Long
)

@Serializable
open class CosuPerformance(
    @field:JsonProperty("aim")
    val aim: Double,

    @field:JsonProperty("speed")
    val speed: Double,

    @field:JsonProperty("accuracy")
    val accuracy: Double,

    @field:JsonProperty("flashlight")
    val flashlight: Double,

    @field:JsonProperty("effective_miss_count")
    val effectiveMissCount: Double,

    @field:JsonProperty("speed_deviation")
    val speedDeviation: Double?,

    @field:JsonProperty("combo_based_estimated_miss_count")
    val comboBasedEstimatedMissCount: Double,

    @field:JsonProperty("score_based_estimated_miss_count")
    val scoreBasedEstimatedMissCount: Double?,

    @field:JsonProperty("aim_estimated_slider_breaks")
    val aimEstimatedSliderBreaks: Double,

    @field:JsonProperty("speed_estimated_slider_breaks")
    val speedEstimatedSliderBreaks: Double,

    @field:JsonProperty("pp")
    val pp: Double
) : CalculatePerformance

data class CosuRequest(
    // 绝对路径
    @field:JsonProperty("file")
    val file: String,

    // 模式，需要小写
    @field:JsonProperty("mode")
    val mode: String,

    // 可以输入成绩或者自己构建的成绩
    @field:JsonProperty("score")
    val score: CosuScore? = null
)

data class CosuScore(
    @field:JsonProperty("accuracy")
    val accuracy: Double? = null,

    @field:JsonProperty("max_combo")
    val maxCombo: Int? = null,

    @field:JsonProperty("statistics")
    val statistics: LazerStatistics? = null,

    @field:JsonProperty("legacy_total_score")
    var legacyScore: Long? = null,

    @field:JsonProperty("mods")
    val mods: List<LazerMod>? = null,
) {
    enum class ScoreType {
        FULL_COMBO, PERFECT, DEFAULT
    }

    companion object {
        fun LazerScore.toCosuScore(type: ScoreType = ScoreType.DEFAULT): CosuScore {
            val legacy = if (this.legacyScore == 0L) {
                null
            } else {
                this.legacyScore
            }

            val max = maximumStatistics

            return when (type) {
                ScoreType.FULL_COMBO -> CosuScore(
                    accuracy = accuracy,
                    maxCombo = beatmap.maxCombo,
                    statistics = statistics.copy().apply {
                        val misses = this.miss

                        when(mode) {
                            OsuMode.MANIA -> {
                                val rate = if (this.perfect + this.great > 0) {
                                    1.0 * this.perfect / (this.perfect + this.great)
                                } else {
                                    -1.0
                                }

                                if (rate < 0.0) {
                                    this.perfect += misses
                                } else {
                                    val perfect = (rate * misses).toInt()
                                    val great = (misses - great).coerceAtLeast(0)

                                    this.perfect += perfect
                                    this.great += great
                                    this.miss = 0
                                }
                            }

                            OsuMode.CATCH, OsuMode.CATCH_RELAX -> {
                                val tickMisses = this.largeTickMiss

                                this.great += misses
                                this.miss = 0
                                this.largeTickHit += tickMisses
                                this.largeTickMiss = 0
                            }

                            else -> {
                                this.great += misses
                                this.miss = 0

                                if (max.sliderTailHit > 0) {
                                    this.sliderTailHit = max.sliderTailHit
                                }

                                this.largeTickHit += this.largeTickMiss
                                this.largeTickMiss = 0
                            }
                        }
                    },
                    legacyScore = legacy,
                    mods = mods,
                )

                ScoreType.PERFECT -> CosuScore(
                    accuracy = 1.0,
                    maxCombo = beatmap.maxCombo,
                    statistics = when(mode) {
                        OsuMode.MANIA -> if (max.perfect > 0) {
                            max
                        } else {
                            val t = this.statistics

                            LazerStatistics(
                                perfect = t.perfect + t.great + t.good + t.ok + t.meh + t.miss
                            )
                        }

                        else -> if (max.great > 0) {
                            max
                        } else {
                            val t = this.statistics

                            when(mode) {
                                OsuMode.CATCH, OsuMode.CATCH_RELAX -> {
                                    LazerStatistics(
                                        great = t.great + t.miss,
                                        largeTickHit = t.largeTickHit + t.largeTickMiss,
                                        smallTickHit = t.smallTickHit + t.smallTickMiss,
                                        largeBonus = t.largeBonus
                                    )
                                }

                                else -> {
                                    LazerStatistics(
                                        great = t.great + t.ok + t.meh + t.miss
                                    )
                                }
                            }
                        }
                    },
                    legacyScore = legacy,
                    mods = mods,
                )

                ScoreType.DEFAULT -> CosuScore(
                    accuracy = accuracy,
                    maxCombo = maxCombo,
                    statistics = statistics,
                    legacyScore = legacy,
                    mods = mods,
                )
            }
        }
    }
}

@Serializable
class FullCosuPerformance(
    @field:JsonUnwrapped
    base: CosuPerformance,

    @field:JsonProperty("full_pp")
    override val fullPP: Double? = null,

    @field:JsonProperty("perfect_pp")
    override val perfectPP: Double? = null,
) : CosuPerformance(
    // 将 base 的属性手动展开传给父类
    base.aim,
    base.speed,
    base.accuracy,
    base.flashlight,
    base.effectiveMissCount,
    base.speedDeviation,
    base.comboBasedEstimatedMissCount,
    base.scoreBasedEstimatedMissCount,
    base.aimEstimatedSliderBreaks,
    base.speedEstimatedSliderBreaks,
    base.pp
), FullCalculatePerformance
