package com.now.nowbot.image.card

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.GameModes
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.Beatmapset
import kotlin.math.abs
import kotlin.math.max

object CardA3 {
    private const val MARK_LABEL = "<g id=\"Label_CA3\">"
    private const val MARK_TEXT = "<g id=\"Text_CA3\">"
    private const val MARK_COVER = "<g style=\"clip-path: url(#clippath-CA3-2);\">"
    private const val MARK_BACKGROUND = "<g style=\"clip-path: url(#clippath-CA3-1);\" filter=\"url(#blur-CA3-1)\">"
    private const val MARK_BASE = "<g id=\"Base_CA3\">"

    fun render(beatmapset: Beatmapset, assets: ImageAssets): String {
        var svg = TEMPLATE

        val aliases = beatmapset.aliases.orEmpty()
        val titles = com.now.nowbot.image.font.Typography.beatMapTitlePath(
            Fonts.torus,
            Fonts.puHuiTi,
            beatmapset.title,
            if (aliases.isNotEmpty()) aliases.first() else beatmapset.titleUnicode,
            null,
            225.0, 40.0 - 2.0, 70.0 - 2.0, 36.0, 18.0, 390.0 - 20.0,
            "left baseline", "#fff", "#aaa",
        )

        val artist = Fonts.torus.textPath(
            Fonts.torus.cutStringTail(beatmapset.artist, 24.0, 390.0 - 20.0),
            225.0, 100.0, 24.0, "left baseline", "#ccc",
        )

        val mapperMap = linkedMapOf<Long, String>()
        beatmapset.beatmaps.orEmpty().forEach { beatmap ->
            beatmap.owners.orEmpty().forEach { owner -> mapperMap[owner.userID] = owner.username }
        }
        var mappers = beatmapset.creator
        if (mapperMap.isNotEmpty()) mappers += " (" + mapperMap.values.joinToString(", ") + ")"
        val mapper = Fonts.torus.textPath(
            Fonts.torus.cutStringTail(mappers, 24.0, 390.0 - 20.0),
            225.0, 130.0, 24.0, "left baseline", "#ccc",
        )

        val image = assets.mapBackground(beatmapset, "list@2x")
        val fav = labelA8(
            image = assets.local("object-beatmap-favorite.png"),
            hasText = true,
            text = Numbers.floor(beatmapset.favouriteCount, 1),
            color = "#1c1719",
        )
        val pc = labelA8(
            image = assets.local("object-beatmap-playcount.png"),
            hasText = true,
            text = Numbers.floor(beatmapset.playCount, 1),
            color = "#1c1719",
        )
        val sid = labelA8(
            image = null,
            hasText = null,
            text = beatmapset.beatmapsetID.toString(),
            color = "#1c1719",
        )
        val statusColor = mapStatusColor(beatmapset.ranked.toInt())
        val status = labelA8(
            image = null,
            hasText = null,
            text = mapStatusName(beatmapset.ranked.toInt()).uppercase(),
            color = statusColor,
        )
        val difficulty = drawDifficultyLabels(beatmapset.beatmaps.orEmpty())

        val base = PanelDraw.rect(0.0, 0.0, 600.0, 210.0, 20.0, "#382E32", 1.0) +
            PanelDraw.rect(0.0, 0.0, 600.0, 210.0, 20.0, statusColor, 0.3) +
            PanelDraw.rect(170.0, 0.0, 430.0, 175.0, 20.0, "#382E32", 1.0)

        svg = Svg.setTexts(svg, listOf(titles.title, titles.titleUnicode, artist, mapper), MARK_TEXT)
        val diffSvg = Svg.group(220.0, 175.0, difficulty)
        val favSvg = Svg.group(220.0, 144.0, fav.svg)
        val pcSvg = Svg.group(220.0 + 10.0 + fav.width, 144.0, pc.svg)
        val sidSvg = Svg.group(600.0 - sid.width - 10.0, 144.0, sid.svg)
        val statusSvg = Svg.group(10.0, 10.0, status.svg)
        svg = Svg.setTexts(svg, listOf(diffSvg, favSvg, pcSvg, sidSvg, statusSvg), MARK_LABEL)
        svg = Svg.setText(svg, image?.let { Svg.image(170.0, 0.0, 430.0, 175.0, it, 0.3) }, MARK_BACKGROUND)
        svg = Svg.setText(svg, image?.let { Svg.image(0.0, 0.0, 210.0, 210.0, it, 1.0) }, MARK_COVER)
        return Svg.setText(svg, base, MARK_BASE)
    }

