package com.now.nowbot.image.panel

import com.now.nowbot.entity.IDUser
import com.now.nowbot.image.card.CardA2
import com.now.nowbot.image.card.CardA4
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerScore
import kotlin.math.ceil
import kotlin.math.max

object PanelA16 {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_BEATMAP = "<g id=\"Beatmap_Card_A2\">"
    private const val MARK_LIST = "<g id=\"List_Card_N1\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PA3-1);\">"

    fun render(
        viewer: IDUser?,
        beatmap: Beatmap,
        mode: String,
        scores: List<LazerScore>,
        group: Long,
        type: String,
        page: Int,
        maxPage: Int,
        assets: ImageAssets,
    ): String {
        var svg = Templates.read("Panel_A3.svg")
        val requestTime = "groupID: $group // request time: ${com.now.nowbot.image.util.TimeFormat.nowTimestamp()}"
        svg = Svg.setText(svg, PanelBatchSupport.panelName("Group Leader Board (!ymgl)", "GL", requestTime), MARK_INDEX)
        svg = Svg.setText(svg, assets.mapBackground(beatmap, "cover")?.let {
            Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7)
        }, MARK_BANNER)

        val beatmapA2 = CardA2.render(CardA2.fromBeatmap(beatmap, assets), assets)
        svg = Svg.setText(svg, beatmapA2, MARK_BEATMAP)

        val viewerId = viewer?.userID
        val cards = scores.mapIndexed { index, score ->
            val previous = scores[max(index - 1, 0)]
            val legacy = (score.legacyScore ?: 0) > 0
            val compareScore = if (legacy) {
                previous.legacyScore?.takeIf { it > 0 } ?: previous.score
            } else {
                previous.score
            }
            val rank = index + (page - 1) * 50 + 1
            CardA4.render(score, viewerId, rank, compareScore, legacy, assets)
        }

        val rowTotal = ceil(cards.size / 2.0).toInt()
        val panelHeight = PanelBatchSupport.panelHeight(cards.size, 62.0, 2, 290.0, 10.0, 40.0)
        val cardHeight = panelHeight - 290.0
        val body = cards.mapIndexed { index, card ->
            val firstColumn = index < rowTotal
            val x = if (firstColumn) 40.0 else 965.0
            val y = 330.0 + (if (firstColumn) index else index - rowTotal) * 72.0
            Svg.group(x, y, card)
        }.joinToString("\n")

        svg = PanelBatchSupport.applyHeights(svg, panelHeight, cardHeight)
        val pageText = PanelBatchSupport.page(page, maxPage, panelHeight)
        return Svg.setTexts(svg, listOf(body, pageText), MARK_LIST)
    }
}
