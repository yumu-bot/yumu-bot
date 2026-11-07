package com.now.nowbot.image.component.profile

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.font.TextPart
import com.now.nowbot.image.font.Typography
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.ColorArray
import com.now.nowbot.image.util.GameModes
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.PanelColor
import kotlin.math.max
import kotlin.math.min

data class D3RankInterval(val end: Int, val improvement: Double)

data class D3RankChartData(
    val ranking: List<Long> = emptyList(),
    val top: Long = 0,
    val bottom: Long = 0,
    val improvement: Double = 0.0,
    val intervals: List<D3RankInterval> = emptyList(),
)

object D3Components {
    fun rankChart(data: D3RankChartData, hue: Int): String {
        var svg = "<g></g>"
        val top = if (data.top != 0L) data.top.toDouble() else (data.ranking.minOrNull() ?: 0).toDouble()
        val bottom = if (data.bottom != 0L) data.bottom.toDouble() else (data.ranking.maxOrNull() ?: 0).toDouble()
        val arr = data.ranking.map { it.toDouble() }
        val axis = Fonts.poppinsBold.textPath(Numbers.floor(top, 1, -1), 25.0, 48.0, 14.0, "center baseline", "#fc2") +
            Fonts.poppinsBold.textPath(Numbers.floor((top + bottom) / 2, 1, -1), 25.0, 88.0, 14.0, "center baseline", "#fc2") +
            Fonts.poppinsBold.textPath(Numbers.floor(bottom, 1, -1), 25.0, 128.0, 14.0, "center baseline", "#fc2")
        val chart = PanelDraw.lineChart(arr, top, bottom, 45.0, 125.0, 1330.0, 90.0, "#ffcc22", 1.0, 0.2, 4.0, true)
        val xAxis = (0..9).joinToString("") { index ->
            val suffix = if (index == 0) "day" else ""
            val anchor = if (index == 0) "right baseline" else if (index == 9) "left baseline" else "center baseline"
            val x = 1375.0 - 1330.0 * index / 9.0
            Fonts.poppinsBold.textPath("-${index * 10}$suffix", x, 144.0, 16.0, anchor, "#aaa")
        }
        val improvement = Fonts.poppinsBold.textPath("Improvement In 90 days: +${Numbers.floor(data.improvement, 1, -1)}", 1375.0, 25.0, 18.0, "right baseline", "#fff")
        val intervalTexts = data.intervals.mapNotNull { interval ->
            val end = interval.end.coerceIn(0, 90)
            val now = arr.getOrNull(end) ?: return@mapNotNull null
            val percent = if (bottom != top) (now - top) / (bottom - top) else 0.0
            var delta = -5.0
            if ((end <= 14 || end >= 64) && percent <= 0.2) delta = 25.0
            val x = 45.0 + 1330.0 * end / 90.0
            val y = 30.0 + percent * 90.0 + delta
            Fonts.poppinsBold.textPath("+${Numbers.floor(interval.improvement, 1, -1)}", x, y, 16.0, "center baseline", "#fff")
        }.joinToString("")
        val title = Fonts.poppinsBold.textPath("Ranking", 45.0, 25.0, 18.0, "left baseline", "#fff")
        val rrect = PanelDraw.rect(0.0, 0.0, 1390.0, 150.0, 20.0, PanelColor.middle(hue))
        svg = Svg.setTexts(svg, listOf(improvement, intervalTexts, chart, xAxis, axis, title, rrect), "<g>")
        return svg
    }

