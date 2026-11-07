package com.now.nowbot.image.card

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.Mods
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.diamond
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.font.TextPart
import com.now.nowbot.image.font.Typography
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.ColorArray
import com.now.nowbot.image.util.Texts
import com.now.nowbot.model.osu.LazerMod
import kotlin.math.max
import kotlin.math.min

data class CardI4Data(
    val cover: String? = null,
    val rank: String? = null,
    val type: String? = null,
    val level: Int = 0,
    val title: String = "",
    val title2: String = "",
    val left1: String = "",
    val left2: String = "",
    val left3: String = "",
    val left4: String = "",
    val indexB: String = "",
    val indexM: String = "",
    val indexBSize: Double = 32.0,
    val indexMSize: Double = 20.0,
    val label1: String = "",
    val label2: String = "",
    val colorText: String = "#fff",
    val colorLabel1: String = "none",
    val colorLabel2: String = "none",
    val colorLeft: String = "#382e32",
    val colorRrect1: String = "none",
    val colorRrect2: String = "none",
    val colorBackgrounds: List<String> = ColorArray.gray,
    val mods: List<LazerMod> = emptyList(),
    val left3IsRight: Boolean = false,
)

object CardI4 {
    private val SVG_DEFS = """
        <defs>
        <clipPath id="clippath-CI4-1">
            <rect x="100" width="252" height="130" rx="20" ry="20" style="fill: none;"/>
        </clipPath>
        <clipPath id="clippath-CI4-2">
             <rect width="80" height="130" rx="20" ry="20" style="fill: none;"/>
        </clipPath>
        <clipPath id="clippath-CI4-3">
             <rect x="15" width="130" height="130" rx="20" ry="20" style="fill: none;"/>
        </clipPath>
        </defs>
        <g id="Base-CI4">
            <rect width="352" height="130" rx="20" ry="20" style="fill: #46393f;"/>
            <rect x="15" width="130" height="130" rx="20" ry="20" style="fill: #382e32;"/>
        </g>
    """.trimIndent()

