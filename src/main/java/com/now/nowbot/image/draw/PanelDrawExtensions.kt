package com.now.nowbot.image.draw

import com.now.nowbot.image.font.FontFace
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Numbers
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun PanelDraw.diamond(
    x: Double = 0.0,
    y: Double = 0.0,
    w: Double = 0.0,
    h: Double = 0.0,
    color: String = "#fff",
    opacity: Double = 1.0,
): String {
    val top = "${Svg.number(x + w / 2)} ${Svg.number(y)}"
    val right = "${Svg.number(x + w)} ${Svg.number(y + h / 2)}"
    val bottom = "${Svg.number(x + w / 2)} ${Svg.number(y + h)}"
    val left = "${Svg.number(x)} ${Svg.number(y + h / 2)}"
    return "<polygon points=\"$top $right $bottom $left $top\" " +
        "style=\"fill: ${Svg.escape(color)}; fill-opacity: ${Svg.number(opacity)}\"/>"
}

fun PanelDraw.hexagonChart(
    data: List<Double>,
    cx: Double = 960.0,
    cy: Double = 600.0,
    r: Double = 230.0,
    lineColor: String = "#fff",
    offset: Double = 0.0,
    strokeWidth: Double = 6.0,
    cr: Double = 10.0,
): String {
    if (data.size < 6) return ""

    val step = PI / 3
    val coordinates = (0 until 6).joinToString("") { index ->
        val value = Numbers.clamp(data[index], 1.0, 0.0)
        val x = cx - r * cos(step * index + offset) * value
        val y = cy - r * sin(step * index + offset) * value
        "${Svg.number(x)} ${Svg.number(y)} L "
    }
    val path = "M " + coordinates.dropLast(2)
    val circles = (0 until 6).joinToString("") { index ->
        val value = Numbers.clamp(data[index], 1.0, 0.0)
        val x = cx - r * cos(step * index + offset) * value
        val y = cy - r * sin(step * index + offset) * value
        PanelDraw.circle(x, y, cr, lineColor)
    }

    return "<path d=\"$path Z\" style=\"fill: none; stroke-width: ${Svg.number(strokeWidth)}; " +
        "stroke: ${Svg.escape(lineColor)}; opacity: 1;\"/> " +
        "<path d=\"$path Z\" style=\"fill: ${Svg.escape(lineColor)}; " +
        "stroke-width: ${Svg.number(strokeWidth)}; stroke: none; opacity: 0.3;\"/> " +
        circles
}

fun PanelDraw.hexagonIndex(
    data: List<String>,
    cx: Double = 960.0,
    cy: Double = 600.0,
    r: Double = 260.0,
    offset: Double = 0.0,
    textColor: String = "#fff",
    backgroundColor: String = "#54454C",
    size: Double = 24.0,
    font: FontFace = Fonts.torusBold,
): String {
    if (data.size < 6) return ""

    val step = PI / 3
    var svg = "<g id=\"Rect\"></g><g id=\"IndexText\"></g>"
    for (index in 0 until 6) {
        val value = data[index]
        val x = cx - r * cos(step * index + offset)
        val y = cy - r * sin(step * index + offset)

        svg = Svg.setText(
            svg,
            font.textPath(value, x, y + 8, size, "center baseline", textColor),
            "<g id=\"IndexText\">",
        )
        val width = font.width(value, size)
        svg = Svg.setText(
            svg,
            PanelDraw.rect(x - width / 2 - 20, y - 15, width + 40, 30.0, 15.0, backgroundColor),
            "<g id=\"Rect\">",
        )
    }
    return svg
}
