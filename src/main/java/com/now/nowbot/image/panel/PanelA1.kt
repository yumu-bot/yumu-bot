package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.draw.randomBanner
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.userlist.PanelA1Param
import com.now.nowbot.image.userlist.UserListCards
import com.now.nowbot.image.userlist.UserListRenderResult
import com.now.nowbot.image.userlist.renderUserList

object PanelA1 {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_ME = "<g id=\"Me_Card_A1\">"
    private const val MARK_FRIEND = "<g id=\"Friend_Card_A1\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PA1-1);\">"

    fun toSvg(param: PanelA1Param, assets: ImageAssets): String {
        val friends = param.friends
        var svg = Templates.read("Panel_A1.svg")
        svg = Svg.setText(svg, PanelBatchSupport.panelName("Friends (!ymf)", "F"), MARK_INDEX)

        val me = CardA1.render(
            UserListCards.me(param.me, null, assets, null, param.bannerIndex),
            assets,
        )
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, me), MARK_ME)

        val friendCards = friends.mapIndexed { index, friend ->
            val card = CardA1.render(
                UserListCards.micro(friend, param.sortType, true, assets),
                assets,
            )
            Svg.group(40.0 + 470.0 * (index % 4), 330.0 + 250.0 * (index / 4), card)
        }
        svg = Svg.setText(svg, friendCards.joinToString("\n"), MARK_FRIEND)

        val panelHeight = PanelBatchSupport.panelHeight(friends.size, 210.0, 4, 290.0, 40.0, 40.0)
        svg = PanelBatchSupport.applyHeights(svg, panelHeight, panelHeight - 290.0)

        val banner = assets.resolveRef(param.profile?.banner) ?: assets.randomBanner(index = param.bannerIndex)
        svg = Svg.setText(svg, banner?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) }, MARK_BANNER)
        return svg
    }

    fun render(
        param: PanelA1Param,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
        report: RenderReport = RenderReport(),
    ): UserListRenderResult {
        return renderUserList(lease, remote, report) { assets -> toSvg(param, assets) }
    }
}
