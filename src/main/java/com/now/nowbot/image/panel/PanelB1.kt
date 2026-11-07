package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.card.CardB6
import com.now.nowbot.image.card.CardB6Data
import com.now.nowbot.image.card.CardB7
import com.now.nowbot.image.card.CardB7Data
import com.now.nowbot.image.component.labels.LabelPPM6
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.draw.hexagonChart
import com.now.nowbot.image.draw.hexagonIndex
import com.now.nowbot.image.draw.randomBanner
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.GameModes
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.Rankings
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.ppminus.PPMinus
import com.now.nowbot.model.ppminus.PPMinus4
import kotlin.math.pow

data class PPMinusView(
    val acc: Double = 0.0,
    val ptt: Double = 0.0,
    val sta: Double = 0.0,
    val stb: Double = 0.0,
    val pre: Double = 0.0,
    val eft: Double = 0.0,
    val sth: Double = 0.0,
    val ova: Double = 0.0,
    val san: Double = 0.0,
) {
    fun value(key: String): Double? {
        return when (key) {
            "ACC" -> acc
            "PTT" -> ptt
            "STA" -> sta
            "STB" -> stb
            "PRE" -> pre
            "EFT" -> eft
            "STH" -> sth
            "OVA" -> ova
            "SAN" -> san
            else -> null
        }
    }

    companion object {
        fun from(map: Map<*, *>?): PPMinusView {
            if (map == null) return PPMinusView()
            fun number(key: String): Double = (map[key] as? Number)?.toDouble() ?: 0.0
            return PPMinusView(
                acc = number("ACC"),
                ptt = number("PTT"),
                sta = number("STA"),
                stb = number("STB"),
                pre = number("PRE"),
                eft = number("EFT"),
                sth = number("STH"),
                ova = number("OVA"),
                san = number("SAN"),
            )
        }

        fun from(minus: PPMinus): PPMinusView {
            return PPMinusView(
                acc = minus.getValue1(),
                ptt = minus.getValue2(),
                sta = minus.getValue3(),
                stb = minus.getValue4(),
                pre = minus.getValue4(),
                eft = minus.getValue5(),
                sth = minus.getValue6(),
                ova = minus.getValue7(),
                san = minus.getValue8(),
            )
        }

        fun from(minus: PPMinus4): PPMinusView {
            val values = minus.values
            fun value(index: Int): Double = values.getOrElse(index) { 0.0 }
            return PPMinusView(
                acc = value(0),
                ptt = value(1),
                sta = value(2),
                stb = value(3),
                pre = value(3),
                eft = value(4),
                sth = value(5),
                ova = value(6),
                san = value(7),
            )
        }
    }
}

data class PanelB1Data(
    val users: List<OsuUser> = emptyList(),
    val my: PPMinusView = PPMinusView(),
    val others: PPMinusView? = null,
    val isVs: Boolean = false,
    val modeInt: Int = 0,
    val panel: String = "",
    val count: Int = 0,
    val delta: Int = 0,
    val myOii: Double = 0.0,
) {
    companion object {
        fun fromMap(map: Map<*, *>): PanelB1Data {
            val users = (map["users"] as? List<*>)?.filterIsInstance<OsuUser>() ?: emptyList()
            val stat = map["stat"] as? Map<*, *>
            val others = (map["others"] as? Map<*, *>)?.let { PPMinusView.from(it) }
            return PanelB1Data(
                users = users,
                my = PPMinusView.from(map["my"] as? Map<*, *>),
                others = others,
                isVs = stat?.get("is_vs") as? Boolean ?: false,
                modeInt = (stat?.get("mode_int") as? Number)?.toInt() ?: 0,
                panel = map["panel"] as? String ?: "",
                count = (map["count"] as? Number)?.toInt() ?: 0,
                delta = (map["delta"] as? Number)?.toInt() ?: 0,
                myOii = (map["my_oii"] as? Number)?.toDouble() ?: 0.0,
            )
        }
    }
}

