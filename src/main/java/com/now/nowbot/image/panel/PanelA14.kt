package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA2
import com.now.nowbot.image.card.CardA3
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.model.osu.BeatmapsetSearch
import kotlin.math.min

object PanelA14 {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_SEARCH = "<g id=\"Search_Card_A2\">"
    private const val MARK_CARD = "<g id=\"Map_Card_A2\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PA2-1);\">"

    fun render(search: BeatmapsetSearch, page: Int, maxPage: Int, assets: ImageAssets): String {
        var svg = Templates.read("Panel_A2.svg")
        svg = Svg.setText(svg, PanelBatchSupport.panelName("Explore Beatmap (!yme)", "E"), MARK_INDEX)
        svg = Svg.setText(svg, PanelBatchSupport.randomBanner(assets)?.let {
            Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7)
        }, MARK_BANNER)

        val beatmapsets = search.beatmapsets
        val count = min(search.resultCount, beatmapsets.size)
        val searchCard = CardA2.fromSearchResult(
            search, count, search.rule ?: "Any", beatmapsets.firstOrNull(), beatmapsets.lastOrNull(), assets,
        )
        svg = Svg.setText(svg, CardA2.render(searchCard, assets), MARK_SEARCH)

        val cards = beatmapsets.mapIndexed { index, beatmapset ->
            val x = index % 3
            val y = index / 3
            Svg.group(40.0 + 620.0 * x, 330.0 + 230.0 * y, CardA3.render(beatmapset, assets))
        }
        svg = Svg.setText(svg, cards.joinToString("\n"), MARK_CARD)

        val panelHeight = PanelBatchSupport.panelHeight(count, 210.0, 3, 290.0, 20.0)
        val cardHeight = panelHeight - 290.0
        svg = PanelBatchSupport.applyHeights(svg, panelHeight, cardHeight)
        return Svg.setText(svg, PanelBatchSupport.page(page, maxPage, panelHeight), MARK_CARD)
    }
}
