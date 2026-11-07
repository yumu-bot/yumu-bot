package com.now.nowbot.image.component.profile

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.PanelColor
import com.now.nowbot.image.util.Numbers

data class D2MainData(
    val playCount: Long = 0,
    val playTime: Long = 0,
    val totalHits: Long = 0,
    val deltaPlayCount: Long = 0,
    val deltaPlayTime: Long = 0,
    val deltaTotalHits: Long = 0,
    val join: String = "",
)

data class D2RankData(val counts: List<Int> = emptyList(), val deltas: List<Long> = emptyList(), val sum: Int = 1)

data class D2RankHistoryData(val ranking: List<Long> = emptyList(), val highestRank: Long = 0, val highestDifference: String = "0d")

data class D2HistoryData(val values: List<Double> = emptyList(), val maxText: String = "", val axis: List<String> = listOf("", "", ""))

object D2Components {
    private val increase = "#c2e5c3"
    private val decrease = "#ffcdd2"

    fun main(data: D2MainData, assets: ImageAssets, hue: Int): String {
        var svg = "<g id=\"Component_D1\"></g>"
        val pc = Numbers.floorParts(data.playCount, -4)
        val tth = Numbers.floorParts(data.totalHits, -4)
        val pt = ProfileTime.parts(data.playTime.toDouble(), false)
        val showDelta = data.deltaPlayTime > 0
        val labels = listOf(
            ProfileLabels.d4(ProfileLabelData(assets.local("object-score-combo.png"), "Play Count", "PC", pc.integer, pc.decimal, "游玩次数", "#ccc", "#00B7EE"), if (showDelta) deltaText(data.deltaPlayCount) else "", deltaColor(data.deltaPlayCount)),
            ProfileLabels.d4(ProfileLabelData(assets.local("object-score-overalldifficulty.png"), "Total Hits", "TTH", tth.integer, tth.decimal, "击打次数", "#ccc", "#FFF467"), if (showDelta) deltaText(data.deltaTotalHits) else "", deltaColor(data.deltaTotalHits)),
            ProfileLabels.d4(ProfileLabelData(assets.local("object-score-length.png"), "Play Time", "PT", pt.large, pt.small, "游玩时间", "#ccc", "#7CC576"), if (showDelta) "+" + ProfileTime.full(data.deltaPlayTime.toDouble()) else "", deltaColor(data.deltaPlayTime)),
        )
        labels.forEachIndexed { index, label -> svg = Svg.setText(svg, Svg.group(20.0, 50.0 + 75.0 * index, label), "<g id=\"Component_D1\">") }
        if (data.join.isNotBlank()) {
            svg = Svg.setText(svg, Fonts.poppinsBold.textPath("Join: ${data.join}", 470.0, 27.0, 18.0, "right baseline", "#fff"), "<g id=\"Component_D1\">")
        }
        val title = Fonts.poppinsBold.textPath("Main Statistics", 15.0, 27.0, 18.0, "left baseline", "#fff")
        val rrect = PanelDraw.rect(0.0, 0.0, 490.0, 270.0, 20.0, PanelColor.middle(hue))
        svg = Svg.setTexts(svg, listOf(title, rrect), "<g id=\"Component_D1\">")
        return svg
    }

    fun bests(cards: List<String>, hue: Int): String {
        var svg = "<g id=\"Component_D2\"></g>"
        val body = cards.mapIndexed { index, card ->
            Svg.group(10.0 + (index % 3) * 160.0, 40.0 + (index / 3) * 130.0, card)
        }.joinToString("")
        val title = Fonts.poppinsBold.textPath("Bests", 15.0, 27.0, 18.0, "left baseline", "#fff")
        val rrect = PanelDraw.rect(0.0, 0.0, 490.0, 300.0, 20.0, PanelColor.middle(hue))
        svg = Svg.setTexts(svg, listOf(body, title, rrect), "<g id=\"Component_D2\">")
        return svg
    }