    fun render(data: CardI4Data, assets: ImageAssets): String {
        val titleFont = titleFont(data.title)
        val title2Font = titleFont(data.title2)
        val left1Font = titleFont(data.left1)
        val left2Font = titleFont(data.left2)
        val left3Font = titleFont(data.left3)
        val left4Font = titleFont(data.left4)

        val titleSize = if (Texts.isAscii(data.title)) 22.0 else 20.0
        val title2Size = if (Texts.isAscii(data.title2)) 16.0 else 14.0
        val left1Size = if (Texts.isAscii(data.left1)) 16.0 else 14.0
        val left2Size = if (Texts.isAscii(data.left2)) 16.0 else 14.0
        val left3Size = if (Texts.isAscii(data.left3)) 24.0 else 22.0
        val left4Size = if (Texts.isAscii(data.left4)) 16.0 else 14.0

        var title2Width = title2Font.width(data.title2, title2Size)
        var left1Width = min(left1Font.width(data.left1, titleSize), 181.0 - title2Width)
        if (title2Width > 131.0) {
            left1Width = min(left1Font.width(data.left1, titleSize), 131.0)
            title2Width = 131.0 - left1Width
        }

        val titleWidth = 191.0 - (if (data.type.isNullOrEmpty()) 0.0 else 35.0)
        val left2Width = 191.0 - 16.0 * data.level

        val titlePath = titleFont.textPath(
            titleFont.cutStringTail(data.title, titleSize, titleWidth),
            155.0, 26.0, titleSize, "left baseline", "#fff",
        )
        val title2Path = title2Font.textPath(
            title2Font.cutStringTail(data.title2, title2Size, title2Width),
            342.0, 46.0, title2Size, "right baseline", "#bbb",
        )
        val left1Path = left1Font.textPath(
            left1Font.cutStringTail(data.left1, left1Size, left1Width),
            155.0, 46.0, left1Size, "left baseline", "#fff",
        )
        val left2Path = left2Font.textPath(
            left2Font.cutStringTail(data.left2, left2Size, left2Width),
            155.0, 66.0, left2Size, "left baseline", "#fff",
        )

        val left3Path = Typography.multipleTextPath(
            listOf(
                TextPart(data.left3, left3Font, left3Size, data.colorText),
                TextPart(data.left4, left4Font, left4Size, data.colorText),
            ),
            if (data.left3IsRight) 342.0 else 155.0,
            90.0,
            if (data.left3IsRight) "right baseline" else "left baseline",
        )
        val indexPath = Typography.multipleTextPath(
            listOf(
                TextPart(data.indexB, Fonts.torus, data.indexBSize, data.colorText),
                TextPart(data.indexM, Fonts.torus, data.indexMSize, data.colorText),
            ),
            342.0, 120.0, "right baseline",
        )

        val leftRrect = PanelDraw.rect(0.0, 0.0, 145.0, 130.0, 20.0, data.colorLeft, 1.0)
        val modsData = Mods.drawLazerMods(
            data.mods, x = 342.0, y = 70.0, height = 24.0, maxWidth = 60.0,
            align = "right", interval = 8.0, allowExpanded = false, allowName = false, assets = assets,
        )

        val label1RectWidth = max(40.0, Fonts.torusBold.width(data.label1, 18.0) + 10.0)
        val label1Path = Fonts.torusBold.textPath(
            data.label1, 25.0 + label1RectWidth / 2, 24.0, 18.0, "center baseline", data.colorLabel1,
        )
        val label1Rrect = PanelDraw.rect(25.0, 8.0, label1RectWidth, 20.0, 10.0, data.colorRrect1, 1.0)

        val label2RectWidth = max(40.0, Fonts.torusBold.width(data.label2, 18.0) + 10.0)
        val label2Path = Fonts.torusBold.textPath(
            data.label2, 25.0 + label2RectWidth / 2, 118.0, 18.0, "center baseline", data.colorLabel2,
        )
        val label2Rrect = PanelDraw.rect(25.0, 102.0, label2RectWidth, 20.0, 10.0, data.colorRrect2, 1.0)

        val stars = stars(data.level)
        val backgroundRrect = PanelDraw.linearGradientRect(
            0.0, 0.0, 350.0, 130.0, 20.0, data.colorBackgrounds,
            listOf(20, 80), listOf(0, 100), 0.4,
        )

        return buildString {
            append(SVG_DEFS)
            append("\n<g id=\"Background-CI4\"><g>")
            append(backgroundRrect)
            append("</g></g>")
            append("\n<g id=\"Difficulty-CI4\"><g style=\"clip-path: url(#clippath-CI4-2);\">")
            append(leftRrect)
            append("</g></g>")
            append("\n<g id=\"Cover-CI4\"><g style=\"clip-path: url(#clippath-CI4-3);\">")
            append(image(15.0, 0.0, 130.0, 130.0, data.cover, 1.0))
            append("</g></g>")
            append("\n<g id=\"Rank-CI4\">")
            append(image(152.0, 96.0, 56.0, 30.0, data.rank, 1.0))
            append("<g transform=\"translate(342, 54)\">")
            append(stars)
            append("</g></g>")
            append("\n<g id=\"Overlay-CI4\">")
            append(label1Rrect)
            append(label2Rrect)
            append(modsData.svg)
            append(image(300.0, 4.0, 45.0, 30.0, data.type, 1.0))
            append("</g>")
            append("\n<g id=\"Text-CI4\">")
            append(titlePath)
            append(title2Path)
            append(left1Path)
            append(left2Path)
            append(left3Path)
            append(indexPath)
            append(label1Path)
            append(label2Path)
            append("</g>")
        }
    }

    private fun titleFont(text: String) = if (Texts.isAscii(text)) Fonts.torus else Fonts.puHuiTi

    private fun image(x: Double, y: Double, width: Double, height: Double, href: String?, opacity: Double): String {
        if (href.isNullOrEmpty()) return ""
        return Svg.image(x, y, width, height, href, opacity)
    }

    private fun stars(level: Int): String {
        if (level == 0) return ""
        val color = starColor(level)
        return (0 until level).joinToString("") { index ->
            PanelDraw.diamond(-10.0 - 14.0 * index, 0.0, 10.0, 10.0, color)
        }
    }

    private fun starColor(level: Int): String = when {
        level >= 5 -> "#fbf365"
        level >= 3 -> "#ffb84d"
        level >= 1 -> "#6fc576"
        else -> "none"
    }
}
