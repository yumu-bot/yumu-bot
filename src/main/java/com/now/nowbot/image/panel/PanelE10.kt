package com.now.nowbot.image.panel

import com.now.nowbot.image.component.e10.E10Accuracy
import com.now.nowbot.image.component.e10.E10Center
import com.now.nowbot.image.component.e10.E10Combo
import com.now.nowbot.image.component.e10.E10Density
import com.now.nowbot.image.component.e10.E10Labels
import com.now.nowbot.image.component.e10.E10Performance
import com.now.nowbot.image.component.e10.E10Star
import com.now.nowbot.image.component.e10.E10Statistics
import com.now.nowbot.image.component.e10.E10Status
import com.now.nowbot.image.component.e10.E10Title
import com.now.nowbot.image.component.e10.E10UserCard
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderAssetRegistry
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.service.messageServiceImpl.ScorePRService.PanelE5Param

data class E10RenderResult(val svg: String, val report: RenderReport)

object PanelE10 {
    private const val MARK_COMPONENT = "<g id=\"Component\">"
    private const val MARK_BACKGROUND = "<g id=\"Background\">"

    fun toSvg(
        param: PanelE5Param,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
        profileCard: String? = null,
        bannerIndex: Int? = null,
        background: String? = null,
    ): String = render(param, lease, remote, profileCard, bannerIndex, background).svg

    fun render(
        param: PanelE5Param,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
        profileCard: String? = null,
        bannerIndex: Int? = null,
        background: String? = null,
    ): E10RenderResult {
        val report = RenderReport()
        val assets = ImageAssets(
            RenderAssetRegistry { bytes -> lease.registerImage(bytes).url },
            remote,
            report = report,
        )
        return E10RenderResult(render(param, assets, report, profileCard, bannerIndex, background), report)
    }

    fun render(
        param: PanelE5Param,
        assets: ImageAssets,
        report: RenderReport = RenderReport(),
        profileCard: String? = null,
        bannerIndex: Int? = null,
        background: String? = null,
    ): String {
        val score = param.score
        val backgroundUrl = assets.resolveRef(background) ?: assets.mapBackground(score, "fullsize")
        var svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="1920" height="1080" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 1920 1080">
                <g id="Base">
                    <rect width="1920" height="1080" rx="0" ry="0" style="fill: #1c1719;"/>
                </g>
                <g id="Background">
                </g>
                <g id="Component">
                </g>
            </svg>
        """.trimIndent()

        val components = listOf(
            Svg.group(275.0, 35.0, E10UserCard.render(param.user, assets, profileCard, bannerIndex)),
            Svg.group(275.0, 295.0, E10Star.render(score)),
            Svg.group(275.0, 405.0, E10Status.render(score, assets)),
            Svg.group(275.0, 515.0, E10Density.render(score, param.density)),
            Svg.group(275.0, 655.0, E10Labels.render(score)),
            E10Center.render(score, param.progress, assets),
            Svg.group(1145.0, 35.0, E10Performance.render(score, param.attributes, assets)),
            Svg.group(1225.0, 295.0, E10Combo.render(score)),
            Svg.group(1330.0, 405.0, E10Accuracy.render(score)),
            Svg.group(1330.0, 515.0, E10Statistics.render(score)),
            Svg.group(275.0, 825.0, E10Title.render(score, backgroundUrl)),
        )
        val copyright = Fonts.poppinsBold.textPath(
            "powered by Yumubot - Show Panel", 960.0, 1070.0, 24.0, "center baseline", "#fff", 0.3,
        )
        svg = Svg.setTexts(svg, components + copyright, MARK_COMPONENT)
        svg = Svg.setText(
            svg,
            backgroundUrl?.let { Svg.image(0.0, 0.0, 1920.0, 1080.0, it, 0.6) },
            MARK_BACKGROUND,
        )
        return svg
    }

}
