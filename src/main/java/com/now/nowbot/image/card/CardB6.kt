package com.now.nowbot.image.card

import com.now.nowbot.image.component.labels.LabelB
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.localSvgText
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.ColorArray
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.Texts

data class CardB6Data(
    val label: LabelB,
    val value: Double = 11.4514,
    val delta: Double? = 0.0,
    val deltaTop: Double = 0.0,
    val dataB: String = "",
    val dataM: String = "",
    val min: Double = label.min,
    val limit: Double = label.limit,
    val max: Double = label.max,
    val colors: List<String> = label.colors,
    val iconColors: List<String> = DEFAULT_ICON_COLORS,
    val limitBreakColors: List<String> = ColorArray.rainbow,
    val roundLevel: Int = 2,
)

object CardB6 {
    private const val MARK_LABEL = "<g id=\"Label\">"
    private const val MARK_TEXT = "<g id=\"Text\">"

    fun render(data: CardB6Data, assets: ImageAssets, toLeft: Boolean = false): String {
        var svg = """
    <defs>
</defs>
  <g id="Background">
  </g>
  <g id="Body">
    <g id="Label">
    </g>
    <g id="Text">
    </g>
  </g>
    
        """.trimIndent()

        val titleWidth = Fonts.poppinsBold.width(data.label.title, 46.0)
        val isLimitBreak = data.value > data.limit
        val delta = data.delta ?: 0.0

        val baseOpacity: Double
        val barColors: List<String>
        val rawWidth: Double
        val rawDeltaWidth: Double
        if (isLimitBreak) {
            baseOpacity = 0.6
            barColors = data.limitBreakColors.ifEmpty { ColorArray.radiant }
            rawWidth = Numbers.normalize(data.value, data.max, data.limit, 390.0)
            rawDeltaWidth = Numbers.normalize(data.value - delta, data.max, data.limit, 390.0)
        } else {
            baseOpacity = 0.2
            barColors = data.colors
            rawWidth = Numbers.normalize(data.value, data.limit, data.min, 390.0)
            rawDeltaWidth = Numbers.normalize(data.value - delta, data.limit, data.min, 390.0)
        }

        val barWidth = Numbers.clampToInteger(rawWidth, 390.0, 30.0)
        val barDeltaWidth = Numbers.clampToInteger(rawDeltaWidth, 390.0, 30.0)

        var redWidth = 0.0
        var greenWidth = 0.0
        var realBarWidth = barWidth
        var deltaColor = "#fff"
        var deltaSign = ""

        if (delta > 0) {
            realBarWidth = barDeltaWidth
            greenWidth = barWidth
            deltaColor = "#c2e5c3"
            deltaSign = "+"
        } else if (delta < 0) {
            realBarWidth = barWidth
            redWidth = barDeltaWidth
            deltaColor = "#ffcdd2"
            deltaSign = ""
        }

        val iconX: Double
        val barBaseX: Double
        val barX: Double
        val barGreenX: Double
        val barRedX: Double
        val titleX: Double
        val leftX: Double
        val valueX: Double
        val barPositionX: List<Int>

        if (!toLeft) {
            iconX = 20.0
            barBaseX = 120.0
            barX = 120.0
            barGreenX = 120.0
            barRedX = 120.0
            titleX = 120.0
            leftX = 120.0 + titleWidth + 10
            valueX = 510.0
            barPositionX = listOf(0, 100)
        } else {
            iconX = 510.0 - 80
            barBaseX = 20.0
            barX = 390.0 - realBarWidth + 20
            barGreenX = 390.0 - greenWidth + 20
            barRedX = 390.0 - redWidth + 20
            titleX = 510.0 - 120 + 20
            leftX = 510.0 - 120 - titleWidth - 10 + 20
            valueX = 20.0
            barPositionX = listOf(100, 0)
        }

        val left1Font = if (Texts.isAscii(data.label.left1)) Fonts.poppinsBold else Fonts.puHuiTi
        val left1Size = if (Texts.isAscii(data.label.left1)) 18.0 else 16.0
        val left2Font = if (Texts.isAscii(data.label.left2)) Fonts.poppinsBold else Fonts.puHuiTi
        val left2Size = if (Texts.isAscii(data.label.left2)) 18.0 else 16.0

        val icon = icon(data.label, assets, iconX + 10, 30.0)
        val iconBase = PanelDraw.rect(iconX, 20.0, 80.0, 80.0, 10.0, "#382E32") +
            PanelDraw.linearGradientRect(
                iconX, 20.0, 80.0, 80.0, 10.0, data.iconColors,
                listOf(0, 100), listOf(20, 80), 0.4,
            )
        val barBase = PanelDraw.linearGradientRect(
            barBaseX, 70.0, 390.0, 30.0, 15.0, data.colors,
            barPositionX, listOf(55, 45), baseOpacity,
        )
        val bar = PanelDraw.linearGradientRect(
            barX, 70.0, realBarWidth, 30.0, 15.0, barColors,
            barPositionX, listOf(55, 45), 1.0,
        )
        val barRed = PanelDraw.linearGradientRect(
            barRedX, 70.0, redWidth, 30.0, 15.0, ColorArray.darkRed,
            barPositionX, listOf(55, 45), 1.0,
        )
        val barGreen = PanelDraw.linearGradientRect(
            barGreenX, 70.0, greenWidth, 30.0, 15.0, ColorArray.lightGreen,
            barPositionX, listOf(55, 45), 1.0,
        )

        val anchor = if (toLeft) "right baseline" else "left baseline"
        val titlePath = Fonts.poppinsBold.textPath(data.label.title, titleX, 54.0, 46.0, anchor, "#fff")
        val left1 = left1Font.textPath(data.label.left1, leftX, 34.0, left1Size, anchor, "#aaa")
        val left2 = left2Font.textPath(data.label.left2, leftX, 54.0, left2Size, anchor, "#aaa")

        val values = Numbers.roundParts(data.value, data.roundLevel)
        val hasDataB = data.dataB.trim().isNotEmpty()
        val valB = if (hasDataB) data.dataB else values.integer
        val valM = if (hasDataB) data.dataM else values.decimal
        val valuePath = Fonts.poppinsBold.get2SizeTextPath(
            valB, valM, 46.0, 30.0, valueX, 54.0,
            if (toLeft) "left baseline" else "right baseline",
        )
        val valueWidth = Fonts.poppinsBold.width(valB, 46.0) + Fonts.poppinsBold.width(valM, 30.0)

        val deltaX: Double
        val deltaAnchor: String
        if (!toLeft) {
            deltaX = valueX - valueWidth - 10
            deltaAnchor = "right baseline"
        } else {
            deltaX = valueX + valueWidth + 10
            deltaAnchor = "left baseline"
        }
        val deltaPath = if (delta != 0.0) {
            Fonts.poppinsBold.textPath(deltaSign + Numbers.round(delta, data.roundLevel), deltaX, 54.0, 18.0, deltaAnchor, deltaColor)
        } else {
            ""
        }
        val deltaTopPath = if (data.deltaTop != 0.0) {
            Fonts.poppinsBold.textPath(
                Numbers.jsRound(data.deltaTop).toLong().toString(), deltaX, 34.0, 18.0, deltaAnchor, "#fff",
            )
        } else {
            ""
        }

        svg = Svg.setTexts(svg, listOf(titlePath, left1, left2, valuePath, deltaPath, deltaTopPath), MARK_TEXT)
        svg = Svg.setTexts(svg, listOf(icon, iconBase, bar, barRed, barGreen, barBase), MARK_LABEL)
        return svg
    }

    private fun icon(label: LabelB, assets: ImageAssets, x: Double, y: Double): String {
        val path = label.iconFile ?: return ""
        if (path.endsWith(".svg", ignoreCase = true)) {
            val text = assets.localSvgText(path) ?: return ""
            if (!text.startsWith("<svg")) return assets.local(path)?.let { Svg.image(x, y, 60.0, 60.0, it) }.orEmpty()
            val body = Svg.bodyOf(text)
            if (body.isBlank()) return ""
            return svgBody(body, x, y)
        }
        return assets.local(path)?.let { Svg.image(x, y, 60.0, 60.0, it) }.orEmpty()
    }

    private fun svgBody(body: String, x: Double, y: Double): String {
        return "<svg x=\"${Svg.number(x)}\" y=\"${Svg.number(y)}\" width=\"60\" height=\"60\" " +
            "style=\"fill: #fff;\" viewBox=\"0 0 640 640\">\n$body\n</svg>"
    }
}

private val DEFAULT_ICON_COLORS = listOf("#382E32", "#382E32")