    private fun drawDifficultyLabels(beatmaps: List<Beatmap>): String {
        val maxWidth = 390.0 - 10.0
        val fullLabels = beatmaps.map {
            labelA8(null, true, Numbers.floor(it.starRating, 2), Colors.getStarRatingColor(it.starRating))
        }
        val halfLabels = beatmaps.map {
            labelA8(null, false, Numbers.floor(it.starRating, 2), Colors.getStarRatingColor(it.starRating))
        }
        val rulesetArr = beatmaps.map { it.modeInt ?: 0 }
        val rulesetSet = rulesetArr.toSet()
        val rulesetCount = rulesetSet.size
        var rulesetCurrent = -1
        var x = 10.0
        val halfWidth = max(halfLabels.sumOf { it.width + 4.0 } - 4.0, 0.0)
        val fullWidth = max(fullLabels.sumOf { it.width + 4.0 } - 4.0, 0.0)
        val card = mutableListOf<String>()

        if (halfWidth + rulesetCount * 40.0 > maxWidth) {
            for (mode in rulesetArr) {
                if (mode != rulesetCurrent) {
                    rulesetCurrent = mode
                    card += Fonts.extra.textPath(
                        GameModes.getGameMode(mode, -1), x - 8.0, 32.0 - 4.0, 30.0, "left baseline", "#fff", 1.0,
                    )
                    x += 28.0
                    val count = rulesetArr.count { it == rulesetCurrent }
                    val countWidth = Fonts.torusBold.width(count.toString(), 20.0)
                    card += Fonts.torusBold.textPath(count.toString(), x, 24.0, 20.0, "left baseline", "#fff", 1.0)
                    x += 18.0 + countWidth
                }
            }
        } else if (fullWidth + rulesetCount * 40.0 > maxWidth) {
            var currentFullWidth = halfWidth + rulesetCount * 40.0
            val topDiffIndex = rulesetSet.map { ruleset -> rulesetArr.indexOfLast { abs(it - ruleset) < 1e-6 } }
            var j = 0
            for ((i, mode) in rulesetArr.withIndex()) {
                if (mode != rulesetCurrent) {
                    rulesetCurrent = mode
                    card += Fonts.extra.textPath(
                        GameModes.getGameMode(mode, -1), x - 8.0, 32.0 - 4.0, 30.0, "left baseline", "#fff", 1.0,
                    )
                    x += 28.0
                }
                if (j < topDiffIndex.size && topDiffIndex[j] - i < 1e-6) {
                    if (currentFullWidth + fullLabels[i].width <= maxWidth) {
                        currentFullWidth -= halfLabels[i].width
                        currentFullWidth += fullLabels[i].width
                        card += Svg.group(x, 5.0, fullLabels[i].svg)
                        x += fullLabels[i].width + 18.0
                    } else {
                        card += Svg.group(x, 5.0, halfLabels[i].svg)
                        x += halfLabels[i].width + 18.0
                    }
                    j++
                } else {
                    card += Svg.group(x, 5.0, halfLabels[i].svg)
                    x += halfLabels[i].width + 4.0
                }
            }
        } else {
            val topDiffIndex = rulesetSet.map { ruleset -> rulesetArr.indexOfLast { abs(it - ruleset) < 1e-6 } }
            var j = 0
            for ((i, mode) in rulesetArr.withIndex()) {
                if (mode != rulesetCurrent) {
                    rulesetCurrent = mode
                    card += Fonts.extra.textPath(
                        GameModes.getGameMode(mode, -1), x - 8.0, 32.0 - 4.0, 30.0, "left baseline", "#fff", 1.0,
                    )
                    x += 28.0
                }
                if (j < topDiffIndex.size && topDiffIndex[j] - i < 1e-6) {
                    card += Svg.group(x, 5.0, fullLabels[i].svg)
                    x += fullLabels[i].width + 18.0
                    j++
                } else {
                    card += Svg.group(x, 5.0, fullLabels[i].svg)
                    x += fullLabels[i].width + 4.0
                }
            }
        }
        return "<g id=\"Difficulty_A8\"> \n" + card.joinToString("\n") + "</g>"
    }

