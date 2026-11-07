package com.now.nowbot.image.card

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Texts

data class CardC3Data(
    val background: String? = null,
    val cover: String? = null,
    val title: String = "",
    val title2: String = "",
    val left1: String = "",
    val left2: String = "",
    val label1: String = "",
    val label2: String? = null,
    val colorRrect: String = "#fff",
    val colorTitleText: String = "#fff",
    val colorTitle2Text: String = "#bbb",
    val colorLabelRrect1: String = "",
    val colorLabelRrect2: String = "",
    val colorLabelText: String = "#fff",
    val colorLeft1Text: String = "#fff",
    val colorLeft2Text: String = "#bbb",
    val blur: Double = 30.0,
)

object CardC3 {
    fun render(data: CardC3Data, assets: ImageAssets): String {
        var svg = TEMPLATE
        val cover = assets.resolveRef(data.cover)
        val background = assets.resolveRef(data.background)
        val titleWidth = Fonts.torus.width(data.title, 36.0)
        val title2Width = 680.0 - titleWidth - 10.0
        val font2 = if (Texts.isAscii(data.title2)) Fonts.torus else Fonts.puHuiTi
        val title2 = if (data.title2.isNotEmpty() && data.title != data.title2 && title2Width > 0) {
            font2.cutStringTail(data.title2, 18.0, title2Width, true)
        } else ""
        val title = Fonts.torus.textPath(Fonts.torus.cutStringTail(data.title, 36.0, 680.0), 210.0, 46.6 - 1.8, 36.0, "left baseline", data.colorTitleText)
        val title2Path = if (title2.isNotEmpty()) font2.textPath(title2, 210.0 + 10.0 + titleWidth, if (font2 === Fonts.puHuiTi) 33.0 else 34.754, 18.0, "left baseline", data.colorTitle2Text) else ""
        val left1 = Fonts.torus.textPath(Fonts.torus.cutStringTail(data.left1, 24.0, 680.0), 210.0, 66.836, 24.0, "left baseline", data.colorLeft1Text)
        val left2 = Fonts.torus.textPath(Fonts.torus.cutStringTail(data.left2, 24.0, 680.0), 210.0, 96.836, 24.0, "left baseline", data.colorLeft2Text)
        svg = Svg.setTexts(svg, listOf(title, title2Path, left1, left2), "<g id=\"Text_CC3\">")
        svg = Svg.setText(svg, data.label1?.let { PanelDraw.rect(10.0, 8.0, 32.0, 24.0, 8.0, data.colorLabelRrect1.ifBlank { data.colorRrect }) + Fonts.torusBold.textPath(it, 26.0, 26.0, 18.0, "center baseline", data.colorLabelText) }, "<g id=\"Label_CC3\">")
        val label2 = data.label2?.let {
            val width = Fonts.torusBold.width(it, 18.0) + 20.0
            PanelDraw.rect(48.0, 8.0, width, 24.0, 8.0, data.colorLabelRrect2.ifBlank { data.colorRrect }) +
                Fonts.torusBold.textPath(it, 48.0 + width / 2, 26.0, 18.0, "center baseline", data.colorLabelText)
        }
        svg = Svg.setText(svg, label2, "<g id=\"Label_CC3\">")
        svg = Svg.setText(svg, cover?.let { Svg.image(20.0, 0.0, 176.0, 110.0, it, 1.0) } ?: (
            PanelDraw.rect(0.0, 0.0, 176.0, 110.0, 20.0, "#382E32") +
                Fonts.torusBold.textPath("?", 176.0 / 2 + 20.0, 66.0, 36.0, "center baseline", "#fff")
            ), "<g id=\"Avatar_CC3\">")
        svg = Svg.setText(svg, background?.let { Svg.image(0.0, 0.0, 900.0, 110.0, it, 0.2) }, "<g style=\"clip-path: url(#clippath-CC3-1);\">")
        return svg
    }

    private val TEMPLATE = """
        <defs><clipPath id="clippath-CC3-1"><rect width="900" height="110" rx="20" ry="20" style="fill: none;"/></clipPath></defs>
        <g id="Base_CC3"><rect width="900" height="110" rx="20" ry="20" style="fill: #382E32;"/></g>
        <g id="Background_CC3"><g style="clip-path: url(#clippath-CC3-1);"></g></g>
        <g id="Avatar_CC3"></g><g id="Label_CC3"></g><g id="Text_CC3"></g>
    """.trimIndent()
}
