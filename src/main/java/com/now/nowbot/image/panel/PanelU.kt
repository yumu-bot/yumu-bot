package com.now.nowbot.image.panel

import com.now.nowbot.entity.UserProfileLite
import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.randomBanner
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.userlist.PanelUParam
import com.now.nowbot.image.userlist.UserListCards
import com.now.nowbot.image.userlist.UserListRenderResult
import com.now.nowbot.image.userlist.renderUserList
import com.now.nowbot.image.util.GameModes
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.model.osu.OsuUser

object PanelU {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_BODY = "<g id=\"Body_Card\">"
    private const val MARK_MAIN = "<g id=\"Main_Card\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PU-1);\">"
    private const val MARK_BG_BLUR =
        "<g id=\"Background_Blur\" style=\"clip-path: url(#clippath-PU-2);\" filter=\"url(#blur-PU-1)\">"
    private const val MARK_BG_LEFT = "<g id=\"Background_Left\" mask=\"url(#mask-PU-1)\">"
    private const val MARK_BG_RIGHT = "<g id=\"Background_Right\" mask=\"url(#mask-PU-2)\">"
    private const val MARK_BG_SILK = "<g id=\"Background_Silk\">"
    private const val MARK_BG_PIPPI = "<g id=\"Background_Pippi\">"

