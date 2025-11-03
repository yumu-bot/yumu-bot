package com.now.nowbot.model.ppysb

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.*

data class SBStatistics(
    @field:JsonProperty("id")
    var id: Long = 0L,

    @set:JsonProperty("mode") @get:JsonIgnoreProperties var modeByte: Byte = 0,

    @get:JsonProperty("total_score")
    @field:JsonAlias("tscore")
    var totalScore: Long = 0L,

    @get:JsonProperty("ranked_score")
    @field:JsonAlias("rscore")
    var rankedScore: Long = 0L,

    @field:JsonProperty("pp")
    var pp: Int = 0,

    @get:JsonProperty("play_count")
    @field:JsonAlias("plays")
    var playCount: Long = 0L,

    @get:JsonProperty("play_time")
    @field:JsonAlias("playtime")
    var playTime: Long = 0L,

    @get:JsonProperty("accuracy")
    @field:JsonAlias("acc")
    var accuracy: Double = 0.0,

    @get:JsonProperty("count_ssh")
    @field:JsonAlias("xh_count")
    var countSSH: Int = 0,

    @get:JsonProperty("count_ss")
    @field:JsonAlias("x_count")
    var countSS: Int = 0,

    @get:JsonProperty("count_sh")
    @field:JsonAlias("sh_count")
    var countSH: Int = 0,

    @get:JsonProperty("count_s")
    @field:JsonAlias("s_count")
    var countS: Int = 0,

    @get:JsonProperty("count_a")
    @field:JsonAlias("a_count")
    var countA: Int = 0,

    @get:JsonProperty("global_rank")
    @field:JsonAlias("rank")
    var globalRank: Long = 0,

    @field:JsonProperty("country_rank")
    var countryRank: Long = 0,

    @get:JsonProperty("maximum_combo")
    @field:JsonAlias("max_combo")
    var maxCombo: Int = 0,

    @field:JsonProperty("total_hits")
    var totalHits: Long = 0,

    @get:JsonProperty("replays_watched_by_others")
    @field:JsonAlias("replay_views")
    var replayWatchedByOthers: Int = 0,

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