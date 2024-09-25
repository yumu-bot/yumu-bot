package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.util.CollectionUtils

class MaiScore {
    // 也就是准确率
    var achievements: Double = 0.0

    // 定数，也就是实际难度，类似于 osu 的星数
    @JsonProperty("ds") var star: Double = 0.0

    // DX 分，Critical Perfect 3 分，Perfect 2 分，Great 1 分
    // 在查别人的时候，这个值是 0
    @JsonProperty("dxScore") var score: Int = 0

    // 连击状态，有 FC 和 AP，空字符串就是啥都没有
    @JsonProperty("fc") var combo: String = ""

    // 同步状态，主要是综合看你和拼机的玩家
    @JsonProperty("fs") var sync: String = ""

    // 定数的实际显示，0.6-0.9 后面会多一个 +，宴会场谱面会多一个
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

    // 谱面种类，有DX和SD之分
    @JsonProperty("type") var type: String = ""

    // 通过 MaiSong 算出来的理论 DX Score
    @JsonIgnoreProperties var max: Int = 0

    // BP 多少
    @JsonIgnoreProperties var position: Int = 0

    // 自己拿
    @JsonIgnoreProperties var artist: String = ""

    // 自己拿
    @JsonIgnoreProperties var charter: String = ""

    fun toMaiScoreLite(): MaiScoreLite {
        val s = MaiScoreLite()

        s.achievements = this.achievements
        s.combo = this.combo
        s.sync = this.sync
        s.songID = this.songID
        s.level = this.level
        s.index = this.index
        s.title = this.title
        s.type = this.type

        return s
    }

    companion object {
        fun insertSongData(scores: MutableList<MaiScore>, data: MutableMap<Int, MaiSong>) {
            for (s in scores) {
                if (s.songID == 0L) {
                    continue
                }

                val d = data[s.songID.toInt()] ?: continue

                insertSongData(s, d)
            }
        }

        fun insertSongData(score: MaiScore, song: MaiSong) {
            val chart = song.getCharts().get(score.index)
            val notes = chart.getNotes()

            score.charter = chart.getCharter()
            score.max = 3 * (notes.tap + notes.touch + notes.hold + notes.slide + notes.break_)

            score.artist = song.getInfo().getArtist()
        }

        fun insertPosition(scores: MutableList<MaiScore>, isBest30: Boolean) {
            if (CollectionUtils.isEmpty(scores)) return

            for (i in scores.indices) {
                val s = scores[i]

                if (isBest30) {
                    s.position = (i + 1)
                } else {
                    s.position = (i + 36)
                }
            }
        }
    }
}
