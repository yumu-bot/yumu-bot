package com.now.nowbot.model.maimai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

// 这个类有个几把用...真不知道他返回这么简单的数据怎么用
class MaiScoreSimplified {

    // 也就是准确率
    @JsonProperty("achievements") var achievements: Double = 0.0

    // 连击状态，有 FC 和 AP，空字符串就是啥都没有
    @JsonProperty("fc") var combo: String = ""

    // 同步状态，主要是综合看你和拼机的玩家
    @JsonProperty("fs") var sync: String = ""

    // 等于 sid
    @JsonProperty("id") var songID: Long = 0

    // 定数的实际显示，0.6-0.9 后面会多一个 +，宴会场谱面会多一个 ?
    @JsonProperty("level") var level: String = ""

    // 定数的位置，0-4
    @JsonProperty("level_index") var index: Int = 0

    // 歌名
    @JsonProperty("title") var title: String = ""

    // 谱面种类，有DX和SD之分
    @JsonProperty("type") var type: String = ""

    // BP 多少
    @JsonIgnoreProperties var position: Int = 0

    fun toMaiScore(): MaiScore {
        val s = MaiScore()

        s.achievements = this.achievements
        s.combo = this.combo
        s.sync = this.sync
        s.songID = this.songID
        s.level = this.level
        s.index = this.index
        s.title = this.title
        s.type = this.type
        s.position = this.position

        return s
    }

    companion object {
        @JvmStatic
        fun parseMaiScoreList(
            full: List<MaiScore>,
            lite: List<MaiScoreSimplified>
        ): List<MaiScore> {
            val out = lite.map { l ->
                val index: String = l.songID.toString() + "_" + l.index.toString()

                val map: Map<String, MaiScore> =
                    full.associateBy { it.songID.toString() + "_" + it.index.toString() }

                val s = map[index] ?: l.toMaiScore()

                return@map s
            }.sortedByDescending { it.rating }

            /*
            for (l in lite) {
                val index: String = l.songID.toString() + "_" + l.index.toString()

                val map: Map<String, MaiScore> =
                        full.associateBy { it.songID.toString() + "_" + it.index.toString() }

                val s = map[index] ?: l.toMaiScore()

                out.add(s)
            }

            out.sortByDescending(MaiScore::rating)

             */

            return out
        }
    }
}
