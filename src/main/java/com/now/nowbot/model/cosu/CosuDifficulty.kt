package com.now.nowbot.model.cosu

import com.fasterxml.jackson.annotation.JsonProperty

data class CosuDifficulty(
    /************ 通用 ****************/
    @field:JsonProperty("star_rating")
    val starRating: Double = 0.0,

    @field:JsonProperty("max_combo")
    val maxCombo: Int = 0,

    /************ osu 专用 ****************/
    @field:JsonProperty("aim_difficulty")
    val aimDifficulty: Double = 0.0,

    @field:JsonProperty("aim_difficult_slider_count")
    val aimDifficultSliderCount: Double = 0.0,

    @field:JsonProperty("speed_difficulty")
    val speedDifficulty: Double = 0.0,

    @field:JsonProperty("speed_note_count")
    val speedNoteCount: Double = 0.0,

    @field:JsonProperty("slider_factor")
    val sliderFactor: Double = 0.0,

    @field:JsonProperty("aim_top_weighted_slider_factor")
    val aimTopWeightedSliderFactor: Double = 0.0,

    @field:JsonProperty("speed_top_weighted_slider_factor")
    val speedTopWeightedSliderFactor: Double = 0.0,

    @field:JsonProperty("aim_difficult_strain_count")
    val aimDifficultStrainCount: Double = 0.0,

    @field:JsonProperty("speed_difficult_strain_count")
    val speedDifficultStrainCount: Double = 0.0,

    @field:JsonProperty("nested_score_per_object")
    val nestedScorePerObject: Double = 0.0,

    @field:JsonProperty("legacy_score_base_multiplier")
    val legacyScoreBaseMultiplier: Double = 0.0,

    @field:JsonProperty("maximum_legacy_combo_score")
    val maximumLegacyComboScore: Double = 0.0,

    /************ mania 专用 ****************/
    @field:JsonProperty("rhythm_difficulty")
    val rhythmDifficulty: Double = 0.0,

    @field:JsonProperty("mono_stamina_factor")
    val monoStaminaFactor: Double = 0.0,

    @field:JsonProperty("consistency_factor")
    val consistencyFactor: Double = 0.0,
)
