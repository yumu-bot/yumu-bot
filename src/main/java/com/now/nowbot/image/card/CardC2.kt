package com.now.nowbot.image.card

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.model.osu.OsuUser
import java.util.Locale

data class CardC2Data(
    val background: String? = null,
    val cover: String? = null,
    val title: String = "",
    val left1: String = "",
    val left2: String = "",
    val label1: String = "",
    val label2: String = "",
    val colorRrect: String = "#fff",
    val colorTitleText: String = "#fff",
    val colorLabelRrect1: String = "",
    val colorLabelRrect2: String = "",
    val colorLabelText: String = "#fff",
    val colorLeft1Text: String = "#bbb",
    val colorLeft2Text: String = "#bbb",
)

object CardC2 {
    private const val MARK_TEXT = "<g id=\"Text\">"
    private const val MARK_COLOR = "<g id=\"Color\">"
    private const val MARK_LABEL = "<g id=\"Label\">"
    private const val MARK_BACKGROUND = "<g style=\"clip-path: url(#clippath-CC2-1);\" filter=\"url(#blur-CC2-BG)\">"
    private const val MARK_COVER = "<g style=\"clip-path: url(#clippath-CC2-2);\">"

    private val TEMPLATE = """
      <defs>
    <clipPath id="clippath-CC2-1">
      <rect x="220" y="0" width="680" height="110" rx="20" ry="20" style="fill: none;"/>
    </clipPath>
    <clipPath id="clippath-CC2-2">
      <rect x="20" y="0" width="238" height="110" rx="20" ry="20" style="fill: none;"/>
    </clipPath>
        <filter id="blur-CC2-BG" height="110%" width="110%" x="-5%" y="-5%" filterUnits="userSpaceOnUse">
            <feGaussianBlur in="userSpaceOnUse" stdDeviation="15" result="blur"/>
        </filter>
  </defs>
  <g id="Base">
    <rect width="900" height="110" rx="20" ry="20" style="fill: #382e32;"/>
  </g>
  <g id="Color">
  </g>
  <g id="Background">
  <rect x="218" y="0" width="682" height="110" rx="20" ry="20" style="fill: #382e32;"/>
    <g style="clip-path: url(#clippath-CC2-1);" filter="url(#blur-CC2-BG)">
    </g>
  </g>
  <g id="Index">
  </g>
  <g id="Avatar">
  <rect x="20" y="0" width="176" height="110" rx="20" ry="20" style="fill: #382e32;"/>
    <g style="clip-path: url(#clippath-CC2-2);">
    </g>
  </g>
  <g id="Text">
  </g>
  <g id="Label">
  </g>
    """.trimIndent()

    fun render(data: CardC2Data, assets: ImageAssets): String {
        var svg = TEMPLATE

        val label1Width = Fonts.torus.getTextWidth(data.label1, 18.0) + 16
        val label2Width = Fonts.torus.getTextWidth(data.label2, 18.0) + 16

        val textLabel1 = Fonts.torusBold.textPath(data.label1, 38.0, 23.877, 18.0, "left baseline", data.colorLabelText)
        val textLabel2 = Fonts.torusBold.textPath(data.label2, 38.0, 97.877, 18.0, "left baseline", data.colorLabelText)

        val rrectLabel1 = if (data.label1.isNotEmpty()) {
            PanelDraw.rect(30.0, 8.0, label1Width, 20.0, 10.0, data.colorLabelRrect1.ifEmpty { "none" })
        } else {
            ""
        }
        val rrectLabel2 = if (data.label2.isNotEmpty()) {
            PanelDraw.rect(30.0, 82.0, label2Width, 20.0, 10.0, data.colorLabelRrect2.ifEmpty { "none" })
        } else {
            ""
        }

        svg = Svg.setText(svg, PanelDraw.rect(0.0, 0.0, 60.0, 110.0, 20.0, data.colorRrect.ifEmpty { "none" }), MARK_COLOR)
        svg = Svg.setTexts(svg, listOf(textLabel1, textLabel2, rrectLabel1, rrectLabel2), MARK_LABEL)

        val title = Fonts.torus.cutStringTail(data.title, 36.0, 610.0)
        val left1 = Fonts.torus.cutStringTail(data.left1, 24.0, 610.0)
        val left2 = Fonts.torus.cutStringTail(data.left2, 24.0, 610.0)

        svg = Svg.setTexts(
            svg,
            listOf(
                Fonts.torus.textPath(title, 272.0, 34.754, 36.0, "left baseline", data.colorTitleText),
                Fonts.torus.textPath(left1, 272.0, 66.836, 24.0, "left baseline", data.colorLeft1Text),
                Fonts.torus.textPath(left2, 272.0, 96.836, 24.0, "left baseline", data.colorLeft2Text),
            ),
            MARK_TEXT,
        )

        svg = Svg.setText(svg, assets.resolveRef(data.cover)?.let {
            Svg.image(20.0, 0.0, 238.0, 110.0, it, 1.0)
        }, MARK_COVER)
        svg = Svg.setText(svg, assets.resolveRef(data.background)?.let {
            Svg.image(0.0, 0.0, 900.0, 110.0, it, 0.2)
        }, MARK_BACKGROUND)
        return svg
    }

