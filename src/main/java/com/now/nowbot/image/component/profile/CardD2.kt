package com.now.nowbot.image.component.profile

import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.font.TextPart
import com.now.nowbot.image.font.Typography
import com.now.nowbot.image.svg.Svg

data class CardD2Data(
    val background: String? = null,
    val topImage: String? = null,
    val title: String = "",
    val titleMid: String = "",
    val subTitle: String = "",
    val left: String = "",
    val leftColor: String = "#fff",
    val leftRrectColor: String = "#382e32",
    val right: String = "",
    val rightColor: String = "#fff",
    val rightRrectColor: String = "#382e32",
    val bottomLeft: String = "",
    val bottomRight: String = "",
)

object CardD2 {
    fun render(data: CardD2Data, background: String?): String {
        var svg = """
            <defs>
            <clipPath id="clippath-CD2-1">
                <rect width="150" height="120" rx="20" ry="20" style="fill: none;"/>
            </clipPath>
            </defs>
            <g id="Base"><rect width="150" height="120" rx="20" ry="20" style="fill: #382e32;"/></g>
            <g id="Background"><g style="clip-path: url(#clippath-CD2-1);"></g></g>
            <g id="Text"></g>
        """.trimIndent()
        val title = Typography.multipleTextPath(
            listOf(
                TextPart(data.title, Fonts.poppinsBold, 40.0, "#fff"),
                TextPart(data.titleMid, Fonts.poppinsBold, 30.0, "#fff"),
            ),
            75.0, 77.0, "center baseline", 1.0, true,
        )
        val subTitle = if (data.subTitle.isNotBlank()) {
            Fonts.poppinsBold.textPath(data.subTitle, 147.0, 96.0, 14.0, "right baseline", "#000") +
                Fonts.poppinsBold.textPath(data.subTitle, 145.0, 94.0, 14.0, "right baseline", "#fff")
        } else {
            ""
        }
        val topImage = if (!data.topImage.isNullOrBlank()) Svg.image(116.0, 2.0, 30.0, 30.0, data.topImage) else ""
        val leftWidth = Fonts.poppinsBold.width(data.left, 16.0)
        val leftRrectWidth = maxOf(48.0, leftWidth + 20.0)
        val left = Fonts.poppinsBold.textPath(data.left, 6.0 + leftRrectWidth / 2, 22.0, 16.0, "center baseline", data.leftColor)
        val leftRrect = PanelDraw.rect(6.0, 6.0, leftRrectWidth, 20.0, 10.0, data.leftRrectColor)
        val right = Fonts.poppinsBold.textPath(data.right, 120.0, 22.0, 16.0, "center baseline", data.rightColor)
        val rightRrect = PanelDraw.rect(96.0, 6.0, 48.0, 20.0, 10.0, data.rightRrectColor)
        val bottomLeft = Fonts.poppinsBold.textPath(data.bottomLeft, 6.0, 112.0, 14.0, "left baseline", "#fff", 1.0, true)
        val bottomRight = Fonts.poppinsBold.textPath(data.bottomRight, 144.0, 112.0, 14.0, "right baseline", "#fff", 1.0, true)
        svg = Svg.setTexts(
            svg,
            listOf(title, topImage, subTitle, left, right, bottomLeft, bottomRight, leftRrect, rightRrect),
            "<g id=\"Text\">",
        )
        svg = Svg.setText(
            svg,
            background?.let { Svg.image(0.0, 0.0, 150.0, 120.0, it, 0.7) },
            "<g style=\"clip-path: url(#clippath-CD2-1);\">",
        )
        return svg
    }
}
