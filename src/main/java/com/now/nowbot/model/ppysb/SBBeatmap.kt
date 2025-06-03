package com.now.nowbot.model.ppysb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.*

data class SBBeatmap(
    @JsonProperty("md5") val md5: String,

    @JsonProperty("id") val beatmapID: Long,

    @JsonProperty("set_id") val beatmapsetID: Long,

    @JsonProperty("artist") val artist: String,

    @JsonProperty("title") val title: String,

    @JsonProperty("version") val version: String,

    @JsonProperty("last_update") val lastUpdated: String,

    @JsonProperty("total_length") val totalLength: Int,

    @JsonProperty("max_combo") val maxCombo: Int,

    @set:JsonProperty("status") @get:JsonIgnoreProperties var statusByte: Byte,

    @JsonProperty("plays") val playCount: Long,

    @JsonProperty("passes") val passCount: Long,

    @set:JsonProperty("mode") @get:JsonIgnoreProperties var modeByte: Byte,

    @JsonProperty("bpm") val bpm: Double,

    @JsonProperty("cs") val cs: Float,

    @JsonProperty("ar") val ar: Float,

    @JsonProperty("od") val od: Float,

    @JsonProperty("hp") val hp: Float,

    @JsonProperty("diff") val starRating: Double,
) {

    @get:JsonProperty("status") val status: String
        get() = when(statusByte.toInt()) {
            -2 -> "graveyard"
            -1 -> "wip"
            0 -> "pending"
            1 -> "approved"
            2 -> "ranked"
            3 -> "loved"
            4 -> "qualified"
            else -> "graveyard"
        }

    @get:JsonProperty("mode") val mode: OsuMode
        get() = when(modeByte.toInt()) {
            0, 4, 8 -> OSU
            1, 5 -> TAIKO
            2, 6 -> CATCH
            3 -> MANIA
            else -> DEFAULT
        }
}
