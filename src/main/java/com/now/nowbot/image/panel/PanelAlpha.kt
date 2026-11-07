package com.now.nowbot.image.panel

import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import java.util.concurrent.ThreadLocalRandom

object PanelAlpha {
    internal val ALPHA_COLORS = listOf(
        "#F5409B", "#E04D4D", "#B2FFF8", "#CBAAFF",
        "#85FFFF", "#FFFF93", "#FFE5DD", "#FFE8FF",
        "#FFFFED", "#FFA6FF", "#FA7C7C", "#62EDFF",
        "#FFFFB6", "#8DE0CA", "#C8FFFF", "#A5C6A3",
    )

    fun toSvg(lines: List<String?>, colorPicker: (Int) -> String = { randomColor() }): String {
        var maxWidth = 0.0
        var bottom = 10.0
        val textLines = lines.mapIndexed { index, line ->
            val text = line.orEmpty()
            val metrics = Fonts.puHuiTi.getTextMetrics(text, 30.0)
            val path = Fonts.puHuiTi.textPath(
                text, 10.0, bottom + metrics.height, 30.0, "left", colorPicker(index),
            )
            maxWidth = maxOf(maxWidth, metrics.width)
            bottom += metrics.height + 10.0
            path
        }

        val width = maxWidth + 50.0
        val height = bottom + 20.0
        return buildString {
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
            append(Svg.number(width))
            append("\" height=\"")
            append(Svg.number(height))
            append("\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" viewBox=\"0 0 ")
            append(Svg.number(width))
            append(' ')
            append(Svg.number(height))
            append("\">\n")
            append("    <rect width=\"100%\" height=\"100%\" fill=\"#2A2226\" />\n")
            append(textLines.joinToString("\n"))
            append("\n</svg>")
        }
    }

    private fun randomColor(): String {
        return ALPHA_COLORS[ThreadLocalRandom.current().nextInt(ALPHA_COLORS.size)]
    }
}
