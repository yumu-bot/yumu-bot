package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Serializable
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
class BeatmapCalculateResult(
    val difficulty: BeatmapDifficulty,
    val performance: ScorePerformance? = null,
)

@Serializable
data class BeatmapDifficulty(
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
data class ScorePerformance(
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
)

data class BeatmapCalculateRequest(
    // 绝对路径
    @field:JsonProperty("file")
    val file: String,

    // 模式，需要小写
    @field:JsonProperty("mode")
    val mode: String,

    // 可以输入成绩或者自己构建的成绩
    @field:JsonProperty("score")
    val score: LazerScoreForCalculate? = null
)

data class LazerScoreForCalculate(
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
    companion object {
        fun LazerScore.toCalculate(): LazerScoreForCalculate {
            return LazerScoreForCalculate(
                accuracy = accuracy,
                maxCombo = maxCombo,
                statistics = statistics,
                legacyScore = legacyScore,
                mods = mods,
            )
        }
    }
}
