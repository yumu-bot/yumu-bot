package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA2
import com.now.nowbot.image.card.CardF2
import com.now.nowbot.image.card.CardF3
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.font.TextPart
import com.now.nowbot.image.font.Typography
import com.now.nowbot.image.scorelist.ScoreListGenerate
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.model.match.Match.MatchRound
import com.now.nowbot.model.match.MatchRating
import kotlin.math.ceil
import kotlin.math.max

object PanelF3 {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PF-2);\">"
    private const val MARK_MAIN = "<g id=\"MainCard\">"
    private const val MARK_BODY = "<g id=\"BodyCard\">"

    fun render(rating: MatchRating, round: MatchRound, roundResult: Boolean, assets: ImageAssets): String {
        var svg = Templates.read("Panel_F2.svg")
        val stat = rating.match.statistics
        val matchTime = if (stat.endTime != null) {
            "match ends at: " + TimeFormat.formatted(stat.endTime)
        } else {
            "match starts at: " + TimeFormat.formatted(stat.startTime) + " - in progress"
        }
        val requestTime = "$matchTime // request time: ${TimeFormat.nowTimestamp()}"
        val panelName = if (roundResult) {
            PanelBatchSupport.panelName("Round Results v2 (passive module)", "RR", requestTime)
        } else {
            PanelBatchSupport.panelName("Match Rounds v2 (!ymro)", "RO", requestTime)
        }
        svg = Svg.setText(svg, panelName, MARK_INDEX)

        val isTeamVs = round.teamType == "team-vs" || round.teamType.isEmpty()
        val info = ScoreListGenerate.matchRating2CardA2(rating, assets, isMatchStart = false)
        val beatmap = round.beatmap?.let { CardA2.fromMatchBeatmap(it, assets) }
        val main = Svg.group(40.0, 40.0, CardA2.render(info, assets)) +
            (beatmap?.let { Svg.group(1450.0, 40.0, CardA2.render(it, assets)) } ?: "")
        svg = Svg.setText(svg, main, MARK_MAIN)

        val banner = drawScoreBanner(round, isTeamVs)
        val reds = round.scores.filter { it.playerStat?.team == "red" }
        val blues = round.scores.filter { it.playerStat?.team == "blue" }
        val body = drawBody(isTeamVs, reds, blues, round.scores, round.beatmap?.maxCombo ?: 0, assets)
        svg = Svg.setTexts(svg, listOf(Svg.group(0.0, 330.0, banner), Svg.group(0.0, 450.0, body)), MARK_BODY)

        svg = Svg.setText(svg, round.beatmap?.let { assets.mapBackground(it, "cover") }?.let {
            Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7)
        }, MARK_BANNER)

        val cardHeight = if (isTeamVs) {
            if (max(reds.size, blues.size) > 6) {
                ceil((max(reds.size, blues.size) - 6) / 2.0).toInt() * 195.0 + 790.0
            } else 790.0
        } else if (round.scores.size > 12) 985.0 else 790.0
        return PanelBatchSupport.applyHeights(svg, 1080.0, cardHeight)
    }

    private fun drawScoreBanner(round: MatchRound, isTeamVs: Boolean): String {
        val base = PanelDraw.rect(-40.0, 0.0, 980.0, 120.0, 20.0, "#382E32") +
            PanelDraw.rect(980.0, 0.0, 980.0, 120.0, 20.0, "#382E32")
        val winningTeam = round.winningTeam
        val left: String
        val right: String
        val leftText: String
        val rightText: String
        if (isTeamVs) {
            left = PanelDraw.linearGradientRect(-40.0, 0.0, 980.0, 120.0, 20.0, listOf("#801D34", "#E50050"), listOf(0, 100), listOf(0, 0), if (winningTeam == "red") 1.0 else 0.2)
            right = PanelDraw.linearGradientRect(980.0, 0.0, 980.0, 120.0, 20.0, listOf("#00A0E9", "#006899"), listOf(0, 100), listOf(0, 0), if (winningTeam == "blue") 1.0 else 0.2)
            leftText = Fonts.poppinsBold.textPath(if (winningTeam == "red") "WIN!" else if (winningTeam == null) "TIE..." else "LOSE...", 40.0, 84.0, 72.0, "left baseline", "#fff", 0.5)
            rightText = Fonts.poppinsBold.textPath(if (winningTeam == "blue") "WIN!" else if (winningTeam == null) "TIE..." else "LOSE...", 1880.0, 84.0, 72.0, "right baseline", "#fff", 0.5)
        } else {
            left = PanelDraw.linearGradientRect(-40.0, 0.0, 980.0, 120.0, 20.0, listOf("#ccc", "#fff"), listOf(0, 100), listOf(0, 0), 0.6)
            right = PanelDraw.linearGradientRect(980.0, 0.0, 980.0, 120.0, 20.0, listOf("#fff", "#ccc"), listOf(0, 100), listOf(0, 0), 0.8)
            leftText = Fonts.poppinsBold.textPath("TOTAL", 40.0, 84.0, 72.0, "left baseline", "#fff", 0.5)
            rightText = Fonts.poppinsBold.textPath("AVERAGE", 1880.0, 84.0, 72.0, "right baseline", "#fff", 0.5)
        }
        val leftScore: String
        val rightScore: String
        if (isTeamVs) {
            val rs = Numbers.floorParts(round.redTeamScore, -4)
            leftScore = Typography.multipleTextPath(
                listOf(
                    TextPart(if (winningTeam == "blue") "${round.redTeamScore - round.blueTeamScore}   " else "", Fonts.poppinsBold, 30.0, "#ccc"),
                    TextPart(rs.integer, Fonts.poppinsBold, 72.0, "#fff"),
                    TextPart(rs.decimal, Fonts.poppinsBold, 48.0, "#fff"),
                ), 900.0, 85.0, "right baseline",
            )
            val bs = Numbers.floorParts(round.blueTeamScore, -4)
            rightScore = Typography.multipleTextPath(
                listOf(
                    TextPart(bs.integer, Fonts.poppinsBold, 72.0, "#fff"),
                    TextPart(bs.decimal, Fonts.poppinsBold, 48.0, "#fff"),
                    TextPart(if (winningTeam == "red") "   ${round.blueTeamScore - round.redTeamScore}" else "", Fonts.poppinsBold, 30.0, "#ccc"),
                ), 1020.0, 85.0, "left baseline",
            )
        } else {
            val ts = Numbers.floorParts(round.totalTeamScore, -4)
            leftScore = Typography.multipleTextPath(
                listOf(TextPart(ts.integer, Fonts.poppinsBold, 72.0, "#fff"), TextPart(ts.decimal, Fonts.poppinsBold, 48.0, "#fff")),
                900.0, 85.0, "right baseline",
            )
            val avg = Numbers.floorParts(if (round.scores.isNotEmpty()) round.totalTeamScore.toDouble() / round.scores.size else 0.0, -4)
            rightScore = Typography.multipleTextPath(
                listOf(TextPart(avg.integer, Fonts.poppinsBold, 72.0, "#fff"), TextPart(avg.decimal, Fonts.poppinsBold, 48.0, "#fff")),
                1020.0, 85.0, "left baseline",
            )
        }
        return "<g>${base + left + right + leftScore + rightScore + leftText + rightText}</g>"
    }

    private fun drawBody(isTeamVs: Boolean, reds: List<com.now.nowbot.model.osu.LazerScore>, blues: List<com.now.nowbot.model.osu.LazerScore>, scores: List<com.now.nowbot.model.osu.LazerScore>, maxCombo: Int, assets: ImageAssets): String {
        if (isTeamVs) {
            return Svg.group(510.0, 40.0, drawTeam(reds, blues, maxCombo, false, assets)) +
                Svg.group(980.0, 40.0, drawTeam(blues, reds, maxCombo, true, assets))
        }
        return Svg.group(40.0, 40.0, drawH2H(scores, maxCombo, assets))
    }

    private fun drawH2H(scores: List<com.now.nowbot.model.osu.LazerScore>, maxCombo: Int, assets: ImageAssets): String {
        if (scores.isEmpty()) return ""
        val p1Size = when {
            scores.size <= 4 -> scores.size
            scores.size <= 6 -> 3
            scores.size <= 8 -> 2
            scores.size <= 10 -> 1
            else -> 0
        }
        val compare = if (scores.size >= 2) (scores[0].score.takeIf { it > 0 } ?: (scores[0].legacyScore ?: 0)) else scores.maxOf { it.score.takeIf { it > 0 } ?: (it.legacyScore ?: 0) }
        val p1 = scores.take(p1Size)
        val p2 = scores.drop(p1Size)
        val offset = when (scores.size) {
            1 -> 960.0 - 430.0 / 2 - 40.0
            2 -> 960.0 - 430.0 - 60.0
            3 -> 960.0 - 430.0 * 3 / 2 - 80.0
            else -> 0.0
        }
        val builder = StringBuilder()
        p1.forEachIndexed { i, score -> builder.append(Svg.group(offset + 470.0 * i, 0.0, CardF2.render(score, maxCombo, compare, assets))) }
        p2.forEachIndexed { i, score ->
            val cols = if (p1Size > 0) 4 - p1Size else 4
            val x = i % cols
            val y = i / cols
            builder.append(Svg.group(470.0 * (x + p1Size), 195.0 * y, CardF3.render(score, maxCombo, compare, assets)))
        }
        return builder.toString()
    }

    private fun drawTeam(teamScores: List<com.now.nowbot.model.osu.LazerScore>, enemyScores: List<com.now.nowbot.model.osu.LazerScore>, maxCombo: Int, toRight: Boolean, assets: ImageAssets): String {
        if (teamScores.isEmpty()) return ""
        val p1Size = if (teamScores.size <= 2) teamScores.size else if (teamScores.size <= 4) 1 else 0
        val e = enemyScores.size
        val p1 = teamScores.take(p1Size)
        val p2 = teamScores.drop(p1Size)
        val sign = if (toRight) 1.0 else -1.0
        val builder = StringBuilder()
        p1.forEachIndexed { i, score -> builder.append(Svg.group(470.0 * sign * i, 0.0, CardF2.render(score, maxCombo, compare(enemyScores, i, teamScores.size, e), assets))) }
        p2.forEachIndexed { i, score ->
            val (x, y) = if (p1Size == 0) (i % 2 to i / 2) else (1 to i)
            builder.append(Svg.group(470.0 * sign * x, 195.0 * y, CardF3.render(score, maxCombo, compare(enemyScores, i, teamScores.size, e), assets)))
        }
        return builder.toString()
    }

    private fun compare(enemy: List<com.now.nowbot.model.osu.LazerScore>, i: Int, size: Int, enemySize: Int): Long {
        if (enemySize == 0) return 0L
        val index = if (enemySize > 1) (i.toDouble() / (size - 1).coerceAtLeast(1) * (enemySize - 1)).toInt() else 0
        val score = enemy[index.coerceIn(0, enemy.lastIndex)]
        return score.score.takeIf { it > 0 } ?: (score.legacyScore ?: 0)
    }
}