object PanelB1 {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PB-1);\">"
    private const val MARK_LEFT = "<g id=\"Left\">"
    private const val MARK_RIGHT = "<g id=\"Right\">"
    private const val MARK_CENTER = "<g id=\"Center\">"
    private const val MARK_MAIN = "<g id=\"MainCard\">"
    private const val MARK_HEXAGON = "<g id=\"HexagonChart\">"

    fun render(data: PanelB1Data, assets: ImageAssets): String = toSvg(data, assets)

    fun toSvg(data: PanelB1Data, assets: ImageAssets): String {
        var svg = Svg.removeXmlDeclaration(Templates.read("Panel_B.svg"))

        val isPm4 = data.panel == "PM4"
        val boundary = listOf(if (isPm4) 100.8 else 120.0, 100.0, 95.0, 90.0, 80.0, 70.0, 60.0, 10.0)
        val sanityBoundary = listOf(if (isPm4) 100.8 else 120.0, 100.0, 95.0, 90.0, 80.0, 70.0, 60.0, 0.0)
        val sanityRanks = listOf("?", "++", "+", "-", "--", "!?", "!", "!!", "X")
        val oiiBoundary = listOf(0.0, 0.5, 1.0, 1.5, 2.0, 3.0, 4.0, 5.0).reversed()

        val isVs = data.isVs
        val mode = GameModes.getGameMode(data.modeInt, 0)
        val order = if (mode == "mania") {
            listOf("ACC", "PTT", "STA", "PRE", "EFT", "STH")
        } else {
            listOf("ACC", "PTT", "STA", "STB", "EFT", "STH")
        }

        val gameModePath = Fonts.torus.textPath(mode, 960.0, 614.0, 60.0, "center baseline", "#fff")
        svg = Svg.setText(svg, PanelDraw.hexagonIndex(order, 960.0, 600.0, 260.0, 0.0), MARK_HEXAGON)
        svg = Svg.setText(svg, assets.randomBanner()?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) }, MARK_BANNER)

        val panelName = if (isPm4) {
            val requestTime = "data count: ${data.count}x // range: +-${data.delta}PP // request time: ${TimeFormat.nowTimestamp()}"
            bPanelName("PP Minus v4.5 (!ympm/!ympv)", "PM", requestTime)
        } else {
            bPanelName("PP Minus v2.5 Legacy (!ympl)", "PL")
        }
        svg = Svg.setTexts(svg, listOf(panelName, gameModePath), MARK_INDEX)

        val my = data.my
        val others = data.others ?: PPMinusView()
        val values = order.map { my.value(it) }
        val vsValues = order.map { others.value(it) }

        var scaleLeft = 1.0
        var scaleRight = 1.0
        if (isVs) {
            val mePp = data.users.getOrNull(0)?.pp ?: 0.0
            val otherPp = data.users.getOrNull(1)?.pp ?: 0.0
            val smoothing = 0.6
            if (mePp >= otherPp && mePp > 0) {
                scaleRight = (otherPp / mePp).pow(smoothing)
            } else if (otherPp > 0) {
                scaleLeft = (mePp / otherPp).pow(smoothing)
            }
        }

        val numberLeft = mutableListOf<Double>()
        val leftCards = mutableListOf<String>()
        for (index in 0 until 6) {
            val abbr = order[index]
            val value = (values[index] ?: 0.0) * 100
            val rank = Rankings.getRankFromValue(value, boundary)
            val card = CardB6.render(
                CardB6Data(
                    label = LabelPPM6.of(abbr),
                    value = value,
                    delta = if (isVs) value - (vsValues[index] ?: 0.0) * 100 else null,
                    iconColors = Colors.getRankColors(rank),
                    roundLevel = 1,
                    max = if (isPm4) 101.0 else 120.0,
                ),
                assets,
                false,
            )
            leftCards += Svg.group(40.0, 340.0 + index * 115, card)
            numberLeft += Numbers.clamp((value / 100 * scaleLeft - 0.6) / 4 * 10, 1.0, 0.01)
        }

        svg = Svg.setText(svg, PanelDraw.hexagonChart(numberLeft, 960.0, 600.0, 230.0, "#00A8EC"), MARK_HEXAGON)
        svg = Svg.setText(svg, leftCards.joinToString("\n"), MARK_LEFT)
        svg = Svg.setText(
            svg,
            Svg.group(40.0, 40.0, CardA1.render(data.users.getOrNull(0), null, assets)),
            MARK_MAIN,
        )

        val centerCards = mutableListOf<String>()
        if (isVs) {
            val rightCards = mutableListOf<String>()
            val numberRight = mutableListOf<Double>()
            for (index in 0 until 6) {
                val abbr = order[index]
                val value = (vsValues[index] ?: 0.0) * 100
                val rank = Rankings.getRankFromValue(value, boundary)
                val card = CardB6.render(
                    CardB6Data(
                        label = LabelPPM6.of(abbr),
                        value = value,
                        delta = value - (values[index] ?: 0.0) * 100,
                        iconColors = Colors.getRankColors(rank),
                        roundLevel = 1,
                        max = if (isPm4) 101.0 else 120.0,
                    ),
                    assets,
                    true,
                )
                rightCards += Svg.group(1350.0, 340.0 + index * 115, card)
                numberRight += Numbers.clamp((value / 100 * scaleRight - 0.6) / 4 * 10, 1.0, 0.01)
            }
            svg = Svg.setText(svg, PanelDraw.hexagonChart(numberRight, 960.0, 600.0, 230.0, "#FF0000"), MARK_HEXAGON)
            svg = Svg.setText(svg, rightCards.joinToString("\n"), MARK_RIGHT)
            svg = Svg.setText(
                svg,
                Svg.group(1450.0, 40.0, CardA1.render(data.users.getOrNull(1), null, assets)),
                MARK_MAIN,
            )

            centerCards += CardB7.render(
                CardB7Data(
                    label = LabelPPM6.OVA,
                    value = my.ova * 100,
                    iconColors = Colors.getRankColors(Rankings.getRankFromValue(my.ova * 100, boundary)),
                    roundLevel = 1,
                    max = if (isPm4) 101.0 else 120.0,
                ),
                assets,
                false,
            )
            centerCards += CardB7.render(
                CardB7Data(
                    label = LabelPPM6.OVA,
                    value = others.ova * 100,
                    iconColors = Colors.getRankColors(Rankings.getRankFromValue(others.ova * 100, boundary)),
                    roundLevel = 1,
                    max = if (isPm4) 101.0 else 120.0,
                ),
                assets,
                true,
            )
        } else {
            centerCards += CardB7.render(
                CardB7Data(
                    label = LabelPPM6.OVA,
                    value = my.ova * 100,
                    iconColors = Colors.getRankColors(Rankings.getRankFromValue(my.ova * 100, boundary)),
                    roundLevel = 1,
                    max = if (isPm4) 101.0 else 120.0,
                ),
                assets,
                false,
            )
            if (isPm4) {
                val oii = data.myOii
                centerCards += CardB7.render(
                    CardB7Data(
                        label = LabelPPM6.OII,
                        value = oii,
                        iconColors = Colors.getRankColors(Rankings.getRankFromValue(oii, oiiBoundary)),
                        roundLevel = 1,
                    ),
                    assets,
                    true,
                )
            } else {
                val sanValue = my.san * 100
                centerCards += CardB7.render(
                    CardB7Data(
                        label = LabelPPM6.SAN,
                        value = sanValue,
                        dataB = Rankings.getRankFromValue(sanValue, sanityBoundary, sanityRanks),
                        dataM = "",
                        iconColors = Colors.getRankColors(Rankings.getRankFromValue(sanValue, sanityBoundary)),
                        roundLevel = 0,
                    ),
                    assets,
                    true,
                )
            }
        }

        svg = Svg.setText(svg, Svg.group(630.0, 890.0, centerCards[0]), MARK_CENTER)
        svg = Svg.setText(svg, Svg.group(970.0, 890.0, centerCards[1]), MARK_CENTER)
        svg = Svg.setText(svg, assets.local("object-hexagon.png")?.let { Svg.image(718.0, 384.0, 484.0, 433.0, it) }, MARK_HEXAGON)
        return svg
    }
}
