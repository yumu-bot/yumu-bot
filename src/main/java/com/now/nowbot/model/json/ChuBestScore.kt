package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.util.CollectionUtils
import java.util.*

class ChuBestScore {
    // 在游戏里的名字
    @JsonProperty("nickname") var name: String = ""

    // CHUNITHM rating，也就是 PP，保留三位小数
    @JsonProperty("ra") var rating: Double = 0.0

    // best30 + recent10
    // 这就是 BP
    @JsonProperty("records") var records: Records = Records()

    data class Records(
            @JsonProperty("b30") val best30: MutableList<ChuScore> = mutableListOf(),
            @JsonProperty("r10") val recent10: MutableList<ChuScore> = mutableListOf(),
    )

    // 在查分器里的名字
    @JsonProperty("username") var probername: String = ""

    @JvmRecord
    data class User(
            val name: String?,
            val probername: String?,
            val rating: Double?,
            val base: Double?,
            val additional: Double?,
    )

    fun getUser(): User {
        val best30 =
                this.records.best30
                        .stream()
                        .map(ChuScore::rating)
                        .filter(Objects::nonNull)
                        .reduce { a, b -> a + b }
                        .orElse(0.0)
        val recent10 =
                this.records.recent10
                        .stream()
                        .map(ChuScore::rating)
                        .filter(Objects::nonNull)
                        .reduce { a, b -> a + b }
                        .orElse(0.0)

        return User(this.name, this.probername, this.rating, best30, recent10)
    }

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
                    s.position = (i + 36)
                }
            }
        }
    }
}
