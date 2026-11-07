package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.card.CardA2
import com.now.nowbot.image.card.CardB6
import com.now.nowbot.image.card.CardB6Data
import com.now.nowbot.image.card.CardB7
import com.now.nowbot.image.card.CardB7Data
import com.now.nowbot.image.component.labels.LabelPPP
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
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.image.util.duration
import com.now.nowbot.model.calculate.PPPlus
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.OsuUser
import kotlin.math.PI
import kotlin.math.pow

data class PanelB3Data(
    val isUser: Boolean = false,
    val isVs: Boolean = false,
    val meUser: OsuUser? = null,
    val meBeatmap: Beatmap? = null,
    val my: PPPlus? = null,
    val otherUser: OsuUser? = null,
    val otherBeatmap: Beatmap? = null,
    val others: PPPlus? = null,
) {
    companion object {
        fun fromMap(map: Map<*, *>): PanelB3Data {
            val me = map["me"]
            val other = map["other"]
            return PanelB3Data(
                isUser = map["isUser"] as? Boolean ?: false,
                isVs = map["isVs"] as? Boolean ?: false,
                meUser = me as? OsuUser,
                meBeatmap = me as? Beatmap,
                my = map["my"] as? PPPlus,
                otherUser = other as? OsuUser,
                otherBeatmap = other as? Beatmap,
                others = map["others"] as? PPPlus,
            )
        }
    }
}

