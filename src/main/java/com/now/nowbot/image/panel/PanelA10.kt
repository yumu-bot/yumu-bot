package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.card.CardC2
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.randomBanner
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.userlist.PanelA10Param
import com.now.nowbot.image.userlist.UserListCards
import com.now.nowbot.image.userlist.UserListRenderResult
import com.now.nowbot.image.userlist.renderUserList

object PanelA10 {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_BODY = "<g id=\"Body_Card\">"
    private const val MARK_MAIN = "<g id=\"Main_Card\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PA10-1);\">"

    fun toSvg(param: PanelA10Param, assets: ImageAssets): String {
        val badges = param.badges.ifEmpty { param.user?.badges.orEmpty() }
        val panelHeight = PanelBatchSupport.panelHeight(badges.size, 110.0, 2, 290.0, 40.0, 40.0)
        val cardHeight = panelHeight - 290.0

        // 源端未提供横幅时会取下载失败兜底图；这里按 setCustomBanner 的本意回退到随机横幅。
        val banner = assets.resolveRef(param.banner) ?: assets.randomBanner(index = param.bannerIndex)

        var svg = """
    <?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1920" height="${Svg.number(panelHeight)}" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 1920 ${Svg.number(panelHeight)}">
    <defs>
        <clipPath id="clippath-PA10-1">
            <rect width="1920" height="320" rx="20" ry="20" style="fill: none;"/>
        </clipPath>
        <clipPath id="clippath-PA10-2">
            <rect x="480" y="330" width="1410" height="240" rx="0" ry="0" style="fill: none;"/>
        </clipPath>
    </defs>
    <g id="Banner">
        <rect width="1920" height="320" rx="20" ry="20" style="fill: #1c1719;"/>
        <g style="clip-path: url(#clippath-PA10-1);">
        </g>
    </g>
    <g id="Background">
        <rect y="290" width="1920" height="${Svg.number(cardHeight)}" rx="20" ry="20" style="fill: #2a2226;"/>
    </g>
    <g id="Main_Card">
    </g>
    <g id="Body_Card">
    </g>
    <g id="IndexBase">
        <rect x="510" y="40" width="195" height="60" rx="15" ry="15" style="fill: #382e32;"/>
    </g>
    <g id="Index">
    </g>
</svg>
        """.trimIndent()

        svg = Svg.setText(svg, PanelBatchSupport.panelName("Badge (!ymbd)", "BD"), MARK_INDEX)

        val mainCard = CardA1.render(
            UserListCards.me(param.user, null, assets, param.profile?.card, param.bannerIndex),
            assets,
        )
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, mainCard), MARK_MAIN)

        val badgeCards = badges.mapIndexed { index, badge ->
            val card = CardC2.render(CardC2.fromBadge(badge, index + 1, assets), assets)
            Svg.group(40.0 + 940.0 * (index % 2), 330.0 + 150.0 * (index / 2), card)
        }
        svg = Svg.setText(svg, badgeCards.joinToString("\n"), MARK_BODY)

        return Svg.setText(svg, banner?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) }, MARK_BANNER)
    }

    fun render(
        param: PanelA10Param,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
        report: RenderReport = RenderReport(),
    ): UserListRenderResult {
        return renderUserList(lease, remote, report) { assets -> toSvg(param, assets) }
    }
}
