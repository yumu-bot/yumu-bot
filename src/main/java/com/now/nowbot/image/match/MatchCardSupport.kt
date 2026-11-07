package com.now.nowbot.image.match

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.Mods
import com.now.nowbot.image.util.GameModes
import com.now.nowbot.model.osu.LazerScore

object MatchCardSupport {
    fun avatar(score: LazerScore, assets: ImageAssets): String? {
        val user = score.user
        return assets.remote(user.avatarUrl) ?: assets.remote("https://a.ppy.sh/${user.userID}") ?: assets.local("avatar-guest.png")
    }

    fun rankBackground(score: LazerScore, assets: ImageAssets): String? {
        return assets.local(GameModes.rankBackgroundFile(score.rank, score.passed))
    }

    fun rankImage(score: LazerScore, assets: ImageAssets): String? {
        return assets.local("object-score-${score.rank}2.png")
    }

    fun userRankColor(ranking: Int): String = when (ranking) {
        1 -> "#B7AA00"
        2 -> "#A0A0A0"
        3 -> "#AC6A00"
        else -> "#46393f"
    }

    fun ordinal(number: Int): String {
        if (number in 11..13) return "th"
        return when (number % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }

    fun judge(score: LazerScore, maxCombo: Int): Pair<String, String> {
        val stat = score.statistics
        val rank = score.rank
        val isPerfect = rank == "X" || rank == "XH"
        val isFc = score.perfectCombo
        val isAlmostFc = score.maxCombo > maxCombo * 0.9
        val isManiaFc = score.ruleset.toInt() == 3 && stat.meh == 0 && stat.ok == 0
        return when {
            rank == "F" -> "L" to "#9E040D"
            isPerfect -> "PF" to "#FF9800"
            isFc -> "FC+" to "#B3D465"
            stat.miss == 0 -> (if (isAlmostFc || isManiaFc) "FC" to "#009944" else "FC-" to "#B7AB00")
            isAlmostFc || isManiaFc -> "C+" to "#00A1E9"
            else -> "C" to "#0068B7"
        }
    }

    fun delta(score: Long, compare: Long): Triple<String, Long, String> {
        val sign = if (score < compare) "-" else "+"
        val value = kotlin.math.abs(score - compare)
        val color = if (score < compare) "#ffcdd2" else "#c2e5c3"
        return Triple(sign, value, color)
    }

    fun mods(score: LazerScore, x: Double, y: Double, height: Double, maxWidth: Double, align: String, assets: ImageAssets): String {
        return Mods.drawLazerMods(score.mods.filter { it.acronym != "NF" }, x, y, height, maxWidth, align, 5.0, false, false, assets).svg
    }
}