    fun fromBadge(badge: OsuUser.UserBadge, index: Int, assets: ImageAssets): CardC2Data {
        val title = badge.description
        val image = badge.image2xUrl.takeIf { it.isNotBlank() } ?: badge.imageUrl

        val titleCut = Fonts.torus.cutStringTail(title, 36.0, 610.0, false)
        val titleExceed = if (titleCut.length < title.length) title.substring(titleCut.length) else ""

        val indexText = if (index > 0) "#$index" else ""
        val left2 = indexText + " // award at: " + TimeFormat.formatted(badge.awardAt) +
            " (" + TimeFormat.difference(badge.awardAt, 1) + ")"

        return CardC2Data(
            background = image,
            cover = image,
            title = titleCut,
            left1 = titleExceed,
            left2 = left2,
            label1 = "",
            label2 = "",
            colorRrect = badgeColor(title),
            colorTitleText = "#fff",
            colorLabelRrect1 = "",
            colorLabelRrect2 = "",
            colorLabelText = "#fff",
            colorLeft1Text = "#fff",
            colorLeft2Text = "#bbb",
        )
    }

    fun badgeColor(badgeName: String?): String {
        val name = badgeName.orEmpty().lowercase(Locale.ROOT)
        fun has(text: String) = name.contains(text)

        return when {
            has("world cup pooling") -> "#A086BF"
            has("world cup news post") -> "#5F5BA8"
            has("world cup art") -> "#8781BE"
            has("mappers' guild") -> "#FF9800"
            has("exemplary") -> "#12B4B1"
            has("mapper's choice") -> "#007130"
            Regex(".*(winn(er|ing)|1st|victor(y|ious)|champion).*").containsMatchIn(name) -> "#FFF100"
            Regex(".*((seco|2)nd|top\\s?2).*").containsMatchIn(name) -> "#CCC"
            Regex(".*((thi|3)rd|top\\s?3).*").containsMatchIn(name) -> "#AC6A00"
            Regex(".*([otmc]wc)|(osu!(catch|mania|taiko)? world cup).*").containsMatchIn(name) -> "#666"
            has("mapping") -> "#00A0E9"
            has("project loved") -> "#EA68A2"
            has("beatmap nominator") -> "#9922EE"
            has("nomination assessment team") -> "#D32F2F"
            has("quality assurance team") -> "#FF6100"
            has("global moderation team") -> "#22AC38"
            has("community mentorship program") -> "#0068B7"
            has("beatmap spotlights") -> "#593350"
            has("wiki") -> "#B3D465"
            has("centurion") -> "#FFF899"
            has("top") -> "#7ECEF4"
            else -> "#1c1719"
        }
    }
}
