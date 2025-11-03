package com.now.nowbot.model.ppysb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.*
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.model.osu.Covers

data class SBBeatmap(
    @JsonProperty("md5") val md5: String,

    @JsonProperty("id") val beatmapID: Long,

    @JsonProperty("set_id") val beatmapsetID: Long,

    @JsonProperty("artist") val artist: String,

    @JsonProperty("title") val title: String,

    @JsonProperty("version") val difficultyName: String,

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

    fun toBeatmapset(): Beatmapset {
        val sb = this

        return Beatmapset().apply {
            artist = sb.artist
            title = sb.title
            beatmapsetID = sb.beatmapsetID
            covers = Covers.getCoverFromCacheID(sb.beatmapsetID)
        }
    }

    fun toBeatmap(): Beatmap {
        val sb = this

        return Beatmap().apply {
            md5 = sb.md5
            beatmapID = sb.beatmapID
            beatmapsetID = sb.beatmapsetID

            difficultyName = sb.difficultyName
            lastUpdated = sb.lastUpdated
            totalLength = sb.totalLength
            hitLength = sb.totalLength // TODO 这里的数据不正确
            maxCombo = sb.maxCombo
            status = sb.status
            playCount = sb.playCount.toInt()
            passCount = sb.passCount.toInt()
            mode = sb.mode
            BPM = sb.bpm.toFloat()
            CS = sb.cs
            AR = sb.ar
            OD = sb.od
            HP = sb.hp
            starRating = sb.starRating
        }
    }
}
