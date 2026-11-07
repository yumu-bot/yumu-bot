package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardF4
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.model.match.Match.MatchRound

object PanelF4 {
    private const val MARK_BODY = "<g id=\"BodyCard\">"

    fun render(round: MatchRound, assets: ImageAssets): String {
        val beatmap = round.beatmap ?: throw IllegalArgumentException("F4 缺少 round.beatmap")
        val scores = round.scores
        if (scores.isEmpty()) throw IllegalArgumentException("F4 缺少 round.scores")
        val first = scores.first()
        val maxCombo = beatmap.maxCombo ?: 0
        val maxScore = scores.maxOf { it.score }
        val minScore = scores.minOf { it.score }
        val compareScore = scores.getOrNull(1)?.score ?: 0L
        var svg = Templates.read("Panel_F2.svg")
        val card = CardF4.render(first, maxCombo, maxScore, minScore, compareScore, assets)
        svg = Svg.setText(svg, Svg.group(40.0, 330.0, card), MARK_BODY)
        return PanelBatchSupport.applyHeights(svg, 1080.0, 790.0)
    }
}