    fun level(current: Int, progress: Int, hue: Int, assets: ImageAssets): String {
        val percent = progress / 100.0
        val bar = PanelDraw.linearGradientRect(95.0, 42.0, 385.0 * percent, 15.0, 7.5, ColorArray.cyan)
        val base = PanelDraw.rect(95.0, 42.0, 385.0, 15.0, 7.5, PanelColor.top(hue))
        val levelText = Fonts.poppinsBold.textPath(current.toString(), 47.5, 60.0, 28.0, "center baseline", "#fff")
        val currentText = Fonts.poppinsBold.textPath("$progress%", 95.0, 82.0, 18.0, "left baseline", "#fff")
        val hexagon = assets.local("object-hexagon-level.png")?.let { Svg.image(15.0, 15.0, 65.0, 70.0, it) }.orEmpty()
        val title = Fonts.poppinsBold.textPath("Level", 440.0, 25.0, 18.0, "right baseline", "#fff")
        val icon = assets.local("Icons/rulesets.png")?.let { Svg.image(450.0, 4.0, 30.0, 30.0, it) }.orEmpty()
        val rrect = PanelDraw.rect(0.0, 0.0, 490.0, 100.0, 20.0, PanelColor.middle(hue))
        var svg = "<g></g>"
        return Svg.setTexts(svg, listOf(hexagon, levelText, currentText, bar, base, title, icon, rrect), "<g>")
    }

    fun bests(cards: List<String>, bestPp: Double, bonusPp: Double, hue: Int): String {
        val body = cards.mapIndexed { index, card -> Svg.group(10.0 + (index % 3) * 160.0, 35.0 + (index / 3) * 130.0, card) }.joinToString("")
        val ppText = Fonts.poppinsBold.textPath("${Numbers.jsRound(bestPp).toLong()} PP [${Numbers.round(bonusPp, 3)} Bonus PP]", 480.0, 25.0, 18.0, "right baseline", "#aaa")
        val title = Fonts.poppinsBold.textPath("Bests", 45.0, 25.0, 18.0, "left baseline", "#fff")
        val rrect = PanelDraw.rect(0.0, 0.0, 490.0, 295.0, 20.0, PanelColor.middle(hue))
        var svg = "<g></g>"
        return Svg.setTexts(svg, listOf(ppText, body, title, rrect), "<g>")
    }

    fun rankCounts(counts: List<Int>, deltas: List<Long>, sum: Int, topPercent: Double, hue: Int, assets: ImageAssets): String {
        val ranks = listOf("XH", "X", "SH", "S", "A")
        val colors = listOf(ColorArray.white, ColorArray.lightYellow, ColorArray.gray, ColorArray.amber, ColorArray.green)
        var svg = "<g></g>"
        var length = 0.0
        val bars = mutableListOf<String>()
        counts.take(5).forEachIndexed { index, count ->
            val delta = deltas.getOrElse(index) { 0L }
            val text = if (count < 1000) " $count" else if (count >= 100000) Numbers.floor(count, 2) else count.toString()
            val label = ProfileLabels.d3(
                ProfileLabelData(assets.local("object-score-${ranks[index]}-small.png"), dataMain = text),
                deltaText(delta), deltaColor(delta),
            )
            svg = Svg.setText(svg, Svg.group(15.0 + index * 90.0, 25.0, label), "<g>")
            val value = count * 460.0 / sum
            if (value > 0) {
                length += max(value, 15.0)
                bars += PanelDraw.linearGradientRect(15.0, 75.0, min(length, 460.0), 15.0, 7.5, colors[index])
            }
        }
        val base = PanelDraw.rect(15.0, 75.0, 460.0, 15.0, 7.5, PanelColor.top(hue))
        val top = Fonts.poppinsBold.textPath("Top ${Numbers.round(100 - topPercent * 100, 2)}%", 480.0, 22.0, 18.0, "right baseline", "#aaa")
        val title = Fonts.poppinsBold.textPath("Rank Count", 45.0, 25.0, 18.0, "left baseline", "#fff")
        val rrect = PanelDraw.rect(0.0, 0.0, 490.0, 105.0, 20.0, PanelColor.middle(hue))
        return Svg.setTexts(svg, bars + listOf(top, base, title, rrect), "<g>")
    }

