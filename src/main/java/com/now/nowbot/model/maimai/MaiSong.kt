package com.now.nowbot.model.maimai

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

class MaiSong {

    @JsonProperty("song_id")
    @JsonAlias("id")
    fun setSongIDFromAny(value: Any) { // 接收 Any 类型
        this.songID = when (value) {
            is Int -> value
            is String -> value.toIntOrNull() ?: 0
            else -> 0
        }
    }

    @get:JsonProperty("song_id")
    @set:JsonIgnore
    var songID: Int = 0

    // 曲名
    @JsonProperty("title") var title: String = ""

    // 曲名外号，需要自己设置
    @get:JsonProperty("alias") var alias: String? = null

    // 曲名外号，需要自己设置
    @get:JsonProperty("aliases") var aliases: List<String>? = null

    // 种类，有 DX 和 SD
    @JsonProperty("type") var type: String = ""

    // 定数，也就是实际难度，类似于 osu 的星数
    @JsonProperty("ds") var star: List<Double> = listOf()

    // 定数的实际显示，0.6-0.9 后面会多一个 +，宴会场谱面会多一个 ?
    @JsonProperty("level") var level: List<String> = listOf()

    @JsonProperty("cids") var chartIDs: List<Int> = listOf()

    @JsonProperty("charts") var charts: List<MaiChart> = listOf()

    @JsonProperty("basic_info") var info: SongInfo = SongInfo()

    @get:JsonProperty("is_deluxe")
    val isDeluxe: Boolean
        get() = this.songID >= 10000

    @get:JsonProperty("is_utage")
    val isUtage: Boolean
        get() = this.songID >= 100000

    // 自己设置，可以高亮的难度，按 0-4 排布。如果是 null，则会全部显示（也包括宴会场）
    @JsonProperty("highlight") var highlight: Set<Int>? = null
    
    @JsonIgnore
    fun updateHighlight(input: Set<Int>) {
        // 1. 根据你的定义：空集合 [] 是满集。
        // 在数学交集逻辑中，任何集合与满集的交集 = 集合本身。
        // 所以如果传入的是空集合，直接 return，不改变现有存储。
        if (input.isEmpty()) {
            return
        }

        val current = this.highlight

        // 2. 如果当前是 null (代表空集)，则直接接受传入的值
        if (current == null) {
            this.highlight = input
        } else {
            // 3. 取交集：只保留两边都存在的元素
            // 例如：[1,2,3] intersect [1,2,4] = [1,2]

            // 注意：根据你的逻辑，如果交集变为空了，
            // 你需要决定 this.highlight 是变成空 Set 还是变成 null。
            // 这里建议转为可变 Set 存储。
            val intersect = current.intersect(input)

            if (intersect.isNotEmpty()) {
                this.highlight = intersect
            }
        }
    }

    class MaiChart {
        // 物件数量
        @get:JsonProperty("notes", access = JsonProperty.Access.READ_ONLY)
        var notes: MaiNote = MaiNote()

        @get:JsonProperty("dx_score")
        val dxScore: Int
            get() = 3 * notes.total

        @JsonProperty("notes", access = JsonProperty.Access.WRITE_ONLY)
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

        data class MaiNote(
                val tap: Int = 0,
                val hold: Int = 0,
                val slide: Int = 0,
                val touch: Int = 0, // 仅 DX 有
                val break_: Int = 0
        ) {
            @get:JsonProperty("total")
            val total: Int
                get() = tap + hold + slide + touch + break_
        }
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

        @JsonProperty("version") var versionInt: Int = 0

        // 歌曲是否为当前版本的新歌
        @JsonProperty("is_new") var current: Boolean = false
    }
}
