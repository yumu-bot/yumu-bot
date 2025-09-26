package com.now.nowbot.model.maimai

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.now.nowbot.util.DataUtil

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class LxChuBestScore(
    val bests: List<LxChuScore>,

    val selections: List<LxChuScore>,

    val newBests: List<LxChuScore>
) {
    fun toChuBestScore(user: LxChuUser, songs: List<ChuSong> = listOf()): ChuBestScore {
        val lx = this
        val map = songs.associateBy { it.songID }

        return ChuBestScore().apply {
            this.name = DataUtil.toHalfWidth(user.name)
            this.rating = user.rating
            this.records = ChuBestScore.Records(
                lx.bests.map {
                    val song = map[it.id.toInt()]
                    it.toChuScore(song)
                },

                lx.newBests.map {
                    val song = map[it.id.toInt()]
                    it.toChuScore(song)
                },

                lx.selections.map {
                    val song = map[it.id.toInt()]
                    it.toChuScore(song)
                },
            )

            this.probername = this.name

        }
    }
}
