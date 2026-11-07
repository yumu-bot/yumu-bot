package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA2
import com.now.nowbot.image.card.CardA5
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.model.osu.BeatmapsetSearch
import kotlin.math.min

object PanelA2 {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PA2-1);\">"
    private const val MARK_SEARCH = "<g id=\"Search_Card_A2\">"
    private const val MARK_CARD_A5 = "<g id=\"Map_Card_M\">"
    private const val MARK_CARD_A2 = "<g id=\"Map_Card_A2\">"

    fun render(search: BeatmapsetSearch, page: Int, maxPage: Int, assets: ImageAssets): String {
        var svg = Templates.read("Panel_A2.svg")
        val sets = search.beatmapsets
        val count = min(search.resultCount, sets.size)
        val compact = count > 12

        svg = Svg.setText(svg, PanelBatchSupport.panelName("Qualified Map List (!ymq)", "Q"), MARK_INDEX)
        svg = Svg.setText(svg, PanelBatchSupport.randomBanner(assets)?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) }, MARK_BANNER)

        val searchCard = CardA2.fromSearchResult(
            search, count, search.rule ?: "Qualified", sets.firstOrNull(), sets.lastOrNull(), assets,
        )
        svg = Svg.setText(svg, CardA2.render(searchCard, assets), MARK_SEARCH)

        if (compact) {
            val cards = (0 until count).map { index ->
                val x = index % 4
                val y = index / 4
                Svg.group(40.0 + 470.0 * x, 330.0 + 250.0 * y, CardA2.render(CardA2.fromSearchMap(sets[index], index + 1, assets), assets))
            }
            svg = Svg.setText(svg, cards.joinToString("\n"), MARK_CARD_A2)
        } else {
            val cardsA2 = (0 until count).map { index ->
                Svg.group(40.0, 330.0 + 250.0 * index, CardA2.render(CardA2.fromSearchMap(sets[index], index + 1, assets), assets))
            }
            val cardsA5 = (0 until count).map { index ->
                Svg.group(510.0, 330.0 + 250.0 * index, CardA5.render(sets[index], assets))
            }
            svg = Svg.setText(svg, cardsA2.joinToString("\n"), MARK_CARD_A2)
            svg = Svg.setText(svg, cardsA5.joinToString("\n"), MARK_CARD_A5)
        }

        val panelHeight = if (count <= 12) {
            PanelBatchSupport.panelHeight(count, 210.0, 1, 290.0, 40.0)
        } else {
            PanelBatchSupport.panelHeight(count, 210.0, 4, 290.0, 40.0)
        }
        val cardHeight = panelHeight - 290.0
        svg = PanelBatchSupport.applyHeights(svg, panelHeight, cardHeight)
        return Svg.setText(svg, PanelBatchSupport.page(page, maxPage, panelHeight), MARK_CARD_A2)
    }
}
