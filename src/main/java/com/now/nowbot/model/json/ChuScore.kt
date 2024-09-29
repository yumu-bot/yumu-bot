package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.util.CollectionUtils

class ChuScore {
    @JsonProperty("cid") var chartID: Int = 0

    // 定数，也就是实际难度，类似于 osu 的星数
    @JsonProperty("ds") var star: Double = 0.0

    // 纯分数
    @JsonProperty("score") var score: Int = 0

    // 连击状态，有 FC 和 AP，空字符串就是啥都没有
    @JsonProperty("fc") var combo: String = ""

    // 定数的实际显示，0.6-0.9 后面会多一个 +
    @JsonProperty("level") var level: String = ""

    // 定数的位置，0-4
    @JsonProperty("level_index") var index: Int = 0

    // 实际所属的难度分类，Basic，Advanced，Expert，Master，Ultima, World's End
    @JsonProperty("level_label") var difficulty: String = ""

    @JsonProperty("mid") var songID: Long = 0

    // CHUNITHM rating，也就是 PP，保留两位小数
    @JsonProperty("ra") var rating: Double = 0.0

    @JsonProperty("title") var title: String = ""

    // BP 多少
    @JsonIgnoreProperties var position: Int = 0

    // 自己拿
    @JsonIgnoreProperties var artist: String = ""

    // 自己拿
    @JsonIgnoreProperties var charter: String = ""

    companion object {
        fun insertSongData(scores: MutableList<ChuScore>, data: MutableMap<Int, ChuSong>) {
            for (s in scores) {
                if (s.songID == 0L) {
                    continue
                }

                val d = data[s.songID.toInt()] ?: continue

                insertSongData(s, d)
            }
        }

        fun insertSongData(score: ChuScore, song: ChuSong) {
            val chart = song.charts.get(score.index)

            score.charter = chart.charter
            score.artist = song.info.artist
        }

        fun insertPosition(scores: MutableList<ChuScore>, isBest30: Boolean) {
            if (CollectionUtils.isEmpty(scores)) return

            for (i in scores.indices) {
                val s = scores[i]

                if (isBest30) {
                    s.position = (i + 1)
                } else {
                    s.position = (i + 31)
                }
            }
        }
    }
}
