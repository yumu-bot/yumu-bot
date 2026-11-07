package com.now.nowbot.image.card

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.Mods
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.font.TextPart
import com.now.nowbot.image.font.Typography
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.PanelColor
import com.now.nowbot.image.util.Texts
import com.now.nowbot.model.osu.LazerMod
import kotlin.math.max
import kotlin.math.min

sealed interface CardCSideColor {
    data class Solid(val value: String) : CardCSideColor

    data class Gradient(val colors: List<String>) : CardCSideColor

    companion object {
        fun of(value: Any?): CardCSideColor? = when (value) {
            is List<*> -> value.filterIsInstance<String>().takeIf { it.size >= 2 }?.let(::Gradient)
            is String -> value.takeIf { it.isNotEmpty() }?.let(::Solid)
            else -> null
        }
    }
}

data class CardCData(
    val background: String? = null,
    val cover: String? = null,
    val type: String? = null,
    val title: String = "",
    val title2: String = "",
    val left1: String = "",
    val left2: String = "",
    val indexB: String = "",
    val indexM: String = "",
    val indexL: String = "",
    val indexBSize: Double = 48.0,
    val indexMSize: Double = 36.0,
    val indexLSize: Double = 24.0,
    val label1: String = "",
    val label2: String = "",
    val label3: String = "",
    val label4: String = "",
    val label5: String = "",
    val mods: List<LazerMod> = emptyList(),
    val colorTitle2: String = "#aaa",
    val colorRight: CardCSideColor? = null,
    val colorLeft: CardCSideColor? = null,
    val colorIndex: String = "#fff",
    val colorLabel1: String = "none",
    val colorLabel2: String = "none",
    val colorLabel3: String = "none",
    val colorLabel4: String = "none",
    val colorLabel5: String = "none",
    val colorLabel12: String = "#fff",
    val colorLeft12: String = "#fff",
    val fontTitle2: String = "torus",
    val fontLabel4: String = "torus",
    val progress: Double = 1.0,
)

object CardC {
    private const val MIN_TITLE2_CHARS = 5

    private val SVG_DEFS = """
        <defs>
            <clipPath id="clippath-CH-1">
                <rect x="160" y="0" width="570" height="110" rx="20" ry="20" style="fill: none;"/>
            </clipPath>
            <clipPath id="clippath-CH-2">
                <rect x="20" y="0" width="176" height="110" rx="20" ry="20" style="fill: none;"/>
            </clipPath>
        </defs>
        <g id="Base">
            <rect width="900" height="110" rx="20" ry="20" style="fill: #382e32;"/>
        </g>
    """.trimIndent()

