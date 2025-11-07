package com.now.nowbot.model.maimai

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.now.nowbot.model.enums.MaiVersion

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class LxMaiSong(
    @field:JsonProperty("id")
    var songID: Int = 0,

    @field:JsonProperty("title")
    var title: String = "",

    @field:JsonProperty("artist")
    var artist: String = "",

    @field:JsonProperty("genre")
    var genre: String = "",

    @field:JsonProperty("bpm")
    var bpm: Int = 0,

    @field:JsonProperty("version")
    var version: Int = 0,

    @field:JsonProperty("difficulties")
    var difficulties: LxMaiDiff = LxMaiDiff()
) {

    // 曲名外号，需要自己设置
    @get:JsonProperty("alias") var alias: String? = null

    // 曲名外号，需要自己设置
    @get:JsonProperty("aliases") var aliases: List<String>? = null

    // 自己设置，可以高亮的难度，按 0-4 排布。如果是 null，则会全部显示（也包括宴会场）
    @JsonProperty("highlight") var highlight: List<Int>? = null

    fun toMaiSong(type: Boolean? = null): MaiSong {
        val lx = this

        val hasDX = difficulties.deluxe.isNotEmpty()
        val hasSD = difficulties.standard.isNotEmpty()

        val isDeluxe = (type == true || type == null) && hasDX

        val isUtage = !hasSD && !hasDX

        val difficulties = if (isUtage) {
            difficulties.utage
        } else if (isDeluxe) {
            difficulties.deluxe
        } else {
            difficulties.standard
        }

        val value = difficulties.maxOf { it.version }

        return MaiSong().apply {
            songID = if (isDeluxe) {
                lx.songID + 10000
            } else {
                lx.songID
            }
            title = lx.title
            alias = lx.alias
            aliases = lx.aliases
            this.type = if (isDeluxe) "DX" else "SD"
            star = difficulties.map { it.levelValue }
            level = difficulties.map { it.level }
            charts = difficulties.map { it.toMaiChart() }
            info = MaiSong.SongInfo().apply {
                title = lx.title
                artist = lx.artist
                genre = lx.genre
                bpm = lx.bpm
                version = MaiVersion.getVersionFromValue(value).full
                versionInt = value
                current = value >= MaiVersion.newestVersion.value
            }
        }
    }
}

data class LxMaiDiff(
    @field:JsonProperty("standard")
    val standard: List<LxMaiDifficulty> = listOf(),

    @field:JsonProperty("deluxe")
    @field:JsonAlias("dx")
    val deluxe: List<LxMaiDifficulty> = listOf(),

    @field:JsonProperty("utage")
    val utage: List<LxMaiDifficulty> = listOf(),
)

data class LxMaiDifficulty(
    @field:JsonProperty("type")
    var type: String = "",

    @field:JsonProperty("difficulty")
    var difficulty: Byte = 0,

    @field:JsonProperty("level")
    var level: String = "",

    // x10
    @field:JsonProperty("level_value")
    var levelValue: Double = 0.0,

    @field:JsonProperty("note_designer")
    var noteDesigner: String = "",

    @field:JsonProperty("version")
    var version: Int = 0,

    @field:JsonProperty("notes")
    var notes: LxMaiNote = LxMaiNote(),

    @field:JsonProperty("kanji")
    var kanji: String? = null,

    @field:JsonProperty("description")
    var description: String? = null,

    @field:JsonProperty("is_buddy")
    var isBuddy: Boolean? = null
) {
    fun toMaiChart(): MaiSong.MaiChart {
        val lx = this

        return MaiSong.MaiChart().apply {
            notes = MaiSong.MaiChart.MaiNote(
                lx.notes.tap,
                lx.notes.hold,
                lx.notes.slide,
                lx.notes.touch,
                lx.notes.`break`
            )

            charter = noteDesigner
        }
    }
}

data class LxMaiNote(
    @field:JsonProperty("total")
    var total: Int = 0,

    @field:JsonProperty("tap")
    var tap: Int = 0,

    @field:JsonProperty("hold")
    var hold: Int = 0,

    @field:JsonProperty("slide")
    var slide: Int = 0,

    @field:JsonProperty("touch")
    var touch: Int = 0,

    @field:JsonProperty("break")
    var `break`: Int = 0,
)