    fun matrix(
        x: Double, y: Double, width: Double, height: Double,
        cellWidth: Double, cellHeight: Double, rows: Int, columns: Int,
        values: List<Int>, hue: Double, saturation: Double, radius: Double, gap: Double,
    ): String {
        val max = values.maxOrNull()?.toDouble()?.takeIf { it > 0 } ?: 1.0
        val builder = StringBuilder()
        values.take(rows * columns).forEachIndexed { index, value ->
            val row = index % rows
            val column = index / rows
            val lightness = 0.2 + 0.6 * (value / max)
            val color = PanelColor.hsl2hex(hue, saturation, lightness)
            val cx = x + column * (cellWidth + gap)
            val cy = y + row * (cellHeight + gap)
            builder.append(PanelDraw.rect(cx, cy, cellWidth, cellHeight, radius, color))
        }
        return builder.toString()
    }

    fun bestMatrix(values: List<Int>, week0: String, week4: String, week8: String, maxValue: Int, time: String, hue: Int): String {
        val matrix = matrix(16.0, 33.0, 178.0, 94.0, 10.0, 10.0, 7, 13, values, 207.0, 0.77, 5.0, 1.0)
        val xAxis = Fonts.poppinsBold.textPath(week0, 194.0, 144.0, 16.0, "right baseline", "#aaa") +
            Fonts.poppinsBold.textPath(week4, 138.0, 144.0, 16.0, "right baseline", "#aaa") +
            Fonts.poppinsBold.textPath(week8, 82.0, 144.0, 16.0, "right baseline", "#aaa")
        val max = Fonts.poppinsBold.textPath("MAX: +$maxValue", 194.0, 15.0, 12.0, "right baseline", "#aaa") +
            Fonts.poppinsBold.textPath(time, 194.0, 28.0, 12.0, "right baseline", "#aaa")
        val title = Fonts.poppinsBold.textPath("BP", 15.0, 25.0, 18.0, "left baseline", "#fff")
        val rrect = PanelDraw.rect(0.0, 0.0, 210.0, 150.0, 20.0, PanelColor.middle(hue))
        return Svg.setTexts("<g></g>", listOf(xAxis, max, matrix, title, rrect), "<g>")
    }

    fun pcMatrix(values: List<Int>, years: List<String>, maxValue: Int, time: String, hue: Int): String {
        val matrix = matrix(16.0, 33.0, 178.0, 94.0, 10.0, 28.0, 3, 13, values, 90.0, 0.4, 5.0, 1.0)
        val xAxis = years.mapIndexed { index, year ->
            Fonts.poppinsBold.textPath(year, 10.0 + index * 56.0, 144.0, 16.0, "left baseline", "#aaa")
        }.joinToString("")
        val max = Fonts.poppinsBold.textPath("MAX: $maxValue", 194.0, 15.0, 12.0, "right baseline", "#aaa") +
            Fonts.poppinsBold.textPath(time, 194.0, 28.0, 12.0, "right baseline", "#aaa")
        val title = Fonts.poppinsBold.textPath("PC", 15.0, 25.0, 18.0, "left baseline", "#fff")
        val rrect = PanelDraw.rect(0.0, 0.0, 210.0, 150.0, 20.0, PanelColor.middle(hue))
        return Svg.setTexts("<g></g>", listOf(xAxis, max, matrix, title, rrect), "<g>")
    }

