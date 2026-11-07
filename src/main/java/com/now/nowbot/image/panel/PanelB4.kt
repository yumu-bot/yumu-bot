package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.card.CardB6
import com.now.nowbot.image.card.CardB6Data
import com.now.nowbot.image.card.CardB7
import com.now.nowbot.image.card.CardB7Data
import com.now.nowbot.image.component.labels.LabelETX
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.draw.hexagonChart
import com.now.nowbot.image.draw.hexagonIndex
import com.now.nowbot.image.draw.randomBanner
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.ColorArray
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.Rankings
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.model.calculate.ETXDuelRating
import com.now.nowbot.service.messageServiceImpl.EliteronixDuelRatingService.ETXParam

object PanelB4 {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PB-1);\">"
    private const val MARK_LEFT = "<g id=\"Left\">"
    private const val MARK_RIGHT = "<g id=\"Right\">"
    private const val MARK_CENTER = "<g id=\"Center\">"
    private const val MARK_MAIN = "<g id=\"MainCard\">"
    private const val MARK_HEXAGON = "<g id=\"HexagonChart\">"

    private val ORDER = listOf("SR", "NM", "HD", "HR", "DT", "FM")
    private val BOUNDARY = listOf(10.0, 8.0, 6.0, 5.5, 5.0, 4.5, 4.0, 0.0)

    fun render(param: ETXParam, assets: ImageAssets): String = toSvg(param, assets)

    fun toSvg(param: ETXParam, assets: ImageAssets): String {
        var svg = Svg.removeXmlDeclaration(Templates.read("Panel_B.svg"))

        val isVs = param.other != null
        val my = param.my
        val others = param.others

        val gameModePath = Fonts.torus.textPath("ETX", 960.0, 614.0, 60.0, "center baseline", "#fff")

        svg = Svg.setText(svg, PanelDraw.hexagonIndex(ORDER, 960.0, 600.0, 260.0, 0.0), MARK_HEXAGON)
        svg = Svg.setText(svg, assets.randomBanner()?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) }, MARK_BANNER)

        val myTime = "${TimeFormat.formatted(my.updatedAt)} (${TimeFormat.difference(my.updatedAt)})"
        val panelName = if (isVs) {
            val othersTime = "(${TimeFormat.difference(others?.updatedAt)})"
            val requestTime = "updated at: $myTime, $othersTime // request time: ${TimeFormat.nowTimestamp()}"
            bPanelName("ETX Duel Rating (!ymex/!ymev)", "EV", requestTime)
        } else {
            val requestTime = "updated at: $myTime // request time: ${TimeFormat.nowTimestamp()}"
            bPanelName("ETX Duel Rating VS (!ymex/!ymev)", "EX", requestTime)
        }
        svg = Svg.setTexts(svg, listOf(panelName, gameModePath), MARK_INDEX)

        val leftCards = mutableListOf<String>()
        val numberLeft = mutableListOf<Double>()
        val values = ORDER.map { ratingValue(my, it) }
        val vsValues = ORDER.map { ratingValue(others, it) }

        for (index in 0 until 6) {
            val value = values[index] ?: 0.0
            val rank = Rankings.getRankFromValue(value, BOUNDARY)
            leftCards += Svg.group(
                40.0,
                340.0 + index * 115,
                CardB6.render(
                    CardB6Data(
                        label = LabelETX.of(ORDER[index]),
                        value = value,
                        delta = if (isVs) value - (vsValues[index] ?: 0.0) else null,
                        iconColors = Colors.getRankColors(rank),
                        roundLevel = 2,
                    ),
                    assets,
                    false,
                ),
            )
            numberLeft += Numbers.clamp((value - 4) / 3.5, 1.0, 0.01)
        }
        svg = Svg.setText(svg, PanelDraw.hexagonChart(numberLeft, 960.0, 600.0, 230.0, "#00A8EC"), MARK_HEXAGON)
        svg = Svg.setText(svg, leftCards.joinToString("\n"), MARK_LEFT)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, CardA1.render(param.me, null, assets)), MARK_MAIN)

        val centerCards = mutableListOf<String>()
        if (isVs) {
            val rightCards = mutableListOf<String>()
            val numberRight = mutableListOf<Double>()
            for (index in 0 until 6) {
                val value = vsValues[index] ?: 0.0
                val rank = Rankings.getRankFromValue(value, BOUNDARY)
                rightCards += Svg.group(
                    1350.0,
                    340.0 + index * 115,
                    CardB6.render(
                        CardB6Data(
                            label = LabelETX.of(ORDER[index]),
                            value = value,
                            delta = value - (values[index] ?: 0.0),
                            iconColors = Colors.getRankColors(rank),
                            roundLevel = 1,
                        ),
                        assets,
                        true,
                    ),
                )
                numberRight += Numbers.clamp((value - 4) / 3.5, 1.0, 0.01)
            }
            svg = Svg.setText(svg, PanelDraw.hexagonChart(numberRight, 960.0, 600.0, 230.0, "#FF0000"), MARK_HEXAGON)
            svg = Svg.setText(svg, rightCards.joinToString("\n"), MARK_RIGHT)
            svg = Svg.setText(svg, Svg.group(1450.0, 40.0, CardA1.render(param.other, null, assets)), MARK_MAIN)

            val effectiveMe = !my.outdated
            centerCards += CardB7.render(
                CardB7Data(
                    label = LabelETX.EF,
                    value = if (effectiveMe) 1.0 else 0.0,
                    dataB = if (effectiveMe) "Y" else "N",
                    dataM = "",
                    iconColors = (if (effectiveMe) ColorArray.blue else ColorArray.deepBlue).reversed(),
                    colors = ColorArray.blue,
                ),
                assets,
                false,
            )

            val effectiveOther = !(others?.outdated ?: false)
            centerCards += CardB7.render(
                CardB7Data(
                    label = LabelETX.EF,
                    value = if (effectiveOther) 1.0 else 0.0,
                    dataB = if (effectiveOther) "Y" else "N",
                    dataM = "",
                    iconColors = (if (effectiveOther) ColorArray.red else ColorArray.deepRed).reversed(),
                    colors = ColorArray.red,
                ),
                assets,
                true,
            )
        } else {
            val effectiveMe = !my.outdated
            centerCards += CardB7.render(
                CardB7Data(
                    label = LabelETX.EF,
                    value = if (effectiveMe) 1.0 else 0.0,
                    dataB = if (effectiveMe) "Y" else "N",
                    dataM = "",
                    iconColors = (if (effectiveMe) ColorArray.blue else ColorArray.deepBlue).reversed(),
                    colors = ColorArray.blue,
                ),
                assets,
                false,
            )

            val permanentMe = !my.provisional
            centerCards += CardB7.render(
                CardB7Data(
                    label = LabelETX.PE,
                    value = if (permanentMe) 1.0 else 0.0,
                    dataB = if (permanentMe) "Y" else "N",
                    dataM = "",
                    iconColors = (if (permanentMe) ColorArray.red else ColorArray.deepRed).reversed(),
                    colors = ColorArray.red,
                ),
                assets,
                true,
            )
        }

        svg = Svg.setText(svg, Svg.group(630.0, 890.0, centerCards[0]), MARK_CENTER)
        svg = Svg.setText(svg, Svg.group(970.0, 890.0, centerCards[1]), MARK_CENTER)
        svg = Svg.setText(
            svg,
            assets.local("object-hexagon.png")?.let { Svg.image(718.0, 384.0, 484.0, 433.0, it) },
            MARK_HEXAGON,
        )
        return svg
    }

    private fun ratingValue(rating: ETXDuelRating?, abbr: String): Double? {
        if (rating == null) return null
        return when (abbr) {
            "SR" -> rating.star
            "NM" -> rating.noMod
            "HD" -> rating.hidden
            "HR" -> rating.hardRock
            "DT" -> rating.doubleTime
            "FM" -> rating.freeMod
            else -> null
        }
    }
}