    private val TEMPLATE = """
    <?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1920" height="1080" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 1920 1080">
    <defs>
        <clipPath id="clippath-PU-1">
            <rect width="1920" height="320" rx="20" ry="20" style="fill: none;"/>
        </clipPath>
        <clipPath id="clippath-PU-2">
            <rect x="0" y="290" width="1920" height="790" rx="20" ry="20" style="fill: none;"/>
        </clipPath>
        <filter id="blur-PU-1" height="120%" width="120%" x="-10%" y="-10%" filterUnits="userSpaceOnUse">
            <feGaussianBlur in="userSpaceOnUse" stdDeviation="15" result="blur"/>
        </filter>
        <linearGradient id="linear-PU-1" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" style="stop-color:rgb(255,255,255);stop-opacity:1" />
            <stop offset="100%" style="stop-color:rgb(255,255,255);stop-opacity:0" />
        </linearGradient>
        <linearGradient id="linear-PU-2" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" style="stop-color:rgb(255,255,255);stop-opacity:0" />
            <stop offset="100%" style="stop-color:rgb(255,255,255);stop-opacity:1" />
        </linearGradient>
        <mask id="mask-PU-1">
            <rect y="290" width="1220" height="790" rx="20" ry="20" fill="url(#linear-PU-1)"/>
        </mask>
        <mask id="mask-PU-2">
            <rect x="700" y="290" width="1220" height="790" rx="20" ry="20" fill="url(#linear-PU-2)"/>
        </mask>
    </defs>
    <g id="Banner">
        <rect width="1920" height="320" rx="20" ry="20" style="fill: #1c1719;"/>
        <g style="clip-path: url(#clippath-PU-1);">
        </g>
    </g>
    <g id="Background">
        <rect y="290" width="1920" height="790" rx="20" ry="20" style="fill: #2a2226;"/>
    </g>
    <g id="Background_Additional">
        <g id="Background_Blur" style="clip-path: url(#clippath-PU-2);" filter="url(#blur-PU-1)">
        </g>
        <g id="Background_Left" mask="url(#mask-PU-1)">
        </g>
        <g id="Background_Right" mask="url(#mask-PU-2)">
        </g>
        <g id="Background_Silk">
        </g>
        <g id="Background_Pippi">
        </g>
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

    fun toSvg(param: PanelUParam, assets: ImageAssets): String {
        val statistics = requireNotNull(param.statistics) { "PanelU requires statistics" }
        val mutual = statistics.isFollowing && statistics.isFollowed == true

        var backgroundBlur = ""
        var backgroundSilk = ""
        var mutualPippi = ""
        var leftBackground: String? = null
        var rightBackground: String? = null
        var centerLabel = ""
        val leftLabel: String
        val rightLabel: String

        if (mutual) {
            backgroundBlur = image(0.0, 290.0, 1920.0, 790.0, assets.local("object-score-backimage-S.webp"), 0.6)
            backgroundSilk = image(0.0, 474.0, 1920.0, 388.0, assets.local("online-background-silk.webp"))
            mutualPippi = image(696.0, 330.0, 528.0, 750.0, assets.local("online-supporter-pippi.webp"))
            centerLabel = Svg.group(
                760.0,
                960.0,
                labelU2(null, "#F58EE8", "#A764CF", "Best Friends", "#fff", 200.0, true),
            )
            leftLabel = Svg.group(
                540.0,
                960.0,
                labelU2(null, "#F193E7", "#F66E8F", "Mutual", "#fff", 120.0, true),
            )
            rightLabel = Svg.group(
                980.0,
                960.0,
                labelU2(null, "#F193E7", "#F66E8F", "Mutual", "#fff", 280.0, false),
            )
        } else {
            val left = followVisual(statistics.isFollowing)
            val right = followVisual(statistics.isFollowed)

            leftBackground = left.background
            rightBackground = right.background

            leftLabel = Svg.group(
                540.0,
                960.0,
                labelU2(left.image?.let { assets.local(it) }, left.color1, left.color2, left.text, left.textColor, 200.0, true),
            )
            rightLabel = Svg.group(
                980.0,
                960.0,
                labelU2(right.image?.let { assets.local(it) }, right.color1, right.color2, right.text, right.textColor, 200.0, false),
            )
        }

        val backgroundLeft = backgroundOf(leftBackground, assets)
        val backgroundRight = backgroundOf(rightBackground, assets)

        var svg = TEMPLATE
        svg = Svg.setText(svg, PanelBatchSupport.panelName("Cupid Friends (!ymf)", "F"), MARK_INDEX)
        svg = Svg.setText(
            svg,
            assets.randomBanner(index = param.bannerIndex)?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) },
            MARK_BANNER,
        )

        val placeholder = CardA1.render(CardA1.factory(null, null, assets), assets)
        val cards = listOf(
            cardU1(
                param.user,
                param.user?.pmFriendsOnly == false,
                statistics.userBind,
                statistics.userFollowing,
                param.userProfile,
                assets,
            ),
            cardU1(
                param.partner,
                param.partner?.pmFriendsOnly == false,
                statistics.partnerBind,
                statistics.partnerFollowing,
                param.partnerProfile,
                assets,
            ),
        )

        svg = Svg.setText(svg, backgroundSilk, MARK_BG_SILK)
        svg = Svg.setText(svg, mutualPippi, MARK_BG_PIPPI)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, placeholder), MARK_MAIN)
        svg = Svg.setText(svg, backgroundBlur, MARK_BG_BLUR)
        svg = Svg.setText(svg, backgroundLeft, MARK_BG_LEFT)
        svg = Svg.setText(svg, backgroundRight, MARK_BG_RIGHT)
        return Svg.setTexts(
            svg,
            listOf(
                Svg.group(40.0, 330.0, cards[0]),
                Svg.group(1420.0, 330.0, cards[1]),
                centerLabel,
                leftLabel,
                rightLabel,
            ),
            MARK_BODY,
        )
    }

    fun render(
        param: PanelUParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
        report: RenderReport = RenderReport(),
    ): UserListRenderResult {
        return renderUserList(lease, remote, report) { assets -> toSvg(param, assets) }
    }

    private data class FollowVisual(
        val image: String?,
        val color1: String,
        val color2: String,
        val text: String,
        val textColor: String,
        val background: String?,
    )

    private fun followVisual(following: Boolean?): FollowVisual {
        return when (following) {
            true -> FollowVisual(
                "online-supporter-required.webp", "#49F9D7", "#54EC8B", "Following", "#fff",
                "object-score-backimage-A.webp",
            )

            false -> FollowVisual(
                "online-not-found.webp", "#22DCFD", "#5865B9", "Not Following", "#fff",
                "object-score-backimage-B.webp",
            )

            null -> FollowVisual(
                "online-avatar-guest.png", "#507DA0", "#253A4B", "Unknown", "#aaa",
                "object-score-backimage-F.webp",
            )
        }
    }

    private fun image(x: Double, y: Double, width: Double, height: Double, href: String?, opacity: Double = 1.0): String {
        val value = href ?: return ""
        return Svg.image(x, y, width, height, value, opacity)
    }

    private fun backgroundOf(name: String?, assets: ImageAssets): String {
        val value = assets.local(name ?: return "") ?: return ""
        return Svg.image(0.0, 290.0, 1920.0, 790.0, value, 0.6)
    }

    private fun cardU1(
        user: OsuUser?,
        allowPrivateMessage: Boolean,
        isBind: Boolean,
        following: Int,
        profile: UserProfileLite?,
        assets: ImageAssets,
    ): String {
        var svg = CARD_TEMPLATE

        val background = assets.resolveRef(profile?.card)
            ?: assets.resolveRef(user?.coverUrl)
            ?: assets.resolveRef(user?.cover?.url)
            ?: assets.localBanner()
        val avatar = UserListCards.avatarOf(user?.userID ?: 0L, user?.avatarUrl, assets)

        val modeIcon = if (user == null) {
            ""
        } else {
            Fonts.extra.textPath(
                GameModes.getGameMode(user.mode.shortName, -1),
                30.0, 66.0, 40.0, "left baseline", "#fff",
            )
        }
        val supporterIcon = if (user?.isSupporter == true) {
            image(390.0, 30.0, 40.0, 40.0, assets.local("object-card-supporter.png"))
        } else {
            ""
        }

        val name = Fonts.torus.textPath(
            Fonts.torus.cutStringTail(user?.username, 60.0, 450.0),
            230.0, 300.0, 60.0, "center baseline",
        )

        val statistics = user?.statistics
        val highestRank = user?.highestRank
        val globalRank = statistics?.globalRank
        val rankUpdated = highestRank?.updatedAt
        val globalRankText = when {
            globalRank != null -> "#$globalRank"
            highestRank != null && rankUpdated != null -> "#${highestRank.rank}^"
            else -> "#0"
        }
        val countryRankText = when {
            globalRank != null -> "#${statistics?.countryRank ?: 0}"
            rankUpdated != null -> "(${TimeFormat.difference(rankUpdated)})"
            else -> "#0"
        }

        val globalRankPath = Fonts.torusBold.textPath(globalRankText, 190.0, 360.0, 30.0, "right baseline")
        val countryFlag = assets.countryFlag(user?.country?.code, 205.0, 320.0, 44.0)
        val countryRankPath = Fonts.torusBold.textPath(countryRankText, 270.0, 360.0, 30.0, "left baseline")
        val pp = Fonts.torus.get2SizeTextPath(
            Numbers.jsRound(user?.pp ?: 0.0).toLong().toString(),
            " PP", 72.0, 48.0, 235.0, 440.0, "center baseline",
        )

        val followingLabel = labelU1(
            assets.local("Icons/player-follow.png"),
            "Following",
            if (following < 0) "?" else following.toString(),
            " / ${(user?.maxFriends ?: 0).takeIf { it > 0 } ?: 500}",
            "#FD5392",
            "#F86F64",
            420.0,
        )
        val followerLabel = labelU1(
            assets.local("Icons/player.png"),
            "Follower",
            (user?.followerCount ?: 0).toString(),
            " (+${user?.mappingFollowerCount ?: 0})",
            "#F988E7",
            "#9F56CB",
            420.0,
        )
        val bindLabel = labelU1(
            assets.local("Icons/collections.png"),
            "Bind",
            if (isBind) "ON" else "OFF",
            "",
            if (isBind) "#F2F047" else "#507DA0",
            if (isBind) "#1ED94F" else "#253A4B",
            200.0,
        )
        val pmLabel = labelU1(
            assets.local("Icons/chat.png"),
            "DM",
            if (allowPrivateMessage) "ON" else "OFF",
            "",
            if (allowPrivateMessage) "#01F2FE" else "#507DA0",
            if (allowPrivateMessage) "#4FACFE" else "#253A4B",
            200.0,
        )

        val labels = Svg.group(20.0, 470.0, followingLabel) +
            Svg.group(20.0, 550.0, followerLabel) +
            Svg.group(20.0, 630.0, bindLabel) +
            Svg.group(240.0, 630.0, pmLabel)

        svg = Svg.setText(svg, background?.let { Svg.image(0.0, 0.0, 460.0, 710.0, it, 0.6) }, MARK_CARD_BG)
        svg = Svg.setText(svg, avatar?.let { Svg.image(130.0, 40.0, 200.0, 200.0, it) }, MARK_CARD_AVATAR)
        svg = Svg.setText(svg, labels, MARK_CARD_LABEL)
        svg = Svg.setTexts(svg, listOf(modeIcon, supporterIcon), MARK_CARD_ICON)
        return Svg.setTexts(svg, listOf(pp, name, countryRankPath, globalRankPath, countryFlag), MARK_CARD_TEXT)
    }

    private fun labelU1(
        icon: String?,
        title: String,
        large: String,
        small: String,
        color1: String,
        color2: String,
        width: Double,
    ): String {
        val iconImage = image(16.0, 16.0, 30.0, 30.0, icon)
        val base = PanelDraw.rect(0.0, 0.0, width, 60.0, 30.0, "#46393F")
        val rrect = PanelDraw.linearGradientRect(
            0.0, 0.0, width, 60.0, 30.0, listOf(color1, color2), listOf(0, 100), listOf(40, 60), 0.6,
        )
        val iconBackground = PanelDraw.circle(31.0, 30.0, 22.0, "#54454C", 1.0)
        val titlePath = Fonts.torusBold.textPath(title, 60.0, 22.0, 18.0, "left baseline")
        val value = Fonts.torusBold.get2SizeTextPath(large, small, 40.0, 30.0, width - 20.0, 44.0, "right baseline")
        return "<g>$base$rrect$value$titlePath$iconBackground$iconImage</g>"
    }

    private fun labelU2(
        image: String?,
        color1: String,
        color2: String,
        text: String,
        textColor: String,
        center: Double,
        towardsRight: Boolean,
    ): String {
        val base = PanelDraw.rect(0.0, 0.0, 400.0, 80.0, 40.0, "#382E32")
        val rrect = PanelDraw.linearGradientRect(
            0.0, 0.0, 400.0, 80.0, 40.0, listOf(color1, color2), listOf(0, 100), listOf(40, 60), 1.0,
        )
        val textPath = Fonts.torusBold.textPath(text, center, 56.0, 48.0, "center baseline", textColor)
        val imagePath = image?.let {
            val direction = if (towardsRight) 1 else -1
            val x = if (towardsRight) 0 else -400
            "<image width=\"400\" height=\"370\" transform=\"scale($direction, 1) translate($x -370)\" " +
                "href=\"${Svg.escape(it)}\" style=\"opacity: 1\" preserveAspectRatio=\"xMidYMid slice\" " +
                "vector-effect=\"non-scaling-stroke\"/>"
        }.orEmpty()
        return "<g>$base$rrect$textPath$imagePath</g>"
    }

    private val CARD_TEMPLATE = """   <defs>
            <clipPath id="clippath-CU1-1">
              <rect x="0" width="460" height="710" rx="40" ry="40" style="fill: none;"/>
            </clipPath>
            <clipPath id="clippath-CU1-2">
              <circle cx="230" cy="140" r="100" style="fill: none;"/>
            </clipPath>
            <filter id="blur-CU1-1" height="120%" width="120%" x="-10%" y="-10%" filterUnits="userSpaceOnUse">
                <feGaussianBlur in="userSpaceOnUse" stdDeviation="15" result="blur"/>
            </filter>
          </defs>
          <g id="Base_CU1">
            <rect width="460" height="710" rx="40" ry="40" style="fill: #382E32;"/>
          </g>
          <g id="Background_CU1">
            <rect width="460" height="710" rx="40" ry="40" style="fill: #382E32;"/>
            <g style="clip-path: url(#clippath-CU1-1);" filter="url(#blur-CU1-1)">
            </g>
          </g>
          <g id="Avatar_CU1">
            <circle cx="230" cy="140" r="100" style="fill: #382E32;"/>
            <g style="clip-path: url(#clippath-CU1-2);">
            </g>
          </g>
          <g id="Text_CU1">
          </g>
          <g id="Icon_CU1">
          </g>
          <g id="Label_CU1">
          </g>"""

    private const val MARK_CARD_TEXT = "<g id=\"Text_CU1\">"
    private const val MARK_CARD_ICON = "<g id=\"Icon_CU1\">"
    private const val MARK_CARD_LABEL = "<g id=\"Label_CU1\">"
    private const val MARK_CARD_AVATAR = "<g style=\"clip-path: url(#clippath-CU1-2);\">"
    private const val MARK_CARD_BG = "<g style=\"clip-path: url(#clippath-CU1-1);\" filter=\"url(#blur-CU1-1)\">"
}
