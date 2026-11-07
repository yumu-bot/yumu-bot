package com.now.nowbot.image.match

import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.Texts
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.tan

object MatchShapes {
    fun calculateTan(height: Double, angle: Double): Double {
        return (height * tan(angle * PI / 180.0)).roundToInt().toDouble()
    }

    fun normalize(value: Double, maxValue: Double, minValue: Double, range: Double = 1.0, bottom: Double = 0.0): Double {
        val low = min(maxValue, minValue)
        val high = max(maxValue, minValue)
        if (value <= low || low == high) return 0.0
        return max(((min(value, high) - low) / (high - low)) * range, bottom)
    }

    fun roundedParallelogram(
        x: Double = 0.0,
        y: Double = 0.0,
        w: Double = 0.0,
        h: Double = 0.0,
        skewW: Double = 0.0,
        skewH: Double = 0.0,
        r: Double = 0.0,
        color: String = "#46393f",
        opacity: Double = 1.0,
    ): String {
        val skewTop = if (skewW >= 0) skewW else 0.0
        val skewBottom = if (skewW >= 0) 0.0 else -skewW
        val skewLeft = if (skewH >= 0) skewH else 0.0
        val skewRight = if (skewH >= 0) 0.0 else -skewH
        val points = listOf(
            x + skewTop to y + skewLeft,
            x + w + skewTop to y + skewRight,
            x + w + skewBottom to y + h + skewRight,
            x + skewBottom to y + h + skewLeft,
        )
        return roundedPolygon(points, r, color, opacity)
    }

    fun linearGradientParallelogram(
        x: Double,
        y: Double,
        w: Double,
        h: Double,
        skewW: Double,
        skewH: Double,
        r: Double,
        colors: List<String>,
        positionX: List<Int> = listOf(0, 100),
        positionY: List<Int> = listOf(0, 0),
        opacity: Double = 1.0,
    ): String {
        if (colors.size < 2) return roundedParallelogram(x, y, w, h, skewW, skewH, r, colors.firstOrNull() ?: "#fff", opacity)
        val id = "gradCF4" + Texts.random(6)
        val stops = colors.mapIndexed { index, color ->
            val offset = 10 + (index.toDouble() / (colors.size - 1) * 80).roundToInt()
            "<stop offset=\"$offset%\" style=\"stop-color:${color};stop-opacity:1\" />"
        }.joinToString("")
        val body = roundedParallelogram(x, y, w, h, skewW, skewH, r, "url(#$id)", 1.0)
        return "<g><defs><linearGradient id=\"$id\" x1=\"${positionX.getOrElse(0) { 0 }}%\" y1=\"${positionY.getOrElse(0) { 0 }}%\" " +
            "x2=\"${positionX.getOrElse(1) { 100 }}%\" y2=\"${positionY.getOrElse(1) { 0 }}%\">$stops</linearGradient></defs>" +
            "<g fill=\"url(#$id)\" fill-opacity=\"${Svg.number(opacity)}\">$body</g></g>"
    }

    fun shadow(
        content: String,
        dx: Double = 2.0,
        dy: Double = 2.0,
        blur: Double = 1.0,
        color: String = "#000",
        opacity: Double = 0.5,
        bleed: Double = 0.5,
    ): String {
        val id = "shadow" + Texts.random(6)
        val bleeding = max((bleed * 100).roundToInt(), 0)
        val size = bleeding * 2 + 100
        return "<g><defs><filter id=\"$id\" x=\"-$bleeding%\" y=\"-$bleeding%\" width=\"$size%\" height=\"$size%\">" +
            "<feDropShadow dx=\"${Svg.number(dx)}\" dy=\"${Svg.number(dy)}\" stdDeviation=\"${Svg.number(blur)}\" " +
            "flood-color=\"$color\" flood-opacity=\"${Svg.number(opacity)}\" /></filter></defs>" +
            "<g filter=\"url(#$id)\">$content</g></g>"
    }

    private fun roundedPolygon(points: List<Pair<Double, Double>>, radius: Double, color: String, opacity: Double): String {
        if (points.size < 3) return ""
        val centerX = points.sumOf { it.first } / points.size
        val centerY = points.sumOf { it.second } / points.size
        val sorted = points.sortedBy { atan2(it.second - centerY, it.first - centerX) }
        var area = 0.0
        for (i in sorted.indices) {
            val p1 = sorted[i]
            val p2 = sorted[(i + 1) % sorted.size]
            area += (p2.first - p1.first) * (p2.second + p1.second)
        }
        val sweep = if (area < 0) 1 else 0
        val path = StringBuilder()
        for (i in sorted.indices) {
            val prev = sorted[(i + sorted.size - 1) % sorted.size]
            val curr = sorted[i]
            val next = sorted[(i + 1) % sorted.size]
            val vPrevX = prev.first - curr.first
            val vPrevY = prev.second - curr.second
            val vNextX = next.first - curr.first
            val vNextY = next.second - curr.second
            val lenPrev = hypot(vPrevX, vPrevY)
            val lenNext = hypot(vNextX, vNextY)
            if (lenPrev == 0.0 || lenNext == 0.0) continue
            val cosTheta = max(-1.0, min(1.0, (vPrevX * vNextX + vPrevY * vNextY) / (lenPrev * lenNext)))
            val halfTan = tan(acos(cosTheta) / 2)
            if (halfTan <= 0.0001) {
                path.append(if (path.isEmpty()) "M ${Svg.number(curr.first)},${Svg.number(curr.second)} " else "L ${Svg.number(curr.first)},${Svg.number(curr.second)} ")
                continue
            }
            val maxR = min(lenPrev, lenNext) / 2 * halfTan
            val safeR = min(radius, maxR)
            val d = safeR / halfTan
            val startX = curr.first + vPrevX * (d / lenPrev)
            val startY = curr.second + vPrevY * (d / lenPrev)
            val endX = curr.first + vNextX * (d / lenNext)
            val endY = curr.second + vNextY * (d / lenNext)
            if (path.isEmpty()) path.append("M ${Svg.number(startX)},${Svg.number(startY)} ")
            else path.append("L ${Svg.number(startX)},${Svg.number(startY)} ")
            path.append("A ${Svg.number(safeR)},${Svg.number(safeR)} 0 0 $sweep ${Svg.number(endX)},${Svg.number(endY)} ")
        }
        path.append("Z")
        return "<path d=\"$path\" style=\"fill: ${Svg.escape(color)}; fill-opacity: ${Svg.number(opacity)};\"/>"
    }
}
