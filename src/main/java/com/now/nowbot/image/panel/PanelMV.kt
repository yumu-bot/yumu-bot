package com.now.nowbot.image.panel

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.maimai.MaiAssets
import com.now.nowbot.image.maimai.MaiCardA1
import com.now.nowbot.image.maimai.MaiDraw
import com.now.nowbot.image.maimai.MaiMV
import com.now.nowbot.image.maimai.MaiRenderResult
import com.now.nowbot.image.maimai.maiImageAssets
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.service.messageServiceImpl.MaiVersionScoreService
import kotlin.math.max

object PanelMV {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_CARD_A1 = "<g id=\"CardA1\">"
    private const val MARK_CARD_I = "<g id=\"CardI\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-MA1-1);\">"

    fun toSvg(
        param: MaiVersionScoreService.MaiVersionResponse,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): String = render(param, lease, remote).svg

    fun render(
        param: MaiVersionScoreService.MaiVersionResponse,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): MaiRenderResult {
        val report = RenderReport()
        val assets = maiImageAssets(lease, remote, report)
        return MaiRenderResult(render(param, assets, report), report)
    }

    fun render(
        param: MaiVersionScoreService.MaiVersionResponse,
        assets: ImageAssets,
        report: RenderReport = RenderReport(),
    ): String {
        val groups = param.plateList
        val bodies = StringBuilder()
        val backgrounds = StringBuilder()
        var currentRow = 0

        for (group in groups) {
            if (group.count == 0) continue

            val label = MaiMV.label(group)
            backgrounds.append(MaiMV.area(currentRow * 12, areaEnd(group, currentRow), MaiMV.levelColor(group.star), 0.4))
            backgrounds.append(Svg.group(40.0, 330.0 + 146.0 * currentRow, label))

            var slot = 1
            for (progress in group.progress) {
                val x = 40.0 + 155.0 * (slot % 12)
                val y = 330.0 + 146.0 * (currentRow + slot / 12)
                bodies.append(Svg.group(x, y, MaiMV.card(progress, assets)))
                slot++
            }

            currentRow += (slot - 1) / 12 + 1
        }

        val cardsHeight = max(currentRow * 146.0 - 20.0, 0.0)
        val cardHeight = cardsHeight + 80.0
        val panelHeight = cardHeight + 290.0

        var svg = Svg.removeXmlDeclaration(Templates.read("Panel_MA.svg"))
        svg = PanelBatchSupport.applyHeights(svg, panelHeight, cardHeight)
        svg = Svg.setText(svg, MaiDraw.panelName("Maimai Version Plate (!ymmv)", "MV"), MARK_INDEX)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, MaiCardA1.render(param.user, assets)) + Svg.group(1300.0, 40.0, MaiMV.component(param, assets)), MARK_CARD_A1)
        svg = Svg.setText(svg, backgrounds.toString() + bodies.toString(), MARK_CARD_I)

        val banner = MaiAssets(assets).randomBanner()
        svg = Svg.setText(
            svg,
            banner?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) },
            MARK_BANNER,
        )
        return svg
    }

    private fun areaEnd(group: MaiVersionScoreService.MaiPlateList, currentRow: Int): Int {
        val slots = 1 + group.progress.size
        val start = currentRow * 12
        return if (group.star == "12-") start else start + slots - 1
    }
}
