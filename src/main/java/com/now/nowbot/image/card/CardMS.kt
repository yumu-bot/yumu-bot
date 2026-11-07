package com.now.nowbot.image.card

import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.font.TextPart
import com.now.nowbot.image.font.Typography
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Texts
import kotlin.math.max

data class CardMSData(
    val background: String? = null,
    val cover: String? = null,
    val overlay: String? = null,
    val type: String? = null,
    val icon1: String? = null,
    val icon2: String? = null,
    val icon3: String? = null,
    val label1: String = "",
    val label1Colors: List<String> = listOf("none", "none"),
    val label2: String = "",
    val label2Colors: List<String> = listOf("none", "none"),
    val left: String = "",
    val right: String = "",
    val title: String = "",
    val mainB: String = "",
    val mainM: String = "",
    val mainL: String = "",
    val mainBSize: Double = 56.0,
    val mainMSize: Double = 36.0,
    val mainLSize: Double = 24.0,
    val additionalB: String = "",
    val additionalM: String = "",
    val additionalBSize: Double = 24.0,
    val additionalMSize: Double = 14.0,
    val rrect1Percent: Double = 0.0,
    val rrect1Color1: String = "",
    val rrect1Color2: String = "",
    val rrect1BaseOpacity: Double = 0.1,
    val stars: String = "",
    val component: String = "",
    val completed: Boolean = false,
)

object CardMS {
    fun render(data: CardMSData): String {
        val label1Width = Fonts.berlinBold.width(data.label1, 36.0)
        val label1Rect = PanelDraw.linearGradientRect(
            20.0, 20.0, label1Width + 40.0, 50.0, 25.0,
            data.label1Colors, listOf(100, 0), listOf(80, 20), 1.0,
        )
        val label1 = Fonts.berlinBold.textPath(
            data.label1, 20.0 + (label1Width + 40.0) / 2.0, 56.0, 36.0,
            "center baseline", "#fff", 1.0, true,
        )

        val label2Width = Fonts.berlinBold.width(data.label2, 48.0)
        val label2Rect = PanelDraw.linearGradientRect(
            310.0 - label2Width - 40.0, 255.0, label2Width + 40.0, 50.0, 25.0,
            data.label2Colors, listOf(100, 0), listOf(80, 20), 1.0,
        )
        val label2 = Fonts.berlinBold.textPath(
            data.label2, 310.0 - (label2Width + 40.0) / 2.0, 290.0, 48.0,
            "center baseline", "#fff", 1.0, true,
        )

        val base1 = Svg.rect(30.0, 30.0, 290.0, 310.0, 20.0, "#46393f", 1.0)
        val left = Fonts.poppinsBold.textPath(data.left, 45.0, 325.0, 14.0, "left baseline", "#fff")
        val rightFont = if (Texts.isAscii(data.right)) Fonts.poppinsBold else Fonts.puHuiTi
        val rightSize = if (Texts.isAscii(data.right)) 14.0 else 12.0
        val right = rightFont.textPath(
            rightFont.cutStringTail(
                data.right, rightSize,
                270.0 - rightFont.width(data.left, rightSize),
            ),
            305.0, 325.0, rightSize, "right baseline", "#fff",
        )
        val title = Fonts.poppinsBold.textPath(data.title, 310.0, 370.0, 14.0, "right baseline", "#fff")
        val main = Typography.multipleTextPath(
            listOf(
                TextPart(data.mainB, Fonts.poppinsBold, data.mainBSize, "#fff"),
                TextPart(data.mainM, Fonts.poppinsBold, data.mainMSize, "#fff"),
                TextPart(data.mainL, Fonts.poppinsBold, data.mainLSize, "#fff"),
            ),
            175.0, 420.0, "center baseline",
        )
        val additional = Typography.multipleTextPath(
            listOf(
                TextPart(data.additionalB, Fonts.poppinsBold, data.additionalBSize, "#fff"),
                TextPart(data.additionalM, Fonts.poppinsBold, data.additionalMSize, "#fff"),
            ),
            135.0, 445.0, "left baseline",
        )

        val colors = if (data.rrect1Color1.isNotEmpty() && data.rrect1Color2.isNotEmpty()) {
            listOf(data.rrect1Color1, data.rrect1Color2)
        } else {
            com.now.nowbot.image.util.ColorArray.cyan
        }
        val rrect1Top = if (data.rrect1Percent > 0) {
            PanelDraw.linearGradientRect(
                130.0, 450.0, max(180.0 * data.rrect1Percent, 10.0), 10.0, 5.0, colors,
            )
        } else {
            ""
        }
        val rrect1Base = PanelDraw.linearGradientRect(
            130.0, 450.0, 180.0, 10.0, 5.0, colors,
            listOf(0, 100), listOf(0, 0),
            data.rrect1BaseOpacity.takeIf { it > 0 } ?: 0.1,
        )
        val base2 = Svg.rect(30.0, 350.0, 290.0, 120.0, 20.0, "#46393f", 1.0)

        val completedOpacity = if (data.completed) 0.2 else 1.0

        return buildString {
            append("<defs>")
            append("<clipPath id=\"clippath-CMS-1\"><rect width=\"350\" height=\"710\" rx=\"20\" ry=\"20\" style=\"fill: none;\"/></clipPath>")
            append("<clipPath id=\"clippath-CMS-2\"><rect x=\"60\" y=\"60\" width=\"230\" height=\"230\" rx=\"20\" ry=\"20\" style=\"fill: none;\"/></clipPath>")
            append("</defs>")
            append("<g id=\"Background\"><rect width=\"350\" height=\"710\" rx=\"20\" ry=\"20\" style=\"fill: #382E32;\"/>")
            append("<g style=\"clip-path: url(#clippath-CMS-1);\">")
            append(Svg.image(0.0, 0.0, 350.0, 710.0, data.background.orEmpty(), 0.6))
            append("</g></g>")
            append("<g id=\"Base\">")
            append(base1)
            append(base2)
            append("</g>")
            append("<g id=\"Cover\"><g style=\"clip-path: url(#clippath-CMS-2);\">")
            append(Svg.image(60.0, 60.0, 230.0, 230.0, data.cover.orEmpty(), completedOpacity))
            append(Svg.image(60.0, 60.0, 230.0, 230.0, data.overlay.orEmpty(), 0.6))
            append("</g></g>")
            append("<g id=\"Component\">")
            append(label1Rect)
            append(label2Rect)
            append(rrect1Top)
            append(rrect1Base)
            append(Svg.image(30.0, 250.0, 90.0, 60.0, data.type.orEmpty()))
            append(Svg.image(42.0, 424.0, 36.0, 40.0, data.icon1.orEmpty()))
            append(Svg.image(84.0, 424.0, 36.0, 40.0, data.icon2.orEmpty()))
            append(Svg.image(232.0, 420.0, 56.0, 30.0, data.icon3.orEmpty()))
            append(Svg.group(30.0, 480.0, data.component))
            append("</g>")
            append("<g id=\"Text\">")
            append(label1)
            append(label2)
            append(left)
            append(right)
            append(title)
            append(main)
            append(additional)
            append(Svg.group(310.0, 445.0, data.stars))
            append("</g>")
        }
    }
}
