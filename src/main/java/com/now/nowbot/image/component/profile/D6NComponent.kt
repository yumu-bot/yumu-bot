package com.now.nowbot.image.component.profile

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Numbers

data class D6NData(
    val rankedScore: Long = 0,
    val totalScore: Long = 0,
    val playedMap: Long = 0,
    val replaysWatched: Long = 0,
    val follower: Long = 0,
    val medal: Long = 0,
    val deltaRankedScore: Long = 0,
    val deltaTotalScore: Long = 0,
    val bonusPp: Double = 0.0,
    val playCounts: IntArray = intArrayOf(),
    val lastYear: String = "19",
    val lastMonth: String = "04",
    val firstYear: String = "19",
    val firstMonth: String = "04",
    val midYear: String = "19",
    val midMonth: String = "04",
    val mode: String = "osu",
)

object D6NComponent {
    fun render(data: D6NData, assets: ImageAssets): String {
        var svg = "<g id=\"Base_CF6\"></g><g id=\"PC_CF6\"></g><g id=\"Label_CF6\"></g><g id=\"Text_CF6\"></g>"
        svg = Svg.setText(svg, playCountChart(data.playCounts), "<g id=\"PC_CF6\">")

        val rksA = Numbers.floor(data.rankedScore, 1)
        val rks = Numbers.floorParts(data.rankedScore, -4, 2)
        val ttsA = Numbers.floor(data.totalScore, 1)
        val tts = Numbers.floorParts(data.totalScore, -4, 2)
        val mpc = Numbers.floorParts(data.playedMap, -4)
        val rep = Numbers.floorParts(data.replaysWatched, -4)
        val fan = Numbers.floorParts(data.follower, -4)
        val mdl = Numbers.floorParts(data.medal, -4)

        val labels = listOf(
            ProfileLabels.d2(ProfileLabelData(assets.local("object-score-max.png"), "Ranked Score", rksA, rks.integer, rks.decimal, "进榜分")),
            ProfileLabels.d2(ProfileLabelData(assets.local("object-score-aimpp.png"), "Total Score", ttsA, tts.integer, tts.decimal, "总分")),
            ProfileLabels.e(ProfileLabelData(assets.local("object-score-approachrate.png"), "Played Map", "MPC", mpc.integer, mpc.decimal, "玩过")),
            ProfileLabels.e(ProfileLabelData(assets.local("object-score-circlesize.png"), "Replay", "REP", rep.integer, rep.decimal, "回放")),
            ProfileLabels.e(ProfileLabelData(assets.local("object-score-healthpoint.png"), "Follower", "FAN", fan.integer, fan.decimal, "粉丝")),
            ProfileLabels.e(ProfileLabelData(assets.local("object-score-rice.png"), "Medal", "MED", mdl.integer, mdl.decimal, "奖章")),
        )
        labels.forEachIndexed { index, label ->
            val x = if (index < 2) 20.0 + (index % 2) * 440.0 else 20.0 + ((index - 2) % 4) * 220.0
            val y = if (index < 2) 65.0 else 145.0
            svg = Svg.setText(svg, Svg.group(x, y, label), "<g id=\"Label_CF6\">")
        }

        val deltas = if (data.deltaRankedScore == 0L) listOf("", "") else listOf(
            deltaText(data.deltaRankedScore), deltaText(data.deltaTotalScore),
        )
        deltas.forEachIndexed { index, delta ->
            if (delta.isNotEmpty()) {
                svg = Svg.setText(
                    svg,
                    Svg.group(78.0 + (index % 2) * 440.0, 130.0, Fonts.torus.textPath(delta, 0.0, 0.0, 18.0, "left baseline", if (delta.startsWith("-")) "#ffcdd2" else "#c2e5c3")),
                    "<g id=\"Label_CF6\">",
                )
            }
        }

        val title = Fonts.torus.textPath("User Data", 15.0, 40.795, 30.0, "left baseline", "#fff")
        val mode = Fonts.torus.get2SizeTextPath(data.mode, " (bonus: ${Numbers.jsRound(data.bonusPp).toLong()}PP)", 30.0, 24.0, 880.0, 40.836, "right baseline", "#fff")
        val first = Fonts.torus.textPath("${data.firstYear}-${data.firstMonth}", 15.0, 334.836, 24.0, "left baseline", "#aaa")
        val mid = Fonts.torus.textPath("${data.midYear}-${data.midMonth}", 450.0, 334.836, 24.0, "center baseline", "#aaa")
        val last = Fonts.torus.textPath("${data.lastYear}-${data.lastMonth}", 885.0, 334.836, 24.0, "right baseline", "#aaa")
        svg = Svg.setTexts(svg, listOf(title, mode, first, mid, last), "<g id=\"Text_CF6\">")
        svg = Svg.setText(svg, PanelDraw.rect(0.0, 0.0, 900.0, 355.0, 20.0, "#382e32"), "<g id=\"Base_CF6\">")
        return svg
    }

    private fun playCountChart(playCounts: IntArray): String {
        val arr = padFront(playCounts.map { it.toDouble() }, 43)
        val maxValue = arr.maxOrNull() ?: 0.0
        val index = arr.indexOf(maxValue).coerceAtLeast(0)
        val text = Fonts.torus.textPath(
            if (maxValue > 0) "${Numbers.jsRound(maxValue).toLong()}PC" else "",
            30.0 + index * 20.0,
            305.0 - (maxValue / kotlin.math.max(1000.0, maxValue)) * 90.0,
            16.0, "center baseline", "#a1a1a1",
        )
        val chart = PanelDraw.barChart(arr, 0.0, 0.0, 22.0, 310.0, 860.0, 90.0, 8.0, 4.0, "#8dcff4", 1000.0, 16.0, "#aaa")
        return text + chart
    }

    private fun deltaText(value: Long): String {
        val sign = if (value > 0) "+" else if (value < 0) "-" else ""
        return sign + Numbers.floor(kotlin.math.abs(value), -4, 2)
    }

    private fun padFront(values: List<Double>, length: Int): List<Double> {
        if (values.size >= length) return values.takeLast(length)
        return List(length - values.size) { 0.0 } + values
    }
}
