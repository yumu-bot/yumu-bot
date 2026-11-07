package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.card.CardC3
import com.now.nowbot.image.card.CardC3Data
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.model.osu.OsuUser

object PanelA8 {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_ME = "<g id=\"Me_Card_A1\">"
    private const val MARK_LIST = "<g id=\"List_Card_H\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PA4-1);\">"

    fun render(user: OsuUser, cards: List<CardC3Data>, assets: ImageAssets): String {
        var svg = Templates.read("Panel_A4.svg")
        svg = Svg.setText(svg, PanelBatchSupport.panelName("Guess Songs (!ymg)", "G"), MARK_INDEX)
        svg = Svg.setText(svg, CardA1.render(user, null, assets), MARK_ME)
        svg = Svg.setText(svg, PanelBatchSupport.randomBanner(assets)?.let {
            Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7)
        }, MARK_BANNER)
        val body = cards.mapIndexed { index, card ->
            val x = if ((index + 1) % 2 == 1) 40.0 else 980.0
            val y = 330.0 + (index / 2) * 150.0
            Svg.group(x, y, CardC3.render(card, assets))
        }.joinToString("\n")
        svg = Svg.setText(svg, body, MARK_LIST)
        val panelHeight = PanelBatchSupport.panelHeight(cards.size, 110.0, 2, 290.0, 40.0)
        return PanelBatchSupport.applyHeights(svg, panelHeight, panelHeight - 290.0)
    }
}
