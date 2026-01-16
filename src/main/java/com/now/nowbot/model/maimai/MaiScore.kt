package com.now.nowbot.model.maimai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

class MaiScore {
    // 也就是准确率
    @JsonProperty("achievements") var achievements: Double = 0.0

    // 定数，也就是实际难度，类似于 osu 的星数
    @JsonProperty("ds") var star: Double = 0.0

    // DX 分，Critical Perfect 3 分，Perfect 2 分，Great 1 分
    // 在查别人的时候，这个值是 0
    @JsonProperty("dxScore") var score: Int = 0

    // 连击状态，有 FC 和 AP，空字符串就是啥都没有
    @JsonProperty("fc") var combo: String = ""

    // 同步状态，主要是综合看你和拼机的玩家
    @JsonProperty("fs") var sync: String = ""

    // 定数的实际显示，0.6-0.9 后面会多一个 +，宴会场谱面会多一个 ?
    @JsonProperty("level") var level: String = ""

    // 定数的位置，0-4
    @JsonProperty("level_index") var index: Int = 0

    // 实际所属的难度分类，Basic，Advanced，Expert，Master，Re:Master
    @JsonProperty("level_label") var difficulty: String = ""

    // DX rating，也就是 PP，通过计算向下取整
    @JsonProperty("ra") var rating: Int = 0

    // 实际的评级
    @JsonProperty("rate") var rank: String = ""

    // 等于 sid
    @JsonProperty("song_id") var songID: Long = 0

    // 歌名
    @JsonProperty("title") var title: String = ""

    // 外号，自己填
    @get:JsonProperty("alias") var alias: String? = null

    // 外号，自己填
    @get:JsonProperty("aliases") var aliases: List<String>? = null

    // 谱面种类，有DX和SD之分
    @JsonProperty("type") var type: String = ""

    // 通过 MaiSong 算出来的理论 DX Score
    @get:JsonProperty("max") var max: Int = 0

    // BP 多少
    @get:JsonProperty("position") var position: Int = 0

    // 自己拿
    @get:JsonProperty("artist") var artist: String = ""

    // 自己拿
    @get:JsonProperty("charter") var charter: String = ""

    // 自己拿
    @get:JsonProperty("genre") var genre: String = ""

    // 自己拿
    @get:JsonProperty("version") var version: String = ""

    // 自己拿
    @get:JsonProperty("notes") var notes: List<Int> = listOf()

    // 自己拿
    @get:JsonProperty("bpm") var bpm: Int = 0

    @get:JsonProperty("is_deluxe")
    val isDeluxe: Boolean
        get() = this.songID >= 10000

    @get:JsonProperty("is_utage")
    val isUtage: Boolean
        get() = this.songID >= 100000

    @get:JsonIgnoreProperties
    val independentID
        get() = this.songID * 10 + this.index
}
