package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA2
import com.now.nowbot.image.card.CardB6
import com.now.nowbot.image.card.CardB6Data
import com.now.nowbot.image.card.CardB7
import com.now.nowbot.image.card.CardB7Data
import com.now.nowbot.image.component.labels.LabelMM
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.draw.hexagonChart
import com.now.nowbot.image.draw.hexagonIndex
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.Rankings
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.skill.DanResult
import com.now.nowbot.model.skill.Skill6
import kotlin.math.PI

data class PanelB2Data(
    val beatmap: Beatmap? = null,
    val mapMinus: Skill6? = null,
    val type: String = "",
    val typePercent: Double = 0.0,
) {
    companion object {
        fun fromMap(map: Map<*, *>): PanelB2Data {
            return PanelB2Data(
                beatmap = map["beatmap"] as? Beatmap,
                mapMinus = map["map_minus"] as? Skill6,
                type = map["type"] as? String ?: "",
                typePercent = (map["type_percent"] as? Number)?.toDouble() ?: 0.0,
            )
        }
    }
}

object PanelB2 {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PB-1);\">"
    private const val MARK_LEFT = "<g id=\"Left\">"
    private const val MARK_RIGHT = "<g id=\"Right\">"
    private const val MARK_CENTER = "<g id=\"Center\">"
    private const val MARK_MAIN = "<g id=\"MainCard\">"
    private const val MARK_HEXAGON = "<g id=\"HexagonChart\">"

    private val SHOW_ARR = listOf("RC", "LN", "CO", "PR", "SP", "ST")
    fun render(data: PanelB2Data, assets: ImageAssets): String = toSvg(data, assets)

    fun toSvg(data: PanelB2Data, assets: ImageAssets): String {
        var svg = Svg.removeXmlDeclaration(Templates.read("Panel_B.svg"))

        val mapMinus = data.mapMinus
        val skills = mapMinus?.skills ?: emptyList()
        val bases = mapMinus?.bases ?: emptyList()
        val abbreviates = mapMinus?.abbreviates ?: emptyList()

        fun maniaValue(abbr: String?): Double? {
            if (abbr == null) return null
            val index = when (abbr) {
                "RC" -> 0
                "ST" -> 1
                "SP" -> 2
                "LN" -> 3
                "CO" -> 4
                "PR" -> 5
                else -> return null
            }
            return skills.getOrNull(index)
        }

        svg = Svg.setText(svg, PanelDraw.hexagonIndex(SHOW_ARR, 960.0, 600.0, 260.0, PI / 3), MARK_HEXAGON)
        svg = Svg.setText(
            svg,
            assets.mapBackground(data.beatmap, "cover")?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) },
            MARK_BANNER,
        )

        val panelName = bPanelName("Map Minus v6.1 - Entering 'Firmament Castle \"Velier\"' ~ 0.6x \"Perfect Snap\" (!ymmm)", "MM")
        val total = mapMinus?.rating ?: 0.0
        val totalNumber = Numbers.floorParts(total, 2)
        val totalPath = Fonts.poppinsBold.get2SizeTextPath(
            totalNumber.integer, totalNumber.decimal, 60.0, 36.0, 960.0, 614.0, "center baseline", "#fff",
        )
        svg = Svg.setTexts(svg, listOf(panelName, totalPath), MARK_INDEX)

        val cardA2 = CardA2.render(CardA2.fromBeatmap(data.beatmap ?: Beatmap(), assets), assets)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, cardA2), MARK_MAIN)

        val cards = mutableListOf<String>()
        for (index in 0 until 6) {
            val abbr = abbreviates.getOrNull(index)
            val value = maniaValue(abbr)
            if (abbr == null || value == null) {
                cards += ""
                continue
            }
            val rank = Rankings.getRankFromValue(value)
            cards += Svg.group(
                40.0,
                340.0 + index * 115,
                CardB6.render(
                    CardB6Data(
                        label = LabelMM.of(abbr),
                        value = value,
                        iconColors = Colors.getRankColors(rank),
                        roundLevel = 2,
                    ),
                    assets,
                    false,
                ),
            )
        }

        val hexagons = SHOW_ARR.map { name -> maniaValue(name)?.div(9) ?: Double.NaN }
        svg = Svg.setText(svg, PanelDraw.hexagonChart(hexagons, 960.0, 600.0, 230.0, "#00A8EC", PI / 3), MARK_HEXAGON)
        svg = Svg.setText(svg, cards.joinToString("\n"), MARK_LEFT)

        val dan = mapMinus?.dan
        val reform = dan?.get("reform") as? DanResult
        val danRc = reform ?: dan?.get("regular") as? DanResult
        val danRcLabel = if (reform != null) LabelMM.RF else LabelMM.RG
        val rcLevel = danRc?.level
        val centerCards = mutableListOf<String>()
        centerCards += Svg.group(
            630.0,
            890.0,
            CardB7.render(
                CardB7Data(
                    label = danRcLabel,
                    value = rcLevel ?: 11.4514,
                    dataB = danRc?.grade ?: "",
                    dataM = "",
                    iconColors = Colors.getRankColors(Rankings.getRankFromValue(rcLevel ?: 0.0)),
                    roundLevel = 0,
                ),
                assets,
                false,
            ),
        )

        val underjoy = dan?.get("underjoy") as? DanResult
        val danLn = underjoy ?: dan?.get("ln") as? DanResult
        val danLnLabel = if (underjoy != null) LabelMM.UJ else LabelMM.JL
        val lnLevel = danLn?.level
        centerCards += Svg.group(
            970.0,
            890.0,
            CardB7.render(
                CardB7Data(
                    label = danLnLabel,
                    value = lnLevel ?: 11.4514,
                    dataB = danLn?.grade ?: "",
                    dataM = "",
                    iconColors = Colors.getRankColors(Rankings.getRankFromValue(lnLevel ?: 0.0)),
                    roundLevel = 0,
                ),
                assets,
                true,
            ),
        )
        svg = Svg.setText(svg, centerCards[0], MARK_CENTER)
        svg = Svg.setText(svg, centerCards[1], MARK_CENTER)

        val graphs = mapMinus?.graphs ?: emptyList()
        val charts = CHART_CAPTIONS.indices.map { index ->
            drawChart(
                series = graphs.map { it.getOrElse(index) { 0.0 } },
                base = bases.getOrNull(index),
                x = CHART_X[index],
                y = CHART_Y[index],
                color = CHART_COLORS[index],
                caption = CHART_CAPTIONS[index],
            )
        }
        svg = Svg.setTexts(svg, charts, MARK_RIGHT)

        svg = Svg.setText(
            svg,
            assets.local("object-hexagon.png")?.let { Svg.image(718.0, 384.0, 484.0, 433.0, it) },
            MARK_HEXAGON,
        )
        return svg
    }

    private val CHART_CAPTIONS = listOf("S", "B", "J", "F", "T", "U", "R", "E", "V", "H", "O", "G", "Y")
    private val CHART_X = listOf(0.0, 170.0, 340.0, 0.0, 0.0, 170.0, 0.0, 170.0, 340.0, 0.0, 170.0, 0.0, 170.0)
    private val CHART_Y = listOf(0.0, 0.0, 0.0, 115.0, 230.0, 230.0, 345.0, 345.0, 345.0, 460.0, 460.0, 575.0, 575.0)
    private val CHART_COLORS = listOf(
        "#39B449", "#8DC73D", "#C2D92F", "#920783", "#D32F2F", "#EB6877", "#00A8EC",
        "#0071BC", "#0054A6", "#FFF100", "#EFC72A", "#FF9800", "#EB6100",
    )

    private fun drawChart(
        series: List<Double>,
        base: Double?,
        x: Double,
        y: Double,
        color: String,
        caption: String,
    ): String {
        return PanelDraw.lineChart(
            series, 0.0, 0.0, 1370.0 + x, 445.0 + y, 150.0, 95.0, color, 0.7, 0.2, 3.0,
        ) + Fonts.poppinsBold.textPath(
            caption + ": " + Numbers.floor(base, 1), 75.0 + 1370.0 + x, -35.0 + 445.0 + y, 24.0, "center baseline", "#fff",
        )
    }
}
