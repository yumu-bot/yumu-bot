package com.now.nowbot.model.cosu

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.osu.LazerMod

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CosuScore(
    @field:JsonProperty("accuracy") val accuracy: Double? = null,

    @field:JsonProperty("max_combo") val maxCombo: Int? = null,

    @field:JsonProperty("statistics") val statistics: CosuStatistics? = null,

    @field:JsonProperty("legacy_total_score") val legacyTotalScore: Int? = null,

    @field:JsonProperty("mods") val mods: List<LazerMod>? = null,
)
