package com.now.nowbot.model.ppysb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.enums.OsuMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.*
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.LazerStatistics
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatterBuilder

data class SBScore(
    @JsonProperty("id") val scoreID: Long,

    @JsonProperty("map_md5") val md5: String,

    @JsonProperty("userid") val userID: Long,

    @JsonProperty("score") val score: Long,

    @JsonProperty("pp") val pp: Double,

    @JsonProperty("acc") val accuracy: Double,

    @JsonProperty("max_combo") val maxCombo: Int,

    @set:JsonProperty("mods") @get:JsonIgnoreProperties var modInt: Int,

    @JsonProperty("n300") val count300: Int,

    @JsonProperty("n100") val count100: Int,

    @JsonProperty("n50") val count50: Int,

    @JsonProperty("nmiss") val countMiss: Int,

    @JsonProperty("ngeki") val countGeki: Int,

    @JsonProperty("nkatu") val countKatu: Int,

    @JsonProperty("grade") val rank: String,

    @set:JsonProperty("status") @get:JsonIgnoreProperties var statusByte: Byte,

    @set:JsonProperty("mode") @get:JsonIgnoreProperties var modeByte: Byte,

    @set:JsonProperty("play_time") @get:JsonIgnoreProperties var endedTime: OffsetDateTime,

    @JsonProperty("time_elapsed") val timeElapsed: Long,

    @JsonProperty("perfect") val perfect: Boolean,

    ) {

    @get:JsonProperty("mods") val mods: List<LazerMod>
        get() = LazerMod.getModsList(OsuMod.getModsList(modInt).map { it.acronym })

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

    @get:JsonProperty("statistics") val statistics: LazerStatistics
        get() = when(mode) {
            OSU -> LazerStatistics(
                great = count300,
                ok = count100,
                meh = count50,
                miss = countMiss,
            )
            TAIKO -> LazerStatistics(
                great = count300,
                ok = count100,
                miss = countMiss,
                )
            CATCH -> LazerStatistics(
                great = count300,
                largeTickHit = count100,
                smallTickHit = count50,
                largeTickMiss = countMiss,
                smallTickMiss = countKatu,
                )
            MANIA -> LazerStatistics(
                perfect = countGeki,
                great = count300,
                good = countKatu,
                ok = count100,
                meh = count50,
                miss = countMiss,
                )
            DEFAULT -> LazerStatistics()
        }

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

    @get:JsonProperty("play_time") val endedTimeString: String
        get() = formatter.format(endedTime)

    fun toLazerScore(): LazerScore {
        val sb = this

        return LazerScore().apply {
            this.scoreID = sb.scoreID
            this.beatmap.md5 = sb.md5
            this.userID = sb.userID
            this.score = sb.score
            this.PP = sb.pp
            this.lazerAccuracy = sb.accuracy
            this.maxCombo = sb.maxCombo
            this.mods = sb.mods
            this.statistics = sb.statistics
            this.rank = sb.rank
            this.beatmap.status = sb.status
            this.ruleset = when(sb.modeByte.toInt()) {
                0, 4, 8 -> 0
                1, 5 -> 1
                2, 6 -> 2
                3 -> 3
                else -> 0
            }
            this.endedTime = sb.endedTime
            this.fullCombo = sb.perfect
        }
    }

    companion object {
        private val formatter = DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .appendLiteral("T")
            .appendPattern("HH:mm:ss")
            .toFormatter()
    }
}
