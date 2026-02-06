package com.now.nowbot.model.ppysb

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.*
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.model.osu.Covers

data class SBBeatmap(
    @field:JsonProperty("md5") val md5: String,

    @field:JsonProperty("id") val beatmapID: Long,

    @field:JsonProperty("set_id") val beatmapsetID: Long,

    @field:JsonProperty("artist") val artist: String,

    @field:JsonProperty("title") val title: String,

    @field:JsonProperty("version") val difficultyName: String,

    @field:JsonProperty("last_update") val lastUpdated: String,

    @field:JsonProperty("total_length") val totalLength: Int,

    @field:JsonProperty("max_combo") val maxCombo: Int,

    @set:JsonProperty("status") @get:JsonIgnore var statusByte: Byte,

    @field:JsonProperty("plays") val playCount: Long,

    @field:JsonProperty("passes") val passCount: Long,

    @set:JsonProperty("mode") @get:JsonIgnore var modeByte: Byte,

    @field:JsonProperty("bpm") val bpm: Double,

    @field:JsonProperty("cs") val cs: Float,

    @field:JsonProperty("ar") val ar: Float,

    @field:JsonProperty("od") val od: Float,

    @field:JsonProperty("hp") val hp: Float,

    @field:JsonProperty("diff") val starRating: Double,
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
            this.bpm = sb.bpm.toFloat()
            this.cs = sb.cs
            this.ar = sb.ar
            this.od = sb.od
            this.hp = sb.hp
            starRating = sb.starRating
        }
    }
}
