package com.now.nowbot.image.component.j2

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.font.TextPart
import com.now.nowbot.image.font.Typography
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.PanelColor
import com.now.nowbot.image.util.Texts
import com.now.nowbot.model.osu.LazerScore
import kotlin.math.max

data class J2LabelData(val iconTitle: String, val abbr: String, val remark: String, val dataB: String, val progress: Double, val color: String, val maxWidth: Double)
data class J2Mapper(val name: String, val avatar: String, val mapCount: Int, val pp: Double)
data class J2Data(
    val hue: Int,
    val bests: List<LazerScore>,
    val ppRaw: List<Double>,
    val ranks: List<String>,
    val lengths: List<Int>,
    val mods: List<List<String>>,
    val stars: List<Double>,
    val times: List<Double>,
    val timeDist: List<Int>,
    val modAttrs: List<J2LabelData>,
    val rankAttrs: List<J2LabelData>,
    val bpmAttrs: List<J2LabelData>,
    val lengthAttrs: List<J2LabelData>,
    val comboAttrs: List<J2LabelData>,
    val starAttrs: List<J2LabelData>,
    val mappers: List<J2Mapper>,
    val ppSum: Double,
    val bestsSize: Int,
)

object PanelJ2Components {
    fun labelJ8(data: J2LabelData): String {
        var svg = "<g id=\"Label_J8\"></g>"
        val remark = Fonts.poppinsBold.textPath(data.remark, data.maxWidth - 10.0, 26.0, 12.0, "right baseline", "#aaa")
        val title = Fonts.poppinsBold.textPath(data.iconTitle, 52.0, 26.0, 14.0, "left baseline", "#fff")
        val value = Typography.multipleTextPath(
            listOf(
                TextPart(data.dataB, Fonts.poppinsBold, 40.0, "#fff"),
                TextPart("  ", Fonts.poppinsBold, 30.0, "#fff"),
            ), 52.0, 72.0, "left baseline",
        )
        val bar = PanelDraw.rect(52.0, 78.0, (data.maxWidth - 52.0).coerceAtLeast(10.0), 8.0, 4.0, data.color, 0.25)
        val progress = PanelDraw.rect(52.0, 78.0, (data.maxWidth - 52.0).coerceAtLeast(10.0) * data.progress.coerceIn(0.0, 1.0), 8.0, 4.0, data.color)
        svg = Svg.setTexts(svg, listOf(title, remark, value, bar, progress), "<g id=\"Label_J8\">")
        return svg
    }

    fun labelJ4(data: J2LabelData, x: Double, y: Double): String {
        var svg = "<g id=\"Label_J4\"></g>"
        val circle = PanelDraw.circle(15.0, 15.0, 8.0, data.color)
        val title = Fonts.poppinsBold.textPath(
            Fonts.poppinsBold.cutStringTail(data.iconTitle, 16.0, data.maxWidth - 30.0), 32.0, 20.0, 16.0, "left baseline", "#fff",
        )
        val remark = Fonts.poppinsBold.textPath(data.remark, data.maxWidth - 10.0, 20.0, 14.0, "right baseline", "#aaa")
        val number = Fonts.poppinsBold.textPath(data.dataB, data.maxWidth - 10.0, 42.0, 12.0, "right baseline", "#fff")
        val bar = PanelDraw.rect(0.0, 30.0, data.maxWidth, 4.0, 2.0, data.color, 0.25)
        val prog = PanelDraw.rect(0.0, 30.0, data.maxWidth * data.progress.coerceIn(0.0, 1.0), 4.0, 2.0, data.color)
        svg = Svg.setTexts(svg, listOf(circle, title, remark, number, bar, prog), "<g id=\"Label_J4\">")
        return Svg.group(x, y, svg)
    }

