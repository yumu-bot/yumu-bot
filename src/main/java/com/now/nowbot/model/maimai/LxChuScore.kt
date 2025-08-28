package com.now.nowbot.model.maimai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.time.OffsetDateTime

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class LxChuScore(
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("song_name")
    val name: String,

    @JsonProperty("level")
    val level: String,

    @JsonProperty("level_index")
    val index: Byte,

    @JsonProperty("score")
    val score: Int,

    @JsonProperty("rating")
    val rating: Double,

    @JsonProperty("over_power")
    val overPower: Double,

    @JsonProperty("clear")
    val clear: String?,

    @JsonProperty("full_combo")
    val combo: String?,

    @JsonProperty("full_chain")
    val chain: String?,

    @JsonProperty("rank")
    val rank: String,

    @JsonProperty("play_time")
    val playTime: OffsetDateTime,

    @JsonProperty("upload_time")
    val uploadTime: OffsetDateTime,

    // BP 多少
    @JsonIgnoreProperties var position: Int = 0
) {
    fun toChuScore(song: ChuSong?): ChuScore {
        val lx = this

        return ChuScore().apply {
            this.chartID = -1
            this.star = -1.0
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

            song?.let {
                this.title = song.title
                this.alias = song.alias
                this.artist = song.info.artist
                this.charter = song.charts[lx.index.toInt()].charter
            }

            this.position = lx.position
        }
    }
}
