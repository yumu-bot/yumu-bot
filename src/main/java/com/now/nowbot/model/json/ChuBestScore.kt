package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.util.CollectionUtils
import java.util.*
import kotlin.math.floor
import kotlin.math.max

class ChuBestScore {
    // 在游戏里的名字
    @JsonProperty("nickname") var name: String = ""

    // CHUNITHM rating，也就是 PP，保留三位小数
    @JsonProperty("rating") var rating: Double = 0.0

    // best30 + recent10
    // 这就是 BP
    @JsonProperty("records") var records: Records = Records()

    data class Records(
        @JsonProperty("r10") val recent10: MutableList<ChuScore> = mutableListOf(),
        @JsonProperty("b30") val best30: MutableList<ChuScore> = mutableListOf(),
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
            val average: Double?,
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

        val sum = best30 + recent10

        val best = if (sum > 0) this.rating * best30 / sum else 0.0
        val recent = if (sum > 0) this.rating * recent10 / sum else 0.0

        val bestAverage = getAverage(this.records.best30)

        return User(this.name, this.probername, this.rating, best, recent, bestAverage)
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
                    s.position = (i + 31)
                }
            }
        }

        fun getAverage(scores: MutableList<ChuScore>): Double {
            if (scores.isEmpty()) return 0.0

            return scores.stream().map{getCHUNITHMRating(it.score, it.star)}.reduce{a, b -> a + b}.orElse(0.0) / scores.size
        }


        private fun getCHUNITHMRating(score : Int = 0, difficulty : Double = 0.0): Double {
            return if (score >= 1009000) difficulty + 2.15
            else if (score >= 1007500) difficulty + 2 + floor((score - 1007500) / 100.0) * 0.01
            else if (score >= 1005000) difficulty + 1.5 + floor((score - 1005000) / 50.0) * 0.01
            else if (score >= 1000000) difficulty + 1 + floor((score - 1000000) / 100.0) * 0.01
            else if (score >= 975000) difficulty + floor((score - 975000) / 250.0) * 0.01
            else if (score >= 925000) max(difficulty - 3, 0.0)
            else if (score >= 900000) max(difficulty - 5, 0.0)
            else if (score >= 800000) max(difficulty - 5, 0.0) / 2
            else 0.0
        }
    }
}
