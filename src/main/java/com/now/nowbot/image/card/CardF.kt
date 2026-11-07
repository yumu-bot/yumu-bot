package com.now.nowbot.image.card

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.match.MatchCardSupport
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.Texts

data class MatchPlayerScore(
    val name: String = "Unknown",
    val avatar: String? = null,
    val score: Long = 0,
    val mods: List<String> = emptyList(),
    val rank: Int = 0,
)

data class CardFData(
    val isTeamVs: Boolean = false,
    val redWin: Boolean = false,
    val blueWin: Boolean = false,
    val redScore: Long = 0,
    val blueScore: Long = 0,
    val totalScore: Long = 0,
    val redWinsBefore: Int = 0,
    val blueWinsBefore: Int = 0,
    val red: List<MatchPlayerScore> = emptyList(),
    val blue: List<MatchPlayerScore> = emptyList(),
    val none: List<MatchPlayerScore> = emptyList(),
)

object CardF {
    private const val MARK_TEXT = "<g id=\"Text\">"
    private const val MARK_BODY = "<g id=\"BodyCard\">"
    private const val MARK_RED_POINT = "<g id=\"RedPoint\">"
    private const val MARK_BLUE_POINT = "<g id=\"BluePoint\">"
    private const val MARK_PLUS_POINT = "<g id=\"PlusPoint\">"
    private const val MARK_SCORE_BAR = "<g id=\"ScoreBar\">"
    private const val MARK_BACK_COLOR = "<g id=\"BackColor\">"
    private const val MARK_H2H_AVATAR = "<g style=\"clip-path: url(#clippath-CC-1);\">"

    fun render(data: CardFData, assets: ImageAssets): String {
        var svg = Templates.read("Card_C.svg")
        val delta = if (data.isTeamVs) kotlin.math.abs(data.redScore - data.blueScore) else 0L
        if (data.isTeamVs) {
            val redPoint = PanelDraw.rect(0.0, 0.0, 12.0, 24.0, 6.0, "#CB3554")
            val bluePoint = PanelDraw.rect(0.0, 0.0, 12.0, 24.0, 6.0, "#009DEA")
            for (i in 0 until data.redWinsBefore) {
                svg = Svg.setText(svg, Svg.group(30.0 + 16.0 * i, 176.0, redPoint), MARK_RED_POINT)
            }
            for (i in 0 until data.blueWinsBefore) {
                svg = Svg.setText(svg, Svg.group(1338.0 - 16.0 * i, 176.0, bluePoint), MARK_BLUE_POINT)
            }
            val rawX = (data.redScore.toDouble() / data.totalScore) * 1330.0
            val midX = Numbers.clampToInteger(rawX, 930.0, 400.0) + 25.0
            val redFont = if (data.redWin) Fonts.torusBold else Fonts.torusRegular
            val blueFont = if (data.blueWin) Fonts.torusBold else Fonts.torusRegular
            if (data.redWin) {
                svg = Svg.setText(svg, PanelDraw.linearGradientRect(0.0, 0.0, 1380.0, 210.0, 20.0, listOf("#CB3554", "#382E32"), listOf(20, 100), listOf(0, 0), 0.6), MARK_BACK_COLOR)
            } else if (data.blueWin) {
                svg = Svg.setText(svg, PanelDraw.linearGradientRect(0.0, 0.0, 1380.0, 210.0, 20.0, listOf("#382E32", "#008FE3"), listOf(0, 80), listOf(0, 0), 0.6), MARK_BACK_COLOR)
            } else {
                svg = Svg.setText(svg, PanelDraw.rect(0.0, 0.0, 1380.0, 210.0, 20.0, "#382e32"), MARK_BACK_COLOR)
            }
            val redText = if (data.redScore != 0L) redFont.textPath(data.redScore.toString(), midX - 5.0, 196.836, 24.0, "right baseline", "#fff") else ""
            val blueText = if (data.blueScore != 0L) blueFont.textPath(data.blueScore.toString(), midX + 5.0, 196.836, 24.0, "left baseline", "#fff") else ""
            val deltaText = if (delta != 0L) Fonts.torus.textPath(delta.toString(), midX, 132.877, 18.0, "center baseline", "#fff") else ""
            svg = Svg.setTexts(svg, listOf(redText, blueText, deltaText), MARK_TEXT)
        } else {
            val none = data.none.ifEmpty { listOf(MatchPlayerScore(score = 0)) }
            val first = none.maxByOrNull { it.score } ?: none.first()
            val noneText = if (data.totalScore != 0L) Fonts.torus.textPath(data.totalScore.toString(), 670.0, 196.836, 24.0, "center baseline", "#fff") else ""
            svg = Svg.setText(svg, "#382e32", MARK_BACK_COLOR)
            svg = Svg.setText(svg, noneText, MARK_TEXT)
            svg = Svg.setText(svg, first.avatar?.let { Svg.image(0.0, 0.0, 1380.0, 210.0, it, 0.2) }, MARK_H2H_AVATAR)
        }

        val redWidth = if (data.isTeamVs && data.red.isNotEmpty()) widths(data, "red", true) else if (!data.isTeamVs) widths(data, "none", true) else emptyList()
        val blueWidth = if (data.isTeamVs && data.blue.isNotEmpty()) widths(data, "blue", true) else emptyList()
        if (data.isTeamVs) {
            svg = implant(svg, data, "red", delta, data.red, redWidth, true, assets)
            svg = implant(svg, data, "blue", delta, data.blue, blueWidth, true, assets)
        } else {
            svg = implant(svg, data, "none", 0L, data.none, redWidth, true, assets)
        }
        return svg
    }

