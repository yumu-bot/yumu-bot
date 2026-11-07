package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.card.CardB6
import com.now.nowbot.image.card.CardB6Data
import com.now.nowbot.image.card.CardB7
import com.now.nowbot.image.card.CardB7Data
import com.now.nowbot.image.card.CardK
import com.now.nowbot.image.component.labels.LabelMM
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.hexagonChart
import com.now.nowbot.image.draw.hexagonIndex
import com.now.nowbot.image.draw.randomBanner
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.Rankings
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.skill.DanResult
import com.now.nowbot.service.messageServiceImpl.SkillService.SkillScore
import kotlin.math.PI

data class PanelKData(
    val user: OsuUser? = null,
    val skill: List<Double> = emptyList(),
    val abbreviates: List<String>? = null,
    val scores: List<SkillScore> = emptyList(),
    val total: Double = 0.0,
    val dan: Map<String, Any> = emptyMap(),
    val vsUser: OsuUser? = null,
    val vsSkill: List<Double> = emptyList(),
    val vsAbbreviates: List<String>? = null,
    val vsTotal: Double = 0.0,
    val vsDan: Map<String, Any> = emptyMap(),
    val panel: String = "KV",
) {
    companion object {
        fun fromMap(map: Map<*, *>): PanelKData {
            return PanelKData(
                user = map["user"] as? OsuUser,
                skill = numbers(map["skill"]),
                abbreviates = strings(map["abbreviates"]),
                scores = (map["scores"] as? List<*>)?.filterIsInstance<SkillScore>() ?: emptyList(),
                total = (map["total"] as? Number)?.toDouble() ?: 0.0,
                dan = anyMap(map["dan"]),
                vsUser = map["vs_user"] as? OsuUser,
                vsSkill = numbers(map["vs_skill"]),
                vsAbbreviates = strings(map["vs_abbreviates"]),
                vsTotal = (map["vs_total"] as? Number)?.toDouble() ?: 0.0,
                vsDan = anyMap(map["vs_dan"]),
                panel = map["panel"] as? String ?: "KV",
            )
        }

        private fun numbers(value: Any?): List<Double> {
            return (value as? List<*>)?.mapNotNull { (it as? Number)?.toDouble() } ?: emptyList()
        }

        private fun strings(value: Any?): List<String>? {
            return (value as? List<*>)?.mapNotNull { it as? String }
        }

        private fun anyMap(value: Any?): Map<String, Any> {
            val map = value as? Map<*, *> ?: return emptyMap()
            return map.entries.mapNotNull { entry ->
                val key = entry.key as? String ?: return@mapNotNull null
                key to (entry.value ?: return@mapNotNull null)
            }.toMap()
        }
    }
}

