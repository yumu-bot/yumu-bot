package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.util.JacksonUtil

class MaiSong {
    @JsonIgnoreProperties var songID: Int = 0

    @JsonProperty("id") private fun setSongID(node: JsonNode) {
        this.songID = JacksonUtil.parseObject(node, String::class.java).toInt()
    }

    // 曲名
    @JsonProperty("title") var title: String = ""

    // 曲名外号，需要自己设置
    @get:JsonProperty("alias") var alias: String? = null

    // 种类，有 DX 和 SD
    @JsonProperty("type") var type: String = ""

    // 定数，也就是实际难度，类似于 osu 的星数
    @JsonProperty("ds") var star: List<Double> = listOf()

    // 定数的实际显示，0.6-0.9 后面会多一个 +，宴会场谱面会多一个 ?
    @JsonProperty("level") var level: List<String> = listOf()

    @JsonProperty("cids") var chartIDs: List<Int> = listOf()

    @JsonProperty("charts") var charts: List<MaiChart> = listOf()

    @JsonProperty("basic_info") var info: SongInfo = SongInfo()

    class MaiChart {
        // 物件数量
        @JsonIgnoreProperties var notes: MaiNote = MaiNote()

        @get:JsonProperty val dxScore: Int
            get() = 3 * (notes.tap + notes.touch + notes.hold + notes.slide + notes.break_)

        @JsonProperty("notes")
        fun setNotes(list: List<Int>) {
            if (list.isEmpty() || list.size < 4) {
                notes = MaiNote(0, 0, 0, 0, 0)
            } else if (list.size == 4) {
                notes = MaiNote(list.first(), list[1], list[2], 0, list[3])
            } else if (list.size == 5) {
                notes = MaiNote(list.first(), list[1], list[2], list[3], list.last())
            }
        }

        // 谱师
        @JsonProperty("charter") var charter: String = ""

        @JvmRecord
        data class MaiNote(
                val tap: Int = 0,
                val hold: Int = 0,
                val slide: Int = 0,
                val touch: Int = 0, // 仅 DX 有
                val break_: Int = 0
        )
    }

    class SongInfo {
        // 曲名
        @JsonProperty("title") var title: String = ""

        // 艺术家名
        @JsonProperty("artist") var artist: String = ""

        // 歌曲分类，有东方Project，niconico & VOCALOID，其他游戏等等
        @JsonProperty("genre") var genre: String = ""

        // 曲速，向下取整过的
        @JsonProperty("bpm") var bpm: Int = 0

        // 预期解禁时间，这个默认为空字符串
        @JsonProperty("release_date") var release: String = ""

        // 加入 maimai 时的版本
        @JsonProperty("from") var version: String = ""

        // 歌曲是否为当前版本的新歌
        @JsonProperty("is_new") var current: Boolean = false
    }
}