    private fun widths(data: CardFData, team: String, less4: Boolean): List<Double> {
        val list = when (team) {
            "red" -> data.red
            "blue" -> data.blue
            else -> data.none
        }
        if (list.isEmpty()) return emptyList()
        val teamScore = when {
            data.isTeamVs -> if (team == "red") data.redScore else data.blueScore
            else -> data.totalScore
        }
        val minTeam = if (data.isTeamVs) if (less4) 400.0 else 20.0 else 0.0
        val minPlayer = if (data.isTeamVs) if (less4) 100.0 else 20.0 else if (less4 && list.size > 2) 100.0 else 0.0
        val raw = (teamScore.toDouble() / data.totalScore.coerceAtLeast(1)) * 1330.0
        var widthCalc = Numbers.clampToInteger(raw, 1330.0 - minTeam, minTeam)
        var scoreCalc = teamScore
        val result = mutableListOf<Double>()
        list.asReversed().forEach { player ->
            var width = widthCalc * player.score / scoreCalc
            if (width < minPlayer) {
                result.add(0, minPlayer)
                widthCalc -= minPlayer
                scoreCalc -= player.score
            } else {
                result.add(0, width)
            }
        }
        return result
    }

    private fun implant(svg: String, data: CardFData, team: String, delta: Long, list: List<MatchPlayerScore>, widthArr: List<Double>, less4: Boolean, assets: ImageAssets): String {
        if (list.isEmpty() || widthArr.isEmpty()) return svg
        var out = svg
        val reverse = team != "red"
        val startX = if (team == "red") 20.0 else 1360.0
        val direction = if (team == "red") 1.0 else -1.0
        val colorList = if (team == "red") RED_COLORS else if (team == "blue") BLUE_COLORS else NONE_COLORS
        val isWin = if (team == "red") data.redWin else if (team == "blue") data.blueWin else true
        val scoreTextColor = if (team == "none") "#1C1719" else "#fff"
        val less = widthArr.count { it < 100.0 }
        var calculateX = startX
        var rectSum = 0.0
        if (less4 || less == 0) {
            for (i in widthArr.indices) {
                val j = list.size - 1 - i
                val width = widthArr[j]
                val deltaWidth = if (j == 0 && data.isTeamVs) Fonts.torus.width(delta.toString(), 18.0) else 0.0
                out = implantLabel(out, list[j], calculateX + width * direction / 2 - 50.0, 10.0, maxOf(100.0, width - deltaWidth), isWin, scoreTextColor, assets)
                rectSum += width
                out = Svg.setText(out, PanelDraw.rect(if (!reverse) startX else calculateX - width, 140.0, rectSum, 30.0, 15.0, colorList.getOrElse(j) { "#fff" }), MARK_SCORE_BAR)
                calculateX += width * direction
            }
        } else {
            out = implantLabel(out, list.first(), startX, 10.0, widthArr.first(), isWin, scoreTextColor, assets)
        }
        return out
    }

