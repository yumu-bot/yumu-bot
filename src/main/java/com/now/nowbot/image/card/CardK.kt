package com.now.nowbot.image.card

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.hexagonChart
import com.now.nowbot.image.draw.hexagonIndex
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import kotlin.math.PI
import kotlin.math.pow

data class CardKData(
    val background: String? = null,
    val star: Double = 0.0,
    val skill: List<Double> = emptyList(),
    val skillSum: Double = 0.0,
    val skillColor: String = "#fff",
    val hexagonColor: String = "#fff",
    val mods: List<LazerMod> = emptyList(),
    val rank: String = "S",
)

object CardK {
    private const val MARK_TEXT = "<g id=\"Text\">"
    private const val MARK_HEXAGON = "<g id=\"Hexagon\">"
    private const val MARK_OVERLAY = "<g id=\"Overlay\">"
    private const val MARK_BACKGROUND = "<g style=\"clip-path: url(#clippath-CK-1);\">"

    private val SHOW_ARR = listOf("RC", "LN", "CO", "PR", "SP", "ST")

    fun render(data: CardKData, assets: ImageAssets): String {
        var svg = """
  <defs>
    <clipPath id="clippath-CK-1">
      <rect width="235" height="118" rx="20" ry="20" style="fill: none;"/>
    </clipPath>
  </defs>
  <g id="Base">
    <rect width="235" height="118" rx="20" ry="20" style="fill: #46393f;"/>
  </g>
  <g id="Background">
    <g style="clip-path: url(#clippath-CK-1);">
    </g>
  </g>
  <g id="Hexagon">
  </g>
  <g id="Text">
  </g>
  <g id="Overlay">
  </g>
        """.trimIndent()

        val starColor = Colors.getStarRatingColor(data.star)
        val starTextColor = if (data.star < 4) "#1c1719" else "#fff"
        val starRect = PanelDraw.rect(10.0, 10.0, 40.0, 20.0, 10.0, starColor)
        val star = Fonts.torusBold.textPath(Numbers.floor(data.star, 2), 30.0, 26.0, 18.0, "center baseline", starTextColor)

        val rankColor = Colors.getRankColor(data.rank)
        val rankTextColor = if (data.rank == "X" || data.rank == "XH") "#1c1719" else "#fff"
        val rankRect = PanelDraw.rect(185.0, 10.0, 40.0, 20.0, 10.0, rankColor)
        val rank = Fonts.torusBold.textPath(data.rank.ifBlank { "F" }, 205.0, 26.0, 18.0, "center baseline", rankTextColor)

        data.mods.forEachIndexed { index, mod ->
            val acronym = mod.acronym
            if (acronym.isBlank()) return@forEachIndexed
            val color = mod.color.ifBlank { Colors.getModColor(acronym) }
            if (index < 3) {
                svg = Svg.setText(
                    svg,
                    PanelDraw.circle(10.0 + 6 + index * 15, 90.0 + 6, 6.0, color),
                    MARK_OVERLAY,
                )
            } else {
                svg = Svg.setText(
                    svg,
                    PanelDraw.circle(10.0 + 6 + (index - 3) * 15, 90.0 + 6 - 15, 6.0, color),
                    MARK_OVERLAY,
                )
            }
        }

        val charts = SHOW_ARR.map { name -> (skillValue(data.skill, name) / 10).pow(0.5) }
        val values = SHOW_ARR.map { name -> skillText(data.skill, name) }

        val hexagon = PanelDraw.hexagonChart(charts, 235.0 / 2, 118.0 / 2, 45.0, data.hexagonColor, PI / 3, 3.0, 3.0)
        val hexagonIndex = PanelDraw.hexagonIndex(
            values, 235.0 / 2, 118.0 / 2, 50.0, PI / 3, data.skillColor, "none", 20.0,
        )
        val hexagonBackground = assets.local("object-hexagon.png")

        val skillSum = Fonts.torusBold.textPath(Numbers.floor(data.skillSum, 2), 235.0 / 2, 68.0, 20.0, "center baseline", "#FFF")

        svg = Svg.setText(svg, skillSum, MARK_OVERLAY)
        svg = Svg.setTexts(svg, listOf(star, starRect, rank, rankRect), MARK_TEXT)
        svg = Svg.setTexts(svg, listOf(hexagonIndex, hexagon), MARK_HEXAGON)
        svg = Svg.setText(
            svg,
            hexagonBackground?.let { Svg.image(72.5, 19.0, 90.0, 80.0, it) },
            MARK_HEXAGON,
        )
        svg = Svg.setText(
            svg,
            data.background?.let { Svg.image(0.0, 0.0, 235.0, 148.0, it, 0.4) },
            MARK_BACKGROUND,
        )
        return svg
    }

    fun fromScore(score: LazerScore, skill: List<Double>, skillSum: Double, assets: ImageAssets): CardKData {
        return CardKData(
            background = assets.mapBackground(score, "list@2x"),
            star = score.beatmap.starRating,
            skill = skill,
            skillSum = skillSum,
            skillColor = "#fff",
            hexagonColor = "#bbb",
            mods = score.mods,
            rank = score.rank,
        )
    }

    private fun skillValue(skill: List<Double>, name: String): Double {
        val index = when (name) {
            "RC" -> 0
            "ST" -> 1
            "SP" -> 2
            "LN" -> 3
            "CO" -> 4
            "PR" -> 5
            else -> return Double.NaN
        }
        return skill.getOrNull(index) ?: Double.NaN
    }

    private fun skillText(skill: List<Double>, name: String): String {
        val value = skillValue(skill, name)
        if (value.isNaN()) return ""
        return Numbers.floor(value, 1)
    }
}
