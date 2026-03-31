package com.now.nowbot.model.cosu

import com.fasterxml.jackson.annotation.JsonProperty

data class CosuPerformance(
    @field:JsonProperty("aim")
    val aim: Double = 0.0,

    @field:JsonProperty("speed")
    val speed: Double = 0.0,

    @field:JsonProperty("accuracy")
    val accuracy: Double = 0.0,

    @field:JsonProperty("flashlight")
    val flashlight: Double = 0.0,

    @field:JsonProperty("effective_miss_count")
    val effectiveMissCount: Double? = 0.0,

    @field:JsonProperty("speed_deviation")
    val speedDeviation: Double = 0.0,

    @field:JsonProperty("combo_based_estimated_miss_count")
    val comboBasedEstimatedMissCount: Double? = 0.0,

    @field:JsonProperty("score_based_estimated_miss_count")
    val scoreBasedEstimatedMissCount: Double? = 0.0,

    @field:JsonProperty("aim_estimated_slider_breaks")
    val aimEstimatedSliderBreaks: Double = 0.0,

    @field:JsonProperty("speed_estimated_slider_breaks")
    val speedEstimatedSliderBreaks: Double = 0.0,

    @field:JsonProperty("pp")
    val pp: Double = 0.0,

    @field:JsonProperty("difficulty")
    val difficulty: Double = 0.0,

    @field:JsonProperty("estimated_unstable_rate")
    val estimatedUnstableRate: Double = 0.0
)