    fun labelJ6(mapper: J2Mapper, index: Int, hue: Int, assets: ImageAssets): String {
        var svg = "<g id=\"Label_J6\"></g>"
        val avatar = assets.resolveRef(mapper.avatar)?.let { Svg.image(0.0, 0.0, 55.0, 55.0, it) }.orEmpty()
        val title = Fonts.poppinsBold.textPath(Fonts.poppinsBold.cutStringTail(mapper.name, 16.0, 160.0), 65.0, 22.0, 16.0, "left baseline", "#fff")
        val rank = Fonts.poppinsBold.textPath("#$index", 225.0, 22.0, 14.0, "right baseline", PanelColor.top(hue))
        val pp = Fonts.poppinsBold.textPath("${Numbers.jsRound(mapper.pp).toLong()} PP [${mapper.mapCount}x]", 65.0, 45.0, 14.0, "left baseline", "#aaa")
        svg = Svg.setTexts(svg, listOf(avatar, title, rank, pp), "<g id=\"Label_J6\">")
        return svg
    }

    fun labelJ7(mapper: J2Mapper, hue: Int, assets: ImageAssets): String {
        var svg = "<g id=\"Label_J7\"></g>"
        val avatar = assets.resolveRef(mapper.avatar)?.let { Svg.image(0.0, 0.0, 80.0, 80.0, it) }.orEmpty()
        val title = Fonts.poppinsBold.textPath(Fonts.poppinsBold.cutStringTail(mapper.name, 20.0, 260.0), 90.0, 25.0, 20.0, "left baseline", "#fff")
        val pp = Fonts.poppinsBold.get2SizeTextPath(Numbers.jsRound(mapper.pp).toLong().toString(), " PP", 32.0, 20.0, 465.0, 50.0, "right baseline", "#fff")
        val count = Fonts.poppinsBold.textPath("${mapper.mapCount}x", 465.0, 72.0, 18.0, "right baseline", "#aaa")
        val rank = Fonts.poppinsBold.textPath("#1", 90.0, 72.0, 18.0, "left baseline", PanelColor.top(hue))
        svg = Svg.setTexts(svg, listOf(avatar, title, pp, count, rank), "<g id=\"Label_J7\">")
        return svg
    }

    fun pie(cx: Double, cy: Double, radius: Double, start: Double, end: Double, color: String): String {
        if (end <= start) return ""
        val a0 = start * 2 * Math.PI - Math.PI / 2
        val a1 = end * 2 * Math.PI - Math.PI / 2
        val x0 = cx + radius * Math.cos(a0)
        val y0 = cy + radius * Math.sin(a0)
        val x1 = cx + radius * Math.cos(a1)
        val y1 = cy + radius * Math.sin(a1)
        val large = if (end - start > 0.5) 1 else 0
        return "<path d=\"M ${Svg.number(cx)},${Svg.number(cy)} L ${Svg.number(x0)},${Svg.number(y0)} A ${Svg.number(radius)},${Svg.number(radius)} 0 $large 1 ${Svg.number(x1)},${Svg.number(y1)} Z\" style=\"fill:${Svg.escape(color)};\"/>"
    }

    fun scatter(x: Double, y: Double, w: Double, h: Double, radius: Double, xs: List<Double>, ys: List<Double>, colors: List<String>, xMin: Double = 0.0, xMax: Double = 24.0, yMin: Double = 0.0, yMax: Double = 1.0): String {
        if (xs.isEmpty() || ys.isEmpty()) return ""
        val ySpan = (yMax - yMin).takeIf { it != 0.0 } ?: 1.0
        return xs.indices.joinToString("") { i ->
            val px = x + (xs[i] - xMin) / (xMax - xMin) * w
            val py = y + h - (ys[i.coerceAtMost(ys.lastIndex)] - yMin) / ySpan * h
            PanelDraw.circle(px, py, radius, colors.getOrElse(i) { "#fff" }, 0.9)
        }
    }

    fun barChart(values: List<Double>, maxValue: Double, x: Double, y: Double, w: Double, h: Double, colors: List<String>, gap: Double = 3.0): String {
        if (values.isEmpty()) return ""
        val step = w / values.size
        return values.mapIndexed { i, value ->
            val bh = if (maxValue > 0) value / maxValue * h else 0.0
            PanelDraw.rect(x + i * step + gap, y + h - bh, step - gap * 2, bh, 2.0, colors.getOrElse(i) { "#fff" })
        }.joinToString("")
    }
}