    private fun labelA8(image: String?, hasText: Boolean?, text: String, color: String, isHighlight: Boolean = true): LabelResult {
        var svg = """
            <g id="Base_LA8">
            </g>
            <g id="Text_LA8">
            </g>
            <g id="Icon_LA8">
            </g>
        """.trimIndent()
        val textWidth = Fonts.torusBold.width(text, 20.0)
        var width = textWidth + 16.0
        var textMiddle = width / 2.0
        val opacity = if (isHighlight) 1.0 else 0.2
        val textOpacity = if (isHighlight) 1.0 else 0.4
        val baseOpacity = if (isHighlight) 0.6 else 0.2

        if (!image.isNullOrBlank()) {
            width += 22.0
            textMiddle += 22.0
            svg = Svg.setText(svg, Svg.image(6.0, 3.5, 18.0, 18.0, image, 1.0), "<g id=\"Icon_LA8\">")
        } else if (hasText == true) {
            width += 12.0
            textMiddle += 12.0
            svg = Svg.setText(svg, PanelDraw.rect(0.0, 0.0, 14.0, 25.0, 7.0, color, opacity), "<g id=\"Icon_LA8\">")
        } else if (hasText == false) {
            svg = Svg.setText(svg, PanelDraw.rect(0.0, 0.0, 14.0, 25.0, 7.0, color, opacity), "<g id=\"Icon_LA8\">")
            return LabelResult(width = 14.0, svg = svg)
        }

        val textPath = Fonts.torusBold.textPath(text, textMiddle, 19.0, 20.0, "center baseline", "#fff", textOpacity)
        val base = PanelDraw.rect(0.0, 0.0, width, 25.0, 7.0, color, baseOpacity)
        svg = Svg.setText(svg, textPath, "<g id=\"Text_LA8\">")
        svg = Svg.setText(svg, base, "<g id=\"Base_LA8\">")
        return LabelResult(width, svg)
    }

    private fun mapStatusName(ranked: Int): String {
        return when (ranked) {
            -2 -> "Graveyard"
            -1 -> "WIP"
            0 -> "Pending"
            1 -> "Ranked"
            2 -> "Approved"
            3 -> "Qualified"
            4 -> "Loved"
            else -> ""
        }
    }

    private fun mapStatusColor(ranked: Int): String {
        return when (ranked) {
            -2 -> "#000000"
            -1 -> com.now.nowbot.image.util.PanelColor.hsl2hex(20.0, 1.0, 0.7)
            0 -> com.now.nowbot.image.util.PanelColor.hsl2hex(45.0, 1.0, 0.7)
            1, 2 -> com.now.nowbot.image.util.PanelColor.hsl2hex(90.0, 1.0, 0.7)
            3 -> com.now.nowbot.image.util.PanelColor.hsl2hex(200.0, 1.0, 0.7)
            4 -> com.now.nowbot.image.util.PanelColor.hsl2hex(333.0, 1.0, 0.7)
            else -> "none"
        }
    }

    private data class LabelResult(val width: Double, val svg: String)

    private val TEMPLATE = """
           <defs>
            <clipPath id="clippath-CA3-1">
              <rect x="170" width="430" height="175" rx="20" ry="20" style="fill: none;"/>
            </clipPath>
            <clipPath id="clippath-CA3-2">
              <rect width="210" height="210" rx="20" ry="20" style="fill: none;"/>
            </clipPath>
            <filter id="blur-CA3-1" height="120%" width="120%" x="-10%" y="-10%" filterUnits="userSpaceOnUse">
                <feGaussianBlur in="userSpaceOnUse" stdDeviation="5" result="blur"/>
            </filter>
          </defs>
          <g id="Base_CA3">
           </g>
          <g id="Background_CA3">
            <g style="clip-path: url(#clippath-CA3-1);" filter="url(#blur-CA3-1)">
            </g>
          </g>
          <g id="Cover_CA3">
            <g style="clip-path: url(#clippath-CA3-2);">
            </g>
          </g>
          <g id="Text_CA3">
          </g>
          <g id="Label_CA3">
          </g>
    """.trimIndent()
}
