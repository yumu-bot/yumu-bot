package com.now.nowbot.image.draw

import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.Texts
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object PanelDraw {
    fun image(
        x: Double = 0.0,
        y: Double = 0.0,
        width: Double = 100.0,
        height: Double = 100.0,
        href: String = "",
        opacity: Double = 1.0,
        ratio: String = "xMidYMid slice",
    ): String = Svg.image(x, y, width, height, href, opacity, ratio)

    fun rect(
        x: Double = 0.0,
        y: Double = 0.0,
        width: Double = 0.0,
        height: Double = 0.0,
        radius: Double = 0.0,
        color: String = "#fff",
        opacity: Double = 1.0,
    ): String = Svg.rect(x, y, width, height, radius, color, opacity)

    fun circle(cx: Double = 0.0, cy: Double = 0.0, radius: Double = 0.0, color: String = "#fff", opacity: Double = 1.0): String {
        return Svg.circle(cx, cy, radius, color, opacity)
    }

    fun polygon(points: List<Double>, color: String = "#fff", opacity: Double = 1.0): String {
        return Svg.polygon(points, color, opacity)
    }

    fun gradientRect(
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        radius: Double,
        colors: List<GradientColor>,
        x1: String = "0%",
        y1: String = "0%",
        x2: String = "100%",
        y2: String = "0%",
        opacity: Double = 1.0,
    ): String {
        val name = "grad" + Texts.random(6)
        val stops = colors.joinToString("") {
            "<stop offset=\"${Svg.escape(it.offset)}\" style=\"stop-color:rgb(${Colors.hex2rgbColor(it.color)});" +
                "stop-opacity:${Svg.number(it.opacity)}\" />"
        }
        return "<g><defs><linearGradient id=\"$name\" x1=\"${Svg.escape(x1)}\" y1=\"${Svg.escape(y1)}\" " +
            "x2=\"${Svg.escape(x2)}\" y2=\"${Svg.escape(y2)}\">$stops</linearGradient></defs>" +
            rect(x, y, width, height, radius, "url(#$name)", opacity) + "</g>"
    }

    fun linearGradientRect(
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        radius: Double,
        colors: List<String>,
        positionX: List<Int> = listOf(0, 100),
        positionY: List<Int> = listOf(0, 0),
        opacity: Double = 1.0,
    ): String {
        if (colors.size < 2) return rect(x, y, width, height, radius, colors.firstOrNull() ?: "#fff", opacity)
        val gradientColors = colors.mapIndexed { index, color ->
            val offset = 10 + Numbers.jsRound(index.toDouble() / (colors.size - 1) * 80).toInt()
            GradientColor("$offset%", color, 1.0)
        }
        return gradientRect(
            x, y, width, height, radius, gradientColors,
            "${positionX.getOrElse(0) { 0 }}%", "${positionY.getOrElse(0) { 0 }}%",
            "${positionX.getOrElse(1) { 100 }}%", "${positionY.getOrElse(1) { 0 }}%", opacity,
        )
    }

    fun barChart(
        values: List<Double>,
        maxValue: Double = 0.0,
        minValue: Double = 0.0,
        x: Double = 900.0,
        y: Double = 1020.0,
        width: Double = 520.0,
        height: Double = 90.0,
        radius: Double = 0.0,
        gap: Double = 0.0,
        color: Any = "#fff",
        maxUndertake: Double = 0.0,
        floor: Double = 0.0,
        minColor: String? = "#aaa",
        opacity: Double = 1.0,
        reverse: Boolean = false,
    ): String {
        if (values.isEmpty()) return ""
        val colors = when (color) {
            is List<*> -> color.filterIsInstance<String>().ifEmpty { listOf("#fff") }
            is String -> listOf(color)
            else -> listOf("#fff")
        }
        val arrayMax = when {
            Numbers.isNumber(maxValue) && maxValue > 0 -> maxValue
            Numbers.isNumber(maxUndertake) -> max(values.maxOrNull() ?: 0.0, maxUndertake)
            else -> values.maxOrNull() ?: 0.0
        }
        val arrayMin = if (Numbers.isNumber(minValue) && minValue > 0) minValue else values.minOrNull() ?: 0.0
        val delta = arrayMax - arrayMin
        val step = (width + gap) / values.size
        val barWidth = step - gap

        val builder = StringBuilder("<g>")
        values.forEachIndexed { index, value ->
            val ratio = if (delta != 0.0) (value - arrayMin) / delta else 0.0
            val barHeight = if (ratio * height <= floor) floor else min(ratio, 1.0) * height
            val isMin = value <= arrayMin && minColor != null
            val barColor = if (isMin) minColor else colors[min(colors.lastIndex, index).coerceAtLeast(0)]
            val x0 = x + step * index
            val y0 = if (reverse) y else y - barHeight
            builder.append(rect(x0, y0, barWidth, barHeight, radius, barColor ?: "#fff", opacity))
        }
        return builder.append("</g>").toString()
    }

    fun lineChart(
        values: List<Double>,
        maxValue: Double = 0.0,
        minValue: Double = 0.0,
        x: Double = 900.0,
        y: Double = 900.0,
        width: Double = 520.0,
        height: Double = 90.0,
        color: String? = null,
        pathOpacity: Double = 1.0,
        areaOpacity: Double = 0.0,
        strokeWidth: Double = 3.0,
        zeroToMin: Boolean = false,
    ): String {
        if (values.isEmpty() || values.size < 2 || color == null) return ""
        val arrayMax = if (maxValue == 0.0) values.maxOrNull() ?: 0.0 else maxValue
        val arrayMin = if (minValue == 0.0) values.minOrNull() ?: 0.0 else minValue
        val delta = abs(arrayMax - arrayMin)
        val step = width / (values.size - 1)
        val initial = if (delta > 0) (values[0] - arrayMin) / (arrayMax - arrayMin) * height else 0.0

        val line = StringBuilder("<svg> <path d=\"M ${Svg.number(x)} ${Svg.number(y - initial)} S ")
        val area = StringBuilder("<svg> <path d=\"M ${Svg.number(x)} ${Svg.number(y - initial)} S ")
        for (index in 1 until values.size) {
            val value = values[index]
            val pointHeight = if (delta > 0) (value - arrayMin) / (arrayMax - arrayMin) * height else 0.0
            val lineX = x + step * index
            val lineY = if (value == 0.0 && zeroToMin) y else y - pointHeight
            val point = "${Svg.number(lineX)} ${Svg.number(lineY)} "
            line.append(point)
            area.append(point)
            if (index < values.lastIndex) {
                val next = values[index + 1]
                val nextHeight = if (delta > 0) (next - arrayMin) / (arrayMax - arrayMin) * height else 0.0
                val nextX = x + step * (index + 1)
                val nextY = if (next == 0.0 && zeroToMin) y else y - nextHeight
                val control = "${Svg.number(lineX + (nextX - lineX) * 0.2)} ${Svg.number(lineY + (nextY - lineY) * 0.2)} "
                line.append(control)
                area.append(control)
            } else {
                line.append(point)
                area.append(point)
            }
        }

        line.append("\" style=\"fill: none; stroke: ${Svg.escape(color)}; opacity: ${Svg.number(pathOpacity)}; " +
            "stroke-miterlimit: 10; stroke-width: ${Svg.number(strokeWidth)}px;\"/> </svg>")
        area.append("L ${Svg.number(x + width)} ${Svg.number(y)} L ${Svg.number(x)} ${Svg.number(y)} Z\" " +
            "style=\"fill: ${Svg.escape(color)}; stroke: none; fill-opacity: ${Svg.number(areaOpacity)};\"/> </svg>")
        return line.toString() + area
    }
}

data class GradientColor(
    val offset: String,
    val color: String,
    val opacity: Double = 1.0,
)
