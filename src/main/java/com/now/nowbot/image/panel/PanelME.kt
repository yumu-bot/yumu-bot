package com.now.nowbot.image.panel

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.maimai.MaiAssets
import com.now.nowbot.image.maimai.MaiCardA1
import com.now.nowbot.image.maimai.MaiChuCardA1
import com.now.nowbot.image.maimai.MaiDraw
import com.now.nowbot.image.maimai.MaiME
import com.now.nowbot.image.maimai.MaiRenderResult
import com.now.nowbot.image.maimai.maiImageAssets
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.service.messageServiceImpl.ChuBestScoreService
import com.now.nowbot.service.messageServiceImpl.MaiBestScoreService

object PanelME {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_CARD_A1 = "<g id=\"Card_A1\">"
    private const val MARK_CARD_E1 = "<g id=\"Card_E1\">"
    private const val MARK_BACKGROUND = "<g filter=\"url(#blur-PE-BG)\" style=\"clip-path: url(#clippath-PE-BG);\">"
    private const val MARK_BANNER = "<g filter=\"url(#blur-PE-BG)\" style=\"clip-path: url(#clippath-PE-BR);\">"

    fun toSvg(
        param: MaiBestScoreService.PanelMEParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): String = render(param, lease, remote).svg

    fun toSvg(
        param: ChuBestScoreService.PanelME2Param,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): String = render(param, lease, remote).svg

    fun render(
        param: MaiBestScoreService.PanelMEParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): MaiRenderResult {
        val report = RenderReport()
        val assets = maiImageAssets(lease, remote, report)
        return MaiRenderResult(render(param, assets, report), report)
    }

    fun render(
        param: ChuBestScoreService.PanelME2Param,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): MaiRenderResult {
        val report = RenderReport()
        val assets = maiImageAssets(lease, remote, report)
        return MaiRenderResult(render(param, assets, report), report)
    }

    fun render(
        param: MaiBestScoreService.PanelMEParam,
        assets: ImageAssets,
        report: RenderReport = RenderReport(),
    ): String {
        val mai = MaiAssets(assets)
        val score = param.score
        val song = param.song
        val components = listOf(
            40.0 to 330.0 to MaiME.difficultyCard(score, assets),
            40.0 to 500.0 to MaiME.versionCard(song, assets),
            40.0 to 770.0 to MaiME.distributionCard(score, param.diff, param.chart.fit),
            550.0 to 330.0 to MaiME.typeCard(score.type, assets),
            1280.0 to 330.0 to MaiME.bpmCard(song.info.bpm, param.chart.count, assets),
            550.0 to 880.0 to MaiME.songCard(song, score.index, assets),
            1390.0 to 330.0 to MaiME.ratingCard(score),
            1390.0 to 500.0 to MaiME.comboCard(score, assets),
            1390.0 to 600.0 to MaiME.achievementCard(score),
            1390.0 to 770.0 to MaiME.chartCard(param.chart),
        )
        var svg = baseSvg("maimai Best Performance (!ymmb)", "MB")
        svg = Svg.setText(svg, panelBody(components), MARK_CARD_E1)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, MaiCardA1.render(param.user, assets)), MARK_CARD_A1)
        svg = Svg.setText(
            svg,
            Svg.image(665.0, 290.0, 590.0, 590.0, mai.local("Maimai/object-score-${score.rank.ifBlank { "d" }}.png").orEmpty()),
            MARK_INDEX,
        )
        svg = Svg.setText(
            svg,
            mai.rankBackground(score.rank)?.let { Svg.image(0.0, 0.0, 1920.0, 1080.0, it, 0.6) },
            MARK_BACKGROUND,
        )
        svg = Svg.setText(
            svg,
            mai.cover(song.songID.toLong())?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) },
            MARK_BANNER,
        )
        return svg
    }

    fun render(
        param: ChuBestScoreService.PanelME2Param,
        assets: ImageAssets,
        report: RenderReport = RenderReport(),
    ): String {
        val mai = MaiAssets(assets)
        val score = param.score
        val song = param.song
        val components = listOf(
            40.0 to 330.0 to MaiME.difficultyCard(score.star, score.difficulty, score.index, null, assets),
            40.0 to 500.0 to MaiME.versionCard(song.level, null),
            40.0 to 770.0 to MaiME.emptyDistributionCard("Distribution: Lv.${score.level}"),
            550.0 to 330.0 to MaiME.typeCard(null, assets),
            1280.0 to 330.0 to MaiME.bpmCard(song.info.bpm, null, assets),
            550.0 to 880.0 to MaiME.songCard(
                song.info.title,
                song.info.artist,
                score.charter,
                song.info.genre,
                song.songID,
                mai.chunithmCover(song.songID.toLong()),
            ),
            1390.0 to 330.0 to MaiME.ratingCard(score.rating, score.star + 2.15),
            1390.0 to 500.0 to MaiME.comboCard(mai.chunithmChainIcon(score.combo), null),
            1390.0 to 600.0 to MaiME.achievementCard(score.score / 10000.0, null, null),
            1390.0 to 770.0 to MaiME.emptyDistributionCard("Distribution: this song"),
        )
        var svg = baseSvg("CHUNITHM Best Performance (!ymcb)", "CB")
        svg = Svg.setText(svg, panelBody(components), MARK_CARD_E1)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, MaiChuCardA1.render(param.user, assets)), MARK_CARD_A1)
        svg = Svg.setText(
            svg,
            mai.chunithmRankImage(score.score)?.let { Svg.image(1390.0, 330.0, 490.0, 270.0, it) },
            MARK_INDEX,
        )
        svg = Svg.setText(
            svg,
            mai.chunithmRankBackground(score.score)?.let { Svg.image(0.0, 0.0, 1920.0, 1080.0, it, 0.6) },
            MARK_BACKGROUND,
        )
        svg = Svg.setText(
            svg,
            mai.chunithmCover(song.songID.toLong())?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) },
            MARK_BANNER,
        )
        return svg
    }

    private fun baseSvg(name: String, sign: String): String {
        var svg = Svg.removeXmlDeclaration(Templates.read("Panel_E.svg"))
        svg = Svg.setText(svg, MaiDraw.panelName(name, sign), MARK_INDEX)
        return svg
    }

    private fun panelBody(components: List<Pair<Pair<Double, Double>, String>>): String {
        return components.joinToString("\n") { (position, body) ->
            Svg.group(position.first, position.second, body)
        }
    }
}
