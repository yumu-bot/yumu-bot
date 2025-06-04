package com.now.nowbot.model.ppysb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.*

data class SBStatistics(
    @JsonProperty("id")
    val id: Long = 0L,

    @set:JsonProperty("mode") @get:JsonIgnoreProperties var modeByte: Byte = 0,

    @JsonProperty("tscore")
    val totalScore: Long = 0L,

    @JsonProperty("rscore")
    val rankedScore: Long = 0L,

    @JsonProperty("pp")
    val pp: Int = 0,

    @JsonProperty("plays")
    val playCount: Long = 0L,

    @JsonProperty("playtime")
    val playTime: Long = 0L,

    @JsonProperty("acc")
    val accuracy: Double = 0.0,

    @JsonProperty("xh_count")
    val countSSH: Int = 0,

    @JsonProperty("x_count")
    val countSS: Int = 0,

    @JsonProperty("sh_count")
    val countSH: Int = 0,

    @JsonProperty("s_count")
    val countS: Int = 0,

    @JsonProperty("a_count")
    val countA: Int = 0,

    @JsonProperty("rank")
    val rank: Int = 0,

    @JsonProperty("country_rank")
    val countryRank: Int = 0,

    ) {

    @get:JsonProperty("mode") val mode: OsuMode
        get() = when(modeByte.toInt()) {
            0, 4, 8 -> OSU
            1, 5 -> TAIKO
            2, 6 -> CATCH
            3 -> MANIA
            else -> DEFAULT
        }

    @get:JsonProperty("is_relax") val relax: Boolean
        get() = when(modeByte.toInt()) {
            4, 5, 6 -> true
            else -> false
        }

    @get:JsonProperty("is_autopilot") val autoPilot: Boolean
        get() = when(modeByte.toInt()) {
            8 -> true
            else -> false
        }
}