package com.now.nowbot.image.component.profile

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Numbers
import kotlin.math.max
import kotlin.math.min

data class D5Data(
    val mode: String = "osu",
    val country: String = "CN",
    val countryRank: Long = 0,
    val globalRank: Long = 0,
    val highestRank: Long = 0,
    val highestDate: String = "?",
    val ranking: List<Long> = emptyList(),
    val bpTimes: IntArray = intArrayOf(),
)

object D5Component {
    fun render(data: D5Data, assets: ImageAssets): String {
        var svg = "<g id=\"Base_CF5\"></g><g id=\"BP_CF5\"></g><g id=\"Ranking_CF5\"></g><g id=\"Text_CF5\"></g>"
        svg = Svg.setText(svg, bpChart(data.bpTimes), "<g id=\"BP_CF5\">")
        svg = Svg.setText(svg, rankingChart(data.ranking), "<g id=\"Ranking_CF5\">")
        val title = Fonts.torus.textPath("Ranking", 15.0, 35.795, 30.0, "left baseline", "#fff")
        val rankB = "#" + if (data.globalRank > 0) data.globalRank else if (data.highestRank > 0) "${data.highestRank}^" else "0"
        val rankM = if (data.globalRank > 0) {
            " ${data.country} #${data.countryRank}"
        } else {
            " (${data.highestDate}) ${data.country}"
        }
        val rankingText = Fonts.torus.get2SizeTextPath(rankB, rankM, 36.0, 24.0, 880.0, 44.754, "right baseline", "#fff")
        svg = Svg.setTexts(svg, listOf(title, rankingText), "<g id=\"Text_CF5\">")
        svg = Svg.setText(svg, PanelDraw.rect(0.0, 0.0, 900.0, 335.0, 20.0, "#382e32"), "<g id=\"Base_CF5\">")
        return svg
    }

    private fun bpChart(bpTimes: IntArray): String {
        val arr = padFront(bpTimes.map { it.toDouble() }, 39)
        val maxValue = arr.maxOrNull() ?: 0.0
        val index = arr.indexOf(maxValue).coerceAtLeast(0)
        val text = Fonts.torus.textPath(
            if (maxValue > 0) "BP+${Numbers.jsRound(maxValue).toLong()}" else "",
            70.0 + index * 20.0,
            195.0 + 90.0 - (maxValue / max(5.0, maxValue)) * 90.0,
            16.0, "center baseline", "#aaa",
        )
        val chart = PanelDraw.barChart(arr, 0.0, 0.0, 62.0, 290.0, 780.0, 90.0, 8.0, 4.0, "#8DCFF4", 5.0, 16.0)
        val days = Fonts.torus.textPath("-90d", 60.0, 316.836, 24.0, "left baseline", "#fc2") +
            Fonts.torus.textPath("-45d", 450.0, 316.836, 24.0, "center baseline", "#fc2") +
            Fonts.torus.textPath("-0d", 840.0, 316.836, 24.0, "right baseline", "#fc2")
        return text + chart + days
    }

    private fun rankingChart(ranking: List<Long>): String {
        val arr = padFront(ranking.map { it.toDouble() }, 90)
        val maxValue = arr.maxOrNull() ?: 0.0
        val nonZero = arr.map { if (it == 0.0) maxValue else it }
        val minValue = nonZero.minOrNull() ?: 0.0
        val mid = (maxValue + minValue) / 2
        val axis = Fonts.torus.textPath(Numbers.floor(minValue, 1, -1), 30.0, 72.836, 24.0, "center baseline", "#fc2") +
            Fonts.torus.textPath(Numbers.floor(mid, 1, -1), 30.0, 179.836, 24.0, "center baseline", "#fc2") +
            Fonts.torus.textPath(Numbers.floor(maxValue, 1, -1), 30.0, 286.836, 24.0, "center baseline", "#fc2")
        val chart = PanelDraw.lineChart(nonZero, minValue, maxValue, 60.0, 280.0, 780.0, 215.0, "#fc2", 1.0, 0.0, 4.0, true)
        return axis + chart
    }

    private fun padFront(values: List<Double>, length: Int): List<Double> {
        if (values.size >= length) return values.takeLast(length)
        return List(length - values.size) { 0.0 } + values
    }
}
