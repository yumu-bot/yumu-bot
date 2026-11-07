package com.now.nowbot.image.card

import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.font.TextPart
import com.now.nowbot.image.font.Typography
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.diamond
import com.now.nowbot.image.maimai.MaiAssets
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Texts
import kotlin.math.max
import kotlin.math.min

data class CardI3Data(
    val background: String? = null,
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
    val indexR: String = "",
    val indexBSize: Double = 32.0,
    val indexMSize: Double = 20.0,
    val indexRSize: Double = 18.0,
    val label1: String = "",
    val label2: String = "",
    val label3: String = "",
    val colorText: String = "#fff",
    val colorLabel1: String = "none",
    val colorLabel2: String = "none",
    val colorLabel3: String = "none",
    val colorLeft: String = "#382e32",
    val colorRrect1: String = "none",
    val colorRrect2: String = "none",
    val colorRrect3: String = "none",
    val component1: String = "",
    val component2: String = "",
    val component3: String = "",
    val left3IsRight: Boolean = false,
)

object CardI3 {
    private const val SVG_DEFS_I3 = """
    <defs>
    <clipPath id="clippath-CI3-1">
        <rect x="100" width="252" height="130" rx="20" ry="20" style="fill: none;"/>
    </clipPath>
    <clipPath id="clippath-CI3-2">
         <rect width="80" height="130" rx="20" ry="20" style="fill: none;"/>
    </clipPath>
    <clipPath id="clippath-CI3-3">
         <rect x="15" width="130" height="130" rx="20" ry="20" style="fill: none;"/>
    </clipPath>
        <filter id="blur-CI3-BG" height="130%" width="130%" x="-5%" y="-5%" filterUnits="userSpaceOnUse">
            <feGaussianBlur in="userSpaceOnUse" stdDeviation="15" result="blur"/>
        </filter>
    </defs>
    <g id="Base-CI3">
        <rect width="352" height="130" rx="20" ry="20" style="fill: #46393f;"/>
        <rect x="15" width="130" height="130" rx="20" ry="20" style="fill: #382e32;"/>
    </g>
"""

    fun render(data: CardI3Data): String {
        val titleFont = if (Texts.isAscii(data.title)) Fonts.torus else Fonts.puHuiTi
        val title2Font = if (Texts.isAscii(data.title2)) Fonts.torus else Fonts.puHuiTi
        val left1Font = if (Texts.isAscii(data.left1)) Fonts.torus else Fonts.puHuiTi
        val left2Font = if (Texts.isAscii(data.left2)) Fonts.torus else Fonts.puHuiTi
        val left3Font = if (Texts.isAscii(data.left3)) Fonts.torus else Fonts.puHuiTi
        val left4Font = if (Texts.isAscii(data.left4)) Fonts.torus else Fonts.puHuiTi

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

        val titleWidth = 191.0 - (if (Texts.isNotBlank(data.type)) 30.0 else 0.0)
        val left2Width = 191.0 - (if (Texts.isNotBlank(data.component3)) 105.0 else 0.0) - 16.0 * data.level

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
                TextPart(text = data.left3, font = left3Font, size = left3Size, color = data.colorText),
                TextPart(text = data.left4, font = left4Font, size = left4Size, color = data.colorText),
            ),
            if (data.left3IsRight) 342.0 else 155.0,
            90.0,
            if (data.left3IsRight) "right baseline" else "left baseline",
        )

        val indexPath = Typography.multipleTextPath(
            listOf(
                TextPart(text = data.indexB, font = Fonts.torus, size = data.indexBSize, color = data.colorText),
                TextPart(text = data.indexM, font = Fonts.torus, size = data.indexMSize, color = data.colorText),
                TextPart(text = data.indexR, font = Fonts.torus, size = data.indexRSize, color = data.colorText),
            ),
            342.0, 120.0, "right baseline",
        )

        val label1Width = Fonts.torusBold.width(data.label1, 18.0)
        val label1RrectWidth = max(40.0, label1Width + 10.0)
        val label1Path = Fonts.torusBold.textPath(
            data.label1, 25.0 + label1RrectWidth / 2.0, 24.0, 18.0, "center baseline", data.colorLabel1,
        )
        val label1Rrect = Svg.rect(25.0, 8.0, label1RrectWidth, 20.0, 10.0, data.colorRrect1, 1.0)

        val label2Width = Fonts.torusBold.width(data.label2, 18.0)
        val label2RrectWidth = max(40.0, label2Width + 10.0)
        val label2Path = Fonts.torusBold.textPath(
            data.label2, 25.0 + label2RrectWidth / 2.0, 118.0, 18.0, "center baseline", data.colorLabel2,
        )
        val label2Rrect = Svg.rect(25.0, 102.0, label2RrectWidth, 20.0, 10.0, data.colorRrect2, 1.0)

        val label3Width = Fonts.torusBold.width(data.label3, 18.0)
        val label3RrectWidth = max(40.0, label3Width + 10.0)
        val label3Path = Fonts.torusBold.textPath(
            data.label3, 135.0 - label3RrectWidth / 2.0, 118.0, 18.0, "center baseline", data.colorLabel3,
        )
        val label3Rrect = Svg.rect(135.0 - label3RrectWidth, 102.0, label3RrectWidth, 20.0, 10.0, data.colorRrect3, 1.0)

        val starsPath = drawStars(data.level)
        val leftRrect = Svg.rect(0.0, 0.0, 145.0, 130.0, 20.0, data.colorLeft, 1.0)

        return buildString {
            append(SVG_DEFS_I3)
            append("<g id=\"Background-CI3\"><g filter=\"url(#blur-CI3-BG)\" style=\"clip-path: url(#clippath-CI3-1);\">")
            append(Svg.image(0.0, 0.0, 352.0, 130.0, data.background.orEmpty(), 0.4))
            append("</g></g>")
            append("<g id=\"Difficulty-CI3\"><g style=\"clip-path: url(#clippath-CI3-2);\">")
            append(leftRrect)
            append("</g></g>")
            append("<g id=\"Cover-CI3\"><g style=\"clip-path: url(#clippath-CI3-3);\">")
            append(Svg.image(15.0, 0.0, 130.0, 130.0, data.cover.orEmpty(), 1.0))
            append("</g></g>")
            append("<g id=\"Rank-CI3\">")
            append(Svg.image(152.0, 96.0, 56.0, 30.0, data.rank.orEmpty(), 1.0))
            append(Svg.group(290.0, 68.0, data.component1))
            append(Svg.group(318.0, 68.0, data.component2))
            append(Svg.group(342.0, 54.0, starsPath))
            append(Svg.group(236.0, 54.0, data.component3))
            append("</g>")
            append("<g id=\"Overlay-CI3\">")
            append(label1Rrect)
            append(label2Rrect)
            append(label3Rrect)
            append(Svg.image(305.0, 2.0, 45.0, 30.0, data.type.orEmpty(), 1.0))
            append("</g>")
            append("<g id=\"Text-CI3\">")
            append(titlePath)
            append(title2Path)
            append(left1Path)
            append(left2Path)
            append(left3Path)
            append(indexPath)
            append(label1Path)
            append(label2Path)
            append(label3Path)
            append("</g>")
        }
    }

    private fun drawStars(level: Int): String {
        if (level <= 0) return ""
        val color = MaiAssets.starColor(level)
        return (0 until level).joinToString("") { index ->
            PanelDraw.diamond(-10.0 - 14.0 * index, 0.0, 10.0, 10.0, color)
        }
    }

}