    fun profileCard(user: com.now.nowbot.model.osu.OsuUser, history: com.now.nowbot.model.osu.OsuUser?, percentiles: Map<String, Double>, hue: Int, assets: ImageAssets): String {
        val stats = user.statistics
        val old = history?.statistics
        val mode = GameModes.getGameMode(user.mode.shortName, -1)
        val modeIcon = Fonts.extra.textPath(mode, 140.0, 138.0, 40.0, "left baseline", "#fff")
        val highest = user.highestRank
        val highestValue = if (highest != null && highest.rank > 0) highest.rank.toString() else "0"
        val highestTime = if (highest != null && highest.rank > 0) {
            com.now.nowbot.image.util.TimeFormat.difference(highest.updatedAt, 2)
        } else "?"
        val highestRank = Typography.multipleTextPath(
            listOf(
                TextPart("#", Fonts.poppinsBold, 30.0, "#fff"),
                TextPart(highestValue, Fonts.poppinsBold, 40.0, "#fff"),
                TextPart("  [$highestTime]", Fonts.poppinsBold, 24.0, "#ccc"),
            ),
            195.0, 136.0, "left baseline",
        )
        val highestIcon = assets.local("Icons/ranking.png")?.let { Svg.image(25.0, 55.0, 100.0, 100.0, it) }.orEmpty()
        val highestTitle = Fonts.poppinsBold.textPath("Highest Rank", 140.0, 84.0, 24.0, "left baseline", "#ccc")
        val mascot = MascotAssets.banner(user.mode.shortName, 140.0, assets)
        val mascotName = Fonts.poppinsBold.textPath(mascot.name, 1315.0, 62.0, 18.0, "right baseline", "#fff")
        val mascotImage = if (mascot.image != null) {
            """
            <defs>
                <linearGradient id="linear-O3-1" x1="0%" y1="0%" x2="100%" y2="0%">
                    <stop offset="0%" style="stop-color:rgb(255,255,255);stop-opacity:0" />
                    <stop offset="60%" style="stop-color:rgb(255,255,255);stop-opacity:1" />
                </linearGradient>
                <mask id="mask-O3-1"><rect x="790" y="40" width="540" height="140" fill="url(#linear-O3-1)"/></mask>
            </defs>
            <g id="Background_Right" mask="url(#mask-O3-1)">
            """.trimIndent() + Svg.image(790.0, 40.0 + mascot.offsetY, 540.0, 720.0, mascot.image, 1.0) + "</g>"
        } else ""
        val elo = if (user.matchmakingRating > 0) {
            Fonts.poppinsBold.textPath("Matchmaking Rating: ${user.matchmakingRating}", 1315.0, 25.0, 18.0, "right baseline", "#fff")
        } else ""
        val title = Fonts.poppinsBold.textPath("Statistics Overview", 45.0, 25.0, 18.0, "left baseline", "#fff")
        val image = assets.local("Icons/gameplay-b.png")?.let { Svg.image(10.0, 4.0, 30.0, 30.0, it) }.orEmpty()
        val overlay = PanelColor.overlay(hue)
        val topColor = PanelColor.top(hue)
        val middle = PanelColor.middle(hue)
        val pc = Numbers.floorParts(stats?.playCount ?: 0, -4)
        val tth = Numbers.floorParts(stats?.totalHits ?: 0, -4)
        val rks = Numbers.floorParts(stats?.rankedScore ?: 0, -4)
        val tts = Numbers.floorParts(stats?.totalScore ?: 0, -4)
        val bpc = Numbers.floorParts(user.beatmapPlaycount.toLong(), -4)
        val rwc = Numbers.floorParts((stats?.replaysWatchedByOthers ?: 0).toLong(), -4)
        val mxc = Numbers.floorParts((stats?.maxCombo ?: 0).toLong(), -4)
        val statsData = listOf(
            D3ProfileStat(assets.local("Icons/play.png"), "Play Count", percentiles["play_count"] ?: 0.0, pc.integer, pc.decimal, stats?.playCount ?: 0, 1000.0, old?.playCount ?: 0, ColorArray.cyan, overlay, topColor),
            D3ProfileStat(assets.local("Icons/gameplay-c.png"), "Total Hits", percentiles["total_hits"] ?: 0.0, tth.integer, tth.decimal, stats?.totalHits ?: 0, 1000.0, old?.totalHits ?: 0, ColorArray.lightYellow, overlay, topColor),
            D3ProfileStat(assets.local("Icons/calendar.png"), "Play Time", percentiles["play_time"] ?: 0.0, ProfileTime.full((stats?.playTime ?: 0).toDouble(), true), "", stats?.playTime ?: 0, 3600.0, old?.playTime ?: 0, ColorArray.lightGreen, overlay, topColor),
            D3ProfileStat(assets.local("Icons/online.png"), "Ranked Score", percentiles["ranked_score"] ?: 0.0, rks.integer, rks.decimal, stats?.rankedScore ?: 0, 1_000_000.0, old?.rankedScore ?: 0, ColorArray.purple, topColor, middle),
            D3ProfileStat(assets.local("Icons/global.png"), "Total Score", percentiles["total_score"] ?: 0.0, tts.integer, tts.decimal, stats?.totalScore ?: 0, 1_000_000.0, old?.totalScore ?: 0, ColorArray.pink, topColor, middle),
            D3ProfileStat(assets.local("Icons/crown.png"), "Medals", percentiles["achievements_count"] ?: 0.0, user.userAchievementsCount.toString(), "", user.userAchievementsCount.toLong(), 1.0, history?.userAchievementsCount?.toLong() ?: 0, ColorArray.gray, topColor, middle),
            D3ProfileStat(assets.local("Icons/input.png"), "Beatmap Play Count", percentiles["beatmap_playcount"] ?: 0.0, bpc.integer, bpc.decimal, user.beatmapPlaycount.toLong(), 1.0, history?.beatmapPlaycount?.toLong() ?: 0, ColorArray.green, topColor, middle),
            D3ProfileStat(assets.local("Icons/graphics.png"), "Replays Watched", percentiles["replays_watched"] ?: 0.0, rwc.integer, rwc.decimal, (stats?.replaysWatchedByOthers ?: 0).toLong(), 1.0, (old?.replaysWatchedByOthers ?: 0).toLong(), ColorArray.magenta, topColor, middle),
            D3ProfileStat(assets.local("Icons/cross-circle.png"), "Maximum Combo", percentiles["maximum_combo"] ?: 0.0, mxc.integer, mxc.decimal, (stats?.maxCombo ?: 0).toLong(), 1.0, (old?.maxCombo ?: 0).toLong(), ColorArray.blue, topColor, middle),
        )
        val labels = statsData.map { labelD6(it) }
        var labelStrings = ""
        for (row in 0 until 3) {
            for (column in 0 until 3) {
                labelStrings += Svg.group(10.0 + column * 440.0, 168.0 + row * 124.0, labels[row * 3 + column])
            }
        }
        val rrect = PanelDraw.rect(0.0, 0.0, 1330.0, 540.0, 20.0, middle)
        val mascotRect = PanelDraw.rect(0.0, 40.0, 1330.0, 140.0, 0.0, topColor)
        return Svg.setTexts(
            "<g></g>",
            listOf(labelStrings, modeIcon, highestRank, highestIcon, highestTitle, mascotName, mascotImage, title, elo, image, mascotRect, rrect),
            "<g>",
        )
    }

