package com.now.nowbot.image.component.j

import com.now.nowbot.image.component.profile.CardD2
import com.now.nowbot.image.component.profile.CardD2Data
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.PanelColor
import com.now.nowbot.image.util.Texts
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.service.messageServiceImpl.BPAnalysisService
import kotlin.math.max
import kotlin.math.min

object PanelJSupport {
    fun scoreRowCard(score: LazerScore, assets: ImageAssets): String {
        val clip = Texts.random(8)
        val cover = assets.mapBackground(score, "list")
        val background = assets.mapBackground(score, "list@2x")
        val star = Numbers.floor(score.beatmap.starRating, 1)
        val accuracy = Numbers.floor(score.accuracy * 100, 2)
        val mods = score.mods.joinToString("") { it.acronym }.ifBlank { "NM" }
        val rankColor = Colors.getRankColor(score.rank)
        val starColor = Colors.getStarRatingColor(score.beatmap.starRating)
        val title = Fonts.torus.cutStringTail(score.beatmapset.title, 18.0, 170.0)
        val difficulty = Fonts.torus.cutStringTail("[${score.beatmap.difficultyName}]", 14.0, 170.0)

        val body = buildString {
            append("<defs>")
            append("<clipPath id=\"clip-j-row-$clip\"><rect width=\"310\" height=\"80\" rx=\"12\" ry=\"12\"/></clipPath>")
            append("<clipPath id=\"clip-j-cover-$clip\"><rect width=\"120\" height=\"80\" rx=\"10\" ry=\"10\"/></clipPath>")
            append("</defs>")
            append("<g><rect width=\"310\" height=\"80\" rx=\"10\" ry=\"10\" style=\"fill:#46393f;\"/></g>")
            background?.let {
                append("<g clip-path=\"url(#clip-j-row-$clip)\" style=\"opacity:0.45;\">")
                append(Svg.image(0.0, 0.0, 310.0, 80.0, it))
                append("</g>")
            }
            cover?.let {
                append("<g clip-path=\"url(#clip-j-cover-$clip)\">")
                append(Svg.image(0.0, 0.0, 120.0, 80.0, it))
                append("</g>")
            }
            append(Fonts.torus.textPath(title, 130.0, 17.877, 18.0, "left baseline", "#fff"))
            append(Fonts.torus.textPath(difficulty, 130.0, 32.571, 14.0, "left baseline", "#fff"))
            append(Fonts.torus.textPath("${score.maxCombo}x $accuracy%", 300.0, 47.571, 14.0, "right baseline", "#fff"))
            append(Fonts.torus.textPath(mods, 130.0, 47.571, 14.0, "left baseline", "#a1a1a1"))
            append(PanelDraw.rect(5.0, 5.0, 40.0, 20.0, 10.0, starColor))
            append(Fonts.torus.textPath(star, 25.0, 20.877, 18.0, "center baseline", "#fff"))
            append(PanelDraw.rect(130.0, 54.0, 40.0, 20.0, 10.0, rankColor))
            append(Fonts.torus.textPath(score.rank, 150.0, 69.877, 18.0, "center baseline", if (score.rank == "X" || score.rank == "XH") "#1c1719" else "#fff"))
            append(Fonts.torus.get2SizeTextPath(Numbers.jsRound(score.pp).toLong().toString(), "PP", 30.0, 20.0, 300.0, 73.795, "right baseline", "#fff"))
        }
        return body
    }