    fun ranks(data: D2RankData, assets: ImageAssets, hue: Int): String {
        var svg = "<g id=\"Component_D3\"></g>"
        val ranks = listOf("XH", "X", "SH", "S", "A")
        val colors = listOf("#FAFAFA", "#FFFF00", "#BDBDBD", "#FF9800", "#22AC38")
        var length = 0.0
        val bars = mutableListOf<String>()
        data.counts.take(5).forEachIndexed { index, count ->
            val delta = data.deltas.getOrElse(index) { 0L }
            val x = 15.0 + index * 90.0
            var text = deltaText(delta)
            if (ranks[index] == "S" || ranks[index] == "SH") text = " $text" else text = " $text"
            if (count < 1000) text = " $count" + " $text" else text = Numbers.floor(count, 2) + " $text"
            val label = ProfileLabels.d3(
                ProfileLabelData(assets.local("object-score-${ranks[index]}-small.png"), dataMain = text),
                "", deltaColor(delta),
            )
            svg = Svg.setText(svg, Svg.group(x, 25.0, label), "<g id=\"Component_D3\">")
            val barLength = count * 460.0 / data.sum
            if (barLength > 0) {
                length += maxOf(barLength, 15.0)
                bars += PanelDraw.rect(15.0, 75.0, minOf(length, 460.0), 15.0, 7.5, colors[index])
            }
        }
        val title = Fonts.poppinsBold.textPath("Ranks", 15.0, 22.0, 18.0, "left baseline", "#fff")
        val rrect = PanelDraw.rect(0.0, 0.0, 490.0, 100.0, 20.0, PanelColor.middle(hue))
        svg = Svg.setTexts(svg, bars + title + rrect, "<g id=\"Component_D3\">")
        return svg
    }

    fun mascot(mascot: String?, assets: ImageAssets, hue: Int, png: Boolean = true): String {
        var svg = "<defs><clipPath id=\"clippath-PD2-C4\"></clipPath></defs><g id=\"Component_D4_Base\"></g><g style=\"clip-path: url(#clippath-PD2-C4);\"></g>"
        val resolved = assets.resolveRef(mascot)
        if (png) {
            svg = Svg.setText(svg, PanelDraw.rect(0.0, 290.0, 1920.0, 790.0, 20.0, "none", 1.0), "<clipPath id=\"clippath-PD2-C4\">")
            svg = Svg.setText(svg, resolved?.let { Svg.image(0.0, 290.0, 1920.0, 790.0, it, 1.0, "xMidYMin meet") }, "<g style=\"clip-path: url(#clippath-PD2-C4);\">")
        } else {
            svg = Svg.setText(svg, PanelDraw.rect(550.0, 330.0, 820.0, 710.0, 20.0, "none", 1.0), "<clipPath id=\"clippath-PD2-C4\">")
            svg = Svg.setText(svg, resolved?.let { Svg.image(550.0, 330.0, 820.0, 710.0, it, 1.0, "xMidYMid slice") }, "<g style=\"clip-path: url(#clippath-PD2-C4);\">")
            svg = Svg.setText(svg, PanelDraw.rect(550.0, 330.0, 820.0, 710.0, 20.0, PanelColor.middle(hue), 1.0), "<g id=\"Component_D4_Base\">")
        }
        return svg
    }

