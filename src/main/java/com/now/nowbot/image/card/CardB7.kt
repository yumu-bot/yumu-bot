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

data class CardB7Data(
    val label: LabelB,
    val value: Double = 11.4514,
    val dataB: String = "",
    val dataM: String = "",
    val min: Double = label.min,
    val limit: Double = label.limit,
    val max: Double = label.max,
    val colors: List<String> = label.colors,
    val iconColors: List<String> = listOf("#382E32", "#382E32"),
    val limitBreakColors: List<String> = ColorArray.rainbow,
    val roundLevel: Int = 2,
)

object CardB7 {
    private const val MARK_LABEL = "<g id=\"Label\">"
    private const val MARK_TEXT = "<g id=\"Text\">"

    fun render(data: CardB7Data, assets: ImageAssets, toLeft: Boolean = false): String {
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

        val isLimitBreak = data.value > data.limit
        val low = if (isLimitBreak) data.limit else data.min
        val high = if (isLimitBreak) data.max else data.limit
        val baseOpacity = if (isLimitBreak) 0.8 else 0.2
        val barColors = if (isLimitBreak) data.limitBreakColors.ifEmpty { ColorArray.radiant } else data.colors

        val rawWidth = Numbers.normalize(data.value, high, low, 320.0)
        val barWidth = Numbers.clampToInteger(rawWidth, 30.0, 320.0)

        val iconX: Double
        val barX: Double
        val titleX: Double
        val valueX: Double
        val barPositionX: List<Int>
        if (!toLeft) {
            iconX = 0.0
            barX = 0.0
            titleX = 95.0
            valueX = 320.0
            barPositionX = listOf(0, 100)
        } else {
            iconX = 320.0 - 80
            barX = 320.0 - barWidth
            titleX = 320.0 - 95
            valueX = 0.0
            barPositionX = listOf(100, 0)
        }

        val left1Font = if (Texts.isAscii(data.label.left1)) Fonts.poppinsBold else Fonts.puHuiTi
        val left1Size = if (Texts.isAscii(data.label.left1)) 18.0 else 16.0
        val left2Font = if (Texts.isAscii(data.label.left2)) Fonts.poppinsBold else Fonts.puHuiTi
        val left2Size = if (Texts.isAscii(data.label.left2)) 18.0 else 16.0

        val icon = icon(data.label, assets, iconX + 10, 10.0)
        val iconBase = PanelDraw.rect(iconX, 0.0, 80.0, 80.0, 10.0, "#382E32") +
            PanelDraw.linearGradientRect(
                iconX, 0.0, 80.0, 80.0, 10.0, data.iconColors,
                listOf(0, 100), listOf(20, 80), 0.4,
            )
        val barBase = PanelDraw.linearGradientRect(
            0.0, 95.0, 320.0, 30.0, 15.0, data.colors,
            barPositionX, listOf(55, 45), baseOpacity,
        )
        val bar = PanelDraw.linearGradientRect(
            barX, 95.0, barWidth, 30.0, 15.0, barColors,
            barPositionX, listOf(55, 45), 1.0,
        )

        val anchor = if (toLeft) "right baseline" else "left baseline"
        val titlePath = Fonts.poppinsBold.textPath(data.label.title, titleX, 30.0, 40.0, anchor, "#fff")
        val left1 = left1Font.textPath(data.label.left1, titleX, 55.0, left1Size, anchor, "#aaa")
        val left2 = left2Font.textPath(data.label.left2, titleX, 78.0, left2Size, anchor, "#aaa")

        val values = Numbers.roundParts(data.value, data.roundLevel)
        val hasDataB = data.dataB.trim().isNotEmpty()
        val valB = if (hasDataB) data.dataB else values.integer
        val valM = if (hasDataB) data.dataM else values.decimal
        val valuePath = Fonts.poppinsBold.get2SizeTextPath(
            valB, valM, 46.0, 30.0, valueX, 80.0,
            if (toLeft) "left baseline" else "right baseline",
        )

        svg = Svg.setTexts(svg, listOf(titlePath, left1, left2, valuePath), MARK_TEXT)
        svg = Svg.setTexts(svg, listOf(icon, iconBase, bar, barBase), MARK_LABEL)
        return svg
    }

    private fun icon(label: LabelB, assets: ImageAssets, x: Double, y: Double): String {
        val path = label.iconFile ?: return ""
        if (path.endsWith(".svg", ignoreCase = true)) {
            val text = assets.localSvgText(path) ?: return ""
            if (!text.startsWith("<svg")) return assets.local(path)?.let { Svg.image(x, y, 60.0, 60.0, it) }.orEmpty()
            val body = Svg.bodyOf(text)
            if (body.isBlank()) return ""
            return "<svg x=\"${Svg.number(x)}\" y=\"${Svg.number(y)}\" width=\"60\" height=\"60\" " +
                "style=\"fill: #fff;\" viewBox=\"0 0 640 640\">\n$body\n</svg>"
        }
        return assets.local(path)?.let { Svg.image(x, y, 60.0, 60.0, it) }.orEmpty()
    }
}
