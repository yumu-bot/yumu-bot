package com.now.nowbot.model.maimai

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
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
        @field:JsonProperty("b30") val best30: List<ChuScore> = listOf(),
        @field:JsonProperty("n20") val new20: List<ChuScore> = listOf(),
        @field:JsonAlias("r10")
        @field:JsonProperty("s10") val selection10: List<ChuScore> = listOf(),
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
        val best30 = this.records.best30.sumOf { it.rating }
        val new20 = this.records.new20.sumOf { it.rating }

        val sum = best30 + new20

        val best = if (sum > 0) this.rating * best30 / sum else 0.0
        val recent = if (sum > 0) this.rating * new20 / sum else 0.0

        val bestAverage = getAverage(this.records.best30)

        return User(this.name, this.probername, this.rating, best, recent, bestAverage)
    }

    companion object {

        fun getAverage(scores: List<ChuScore>): Double {
            if (scores.isEmpty()) return 0.0

            return scores.map { getChunithmRating(it.score, it.star) }.average()
        }

        private fun getChunithmRating(score : Int = 0, difficulty : Double = 0.0): Double {
            val rating: Double = if (score >= 1009000) difficulty + 2.15
            else if (score >= 1007500) difficulty + 2 + 0.15 * (score - 1007500) / (1009000 - 1007500)
            else if (score >= 1005000) difficulty + 1.5 + 0.5 * (score - 1005000) / (1007500 - 1005000)
            else if (score >= 1000000) difficulty + 1 + 0.5 * (score - 1000000) / (1005000 - 1000000)
            else if (score >= 975000) difficulty + 1.0 * (score - 975000) / (1000000 - 975000)
            else if (score >= 925000) difficulty - 3 + 3.0 * (score - 925000) / (975000 - 925000)
            else if (score >= 900000) difficulty - 5 + 2.0 * (score - 900000) / (925000 - 900000)
            else if (score >= 800000) (difficulty - 5) * (0.5 + 0.5 * (score - 800000) / (900000 - 800000))
            else if (score >= 500000) (difficulty - 5) * (0.5 * (score - 500000) / (800000 - 500000))
            else 0.0

            return floor(max(rating, 0.0) * 100.0) / 100.0
        }
    }
}
