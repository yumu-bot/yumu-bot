package com.now.nowbot.model.maimai

import com.fasterxml.jackson.annotation.JsonProperty

class ChuSong {
    @JsonProperty("id") var songID: Int = 0

    // 曲名
    @JsonProperty("title") var title: String = ""

    // 曲名外号，需要自己设置
    @get:JsonProperty("alias") var alias: String? = null

    // 定数，也就是实际难度，类似于 osu 的星数
    @JsonProperty("ds") var star: List<Double> = listOf()

    // 定数的实际显示，0.6-0.9 后面会多一个 +，宴会场谱面会写在六号位，是 ?
    @JsonProperty("level") var level: List<String> = listOf()

    @JsonProperty("cids") var chartIDs: List<Int> = listOf()

    @JsonProperty("charts") var charts: List<ChuChart> = listOf()

    @JsonProperty("basic_info") var info: SongInfo = SongInfo()

    class ChuChart {
        // 物件数量
        @JsonProperty("combo") var combo: Int = 0

        // 谱师
        @JsonProperty("charter") var charter: String = ""
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

        // 预期解禁时间，chu 没有这个
        // @JsonProperty("release_date") var release: String = ""

        // 加入 chunithm 时的版本
        @JsonProperty("from") var version: String = ""

    }
}
