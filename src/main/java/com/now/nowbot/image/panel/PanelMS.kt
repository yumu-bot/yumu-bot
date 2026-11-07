package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA2
import com.now.nowbot.image.card.CardMS
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.maimai.MaiAssets
import com.now.nowbot.image.maimai.MaiDraw
import com.now.nowbot.image.maimai.MaiMS
import com.now.nowbot.image.maimai.MaiRenderResult
import com.now.nowbot.image.maimai.maiImageAssets
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.model.maimai.MaiScore
import com.now.nowbot.model.maimai.MaiSong
import com.now.nowbot.service.messageServiceImpl.MaiScoreService

object PanelMS {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_CARD_A2 = "<g id=\"CardA2\">"
    private const val MARK_CARD_G = "<g id=\"CardG\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-MS1-1);\">"
    private const val PANEL_HEIGHT_TOKEN = "\${panelheight}"

    fun toSvg(
        param: MaiScoreService.MaiScoreParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): String = render(param, lease, remote).svg

    fun render(
        param: MaiScoreService.MaiScoreParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): MaiRenderResult {
        val report = RenderReport()
        val assets = maiImageAssets(lease, remote, report)
        return MaiRenderResult(render(param, assets, report), report)
    }

    fun render(
        param: MaiScoreService.MaiScoreParam,
        assets: ImageAssets,
        report: RenderReport = RenderReport(),
    ): String {
        val song = param.songs.firstOrNull()
            ?: throw IllegalArgumentException("MS 需要至少一首歌曲")
        val cards = applyScore(song, param.scores, assets)
        val length = cards.size.coerceAtLeast(1)
        val interval = if (length == 5) 22.0 else 30.0
        val x = (1920.0 + interval - (interval + 350.0) * length) / 2.0

        var svg = Svg.removeXmlDeclaration(Templates.read("Panel_MS.svg"))
        svg = svg.replace(PANEL_HEIGHT_TOKEN, "1080")

        svg = Svg.setText(
            svg,
            MaiDraw.panelName("Maimai Song Info (!ymms)", "MS"),
            MARK_INDEX,
        )

        val cardBodies = cards.mapIndexed { index, card ->
            Svg.group(x + index * (interval + 350.0), 330.0, card)
        }.joinToString("\n")
        svg = Svg.setText(svg, cardBodies, MARK_CARD_G)

        val cardA2 = CardA2.render(MaiMS.songToCardA2(song, param.cabinet.name, assets), assets)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, cardA2), MARK_CARD_A2)

        val banner = MaiAssets(assets).randomBanner(
            MaiAssets.maimaiBannerIndex(song.songID, song.info.version),
        )
        svg = Svg.setText(
            svg,
            banner?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) },
            MARK_BANNER,
        )
        svg = Svg.setText(svg, MaiMS.componentG2(param.user ?: com.now.nowbot.model.maimai.MaiBestScore.User(), assets), MARK_INDEX)
        return svg
    }

    private fun applyScore(song: MaiSong, scores: List<MaiScore>, assets: ImageAssets): List<String> {
        return song.level.indices.map { index ->
            val star = song.star.getOrElse(index) { 0.0 }
            val score = scores.firstOrNull { it.type == song.type && it.star == star }
            CardMS.render(MaiMS.scoreToCardMS(song, index, score, assets))
        }
    }
}
