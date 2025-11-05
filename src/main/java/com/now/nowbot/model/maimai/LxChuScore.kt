package com.now.nowbot.model.maimai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.time.OffsetDateTime
import kotlin.math.max
import kotlin.math.round

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class LxChuScore(
    @field:JsonProperty("id")
    val id: Long,

    @field:JsonProperty("song_name")
    val title: String,

    @field:JsonProperty("level")
    val level: String,

    @field:JsonProperty("level_index")
    val index: Byte,

    @field:JsonProperty("score")
    val score: Int,

    @field:JsonProperty("rating")
    val rating: Double,

    @field:JsonProperty("over_power")
    val overPower: Double,

    @field:JsonProperty("clear")
    val clear: String?,

    @field:JsonProperty("full_combo")
    val combo: String?,

    @field:JsonProperty("full_chain")
    val chain: String?,

    @field:JsonProperty("rank")
    val rank: String,

    @field:JsonProperty("play_time")
    val playTime: OffsetDateTime?,

    @field:JsonProperty("upload_time")
    val uploadTime: OffsetDateTime?,

    // BP 多少
    @JsonIgnoreProperties var position: Int = 0
) {
    fun toChuScore(song: ChuSong?): ChuScore {
        val lx = this

        return ChuScore().apply {
            val calDiff = getChunithmDifficulty(lx.score, lx.rating)

            this.chartID = -1
            this.star = if (calDiff <= 0.0) -1.0 else round(calDiff * 10) / 10.0
            this.score = lx.score
            this.combo = lx.combo ?: ""
            this.level = lx.level
            this.index = lx.index.toInt()
            this.difficulty = when(lx.index.toInt()) {
                0 -> "Basic"
                1 -> "Advanced"
                2 -> "Expert"
                3 -> "Master"
                4 -> "Ultima"
                5 -> "World's End"
                else -> ""
            }

            this.songID = lx.id
            this.rating = lx.rating
            this.title = lx.title

            song?.let {
                this.title = song.title
                this.alias = song.alias
                this.artist = song.info.artist
                this.charter = song.charts[lx.index.toInt()].charter
            }

            this.position = lx.position
        }
    }

    companion object {
        private fun getChunithmDifficulty(score : Int = 0, rating : Double = 0.0): Double {
            val difficulty: Double = if (score >= 1009000) rating - 2.15
            else if (score >= 1007500) rating - 2 - 0.15 * (score - 1007500) / (1009000 - 1007500)
            else if (score >= 1005000) rating - 1.5 - 0.5 * (score - 1005000) / (1007500 - 1005000)
            else if (score >= 1000000) rating - 1 - 0.5 * (score - 1000000) / (1005000 - 1000000)
            else if (score >= 975000) rating - 1.0 * (score - 975000) / (1000000 - 975000)
            else if (score >= 925000) rating + 3.0 - 3.0 * (score - 925000) / (975000 - 925000)
            else if (score >= 900000) rating + 5.0 - 2.0 * (score - 900000) / (925000 - 900000)
            else if (score >= 800000) rating / (0.5 + 0.5 * (score - 800000) / (900000 - 800000)) + 5.0
            else if (score >= 500000) rating / (0.5 * (score - 500000) / (800000 - 500000)) + 5.0
            else 0.0

            return round(max(difficulty, 0.0) * 10.0) / 10.0
        }
    }
}
