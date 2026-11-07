package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.component.E10Component
import com.now.nowbot.image.component.E10PComponent
import com.now.nowbot.image.component.E1Component
import com.now.nowbot.image.component.E2Component
import com.now.nowbot.image.component.E3Component
import com.now.nowbot.image.component.E4Component
import com.now.nowbot.image.component.E5StatsComponent
import com.now.nowbot.image.component.E6Component
import com.now.nowbot.image.component.E7Component
import com.now.nowbot.image.component.E8Component
import com.now.nowbot.image.component.E9Component
import com.now.nowbot.image.component.PerformanceValues
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderAssetRegistry
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.GameModes
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.service.messageServiceImpl.ScorePRService.PanelE5Param

data class E5RenderResult(val svg: String, val report: RenderReport)

object PanelE5 {
    private const val VERSION = "v0.8.5"
    private const val VERSION_CODE = "VS"
    private const val POWERED_BY = "Yumubot"

    private const val MARK_INDEX_PLUS = "<g id=\"IndexPlus\">"
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_BACKGROUND = "<g filter=\"url(#blur-PE-BG)\" style=\"clip-path: url(#clippath-PE-BG);\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PE-BR);\">"
    private const val MARK_CARD_A1 = "<g id=\"Card_A1\">"
    private const val MARK_CARD_E1 = "<g id=\"Card_E1\">"

    fun toSvg(
        param: PanelE5Param,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
        matchId: Long? = null,
        profileCard: String? = null,
        bannerIndex: Int? = null,
    ): String = render(param, lease, remote, matchId, profileCard, bannerIndex).svg

    fun render(
        param: PanelE5Param,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
        matchId: Long? = null,
        profileCard: String? = null,
        bannerIndex: Int? = null,
    ): E5RenderResult {
        val report = RenderReport()
        val assets = ImageAssets(
            RenderAssetRegistry { bytes -> lease.registerImage(bytes).url },
            remote,
            report = report,
        )
        val svg = render(param, assets, matchId, report, profileCard, bannerIndex)
        return E5RenderResult(svg, report)
    }

    fun render(
        param: PanelE5Param,
        assets: ImageAssets,
        matchId: Long? = null,
        report: RenderReport = RenderReport(),
        profileCard: String? = null,
        bannerIndex: Int? = null,
    ): String {
        val score = param.score
        val user = param.user
        val history = param.history
        val ruleset = score.ruleset.toInt()
        val performance = PerformanceValues.from(param.attributes, ruleset)
        val bannerImage = banner(score, assets)
        val rank = score.rank
        val rankBackground = assets.local(GameModes.rankBackgroundFile(rank, score.passed))
        val rankImage = assets.local(GameModes.rankImageFile(rank))
        val lazerOnly = score.beatmap.lazerOnly
        val scoreTypeFile = GameModes.scoreTypeFile(
            score.isLazer,
            if (lazerOnly) "-only" else "2",
            if (lazerOnly) null else score.type,
        )
        val scoreType = scoreTypeFile?.let { assets.local(it) }

        var svg = Svg.removeXmlDeclaration(Templates.read("Panel_E.svg"))
        val card = CardA1.render(user, history, assets, profileCard, bannerIndex)
        val components = listOf(
            E1Component.render(score),
            E2Component.render(score, param.density, param.progress),
            E3Component.render(score, param.original, assets),
            E4Component.render(score, assets),
            E5StatsComponent.render(score, assets),
            E6Component.render(score, bannerImage),
            E7Component.render(score, performance),
            E8Component.render(score, score.isLazer, assets),
            E9Component.render(score),
            E10PComponent.render(score, param.progress, assets),
            E10Component.render(score, performance),
        )
        val positions = listOf(
            40.0 to 330.0,
            40.0 to 500.0,
            40.0 to 770.0,
            550.0 to 330.0,
            1280.0 to 330.0,
            550.0 to 880.0,
            1390.0 to 330.0,
            1390.0 to 500.0,
            1390.0 to 600.0,
            1390.0 to 770.0,
            1390.0 to 770.0,
        )
        val bodies = components.mapIndexed { index, body ->
            Svg.group(positions[index].first, positions[index].second, body)
        }

        if (scoreType != null) {
            val x = if (lazerOnly) 1615.0 else 1725.0
            val width = if (lazerOnly) 280.0 else 170.0
            svg = Svg.setText(svg, Svg.image(x, 220.0, width, 70.0, scoreType), MARK_INDEX_PLUS)
        }
        svg = Svg.setText(svg, panelName(param, matchId), MARK_INDEX)
        svg = Svg.setText(svg, rankImage?.let { Svg.image(665.0, 290.0, 590.0, 590.0, it) }, MARK_INDEX)
        svg = Svg.setText(svg, rankBackground?.let { Svg.image(0.0, 0.0, 1920.0, 1080.0, it, 0.6) }, MARK_BACKGROUND)
        svg = Svg.setText(svg, bannerImage?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) }, MARK_BANNER)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, card), MARK_CARD_A1)
        svg = Svg.setTexts(svg, bodies, MARK_CARD_E1)
        return svg
    }

    private fun banner(score: LazerScore, assets: ImageAssets): String? {
        val covers = score.beatmapset.covers
        val cover = covers.cover.ifBlank {
            if (score.beatmapset.beatmapsetID > 0) {
                "https://assets.ppy.sh/beatmaps/${score.beatmapset.beatmapsetID}/covers/cover.jpg"
            } else {
                ""
            }
        }
        return assets.remote(cover) ?: assets.local("card-default.webp")
    }

    private fun panelName(param: PanelE5Param, matchId: Long?): String {
        val score = param.score
        val panel = param.panel
        val recent = panel == "P" || panel == "R"
        val best = if ((score.bestID ?: 0) > 0 && recent) "Personal Best! // " else ""
        val position = param.position ?: 0
        val positionInfo = if (position > 0 && !(position == 1 && recent)) "position: #$position // " else ""
        val scoreTime = TimeFormat.formatted(score.endedTime)
        val deltaTime = TimeFormat.difference(score.endedTime)
        val requestTime = "score time: $scoreTime ($deltaTime) // request time: ${TimeFormat.nowTimestamp()}"

        if (panel == "MR") {
            require(matchId != null) { "MR 面板需要显式传入 matchId" }
            val extra = "matchID: $matchId // request time: ${TimeFormat.nowTimestamp()}"
            return panelNameSvg("Match Recent Score (!ymmr)", "MR", extra)
        }

        val (title, sign) = when (panel) {
            "B" -> "Best Score (!ymb)" to "B"
            "P" -> "Passed Score (!ymp)" to "P"
            "R" -> "Recent Score (!ymr)" to "R"
            "S" -> "Score (!yms)" to "S"
            "L" -> "Leader Board (!yml)" to "L"
            "T" -> "Today Bests (!ymt)" to "T"
            else -> "Excellent Score (!ymp / !ymr / !yms)" to "S"
        }
        return panelNameSvg(title, sign, best + positionInfo + requestTime)
    }

    private fun panelNameSvg(name: String, sign: String, requestTime: String): String {
        val powered = Fonts.torus.textPath(
            "powered by $POWERED_BY $VERSION $VERSION_CODE // $name",
            20.0, 26.84, 24.0, "left baseline", "#fff",
        )
        val request = Fonts.torus.textPath(requestTime, 1900.0, 26.84, 24.0, "right baseline", "#fff")
        val signPath = Fonts.torus.textPath(sign, 607.5, 83.67, 48.0, "center baseline", "#fff")
        return powered + request + signPath
    }
}