    fun render(data: CardCData, assets: ImageAssets): String {
        val modsData = Mods.drawLazerMods(
            data.mods, x = 710.0, y = 24.0, height = 60.0, maxWidth = 160.0,
            align = "right", interval = 6.0, allowExpanded = true, allowName = false, assets = assets,
        )

        val fontLabel4 = if (data.fontLabel4 == "PuHuiTi") Fonts.puHuiTi else Fonts.torus
        val fontTitle2 = if (data.fontTitle2 == "PuHuiTi") Fonts.puHuiTi else Fonts.torus

        val label1Width = Fonts.torus.width(data.label1, 18.0) + 16
        val label2Width = Fonts.torus.width(data.label2, 18.0) + 16
        val label3Width = Fonts.torus.width(data.label3, 24.0) + 30
        val label4Width = fontLabel4.width(data.label4, 24.0) + 30
        val label5Width = Fonts.torus.width(data.label5, 18.0) + 16

        val label1Path = if (data.label1.isNotEmpty()) {
            Fonts.torusBold.textPath(data.label1, 38.0, 23.877, 18.0, "left baseline", data.colorLabel12)
        } else {
            ""
        }
        val label2Path = if (data.label2.isNotEmpty()) {
            Fonts.torusBold.textPath(data.label2, 38.0, 97.877, 18.0, "left baseline", data.colorLabel12)
        } else {
            ""
        }
        val label3Path = if (data.label3.isNotEmpty()) {
            Fonts.torus.textPath(data.label3, 710.0 - label3Width / 2, 34.836, 24.0, "center baseline", "#fff")
        } else {
            ""
        }
        val label4Path = if (data.label4.isNotEmpty()) {
            fontLabel4.textPath(data.label4, 710.0 - label4Width / 2, 78.572, 24.0, "center baseline", "#fff")
        } else {
            ""
        }
        val label5Path = if (data.label5.isNotEmpty()) {
            Fonts.torusBold.textPath(data.label5, 177.0, 97.877, 18.0, "right baseline", data.colorLabel12)
        } else {
            ""
        }

        val rrectLabel1 = if (data.label1.isNotEmpty()) {
            PanelDraw.rect(30.0, 8.0, label1Width, 20.0, 10.0, data.colorLabel1)
        } else {
            ""
        }
        val rrectLabel2 = if (data.label2.isNotEmpty()) {
            PanelDraw.rect(30.0, 82.0, label2Width, 20.0, 10.0, data.colorLabel2)
        } else {
            ""
        }
        val rrectLabel3 = if (data.label3.isNotEmpty()) {
            PanelDraw.rect(710.0 - label3Width, 10.0, label3Width, 34.0, 17.0, data.colorLabel3)
        } else {
            ""
        }
        val rrectLabel4 = if (data.label4.isNotEmpty()) {
            PanelDraw.rect(710.0 - label4Width, 54.0, label4Width, 34.0, 17.0, data.colorLabel4)
        } else {
            ""
        }
        val rrectLabel5 = if (data.label5.isNotEmpty()) {
            PanelDraw.rect(185.0 - label5Width, 82.0, label5Width, 20.0, 10.0, data.colorLabel5)
        } else {
            ""
        }

        val indexPath = Typography.multipleTextPath(
            listOf(
                TextPart(data.indexB, Fonts.torus, data.indexBSize, data.colorIndex),
                TextPart(data.indexM, Fonts.torus, data.indexMSize, data.colorIndex),
            ),
            815.0, 73.672, "center baseline",
        ) + if (data.indexL.isNotEmpty()) {
            Fonts.torus.textPath(data.indexL, 815.0, 33.672, data.indexLSize, "center baseline", data.colorIndex)
        } else {
            ""
        }

        val rrectLeft = sideRect(
            data.colorLeft, 0.0, 0.0, 60.0, 110.0, 20.0,
            listOf(40, 60), listOf(0, 100),
        )
        val rrectRightWidth = Numbers.clampToInteger(50.0 + 170.0 * data.progress, 220.0, 40.0)
        val rrectRight = sideRect(
            data.colorRight, 680.0, 0.0, rrectRightWidth, 110.0, 20.0,
            listOf(0, 100), listOf(20, 80),
        )

        val titleMaxWidth = 500.0 - max(modsData.width, label3Width - 10.0)
        val leftMaxWidth = 500.0 - modsData.width

        val isTitleNotEqual = data.title != data.title2
        var titleWidth = min(titleMaxWidth, Fonts.torus.width(data.title, 36.0))
        var title2Width = titleMaxWidth - titleWidth - 10.0

        if (isTitleNotEqual && data.title2.isNotEmpty() && !Texts.isAscii(title2Head(data.title2))) {
            val head = title2Head(data.title2)
            val prefix = head + if (data.title2.length > MIN_TITLE2_CHARS) "..." else ""
            val title2PrefixWidth = fontTitle2.width(prefix, 18.0)
            if (title2Width < title2PrefixWidth) {
                titleWidth = max(0.0, titleMaxWidth - title2PrefixWidth - 20.0)
            }
        }

        val textTitle = Fonts.torus.cutStringTail(data.title, 36.0, titleWidth)
        titleWidth = Fonts.torus.width(textTitle, 36.0)
        title2Width = titleMaxWidth - titleWidth - 10.0
        val textTitle2 = if (isTitleNotEqual && title2Width > 0.0) {
            fontTitle2.cutStringTail(data.title2, 18.0, title2Width, true)
        } else {
            ""
        }

        val titlePath = Fonts.torus.textPath(textTitle, 210.0, 34.754, 36.0, "left baseline", "#fff")
        val title2Path = if (textTitle2.isNotEmpty()) {
            val y = if (data.fontTitle2 == "PuHuiTi") 33.0 else 34.754
            fontTitle2.textPath(textTitle2, 210.0 + 10.0 + titleWidth, y, 18.0, "left baseline", data.colorTitle2)
        } else {
            ""
        }

        val left1Path = Fonts.torus.textPath(
            Fonts.torus.cutStringTail(data.left1, 24.0, leftMaxWidth), 210.0, 66.836, 24.0, "left baseline", data.colorLeft12,
        )
        val left2Path = Fonts.torus.textPath(
            Fonts.torus.cutStringTail(data.left2, 24.0, leftMaxWidth), 210.0, 96.836, 24.0, "left baseline", data.colorLeft12,
        )

        return buildString {
            append(SVG_DEFS)
            append("\n<g id=\"Color\">")
            append(rrectLeft)
            append(rrectRight)
            append("</g>")
            append("\n<g id=\"Background\">")
            append("""<rect x="160" y="0" width="570" height="110" rx="20" ry="20" style="fill: #382e32;"/>""")
            append("""<g style="clip-path: url(#clippath-CH-1);">""")
            append(assetImage(0.0, 0.0, 900.0, 110.0, data.background, 0.2))
            append("</g></g>")
            append("\n<g id=\"Index\">")
            append(indexPath)
            append("</g>")
            append("\n<g id=\"Avatar\">")
            append("""<rect x="20" y="0" width="176" height="110" rx="20" ry="20" style="fill: #382e32;"/>""")
            append("""<g style="clip-path: url(#clippath-CH-2);">""")
            append(assetImage(20.0, 0.0, 176.0, 110.0, data.cover, 1.0))
            append("</g></g>")
            append("\n<g id=\"Mods\">")
            append(modsData.svg)
            append("</g>")
            append("\n<g id=\"Text\">")
            append(titlePath)
            append(title2Path)
            append(left1Path)
            append(left2Path)
            append("</g>")
            append("\n<g id=\"Label\">")
            append(assetImage(140.0, 4.0, 45.0, 30.0, data.type, 1.0))
            append(rrectLabel1)
            append(rrectLabel2)
            append(rrectLabel3)
            append(rrectLabel4)
            append(rrectLabel5)
            append(label1Path)
            append(label2Path)
            append(label3Path)
            append(label4Path)
            append(label5Path)
            append("</g>")
        }
    }

    private fun sideRect(
        color: CardCSideColor?,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        radius: Double,
        positionX: List<Int>,
        positionY: List<Int>,
    ): String {
        return when (color) {
            is CardCSideColor.Gradient -> PanelDraw.linearGradientRect(
                x, y, width, height, radius, color.colors, positionX, positionY, 1.0,
            )
            is CardCSideColor.Solid -> PanelDraw.rect(x, y, width, height, radius, color.value, 1.0)
            null -> PanelDraw.rect(x, y, width, height, radius, PanelColor.top(342), 1.0)
        }
    }

    private fun assetImage(x: Double, y: Double, width: Double, height: Double, href: String?, opacity: Double): String {
        if (href.isNullOrEmpty()) return ""
        return Svg.image(x, y, width, height, href, opacity)
    }

    private fun title2Head(value: String): String {
        return value.substring(0, min(MIN_TITLE2_CHARS, value.length))
    }
}