object PanelK {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PK-1);\">"
    private const val MARK_BODY = "<g id=\"BodyCard\">"
    private const val MARK_MAIN = "<g id=\"MainCard\">"
    private const val MARK_HEXAGON = "<g id=\"HexagonChart\">"

    private val DEFAULT_ABBR = listOf("RC", "ST", "SP", "LN", "CO", "PR", "SV")
    private val HEXAGON_ORDER = listOf("RC", "LN", "CO", "PR", "SP", "ST")
    private val LABEL_ORDER = listOf("RC", "ST", "SP", "LN", "CO", "PR")
    private val MAPPER = mapOf(
        "reform" to LabelMM.RF,
        "underjoy" to LabelMM.UJ,
        "regular" to LabelMM.RG,
        "ln" to LabelMM.JL,
    )

    fun render(data: PanelKData, assets: ImageAssets): String = toSvg(data, assets)

    fun toSvg(data: PanelKData, assets: ImageAssets): String {
        var svg = """
<svg xmlns="http://www.w3.org/2000/svg" width="1920" height="1080" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 1920 1080">
    <defs>
        <clipPath id="clippath-PK-1">
            <rect width="1920" height="320" rx="20" ry="20" style="fill: none;"/>
        </clipPath>
    </defs>
    <g id="Banner">
        <rect width="1920" height="320" rx="20" ry="20" style="fill: #1c1719;"/>
        <g style="clip-path: url(#clippath-PK-1);">
        </g>
    </g>
    <g id="Background">
        <rect y="290" width="1920" height="790" rx="20" ry="20" style="fill: #2a2226;"/>
        <rect x="40" y="330" width="530" height="710" rx="20" ry="20" style="fill: #382E32;"/>
        <rect x="1350" y="330" width="530" height="710" rx="20" ry="20" style="fill: #382E32;"/>
        <rect x="610" y="330" width="700" height="710" rx="20" ry="20" style="fill: #382E32;"/>
    </g>
    <g id="HexagonChart">
    </g>
    <g id="BodyCard">
    </g>
    <g id="MainCard">
    </g>
    <g id="IndexBase">
        <rect x="510" y="40" width="195" height="60" rx="15" ry="15" style="fill: #382e32;"/>
    </g>
    <g id="Index">
    </g>
</svg>
        """.trimIndent()

        val isVs = data.panel == "KV"
        val skills = if (data.scores.size > 10) data.scores.take(10) else data.scores

        val middle = if (isVs) {
            val difference = data.total - data.vsTotal
            Numbers.roundParts(if (difference == 0.0 || difference.isNaN()) 0.0 else difference, 2)
        } else {
            Numbers.roundParts(data.total, 2)
        }
        val middlePath = Fonts.poppinsBold.get2SizeTextPath(
            middle.integer, middle.decimal, 60.0, 48.0, 960.0, 614.0, "center baseline", "#fff",
        )

        val abbrList = data.abbreviates ?: DEFAULT_ABBR
        val valueMap = HashMap<String, Double>()
        val skillList = data.skill
        abbrList.forEachIndexed { index, key -> valueMap[key] = skillList.getOrNull(index) ?: 0.0 }

        val hexagonValues = HEXAGON_ORDER.map { (valueMap[it] ?: 0.0) / 10 }
        val values = LABEL_ORDER.map { valueMap[it] ?: 0.0 }

        svg = Svg.setText(svg, PanelDraw.hexagonIndex(HEXAGON_ORDER, 960.0, 600.0, 260.0, PI / 3), MARK_HEXAGON)
        svg = Svg.setText(svg, assets.randomBanner()?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) }, MARK_BANNER)

        val panelName = if (isVs) {
            bPanelName("Skill VS v6.1 (!ymkv)", "KV")
        } else {
            bPanelName("Skill v6.1 (!ymk)", "K")
        }
        svg = Svg.setTexts(svg, listOf(panelName, middlePath), MARK_INDEX)

        svg = Svg.setText(svg, Svg.group(40.0, 40.0, CardA1.render(data.user, null, assets)), MARK_MAIN)

        var body = ""
        if (isVs) {
            svg = Svg.setText(
                svg,
                Svg.group(1920.0 - 470, 40.0, CardA1.render(data.vsUser, null, assets)),
                MARK_MAIN,
            )
        } else {
            val cards = skills.map { score ->
                CardK.render(CardK.fromScore(score.score, score.skill, score.skillSum, assets), assets)
            }
            body += cards.mapIndexed { index, card ->
                val x = index % 2
                val y = index / 2
                Svg.group(1370.0 + x * 255, 350.0 + y * 138, card)
            }.joinToString("\n")
        }

        val vsAbbrList = data.vsAbbreviates ?: DEFAULT_ABBR
        val vsValueMap = HashMap<String, Double>()
        val vsSkillList = data.vsSkill
        vsAbbrList.forEachIndexed { index, key -> vsValueMap[key] = vsSkillList.getOrNull(index) ?: 0.0 }

        val vsHexagonValues = HEXAGON_ORDER.map { (vsValueMap[it] ?: 0.0) / 10 }
        val vsValues = LABEL_ORDER.map { vsValueMap[it] ?: 0.0 }

        val leftCards = mutableListOf<String>()
        val rightCards = mutableListOf<String>()
        for (index in 0 until 6) {
            val abbr = LABEL_ORDER[index]
            val value = values[index]
            leftCards += CardB6.render(
                CardB6Data(
                    label = LabelMM.of(abbr),
                    value = value,
                    delta = if (isVs) value - (vsValues.getOrNull(index) ?: 0.0) else null,
                    iconColors = Colors.getRankColors(Rankings.getRankFromValue(value)),
                    roundLevel = 2,
                ),
                assets,
                false,
            )
        }

        svg = Svg.setText(
            svg,
            PanelDraw.hexagonChart(hexagonValues, 960.0, 600.0, 230.0, "#00A8EC", PI / 3),
            MARK_HEXAGON,
        )

        if (isVs) {
            for (index in 0 until 6) {
                val abbr = LABEL_ORDER[index]
                val value = vsValues.getOrNull(index) ?: 0.0
                rightCards += CardB6.render(
                    CardB6Data(
                        label = LabelMM.of(abbr),
                        value = value,
                        delta = value - (values.getOrNull(index) ?: 0.0),
                        iconColors = Colors.getRankColors(Rankings.getRankFromValue(value)),
                        roundLevel = 2,
                    ),
                    assets,
                    true,
                )
            }
            svg = Svg.setText(
                svg,
                PanelDraw.hexagonChart(vsHexagonValues, 960.0, 600.0, 230.0, "#FF0000", PI / 3),
                MARK_HEXAGON,
            )
            for (index in 0 until 6) {
                body += Svg.group(1350.0, 340.0 + index * 115, rightCards[index])
            }
        }

        for (index in 0 until 6) {
            body += Svg.group(40.0, 340.0 + index * 115, leftCards[index])
        }

        val centerCards = mutableListOf<String>()
        if (isVs) {
            val myDan = highestDan(data.dan)
            val myDanLevel = myDan?.level ?: 0.0
            centerCards += CardB7.render(
                CardB7Data(
                    label = myDan?.name?.let { MAPPER[it] } ?: LabelMM.DN,
                    value = myDanLevel,
                    dataB = myDan?.grade ?: "-",
                    dataM = "",
                    iconColors = Colors.getRankColors(Rankings.getRankFromValue(myDanLevel)),
                    roundLevel = 0,
                ),
                assets,
                false,
            )

            val otherDan = highestDan(data.vsDan)
            val otherDanLevel = otherDan?.level ?: 0.0
            centerCards += CardB7.render(
                CardB7Data(
                    label = otherDan?.name?.let { MAPPER[it] } ?: LabelMM.DN,
                    value = otherDanLevel,
                    dataB = otherDan?.grade ?: "-",
                    dataM = "",
                    iconColors = Colors.getRankColors(Rankings.getRankFromValue(otherDanLevel)),
                    roundLevel = 0,
                ),
                assets,
                true,
            )
        } else {
            val reform = data.dan["reform"] as? DanResult
            val danRc = reform ?: data.dan["regular"] as? DanResult
            val rcLabel = if (reform != null) LabelMM.RF else LabelMM.RG
            centerCards += CardB7.render(
                CardB7Data(
                    label = rcLabel,
                    value = danRc?.level ?: 11.4514,
                    dataB = danRc?.grade ?: "",
                    dataM = "",
                    iconColors = Colors.getRankColors(Rankings.getRankFromValue(danRc?.level ?: 0.0)),
                    roundLevel = 0,
                ),
                assets,
                false,
            )

            val underjoy = data.dan["underjoy"] as? DanResult
            val danLn = underjoy ?: data.dan["ln"] as? DanResult
            val lnLabel = if (underjoy != null) LabelMM.UJ else LabelMM.JL
            centerCards += CardB7.render(
                CardB7Data(
                    label = lnLabel,
                    value = danLn?.level ?: 11.4514,
                    dataB = danLn?.grade ?: "",
                    dataM = "",
                    iconColors = Colors.getRankColors(Rankings.getRankFromValue(danLn?.level ?: 0.0)),
                    roundLevel = 0,
                ),
                assets,
                true,
            )
        }

        body += Svg.group(630.0, 890.0, centerCards[0]) + Svg.group(970.0, 890.0, centerCards[1])
        svg = Svg.setText(svg, body, MARK_BODY)
        svg = Svg.setText(
            svg,
            assets.local("object-hexagon.png")?.let { Svg.image(718.0, 384.0, 484.0, 433.0, it) },
            MARK_HEXAGON,
        )
        return svg
    }

    private fun highestDan(dan: Map<String, Any>): DanResult? {
        return dan.values.filterIsInstance<DanResult>().fold(null as DanResult?) { max, current ->
            if (current.level > (max?.level ?: -1.0)) current else max
        }
    }
}