    private data class D3ProfileStat(
        val icon: String?,
        val title: String,
        val percent: Double,
        val textMain: String,
        val textMid: String,
        val value: Long,
        val divisor: Double,
        val history: Long,
        val colors: List<String>,
        val background: String,
        val iconBackground: String,
    )

    private fun labelD6(data: D3ProfileStat): String {
        val text = Fonts.poppinsBold.get2SizeTextPath(data.textMain, data.textMid, 36.0, 28.0, 110.0, 72.0, "left baseline", "#fff")
        val percentRect = if (data.percent >= 0) {
            PanelDraw.linearGradientRect(400.0, 18.0 + (1 - data.percent) * 78.0, 15.0, if (data.percent > 0) maxOf(data.percent * 78.0, 15.0) else 0.0, 7.5, data.colors, listOf(0, 0), listOf(80, 20), 1.0)
        } else {
            PanelDraw.linearGradientRect(400.0, 18.0, 15.0, 78.0, 7.5, data.colors, listOf(0, 0), listOf(20, 80), 1.0)
        }
        val top = if (data.percent >= 0) {
            Fonts.poppinsBold.textPath("Top ${Numbers.round(100 - data.percent * 100, 2)}%", 395.0, 96.0, 14.0, "right baseline", "#aaa")
        } else ""
        val roman = Svg.group(110.0, 84.0, romanSigns(data.value, data.divisor))
        var delta = data.value - data.history
        if (data.history == 0L) delta = 0
        val deltaPath = Fonts.poppinsBold.textPath(profileDeltaText(delta), 395.0, 32.0, 16.0, "right baseline", profileDeltaColor(delta))
        val icon = data.icon?.let { Svg.image(20.0, 22.0, 70.0, 70.0, it) }.orEmpty()
        val title = Fonts.poppinsBold.textPath(data.title, 110.0, 32.0, 18.0, "left baseline", "#ccc")
        val rrect = PanelDraw.rect(0.0, 0.0, 430.0, 114.0, 20.0, data.background)
        val iconRrect = PanelDraw.rect(15.0, 17.0, 80.0, 80.0, 20.0, data.iconBackground)
        val percentBase = PanelDraw.rect(400.0, 18.0, 15.0, 78.0, 7.5, data.iconBackground)
        return Svg.setTexts("<g></g>", listOf(title, text, top, deltaPath, percentRect, roman, icon, iconRrect, percentBase, rrect), "<g>")
    }

