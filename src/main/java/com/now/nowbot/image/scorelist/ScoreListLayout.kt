package com.now.nowbot.image.scorelist

import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.TimeFormat
import kotlin.math.ceil

internal object ScoreListLayout {
    const val MARK_INDEX = "<g id=\"Index\">"
    const val MARK_ME = "<g id=\"Me_Card_A1\">"
    const val MARK_LIST = "<g id=\"List_Card_H\">"
    const val MARK_MAIN = "<g id=\"MainCard\">"
    const val MARK_BANNER_A4 = "<g style=\"clip-path: url(#clippath-PA4-1);\">"
    const val MARK_BANNER_C = "<g style=\"clip-path: url(#clippath-PC-1);\">"

    private const val PLACEHOLDER_PANEL_HEIGHT = "\${panelheight}"
    private const val PLACEHOLDER_CARD_HEIGHT = "\${cardheight}"
    private const val VERSION = "v0.8.5"
    private const val VERSION_CODE = "VS"
    private const val POWERED_BY = "Yumubot"

    fun panelA4(panelHeight: Int, cardHeight: Int): String {
        return template("Panel_A4.svg", panelHeight, cardHeight, hasBodyHeight = false)
    }

    fun panelC(panelHeight: Int, cardHeight: Int): String {
        return template("Panel_C.svg", panelHeight, cardHeight, hasBodyHeight = true)
    }

    fun panelHeight(
        cardCount: Int,
        cardHeight: Int = 110,
        cardPerRow: Int = 2,
        bannerHeight: Int = 290,
        interval: Int = 40,
        spacing: Int = 40,
    ): Int {
        if (cardPerRow == 0) return bannerHeight + spacing
        val row = ceil(cardCount.toDouble() / cardPerRow).toInt()
        return (bannerHeight + 2 * spacing) + (cardHeight + interval) * row - interval
    }

    fun panelName(name: String, sign: String, requestTime: String = "request time: " + TimeFormat.nowTimestamp()): String {
        val powered = Fonts.torus.textPath(
            "powered by $POWERED_BY $VERSION $VERSION_CODE // $name",
            20.0, 26.84, 24.0, "left baseline", "#fff",
        )
        val request = Fonts.torus.textPath(requestTime, 1900.0, 26.84, 24.0, "right baseline", "#fff")
        val index = Fonts.torus.textPath(sign, 607.5, 83.67, 48.0, "center baseline", "#fff")
        return powered + request + index
    }

    fun bannerSvg(image: String?): String {
        return image?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) } ?: ""
    }

    fun cardAt(x: Double, y: Double, card: String): String = Svg.group(x, y, card)

    fun rankAt(ranks: List<Int>, index: Int): Int = ranks.getOrNull(index) ?: 1

    private fun template(name: String, panelHeight: Int, cardHeight: Int, hasBodyHeight: Boolean): String {
        var svg = Svg.removeXmlDeclaration(Templates.read(name))
        svg = svg.replace(PLACEHOLDER_PANEL_HEIGHT, panelHeight.toString())
        svg = svg.replace(PLACEHOLDER_CARD_HEIGHT, cardHeight.toString())
        if (hasBodyHeight) {
            svg = svg.replace("\${height}", panelHeight.toString())
        }
        return svg
    }
}