    fun scoreCardD2(score: LazerScore, assets: ImageAssets): String {
        val background = assets.mapBackground(score, "list@2x")
        val rankColor = Colors.getRankColor(score.rank)
        val mods = score.mods.joinToString("") { it.acronym }.ifBlank { "NM" }
        val data = CardD2Data(
            background = background,
            title = Numbers.floor(score.beatmap.starRating, 1),
            titleMid = "*",
            subTitle = score.rank,
            left = mods,
            leftColor = "#fff",
            leftRrectColor = rankColor,
            right = "${Numbers.jsRound(score.pp).toLong()} PP",
            rightColor = "#fff",
            rightRrectColor = rankColor,
            bottomLeft = Fonts.poppinsBold.cutStringTail(score.beatmap.difficultyName, 14.0, 100.0),
            bottomRight = Fonts.poppinsBold.cutStringTail(score.beatmapset.creator, 14.0, 90.0),
        )
        return CardD2.render(data, background)
    }

    fun labelJ8(
        icon: String?,
        title: String,
        abbr: String,
        remark: String,
        minB: String,
        minM: String,
        midB: String,
        midM: String,
        maxB: String,
        maxM: String,
        barMin: Double,
        barMax: Double,
        minValue: Double,
        maxValue: Double,
        barColor: String,
        assets: ImageAssets,
    ): String {
        val span = (barMax - barMin).takeIf { it > 0.0 } ?: 1.0
        val before = ((minValue - barMin) / span).coerceIn(0.0, 1.0)
        val after = ((maxValue - barMin) / span).coerceIn(0.0, 1.0)
        val barWidth = max((after - before) * 390.0, 10.0)
        val iconSvg = icon?.let { assets.local(it) }?.let { Svg.image(0.0, 0.0, 50.0, 50.0, it) }.orEmpty()
        return buildString {
            append(iconSvg)
            append(Fonts.poppinsBold.textPath(title, 25.0, 65.0, 14.0, "center baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("Min", 65.0, 15.0, 18.0, "left baseline", "#666"))
            append(Fonts.poppinsBold.textPath("Mid", 255.0, 15.0, 18.0, "center baseline", "#666"))
            append(Fonts.poppinsBold.textPath("Max", 445.0, 15.0, 18.0, "right baseline", "#666"))
            append(Fonts.poppinsBold.get2SizeTextPath(minB, minM, 40.0, 30.0, 65.0, 52.0, "left baseline"))
            append(Fonts.poppinsBold.get2SizeTextPath(midB, midM, 40.0, 30.0, 255.0, 52.0, "center baseline"))
            append(Fonts.poppinsBold.get2SizeTextPath(maxB, maxM, 40.0, 30.0, 445.0, 52.0, "right baseline"))
            append(PanelDraw.rect(60.0, 58.0, 390.0, 10.0, 5.0, barColor, 0.2))
            append(PanelDraw.rect(60.0 + 390.0 * before, 58.0, barWidth, 10.0, 5.0, barColor))
            append(Fonts.poppinsBold.textPath(remark, 445.0, 78.0, 14.0, "right baseline", "#aaa"))
            append(Fonts.poppinsBold.textPath(abbr, 0.0, 15.0, 12.0, "left baseline", "#666"))
        }
    }

    fun labelJ1(mod: String, fullName: String, count: Int, pp: Double): String {
        val color = Colors.getModColor(mod)
        val points = listOf(14.0, 0.0, 42.0, 0.0, 56.0, 20.0, 42.0, 40.0, 14.0, 40.0, 0.0, 20.0)
        return buildString {
            append(Svg.polygon(points, color))
            append(Fonts.torus.textPath(mod, 35.0, 36.795, 30.0, "center baseline", "#fff"))
            append(Fonts.torus.textPath(Fonts.torus.cutStringTail(fullName, 18.0, 135.0), 75.0, 12.877, 18.0, "left baseline", "#fff"))
            append(Fonts.torus.textPath("${count}x", 75.0, 28.877, 18.0, "left baseline", "#aaa"))
            append(Fonts.torus.get2SizeTextPath(Numbers.jsRound(pp).toLong().toString(), "PP", 30.0, 18.0, 210.0, 54.795, "right baseline", "#fff"))
        }
    }

    fun labelJ2(index: Int, avatar: String?, name: String, count: Int, pp: Double, assets: ImageAssets): String {
        val clip = Texts.random(8)
        val avatarUrl = assets.resolveRef(avatar)
        val color = PanelColor.top(342)
        return buildString {
            append("<defs><clipPath id=\"clip-j2-$clip\"><rect x=\"8\" y=\"8\" width=\"70\" height=\"70\" rx=\"10\" ry=\"10\"/></clipPath></defs>")
            append(PanelDraw.rect(8.0, 8.0, 70.0, 70.0, 10.0, "#46393f"))
            append("<g clip-path=\"url(#clip-j2-$clip)\">")
            append(avatarUrl?.let { Svg.image(8.0, 8.0, 70.0, 70.0, it) }.orEmpty())
            append("</g>")
            append(PanelDraw.circle(14.0, 14.0, 14.0, color))
            append(Fonts.torus.textPath(index.toString(), 14.0, 21.836, 24.0, "center baseline", "#fff"))
            append(Fonts.torus.textPath(Fonts.torus.cutStringTail(name, 24.0, 160.0), 87.867, 21.836, 24.0, "left baseline", "#fff"))
            append(Fonts.torus.get2SizeTextPath(count.toString(), "x", 24.0, 18.0, 87.867, 49.836, "left baseline", "#aaa"))
            append(Fonts.torus.get2SizeTextPath(Numbers.jsRound(pp).toLong().toString(), "PP", 30.0, 18.0, 248.0, 76.795, "right baseline", "#fff"))
        }
    }

    fun labelJ3(rank: String, mapCount: Int, ppCount: Double, percentage: Double, assets: ImageAssets): String {
        val icon = assets.local("object-score-$rank-small.png")
            ?: assets.local("object-score-${rank}2.png")
        return buildString {
            append(icon?.let { Svg.image(0.0, 0.0, 40.0, 40.0, it) }.orEmpty())
            append(Fonts.torus.get2SizeTextPath(mapCount.toString(), "x", 36.0, 24.0, 150.0, 26.0, "right baseline", "#fff"))
            append(Fonts.torus.textPath(Numbers.floor(percentage * 100.0, 1) + "%", 2.0, 52.0, 16.0, "left baseline", "#aaa"))
            append(Fonts.torus.get2SizeTextPath(Numbers.jsRound(ppCount).toLong().toString(), "PP", 24.0, 18.0, 150.0, 52.0, "right baseline", "#aaa"))
        }
    }

    fun miniScoreCard(analysis: BPAnalysisService.Companion.BeatmapAnalysis, assets: ImageAssets): String {
        val clip = Texts.random(8)
        val background = assets.resolveRef(analysis.cover)
        val starColor = Colors.getStarRatingColor(analysis.star)
        val rankColor = Colors.getRankColor(analysis.rank)
        return buildString {
            append("<defs><clipPath id=\"clip-j-mini-$clip\"><rect width=\"70\" height=\"50\" rx=\"12\" ry=\"12\"/></clipPath></defs>")
            append(PanelDraw.rect(0.0, 0.0, 70.0, 50.0, 12.0, "#46393f"))
            append("<g clip-path=\"url(#clip-j-mini-$clip)\" style=\"opacity:0.5;\">")
            append(background?.let { Svg.image(0.0, 0.0, 70.0, 50.0, it) }.orEmpty())
            append("</g>")
            append(PanelDraw.circle(10.0, 10.0, 5.0, starColor))
            append(PanelDraw.circle(60.0, 10.0, 5.0, rankColor))
            append(Fonts.torus.textPath("#${analysis.ranking}", 35.0, 45.224, 16.0, "center baseline", "#fff"))
        }
    }

    fun componentK(title: String, items: List<BPAnalysisService.Companion.BeatmapAnalysis>, assets: ImageAssets, unit: String): String {
        val values = items.take(3)
        return buildString {
            append(PanelDraw.rect(0.0, 0.0, 290.0, 165.0, 20.0, PanelColor.middle(342)))
            append(Fonts.torus.textPath(title, 60.0, 22.836, 24.0, "left baseline", "#fff"))
            append(Fonts.torus.textPath("Max", 90.0, 51.0, 18.0, "center baseline", "#fff550"))
            append(Fonts.torus.textPath("Mid", 170.0, 51.0, 18.0, "center baseline", "#fff550"))
            append(Fonts.torus.textPath("Min", 250.0, 51.0, 18.0, "center baseline", "#fff550"))
            values.forEachIndexed { index, item ->
                val x = 55.0 + index * 80.0
                append(Svg.group(x, 100.0, miniScoreCard(item, assets)))
                val text = when (title) {
                    "Length" -> "${item.length / 60}:${(item.length % 60).toString().padStart(2, '0')}"
                    "Combo" -> "${item.combo}$unit"
                    else -> Numbers.floor(item.star, 2)
                }
                append(Fonts.torus.textPath(text, 90.0 + index * 80.0, 91.303, 24.0, "center baseline", "#fff"))
            }
        }
    }

    fun lineChart(values: List<Double>, maxValue: Double, minValue: Double, x: Double, y: Double, width: Double, height: Double, color: String, fill: Boolean = false): String {
        if (values.isEmpty()) return ""
        val span = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
        val step = if (values.size > 1) width / (values.size - 1) else 0.0
        val points = values.mapIndexed { index, value ->
            val px = x + step * index
            val py = y + height - (value - minValue) / span * height
            px to py
        }
        val path = points.mapIndexed { index, (px, py) ->
            (if (index == 0) "M" else "L") + Svg.number(px) + "," + Svg.number(py)
        }.joinToString(" ")
        val area = if (fill && points.isNotEmpty()) {
            "M" + Svg.number(x) + "," + Svg.number(y + height) + " L" +
                points.joinToString(" L") { Svg.number(it.first) + "," + Svg.number(it.second) } +
                " L" + Svg.number(x + width) + "," + Svg.number(y + height) + " Z"
        } else ""
        return buildString {
            if (area.isNotEmpty()) append("<path d=\"$area\" style=\"fill:${Svg.escape(color)};fill-opacity:0.12;\"/>")
            append("<path d=\"$path\" style=\"fill:none;stroke:${Svg.escape(color)};stroke-width:2;stroke-linejoin:round;stroke-linecap:round;\"/>")
        }
    }

    fun barChart(values: List<Int>, maxValue: Double, x: Double, y: Double, width: Double, height: Double, colors: List<String>, gap: Double = 2.0): String {
        if (values.isEmpty()) return ""
        val step = width / values.size
        return values.mapIndexed { index, value ->
            val barHeight = if (maxValue > 0) value / maxValue * height else 0.0
            PanelDraw.rect(x + index * step + gap, y + height - barHeight, step - gap * 2, barHeight, 2.0, colors.getOrElse(index) { "#fff" })
        }.joinToString("")
    }

    fun valueBar(title: String, value: Double, maxValue: Double, color: String, width: Double): String {
        val progress = if (maxValue > 0) (value / maxValue).coerceIn(0.0, 1.0) else 0.0
        val valueWidth = max(width * progress, min(width, 10.0))
        return buildString {
            append(PanelDraw.rect(0.0, 0.0, width, 40.0, 10.0, "#46393f"))
            append(PanelDraw.rect(0.0, 0.0, valueWidth, 40.0, 10.0, color, 0.2))
            append(PanelDraw.rect(width - 12.0, 8.0, 4.0, 24.0, 2.0, color))
            append(Fonts.poppinsBold.textPath(title, 10.0, 26.0, 18.0, "left baseline", "#fff"))
            append(Fonts.poppinsBold.textPath(Numbers.floor(value, 1), width - 20.0, 26.0, 18.0, "right baseline", "#fff"))
        }
    }
}