    private fun romanSigns(value: Long, divisor: Double): String {
        val number = Numbers.jsRound(value / divisor).toLong()
        val roman = roman(number)
        val builder = StringBuilder()
        for (index in roman.indices) {
            val x = index * 14.0
            builder.append(
                when (roman[index]) {
                    'I' -> PanelDraw.polygon(listOf(x + 8, 0.0, x + 12, 0.0, x + 4, 10.0, x, 10.0), "#aaa")
                    'V' -> PanelDraw.rect(x, 0.0, 10.0, 10.0, 0.0, "#aaa")
                    'X' -> PanelDraw.polygon(listOf(x + 6, -1.0, x, 5.0, x + 6, 11.0, x + 12, 5.0), "#aaa")
                    'L' -> PanelDraw.polygon(listOf(x - 1, 10.0, x + 11, 10.0, x + 5, -0.392), "#aaa")
                    'C' -> PanelDraw.polygon(listOf(x + 6, 6 - 0.854 - 6.308, x + 11.706, 6 - 0.854 - 1.854, x + 9.526, 10.0, x + 2.474, 10.0, x + 0.294, 6 - 0.854 - 1.854), "#aaa")
                    'D' -> PanelDraw.polygon(listOf(x, 5.196 - 0.3923, x + 3, 10.0, x + 9, 10.0, x + 12, 5.196 - 0.3923, x + 9, -0.3923, x + 3, -0.3923), "#aaa")
                    'M' -> PanelDraw.circle(x + 5, 5.0, 5.0, "#aaa")
                    else -> ""
                },
            )
        }
        return builder.toString()
    }

    private fun roman(number: Long): String {
        var value = number
        if (value <= 0) return ""
        if (value >= 4000) value %= 4000
        val numerals = listOf(1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC", 50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")
        val builder = StringBuilder()
        for ((unit, numeral) in numerals) {
            while (value >= unit) {
                builder.append(numeral)
                value -= unit
            }
        }
        return builder.toString()
    }

    private fun profileDeltaText(value: Long): String {
        val sign = if (value > 0) "+" else if (value < 0) "-" else ""
        val abs = kotlin.math.abs(value)
        return if (abs >= 100000) sign + Numbers.floor(abs, 2) else sign + abs
    }

    private fun profileDeltaColor(value: Long): String = when {
        value > 0 -> "#c2e5c3"
        value < 0 -> "#ffcdd2"
        else -> "none"
    }

    private fun deltaText(value: Long): String = if (value == 0L) "" else (if (value > 0) "+" else "-") + kotlin.math.abs(value)
    private fun deltaColor(value: Long): String = when { value > 0 -> "#c2e5c3"; value < 0 -> "#ffcdd2"; else -> "none" }
}