object PanelB3 {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PB-1);\">"
    private const val MARK_CENTER = "<g id=\"Center\">"
    private const val MARK_MAIN = "<g id=\"MainCard\">"
    private const val MARK_HEXAGON = "<g id=\"HexagonChart\">"

    private val ABBR = listOf("JUMP", "FLOW", "ACC", "STA", "SPD", "PRE")
    private val ORDER = listOf("jumpAim", "flowAim", "accuracy", "stamina", "speed", "precision", "aim", "total")
    private val LV_BOUNDARY = listOf(11.0, 9.0, 7.0, 5.0, 3.0, 1.0, 0.75, 0.25)
    private val GRAPH_MAX = listOf(5800.0, 1400.0, 3200.0, 2800.0, 3800.0, 1200.0)

    fun render(data: PanelB3Data, assets: ImageAssets): String = toSvg(data, assets)

    fun toSvg(data: PanelB3Data, assets: ImageAssets): String {
        var svg = Svg.removeXmlDeclaration(Templates.read("Panel_B.svg"))

        val isUser = data.isUser
        val isVs = data.isVs
        val my = data.my
        val others = data.others

        val leftCards = mutableListOf<String>()
        val rightCards = mutableListOf<String>()
        val centerCards = mutableListOf<String>()
        var graphLeft: List<Double> = emptyList()
        var graphRight: List<Double> = emptyList()
        var type: String
        var panelName: String

        svg = Svg.setText(svg, PanelDraw.hexagonIndex(ABBR, 960.0, 600.0, 260.0, PI / 3), MARK_HEXAGON)

        if (isUser) {
            val me = data.meUser
            val banner = userBanner(assets, me)
            val levels = my?.advancedStats?.index ?: emptyList()
            val vsLevels = others?.advancedStats?.index ?: emptyList()
            val values = ORDER.map { stat(my?.performance, it) }
            val vsValues = ORDER.map { stat(others?.performance, it) }

            graphLeft = ORDER.mapIndexed { index, key ->
                ((stat(my?.performance, key) ?: 0.0) / GRAPH_MAX[index]).pow(0.8)
            }

            if (isVs) {
                graphRight = ORDER.mapIndexed { index, key ->
                    ((stat(others?.performance, key) ?: 0.0) / GRAPH_MAX[index]).pow(0.8)
                }
                type = "PX+"
                panelName = bPanelName("PP Plus: User VS (!ympx)", "PX")

                val cardLeft = CardA1.render(me, null, assets)
                val cardRight = CardA1.render(data.otherUser, null, assets)
                drawUserPlus(leftCards, values, vsValues, levels, false, assets)
                drawUserPlus(rightCards, vsValues, values, vsLevels, true, assets)

                val valueO1 = my?.performance?.total ?: 11.4514
                val levelO1 = my?.advancedStats?.advanced ?: 0.0
                centerCards += CardB7.render(
                    CardB7Data(
                        label = LabelPPP.OVA,
                        value = valueO1,
                        dataB = getRoman(levelO1),
                        dataM = "",
                        iconColors = Colors.getRankColors(Rankings.getRankFromValue(levelO1, LV_BOUNDARY)),
                        roundLevel = -6,
                    ),
                    assets,
                    false,
                )

                val valueO2 = others?.performance?.total ?: 11.4514
                val levelO2 = others?.advancedStats?.advanced ?: 0.0
                centerCards += CardB7.render(
                    CardB7Data(
                        label = LabelPPP.OVA,
                        value = valueO2,
                        dataB = getRoman(levelO2),
                        dataM = "",
                        iconColors = Colors.getRankColors(Rankings.getRankFromValue(levelO2, LV_BOUNDARY)),
                        roundLevel = -6,
                    ),
                    assets,
                    true,
                )
                svg = renderMain(svg, cardLeft, cardRight)
            } else {
                type = "PP+"
                val requestTime = if ((my?.combo ?: 0) > 0) {
                    "compare to: ${TimeFormat.duration((my?.combo ?: 0) * 60 * 60.0)} // request time: ${TimeFormat.nowTimestamp()}"
                } else {
                    "request time: ${TimeFormat.nowTimestamp()}"
                }
                panelName = bPanelName("PP Plus: User (!ympp)", "PP", requestTime)

                val beforeValues = ORDER.map { stat(others?.difficulty, it) }
                drawUserPlus(leftCards, values, beforeValues, levels, false, assets)
                svg = renderMain(svg, CardA1.render(me, null, assets), "")

                val valueO1 = my?.performance?.aim ?: 11.4514
                centerCards += CardB7.render(
                    CardB7Data(
                        label = LabelPPP.AIM,
                        value = valueO1,
                        iconColors = Colors.getRankColors(
                            Rankings.getRankFromValue(valueO1, listOf(6900.0, 4900.0, 3800.0, 3075.0, 2525.0, 1975.0, 1700.0, 1300.0)),
                        ),
                        roundLevel = -6,
                    ),
                    assets,
                    false,
                )

                val valueO2 = my?.performance?.total ?: 11.4514
                val levelO2 = my?.advancedStats?.advanced ?: 0.0
                centerCards += CardB7.render(
                    CardB7Data(
                        label = LabelPPP.OVA,
                        value = valueO2,
                        iconColors = Colors.getRankColors(Rankings.getRankFromValue(levelO2, LV_BOUNDARY)),
                        roundLevel = -6,
                    ),
                    assets,
                    true,
                )
            }
            svg = Svg.setText(
                svg,
                banner?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) },
                MARK_BANNER,
            )
        } else {
            graphLeft = ORDER.map { key ->
                ((stat(my?.difficulty, key) ?: 0.0) / 5).pow(0.8)
            }
            val values = ORDER.map { stat(my?.difficulty, it) }
            val vsValues = ORDER.map { stat(others?.difficulty, it) }
            val banner = assets.mapBackground(data.meBeatmap, "list@2x")

            if (isVs) {
                graphRight = ORDER.map { key ->
                    ((stat(others?.difficulty, key) ?: 0.0) / 5).pow(0.8)
                }
                type = "PA+"
                panelName = bPanelName("PP Plus: BeatMap (!ympa)", "PA")
                drawMapPlus(leftCards, values, vsValues, false, assets)
                drawMapPlus(rightCards, vsValues, values, true, assets)
                svg = renderMain(
                    svg,
                    CardA2.render(CardA2.fromBeatmap(data.meBeatmap ?: Beatmap(), assets), assets),
                    CardA2.render(CardA2.fromBeatmap(data.otherBeatmap ?: Beatmap(), assets), assets),
                )

                val valueO1 = my?.difficulty?.total ?: 0.0
                centerCards += CardB7.render(
                    CardB7Data(
                        label = LabelPPP.OVA,
                        value = valueO1,
                        limit = 10.0,
                        max = 15.0,
                        iconColors = Colors.getRankColors(Rankings.getRankFromValue(valueO1)),
                        roundLevel = 2,
                    ),
                    assets,
                    false,
                )
                val valueO2 = others?.difficulty?.total ?: 0.0
                centerCards += CardB7.render(
                    CardB7Data(
                        label = LabelPPP.OVA,
                        value = valueO2,
                        limit = 10.0,
                        max = 15.0,
                        iconColors = Colors.getRankColors(Rankings.getRankFromValue(valueO2)),
                        roundLevel = 2,
                    ),
                    assets,
                    true,
                )
            } else {
                type = "PA+"
                panelName = bPanelName("PP Plus: BeatMap (!ympa)", "PA")
                drawMapPlus(leftCards, values, emptyList(), false, assets)
                svg = renderMain(
                    svg,
                    CardA2.render(CardA2.fromBeatmap(data.meBeatmap ?: Beatmap(), assets), assets),
                    "",
                )

                val valueO1 = my?.difficulty?.aim ?: 0.0
                centerCards += CardB7.render(
                    CardB7Data(
                        label = LabelPPP.AIM,
                        value = valueO1,
                        limit = 8.0,
                        max = 12.0,
                        iconColors = Colors.getRankColors(
                            Rankings.getRankFromValue(valueO1, listOf(7.0, 6.0, 5.0, 4.5, 4.0, 3.0, 2.0, 1.0)),
                        ),
                        roundLevel = 2,
                    ),
                    assets,
                    false,
                )
                val valueO2 = my?.difficulty?.total ?: 0.0
                centerCards += CardB7.render(
                    CardB7Data(
                        label = LabelPPP.OVA,
                        value = valueO2,
                        limit = 10.0,
                        max = 15.0,
                        iconColors = Colors.getRankColors(Rankings.getRankFromValue(valueO2)),
                        roundLevel = 2,
                    ),
                    assets,
                    true,
                )
            }
            svg = Svg.setText(
                svg,
                banner?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) },
                MARK_BANNER,
            )
        }

        val typePath = Fonts.poppinsBold.textPath(type, 960.0, 614.0, 60.0, "center baseline", "#fff")
        svg = Svg.setTexts(svg, listOf(panelName, typePath), MARK_INDEX)

        val leftPart = leftCards.mapIndexed { index, card ->
            Svg.group(40.0, 340.0 + index * 115, card)
        }
        val rightPart = rightCards.mapIndexed { index, card ->
            Svg.group(1350.0, 340.0 + index * 115, card)
        }
        val center = buildList {
            addAll(leftPart)
            addAll(rightPart)
            addAll(centerCards)
        }
        svg = Svg.setText(svg, center.joinToString("\n"), MARK_CENTER)

        if (graphLeft.isNotEmpty()) {
            svg = Svg.setText(
                svg,
                PanelDraw.hexagonChart(graphLeft, 960.0, 600.0, 230.0, "#00A8EC", PI / 3),
                MARK_HEXAGON,
            )
        }
        if (graphRight.isNotEmpty()) {
            svg = Svg.setText(
                svg,
                PanelDraw.hexagonChart(graphRight, 960.0, 600.0, 230.0, "#FF0000", PI / 3),
                MARK_HEXAGON,
            )
        }
        svg = Svg.setText(
            svg,
            assets.local("object-hexagon.png")?.let { Svg.image(718.0, 384.0, 484.0, 433.0, it) },
            MARK_HEXAGON,
        )
        return svg
    }

    private fun renderMain(svg: String, left: String, right: String): String {
        return Svg.setTexts(svg, listOf(Svg.group(40.0, 40.0, left), Svg.group(1450.0, 40.0, right)), MARK_MAIN)
    }

    private fun drawMapPlus(
        svgs: MutableList<String>,
        values: List<Double?>,
        vsValues: List<Double?>,
        atRight: Boolean,
        assets: ImageAssets,
    ) {
        for (index in 0 until 6) {
            val value = values[index]
            val vsValue = vsValues.getOrNull(index)
            val rank = Rankings.getRankFromValue(value ?: 0.0, listOf(4.0, 3.5, 3.0, 2.5, 2.0, 1.5, 1.0, 0.5))
            if (value == null) continue
            svgs += CardB6.render(
                CardB6Data(
                    label = LabelPPP.of(ABBR[index]),
                    value = value,
                    delta = if (vsValue == null) null else value - vsValue,
                    iconColors = Colors.getRankColors(rank),
                    roundLevel = 2,
                    limit = 6.0,
                    max = 10.0,
                ),
                assets,
                atRight,
            )
        }
    }

    private fun drawUserPlus(
        svgs: MutableList<String>,
        values: List<Double?>,
        vsValues: List<Double?>,
        levels: List<Double>,
        atRight: Boolean,
        assets: ImageAssets,
    ) {
        val boundary = listOf(11.0, 9.0, 7.0, 5.0, 3.0, 1.0, 0.75, 0.25)
        for (index in 0 until 6) {
            val value = values[index]
            val vsValue = vsValues.getOrNull(index)
            val level = levels.getOrNull(index)
            val rank = getRoman(level)
            val fakeRank = Rankings.getRankFromValue(level ?: 0.0, boundary)
            if (value == null) continue
            svgs += CardB6.render(
                CardB6Data(
                    label = LabelPPP.of(ABBR[index]),
                    value = value,
                    dataB = rank,
                    dataM = "",
                    delta = if (vsValue == null) null else value - vsValue,
                    deltaTop = value,
                    iconColors = Colors.getRankColors(fakeRank),
                    roundLevel = -6,
                ),
                assets,
                atRight,
            )
        }
    }

    private fun getRoman(level: Double?): String {
        if (level == null) return "-"
        val roman = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")
        if (level <= 0) return "-"
        if (level < 1) {
            if (level < 0.25) return "-III"
            if (level < 0.75) return "-II"
            return "-I"
        }
        if (level < 11) return roman[Numbers.jsRound(level - 1).toInt()]
        return "EX"
    }

    private fun stat(stats: PPPlus.Stats?, key: String): Double? {
        if (stats == null) return null
        return when (key) {
            "aim" -> stats.aim
            "jumpAim" -> stats.jumpAim
            "flowAim" -> stats.flowAim
            "precision" -> stats.precision
            "speed" -> stats.speed
            "stamina" -> stats.stamina
            "accuracy" -> stats.accuracy
            "total" -> stats.total
            else -> null
        }
    }

    private fun userBanner(assets: ImageAssets, user: OsuUser?): String? {
        val url = user?.coverUrl?.takeIf { it.isNotBlank() }
            ?: user?.cover?.url?.takeIf { it.isNotBlank() }
            ?: ""
        return assets.remote(url) ?: assets.localBanner()
    }
}
