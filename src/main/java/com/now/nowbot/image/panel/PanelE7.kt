package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA2
import com.now.nowbot.image.component.E1Component
import com.now.nowbot.image.component.E3Component
import com.now.nowbot.image.component.E4Component
import com.now.nowbot.image.component.E6Component
import com.now.nowbot.image.component.e7.E11Data
import com.now.nowbot.image.component.e7.E12Data
import com.now.nowbot.image.component.e7.E13Data
import com.now.nowbot.image.component.e7.E2Data
import com.now.nowbot.image.component.e7.E5Data
import com.now.nowbot.image.component.e7.E7Components
import com.now.nowbot.image.component.e7.E8Data
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.scorelist.ScoreListGenerate
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.GameModes
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.service.messageServiceImpl.MatchMapService.PanelE7Param

object PanelE7 {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PE-BR);\">"
    private const val MARK_BACKGROUND = "<g filter=\"url(#blur-PE-BG)\" style=\"clip-path: url(#clippath-PE-BG);\">"
    private const val MARK_CARD_A1 = "<g id=\"Card_A1\">"
    private const val MARK_CARD_E1 = "<g id=\"Card_E1\">"

    fun render(param: PanelE7Param, assets: ImageAssets): String {
        var svg = Templates.read("Panel_E.svg")
        svg = Svg.setText(svg, PanelBatchSupport.panelName("Match Start (passive module)", "ST"), MARK_INDEX)
        val beatmap = param.beatmap
        val beatmapset = beatmap.beatmapset ?: throw IllegalArgumentException("E7 需要 beatmapset")
        val players = param.players
        val mode = param.mode
        val rank = rank(players.size)
        val background = assets.local(GameModes.rankBackgroundFile(rank, true))
        val banner = assets.mapBackground(beatmap, "cover")
        val ruleset = mode.safeModeValue.toInt()
        val synthetic = LazerScore(ruleset = mode.safeModeValue).apply {
            this.beatmap = beatmap
            this.beatmapset = beatmapset
        }
        val cardA2 = CardA2.render(
            ScoreListGenerate.matchRating2CardA2(
                param.match,
                assets,
                background = assets.mapBackground(beatmap, "list@2x"),
                isMatchStart = true,
            ),
            assets,
        )
        val e1 = E1Component.render(beatmap, ruleset, param.mods, beatmap.originalStar)
        val e2 = E7Components.e2(E2Data(param.density, beatmap, rank))
        val e3 = E3Component.render(synthetic, param.original, assets)
        val e4 = E4Component.render(beatmap, assets)
        val e5 = E7Components.e5(E5Data(beatmapset.favouriteCount, beatmapset.playCount), assets)
        val e6 = E6Component.render(beatmap, beatmapset, background)
        val e8 = E7Components.e8(E8Data(param.mods), assets)
        val e11 = E7Components.e11(E11Data(banner), assets)
        val lastRound = param.match.rounds.lastOrNull() ?: param.match.match.lastRound
        val e12 = E7Components.e12(E12Data(
            scoringType = scoringType(lastRound?.scoringType),
            teamType = teamType(lastRound?.teamType).first,
            vsType = teamType(lastRound?.teamType).second,
            playerCount = players.size,
        ))
        val e13 = E7Components.e13(E13Data(players), assets)

        svg = Svg.setText(svg, Svg.group(40.0, 40.0, cardA2), MARK_CARD_A1)
        svg = Svg.setTexts(
            svg,
            listOf(
                Svg.group(40.0, 330.0, e1),
                Svg.group(40.0, 500.0, e2),
                Svg.group(40.0, 770.0, e3),
                Svg.group(550.0, 330.0, e4),
                Svg.group(1280.0, 330.0, e5),
                Svg.group(550.0, 880.0, e6),
                Svg.group(1390.0, 500.0, e8),
                Svg.group(550.0, 290.0, e11),
                Svg.group(1390.0, 330.0, e12),
                Svg.group(1390.0, 600.0, e13),
            ),
            MARK_CARD_E1,
        )
        svg = Svg.setText(svg, background?.let { Svg.image(0.0, 0.0, 1920.0, 1080.0, it, 0.6) }, MARK_BACKGROUND)
        return Svg.setText(svg, banner?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) }, MARK_BANNER)
    }

    private fun rank(count: Int): String = when {
        count <= 1 -> "F"
        count <= 2 -> "D"
        count <= 4 -> "C"
        count <= 6 -> "B"
        count <= 8 -> "A"
        count <= 10 -> "S"
        count <= 12 -> "SH"
        count <= 14 -> "X"
        else -> "XH"
    }

    private fun scoringType(value: String?): String = when (value) {
        "score" -> "score V1"
        "scorev2" -> "score V2"
        "accuracy" -> "accuracy"
        "combo" -> "combo"
        else -> "unknown"
    }

    private fun teamType(value: String?): Pair<String, String> = when (value) {
        "team-vs" -> "VS" to "TEAM"
        "head-to-head" -> "VS" to "H2H"
        "tag-coop" -> "TAG" to "COOP"
        "tag-team-vs" -> "TAG" to "VS"
        else -> "?" to "?"
    }
}