    fun rankHistory(data: D2RankHistoryData, hue: Int): String {
        var svg = "<g id=\"Component_D5\"></g>"
        val arr = padFront(data.ranking.map { it.toDouble() }, 90)
        val max = arr.maxOrNull() ?: 0.0
        val nonZero = arr.map { if (it == 0.0) max else it }
        val min = nonZero.minOrNull() ?: 0.0
        val mid = (max + min) / 2
        val axis = Fonts.poppinsBold.textPath(Numbers.floor(min, 1, -1), 15.0, 48.0, 14.0, "left baseline", "#fc2") +
            Fonts.poppinsBold.textPath(Numbers.floor(mid, 1, -1), 15.0, 101.0, 14.0, "left baseline", "#fc2") +
            Fonts.poppinsBold.textPath(Numbers.floor(max, 1, -1), 15.0, 154.0, 14.0, "left baseline", "#fc2")
        val days = Fonts.poppinsBold.textPath("-90d", 35.0, 170.0, 14.0, "left baseline", "#fc2") +
            Fonts.poppinsBold.textPath("-45d", 257.5, 170.0, 14.0, "center baseline", "#fc2") +
            Fonts.poppinsBold.textPath("-0d", 480.0, 170.0, 14.0, "right baseline", "#fc2")
        val chart = PanelDraw.lineChart(nonZero, min, max, 50.0, 155.0, 415.0, 115.0, "#fc2", 1.0, 0.0, 4.0, true)
        val maxText = Fonts.poppinsBold.textPath("Max: #${if (data.highestRank > 0) data.highestRank else Numbers.floor(min, 1, -1)} [${data.highestDifference}]", 475.0, 27.0, 18.0, "right baseline", "#fff")
        val title = Fonts.poppinsBold.textPath("Rank History", 15.0, 27.0, 18.0, "left baseline", "#fff")
        val rrect = PanelDraw.rect(0.0, 0.0, 490.0, 180.0, 20.0, PanelColor.middle(hue))
        svg = Svg.setTexts(svg, listOf(maxText, chart, axis, days, title, rrect), "<g id=\"Component_D5\">")
        return svg
    }

    fun bpHistory(data: D2HistoryData, hue: Int): String {
        var svg = "<g id=\"Component_D6\"></g>"
        val arr = padFront(data.values, 90)
        val max = arr.maxOrNull() ?: 0.0
        val axis = Fonts.poppinsBold.textPath(Numbers.floor(max, 1, -1), 15.0, 45.0, 14.0, "left baseline", "#CCE199") +
            Fonts.poppinsBold.textPath("0", 15.0, 74.0, 14.0, "left baseline", "#CCE199")
        val days = Fonts.poppinsBold.textPath("-90d", 35.0, 90.0, 14.0, "left baseline", "#CCE199") +
            Fonts.poppinsBold.textPath("-45d", 257.5, 90.0, 14.0, "center baseline", "#CCE199") +
            Fonts.poppinsBold.textPath("-0d", 480.0, 90.0, 14.0, "right baseline", "#CCE199")
        val chart = PanelDraw.lineChart(arr, max, 0.0, 50.0, 75.0, 415.0, 40.0, "#CCE199", 1.0, 0.0, 4.0, false)
        val maxText = Fonts.poppinsBold.textPath("Max: +${Numbers.jsRound(max).toLong()} [${data.maxText}]", 475.0, 22.0, 18.0, "right baseline", "#fff")
        val title = Fonts.poppinsBold.textPath("Bests History", 15.0, 22.0, 18.0, "left baseline", "#fff")
        val rrect = PanelDraw.rect(0.0, 0.0, 490.0, 100.0, 20.0, PanelColor.middle(hue))
        svg = Svg.setTexts(svg, listOf(maxText, chart, axis, days, title, rrect), "<g id=\"Component_D6\">")
        return svg
    }

    fun pcHistory(data: D2HistoryData, hue: Int): String {
        var svg = "<g id=\"Component_D7\"></g>"
        val arr = data.values
        val max = arr.maxOrNull() ?: 0.0
        val axis = Fonts.poppinsBold.textPath(Numbers.floor(max, 1, -1), 15.0, 45.0, 14.0, "left baseline", "#8DCFF4") +
            Fonts.poppinsBold.textPath("0", 15.0, 74.0, 14.0, "left baseline", "#8DCFF4")
        val months = Fonts.poppinsBold.textPath(data.axis.getOrElse(0) { "" }, 35.0, 90.0, 14.0, "left baseline", "#8DCFF4") +
            Fonts.poppinsBold.textPath(data.axis.getOrElse(1) { "" }, 257.5, 90.0, 14.0, "center baseline", "#8DCFF4") +
            Fonts.poppinsBold.textPath(data.axis.getOrElse(2) { "" }, 480.0, 90.0, 14.0, "right baseline", "#8DCFF4")
        val chart = PanelDraw.lineChart(arr, max, 0.0, 50.0, 75.0, 415.0, 40.0, "#8DCFF4", 1.0, 0.0, 4.0, false)
        val maxText = Fonts.poppinsBold.textPath("Max: ${Numbers.jsRound(max).toLong()} [${data.maxText}]", 475.0, 22.0, 18.0, "right baseline", "#fff")
        val title = Fonts.poppinsBold.textPath("Play History", 15.0, 22.0, 18.0, "left baseline", "#fff")
        val rrect = PanelDraw.rect(0.0, 0.0, 490.0, 100.0, 20.0, PanelColor.middle(hue))
        svg = Svg.setTexts(svg, listOf(maxText, chart, axis, months, title, rrect), "<g id=\"Component_D7\">")
        return svg
    }