    private fun implantLabel(svg: String, player: MatchPlayerScore, x: Double, y: Double, maxWidth: Double, isWin: Boolean, scoreColor: String, assets: ImageAssets): String {
        val label = stackLabel(player, maxWidth, isWin, scoreColor, assets)
        return Svg.setText(svg, Svg.group(x, y, label), MARK_BODY)
    }

    private fun stackLabel(player: MatchPlayerScore, maxWidth: Double, isWin: Boolean, scoreColor: String, assets: ImageAssets): String {
        var svg = """
            <defs><clipPath id="clippath-LC1-1"><circle cx="50" cy="50" r="50" style="fill: none;"/></clipPath></defs>
            <g id="Avatar_LC1"><circle cx="50" cy="50" r="50" style="fill: #46393f;"/><g style="clip-path: url(#clippath-LC1-1);"></g></g>
            <g id="Mods_LC1"></g><g id="Label_LC1"></g><g id="Text_LC1"></g>
        """.trimIndent()
        player.mods.forEachIndexed { index, mod ->
            val offset = 100.0 - player.mods.size * 10.0
            svg = Svg.setText(svg, PanelDraw.circle(offset + index * 10.0, 90.0, 10.0, Colors.getModColor(mod)), "<g id=\"Mods_LC1\">")
        }
        val name = Fonts.torusBold.textPath(Fonts.torusBold.cutStringTail(player.name, 18.0, maxWidth), 50.0, 118.877, 18.0, "center baseline", "#fff")
        val score = Numbers.floorParts(player.score, -4)
        val scoreText = Fonts.torusBold.get2SizeTextPath(score.integer, score.decimal, 24.0, 18.0, 50.0, 152.836, "center baseline", scoreColor)
        val rank = Fonts.torusBold.textPath(player.rank.toString(), 15.0, 15.877, 18.0, "center baseline", "#fff")
        val label = PanelDraw.rect(0.0, 0.0, 30.0, 20.0, 10.0, MatchCardSupport.userRankColor(player.rank))
        svg = Svg.setTexts(svg, listOf(name, scoreText, rank), "<g id=\"Text_LC1\">")
        svg = Svg.setText(svg, label, "<g id=\"Label_LC1\">")
        val avatar = player.avatar?.let { assets.resolveRef(it) } ?: assets.local("avatar-guest.png")
        return Svg.setText(svg, avatar?.let { Svg.image(0.0, 0.0, 100.0, 100.0, it, if (isWin) 1.0 else 0.3) }, "<g style=\"clip-path: url(#clippath-LC1-1);\">")
    }

    private val RED_COLORS = listOf("#D56E74", "#CB3554", "#801D34", "#601025", "#61003F", "#811554", "#CA2C82", "#D46DA1")
    private val BLUE_COLORS = listOf("#55B1EF", "#009DEA", "#006899", "#004E74", "#002D59", "#0D3F75", "#1B62B2", "#587EC2")
    private val NONE_COLORS = listOf("#fff", "#aaa", "#ccc", "#888", "#eee", "#999", "#bbb", "#777")
}
