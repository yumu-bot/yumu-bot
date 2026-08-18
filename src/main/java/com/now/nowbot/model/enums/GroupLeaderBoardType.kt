package com.now.nowbot.model.enums

import com.now.nowbot.entity.RankConverter
import com.now.nowbot.model.osu.LazerScore

enum class GroupLeaderBoardType {
    PP, ACCURACY, COMBO, RANK, SCORE
    ;

    companion object {
        fun getType(string: String?): GroupLeaderBoardType {
            return when(string?.trim()?.lowercase()) {
                "rank", "ranking", "r", "k", "评级", "评价" -> RANK
                "accuracy", "ac", "acc", "a", "准度", "准确率", "准率", "精确", "精确率" -> ACCURACY
                "combo", "cb", "c", "连击" -> COMBO
                "score", "s", "成绩", "分数" -> SCORE
                else -> PP
            }
        }

        fun GroupLeaderBoardType.getComparator(): Comparator<LazerScore> {
            return Comparator { a, b ->
                val aIsF = a.rank == "F"
                val bIsF = b.rank == "F"

                when {
                    aIsF != bIsF -> if (aIsF) 1 else -1

                    aIsF -> {
                        val aStats = if (a.mode.safeModeValue == 3.toByte()) a.statistics.perfect else a.statistics.great
                        val bStats = if (b.mode.safeModeValue == 3.toByte()) b.statistics.perfect else b.statistics.great
                        bStats.compareTo(aStats)
                    }

                    else -> when (this) {
                        PP -> b.pp.compareTo(a.pp)
                        ACCURACY -> b.accuracy.compareTo(a.accuracy)
                        COMBO -> b.maxCombo.compareTo(a.maxCombo)
                        SCORE -> b.score.compareTo(a.score)
                        RANK -> RankConverter.rankToByte(b.rank).compareTo(RankConverter.rankToByte(a.rank))
                    }
                }
            }
        }
    }
}