    fun secondary(
        rankedScore: Long,
        totalScore: Long,
        medal: Long,
        playedMap: Long,
        maxCombo: Long,
        follower: Long,
        deltaRanked: Long,
        deltaTotal: Long,
        deltaCombo: Long,
        assets: ImageAssets,
        hue: Int,
    ): String {
        var svg = "<g id=\"Component_D8\"></g>"
        val rks = Numbers.floorParts(rankedScore, 1)
        val tts = Numbers.floorParts(totalScore, 1)
        val mpc = Numbers.floorParts(playedMap, -4)
        val med = Numbers.floorParts(medal, -4)
        val mxc = Numbers.floorParts(maxCombo, -4)
        val fan = Numbers.floorParts(follower, -4)
        val labels = listOf(
            ProfileLabels.d5(ProfileLabelData(assets.local("object-score-max.png"), "Ranked Score", "RKS", rks.integer, rks.decimal, "进榜分", "#aaa", "#A864A8"), if (deltaRanked > 0) deltaText(deltaRanked) else "", deltaColor(deltaRanked)),
            ProfileLabels.d5(ProfileLabelData(assets.local("object-score-aimpp.png"), "Total Score", "TTS", tts.integer, tts.decimal, "总分", "#aaa", "#F06EA9"), if (deltaTotal > 0) deltaText(deltaTotal) else "", deltaColor(deltaTotal)),
            ProfileLabels.d5(ProfileLabelData(assets.local("object-score-approachrate.png"), "Played Map", "MPC", mpc.integer, mpc.decimal, "玩过", "#aaa", "#5eb0ab")),
            ProfileLabels.d5(ProfileLabelData(assets.local("object-score-rice.png"), "Medal", "MED", med.integer, med.decimal, "奖章", "#aaa", "#FFF467")),
            ProfileLabels.d5(ProfileLabelData(assets.local("object-score-combo.png"), "Max Combo", "MXC", mxc.integer, mxc.decimal, "最大连击", "#aaa", null), deltaText(deltaCombo), deltaColor(deltaCombo)),
            ProfileLabels.d5(ProfileLabelData(assets.local("object-score-healthpoint.png"), "Follower", "FAN", fan.integer, fan.decimal, "粉丝", "#aaa", "#de96bb")),
        )
        val positions = listOf(20.0 to 50.0, 250.0 to 50.0, 20.0 to 125.0, 250.0 to 125.0, 20.0 to 200.0, 250.0 to 200.0)
        labels.forEachIndexed { index, label -> svg = Svg.setText(svg, Svg.group(positions[index].first, positions[index].second, label), "<g id=\"Component_D8\">") }
        val title = Fonts.poppinsBold.textPath("Secondary Statistics", 15.0, 27.0, 18.0, "left baseline", "#fff")
        val rrect = PanelDraw.rect(0.0, 0.0, 490.0, 270.0, 20.0, PanelColor.middle(hue))
        svg = Svg.setTexts(svg, listOf(title, rrect), "<g id=\"Component_D8\">")
        return svg
    }

    private fun deltaText(value: Long): String {
        if (value == 0L) return ""
        return (if (value > 0) "+" else "-") + kotlin.math.abs(value)
    }

    private fun deltaColor(value: Long): String = when {
        value > 0 -> increase
        value < 0 -> decrease
        else -> "none"
    }

    private fun padFront(values: List<Double>, length: Int): List<Double> {
        if (values.size >= length) return values.takeLast(length)
        return List(length - values.size) { 0.0 } + values
    }
}
