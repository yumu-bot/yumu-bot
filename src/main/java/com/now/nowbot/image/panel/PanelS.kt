package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderAssetRegistry
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.randomBanner
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.font.TextPart
import com.now.nowbot.image.font.Typography
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.ColorArray
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.GameModes
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.PanelColor
import com.now.nowbot.image.util.Texts
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.multiplayer.RoomRound
import com.now.nowbot.model.multiplayer.RoomStatistics
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.service.messageServiceImpl.QuickplayInfoService
import com.now.nowbot.service.web.QuickplayLeaderboardItem
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class PanelSParam(
    val user: OsuUser,
    val stats: QuickplayInfoService.QuickplayStatistics = QuickplayInfoService.QuickplayStatistics(0, 0, 0, 0.0, 0.0),
    val recently: List<RoomStatistics> = emptyList(),
    val surrounding: List<QuickplayLeaderboardItem> = emptyList(),
    val rating: Double = 0.0,
    val totalPlayers: Int = 0,
    val variant: Byte = 0,
    val banner: String? = null,
    val bannerIndex: Int? = null,
) {
    val mode: OsuMode get() = user.mode

    companion object {
        fun fromMap(body: Map<String, Any>): PanelSParam {
            val user = body["user"] as? OsuUser ?: OsuUser()
            val stats = body["stats"] as? QuickplayInfoService.QuickplayStatistics
                ?: QuickplayInfoService.QuickplayStatistics(0, 0, 0, 0.0, 0.0)
            return PanelSParam(
                user = user,
                stats = stats,
                recently = (body["recently"] as? Collection<*>).orEmpty().filterIsInstance<RoomStatistics>(),
                surrounding = (body["surrounding"] as? Collection<*>).orEmpty().filterIsInstance<QuickplayLeaderboardItem>(),
                rating = (body["rating"] as? Number)?.toDouble() ?: 0.0,
                totalPlayers = (body["total_players"] as? Number)?.toInt() ?: 0,
                variant = (body["variant"] as? Number)?.toByte() ?: 0,
            )
        }
    }
}

data class PanelSRenderResult(val svg: String, val report: RenderReport)

object PanelS {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PS-1);\">"
    private const val MARK_COMPONENT = "<g id=\"Component\">"
    private const val MARK_CARD_A1 = "<g id=\"Card_A1\">"
    private const val MARK_BACKGROUND = "<g filter=\"url(#blur-PS-BG)\" style=\"clip-path: url(#clippath-PS-BG);\">"

    fun toSvg(param: PanelSParam, assets: ImageAssets): String {
        val user = param.user
        val hue = user.profileHue
        val mm = param.user.matchmakingStats
        val mmFirst = if (param.variant.toInt() > 0) {
            mm.firstOrNull { it.pool.variantID == param.variant } ?: OsuUser.MatchmakingStats()
        } else {
            mm.firstOrNull() ?: OsuUser.MatchmakingStats()
        }

        val title = PanelBatchSupport.panelName("Ranked Play Info", "RI")
        var svg = template(hue)
        svg = Svg.setText(svg, title, MARK_INDEX)

        val s1 = componentS1(
            rank = mmFirst.rank,
            totalPlayers = param.totalPlayers,
            mode = param.mode,
            percent = mmFirst.rankPercent,
            provisional = mmFirst.provisional,
            hue = hue,
        )
        val s2 = componentS2(param.rating, mmFirst.rating, hue)
        val s4 = componentS4(param.recently, user.userID, assets, hue)
        val s5 = componentS5(mmFirst.firstPlacements, mmFirst.plays, hue)
        val s6 = componentS6(param.surrounding, assets, hue)
        val s3 = componentS3(param.stats, mmFirst.plays, assets, hue)

        val cardA1 = cardA1(user, assets, param.bannerIndex)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, cardA1), MARK_CARD_A1)
        svg = Svg.setTexts(
            svg,
            listOf(
                Svg.group(40.0, 330.0, s1),
                Svg.group(40.0, 700.0, s2),
                Svg.group(500.0, 330.0, s3),
                Svg.group(500.0, 560.0, s4),
                Svg.group(1450.0, 330.0, s5),
                Svg.group(1450.0, 500.0, s6),
            ),
            MARK_COMPONENT,
        )

        val banner = param.banner?.takeIf { it.isNotBlank() }?.let(assets::resolveRef)
            ?: assets.randomBanner("default", param.bannerIndex)
        svg = Svg.setText(svg, banner?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) }, MARK_BANNER)
        svg = Svg.setText(
            svg,
            rankBackground(user, assets)?.let { Svg.image(0.0, 280.0, 1920.0, 1080.0, it, 0.6) },
            MARK_BACKGROUND,
        )
        return svg
    }

    fun render(
        param: PanelSParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
        report: RenderReport = RenderReport(),
    ): PanelSRenderResult {
        val assets = ImageAssets(
            RenderAssetRegistry { bytes -> lease.registerImage(bytes).url },
            remote,
            report = report,
        )
        return PanelSRenderResult(toSvg(param, assets), report)
    }

    private fun template(hue: Int): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1920" height="1080" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 1920 1080">
    <defs>
        <clipPath id="clippath-PS-1">
            <rect width="1920" height="320" rx="20" ry="20" style="fill: none;"/>
        </clipPath>
        <clipPath id="clippath-PS-BG">
        <rect y="290" width="1920" height="790" rx="30" ry="30" style="fill: none;"/>
        </clipPath>
        <filter id="blur-PS-BG" height="110%" width="110%" x="-5%" y="-5%" filterUnits="userSpaceOnUse">
            <feGaussianBlur in="userSpaceOnUse" stdDeviation="15" result="blur"/>
        </filter>
    </defs>
    <g id="BannerBase">
        <rect width="1920" height="320" rx="20" ry="20" style="fill: ${PanelColor.base(hue)}"/>
    </g>
    <g id="Banner">
        <g style="clip-path: url(#clippath-PS-1);">
        </g>
    </g>
    <g id="Background">
        <rect y="290" width="1920" height="790" rx="30" ry="30" style="fill: ${PanelColor.bottom(hue)};"/>
        <g filter="url(#blur-PS-BG)" style="clip-path: url(#clippath-PS-BG);">
        </g>
    </g>
    <g id="Component">
    </g>
    <g id="Card_A1">
    </g>
    <g id="IndexBase">
        <rect x="510" y="40" width="195" height="60" rx="15" ry="15" style="fill: ${PanelColor.middle(hue)};"/>
    </g>
    <g id="Index">
    </g>
</svg>""".trimIndent()
    }

    private fun cardA1(user: OsuUser, assets: ImageAssets, bannerIndex: Int?): String {
        val data = CardA1.factory(user, null, assets)
        return CardA1.render(
            data.copy(
                background = assets.resolveRef(data.background) ?: assets.localBanner(bannerIndex),
                avatar = assets.resolveRef(data.avatar) ?: assets.local("avatar-guest.png"),
            ),
            assets,
        )
    }

    private fun componentS1(
        rank: Int,
        totalPlayers: Int,
        mode: OsuMode,
        percent: Double,
        provisional: Boolean,
        hue: Int,
    ): String {
        val ratio = rank.toDouble() / max(totalPlayers, 1)
        val colors = Colors.getGlobalRankPercentColor(rank.toLong(), ratio, rankedPlay = true)
        val rankName = rankedPlayName(rank, ratio)
        val base = rotateSvgBody(
            PanelDraw.linearGradientRect(135.0, 70.0, 160.0, 160.0, 20.0, colors, listOf(100, 0), listOf(60, 40), 1.0),
            215.0, 150.0, 45.0,
        )
        val random = Texts.random(6)
        val title = Fonts.poppinsBold.textPath("Rank", 15.0, 27.0, 18.0, "left baseline", "#fff")
        val rrect = PanelDraw.rect(0.0, 0.0, 430.0, 340.0, 20.0, PanelColor.middle(hue), 1.0)
        val provisionalText = if (provisional) " (provisional)" else ""
        val top = Fonts.poppinsBold.textPath(
            "Top ${Numbers.round(percent * 100, 2)}%$provisionalText",
            415.0, 27.0, 18.0, "right baseline", "#fff",
        )
        val modeText = Fonts.extra.textPath(
            GameModes.getGameMode(mode, -1), 215.0, 195.0, 128.0, "center baseline", "#fff",
        )
        val modeBase = PanelDraw.linearGradientRect(
            155.0, 90.0, 120.0, 120.0, 0.0,
            listOf(PanelColor.base(hue), PanelColor.middle(hue)), listOf(80, 20), listOf(60, 40), 1.0,
        )
        val rankText = Fonts.poppinsBold.textPath(rankName, 215.0, 305.0, 30.0, "center baseline", "#fff")

        return buildString {
            append("<g id=\"Component_S1\">")
            append("<defs><mask id=\"Mask_S1_$random\">")
            append(modeText)
            append("</mask></defs>")
            append(rrect)
            append(base)
            append("<g mask=\"url(#Mask_S1_$random)\">")
            append(modeBase)
            append("</g>")
            append(rankText)
            append(title)
            append(top)
            append("</g>")
        }
    }

    private fun componentS2(muRating: Double, mmRating: Int, hue: Int): String {
        val ratingRound = Numbers.roundParts(muRating, 2)
        val rating = Fonts.poppinsBold.get2SizeTextPath(
            ratingRound.integer, ratingRound.decimal, 90.0, 72.0, 215.0, 205.0, "center baseline", "#fff",
        )
        val elo = Fonts.poppinsBold.textPath(mmRating.toString(), 215.0, 305.0, 30.0, "center baseline", "#fff")
        val arc = roundedArcBar(215.0, 175.0, 105.0, 135.0, -45.0, 225.0, PanelColor.top(hue))
        val angle = 270.0
        val greenAngle = if (muRating < 1.0) {
            -45.0 + muRating * angle / 2
        } else {
            -45.0 + (min(1.0, muRating - 1.0) + 1.0) * angle / 2
        }
        val arcGreen = roundedArcBar(215.0, 175.0, 105.0, 135.0, -45.0, greenAngle, "#fff")
        val title = Fonts.poppinsBold.textPath("Rating", 15.0, 27.0, 18.0, "left baseline", "#fff")
        val rrect = PanelDraw.rect(0.0, 0.0, 430.0, 340.0, 20.0, PanelColor.middle(hue), 1.0)

        return buildString {
            append("<g id=\"Component_S2\">")
            append("<defs><mask id=\"arc-mask-1\">")
            append(arcGreen)
            append("</mask></defs>")
            append(rrect)
            append(title)
            append(elo)
            append(rating)
            append(arc)
            append("<g mask=\"url(#arc-mask-1)\">")
            append(PanelDraw.linearGradientRect(80.0, 40.0, 270.0, 270.0, 0.0, ColorArray.cyan))
            append("</g>")
            append("</g>")
        }
    }

    private fun componentS3(
        stats: QuickplayInfoService.QuickplayStatistics,
        plays: Int,
        assets: ImageAssets,
        hue: Int,
    ): String {
        val sr = Numbers.roundParts(stats.averageStarRating, 2)
        val acc = Numbers.roundParts(stats.averageAccuracy * 100, 2)
        val totalSec = stats.playTime
        val hours = floor(totalSec / 3600.0).toInt()
        val minutes = floor((totalSec % 3600) / 60.0).toInt()
        val seconds = totalSec % 60

        val labels = listOf(
            SizedLabel(LabelS.PC, plays.toString(), "", normalize(plays, LabelS.PC.barMax, LabelS.PC.barMin)),
            SizedLabel(LabelS.MAP_PC, stats.mapCount.toString(), "", normalize(stats.mapCount, LabelS.MAP_PC.barMax, LabelS.MAP_PC.barMin)),
            SizedLabel(LabelS.SR, sr.integer, sr.decimal, normalize(stats.averageStarRating, LabelS.SR.barMax, LabelS.SR.barMin)),
            SizedLabel(LabelS.PT, "${hours}h", " ${minutes}m ${seconds}s", normalize(hours.toDouble(), LabelS.PT.barMax, LabelS.PT.barMin)),
            SizedLabel(LabelS.COMBO, stats.averageCombo.toString(), "", normalize(stats.averageCombo.toDouble(), LabelS.COMBO.barMax, LabelS.COMBO.barMin)),
            SizedLabel(LabelS.ACC, acc.integer, "${acc.decimal}%", normalize(stats.averageAccuracy * 100, LabelS.ACC.barMax, LabelS.ACC.barMin)),
        )
        val bodies = labels.mapIndexed { index, label ->
            val x = 15.0 + 300.0 * (index % 3)
            val y = 40.0 + 75.0 * (index / 3)
            Svg.group(x, y, sizedLabel(label, assets, 280.0))
        }.joinToString("")

        val title = Fonts.poppinsBold.textPath("Mapping Statistics", 15.0, 27.0, 18.0, "left baseline", "#fff")
        val rrect = PanelDraw.rect(0.0, 0.0, 920.0, 200.0, 20.0, PanelColor.middle(hue), 1.0)
        return "<g id=\"Component_S3\">$rrect$title$bodies</g>"
    }

    private fun componentS4(
        recently: List<RoomStatistics>,
        me: Long,
        assets: ImageAssets,
        hue: Int,
    ): String {
        val cards = recently.mapIndexed { index, room ->
            Svg.group(20.0, 40.0 + index * 110, cardS1(room, me, assets, hue))
        }.take(4).joinToString("\n")
        val title = Fonts.poppinsBold.textPath("Match History", 15.0, 27.0, 18.0, "left baseline", "#fff")
        val rrect = PanelDraw.rect(0.0, 0.0, 920.0, 480.0, 20.0, PanelColor.middle(hue), 1.0)
        return "<g id=\"Component_S4\">$rrect$title$cards</g>"
    }

    private fun componentS5(firstPlacements: Int, plays: Int, hue: Int): String {
        val winRate = firstPlacements.toDouble() / max(1, plays)
        val wr = Numbers.roundParts(winRate * 100, 2)
        val text = Typography.multipleTextPath(
            listOf(
                TextPart(wr.integer, Fonts.poppinsBold, 84.0, "#fff"),
                TextPart(wr.decimal + "%", Fonts.poppinsBold, 48.0, "#fff"),
                TextPart(" [$firstPlacements/$plays]", Fonts.poppinsBold, 24.0, "#fff"),
            ),
            20.0, 88.0, "left baseline",
        )
        val progress = if (winRate > 1e-4) Numbers.clamp(winRate * 395.0, 395.0, 30.0) else 0.0
        val winRrect = PanelDraw.linearGradientRect(20.0, 105.0, progress, 30.0, 15.0, ColorArray.lightGreen)
        val loseRrect = PanelDraw.linearGradientRect(20.0, 105.0, 395.0, 30.0, 15.0, ColorArray.red)
        val title = Fonts.poppinsBold.textPath("Win Rate", 415.0, 27.0, 18.0, "right baseline", "#fff")
        val rrect = PanelDraw.rect(0.0, 0.0, 430.0, 150.0, 20.0, PanelColor.middle(hue), 1.0)
        return "<g id=\"Component_S5\">$rrect$title$loseRrect$winRrect$text</g>"
    }

    private fun componentS6(
        surrounding: List<QuickplayLeaderboardItem>,
        assets: ImageAssets,
        hue: Int,
    ): String {
        val cards = surrounding.mapIndexed { index, item ->
            Svg.group(20.0, 45.0 + index * 70, cardS2(item, assets, hue))
        }.take(7).joinToString("\n")
        val title = Fonts.poppinsBold.textPath("Your Ranking", 15.0, 27.0, 18.0, "left baseline", "#fff")
        val rrect = PanelDraw.rect(0.0, 0.0, 430.0, 540.0, 20.0, PanelColor.middle(hue), 1.0)
        return "<g id=\"Component_S6\">$rrect$title$cards</g>"
    }

    private fun cardS1(room: RoomStatistics, me: Long, assets: ImageAssets, hue: Int): String {
        val random = Texts.random(6)
        val defs = "<defs>" +
            "<clipPath id=\"clippath-S1-1-$random\">" +
            PanelDraw.rect(110.0, 0.0, 60.0, 60.0, 20.0, "none") +
            "</clipPath>" +
            "<clipPath id=\"clippath-S1-2-$random\">" +
            PanelDraw.rect(710.0, 0.0, 60.0, 60.0, 20.0, "none") +
            "</clipPath></defs>"

        val rounds = room.rounds.sortedBy { it.playlistID }
        val lastRound = rounds.lastOrNull()
        val leftID = room.players.getOrNull(0)
        val rightID = room.players.getOrNull(1)
        val leftFirst = room.wins.getOrNull(0) ?: 0
        val rightFirst = room.wins.getOrNull(1) ?: 0
        val leftHealth = lastRound?.scores?.getOrNull(0)?.health ?: 0L
        val rightHealth = lastRound?.scores?.getOrNull(1)?.health ?: 0L

        val leftWin: Boolean?
        val rightWin: Boolean?
        if (leftHealth > rightHealth) {
            leftWin = true
            rightWin = false
        } else if (leftHealth < rightHealth) {
            leftWin = false
            rightWin = true
        } else if (lastRound != null) {
            leftWin = lastRound.winnerID == leftID
            rightWin = lastRound.winnerID == rightID
        } else {
            leftWin = null
            rightWin = null
        }

        val leftLabel = Svg.group(0.0, 0.0, labelS1(leftWin, leftID != me, hue))
        val rightLabel = Svg.group(790.0, 0.0, labelS1(rightWin, rightID != me, hue))
        val rrect = PanelDraw.rect(110.0, 0.0, 660.0, 90.0, 20.0, PanelColor.top(hue))
        val top = PanelDraw.rect(110.0, 0.0, 660.0, 60.0, 20.0, PanelColor.overlay(hue))
        val baseHealth = PanelDraw.rect(180.0, 38.0, 255.0, 15.0, 7.5, PanelColor.top(hue)) +
            PanelDraw.rect(445.0, 38.0, 255.0, 15.0, 7.5, PanelColor.top(hue))

        val last7 = rounds.takeLast(7)
        val labelS2s = last7.mapIndexed { index, round ->
            val x = 125.0 + 81.0 * index
            val cover = roundCover(round, assets)
            Svg.group(x, 65.0, labelS2(cover, round.winnerID == me, hue))
        }.joinToString("\n")

        val leftProgress = Numbers.clamp(leftHealth.toDouble() / 1_000_000.0, 1.0, 0.0)
        val rightProgress = Numbers.clamp(rightHealth.toDouble() / 1_000_000.0, 1.0, 0.0)
        val leftProgressWidth = Numbers.clampToInteger(leftProgress * 255.0, 255.0, 15.0)
        val rightProgressWidth = Numbers.clampToInteger(rightProgress * 255.0, 255.0, 15.0)
        val leftWidth = Fonts.torusBold.width(leftHealth.toString(), 12.0)
        val rightWidth = Fonts.torusBold.width(rightHealth.toString(), 12.0)

        val leftHealthText: String
        val rightHealthText: String
        if ((1.0 - leftProgress) * 255.0 > leftWidth + 12.0) {
            leftHealthText = Fonts.torusBold.textPath(
                leftHealth.toString(), 440.0 - 5.0 - 8.0 - leftProgressWidth, 49.0, 12.0, "right baseline", "#fff",
            )
        } else {
            leftHealthText = Fonts.torusBold.textPath(
                leftHealth.toString(), 440.0 - 5.0 + 8.0 - leftProgressWidth, 49.0, 12.0, "left baseline", PanelColor.base(hue),
            )
        }
        if ((1.0 - rightProgress) * 255.0 > rightWidth + 12.0) {
            rightHealthText = Fonts.torusBold.textPath(
                rightHealth.toString(), 440.0 + 5.0 + 8.0 + rightProgressWidth, 49.0, 12.0, "left baseline", "#fff",
            )
        } else {
            rightHealthText = Fonts.torusBold.textPath(
                rightHealth.toString(), 440.0 + 5.0 - 8.0 + rightProgressWidth, 49.0, 12.0, "right baseline", PanelColor.base(hue),
            )
        }

        val vsText = Typography.multipleTextPath(
            listOf(
                TextPart(leftFirst.toString(), Fonts.poppinsBold, 30.0, "#fff"),
                TextPart("  vs  ", Fonts.poppinsBold, 18.0, "#fff"),
                TextPart(rightFirst.toString(), Fonts.poppinsBold, 30.0, "#fff"),
            ),
            440.0, 30.0, "center baseline",
        )
        val leftHealthSvg = PanelDraw.linearGradientRect(
            440.0 - 5.0 - leftProgressWidth, 38.0, leftProgressWidth, 15.0, 7.5, ColorArray.red, listOf(100, 0), listOf(0, 0), 1.0,
        )
        val rightHealthSvg = PanelDraw.linearGradientRect(
            440.0 + 5.0, 38.0, rightProgressWidth, 15.0, 7.5, ColorArray.cyan, listOf(0, 100), listOf(0, 0), 1.0,
        )
        val leftIndicator = PanelDraw.linearGradientRect(180.0, 12.0, 15.0, 15.0, 7.5, ColorArray.red, listOf(50, 50), listOf(100, 0), 1.0)
        val rightIndicator = PanelDraw.linearGradientRect(685.0, 12.0, 15.0, 15.0, 7.5, ColorArray.cyan, listOf(50, 50), listOf(100, 0), 1.0)
        val leftName = Fonts.poppinsBold.textPath(
            room.names.getOrNull(0) ?: "Unknown", 204.0, 25.0, 18.0, "left baseline", if (leftWin == true) "#fff" else "#aaa",
        )
        val rightName = Fonts.poppinsBold.textPath(
            room.names.getOrNull(1) ?: "Unknown", 676.0, 25.0, 18.0, "right baseline", if (rightWin == true) "#fff" else "#aaa",
        )
        val leftAvatar = "<g clip-path=\"url(#clippath-S1-1-$random)\">" +
            avatarImage(110.0, 0.0, 60.0, 60.0, leftID, assets) + "</g>"
        val rightAvatar = "<g clip-path=\"url(#clippath-S1-2-$random)\">" +
            avatarImage(710.0, 0.0, 60.0, 60.0, rightID, assets) + "</g>"

        return "<g id=\"Card_S1\">" + defs + rrect + top + baseHealth + leftHealthSvg + rightHealthSvg +
            leftIndicator + rightIndicator + leftAvatar + rightAvatar + leftLabel + rightLabel + labelS2s +
            leftHealthText + rightHealthText + vsText + leftName + rightName + "</g>"
    }

    private fun cardS2(item: QuickplayLeaderboardItem, assets: ImageAssets, hue: Int): String {
        val random = Texts.random(6)
        val defs = "<defs><clipPath id=\"Card_S2_$random\">" +
            PanelDraw.rect(0.0, 0.0, 55.0, 55.0, 15.0, "none") +
            "</clipPath></defs>"
        val avatar = "<g clip-path=\"url(#Card_S2_$random)\">" +
            (avatarId(item.userID, assets)?.let { Svg.image(0.0, 0.0, 55.0, 55.0, it) } ?: "") + "</g>"
        val country = assets.countryFlag(item.country.code, 65.0, 2.0, 24.0)
        val rrect = PanelDraw.rect(0.0, 0.0, 400.0, 55.0, 15.0, PanelColor.top(hue))
        val sub = Fonts.poppinsBold.textPath(
            "${item.wins} Wins [${item.playCount} Plays]", 65.0, 48.0, 16.0, "left baseline", "#fff",
        )
        val provisional = if (item.isProvisional) "*" else ""
        val rank = Fonts.poppinsBold.get2SizeTextPath("#", "${item.rank}$provisional", 16.0, 24.0, 385.0, 38.0, "right baseline", "#fff")
        val rankWidth = Fonts.poppinsBold.width("#", 16.0) + Fonts.poppinsBold.width("${item.rank}$provisional", 24.0)
        val name = Fonts.poppinsBold.textPath(
            Fonts.poppinsBold.cutStringTail(item.username, 24.0, 385.0 - 105.0 - rankWidth),
            105.0, 25.0, 24.0, "left baseline", "#fff",
        )
        return "<g id=\"Card_S2\">$defs$rrect$avatar$country$name$sub$rank</g>"
    }

    private fun labelS1(winCondition: Boolean?, notHighlight: Boolean, hue: Int): String {
        val colors: List<String>
        val char: String
        val text: String
        val offset: Int
        when (winCondition) {
            true -> {
                colors = ColorArray.lightGreen
                char = "W"
                text = "Win!"
                offset = 1
            }
            false -> {
                colors = ColorArray.red
                char = "L"
                text = "Lose..."
                offset = 2
            }
            else -> {
                colors = ColorArray.gray
                char = "Q"
                text = "Quit..."
                offset = 2
            }
        }
        val texts = Fonts.torusBold.textPath(text, 45.0 + offset, 75.0, 16.0, "center baseline", PanelColor.base(hue))
        val chars = Fonts.torusBold.textPath(char, 45.0, 52.0, 48.0, "center baseline", PanelColor.base(hue))
        val base = PanelDraw.rect(0.0, 0.0, 90.0, 90.0, 20.0, PanelColor.middle(hue), 1.0)
        val opacity = if (notHighlight) 0.2 else 1.0
        val color = PanelDraw.linearGradientRect(0.0, 0.0, 90.0, 90.0, 20.0, colors, listOf(80, 20), listOf(60, 40), opacity)
        return "<g id=\"Label_S1\">$base$color$texts$chars</g>"
    }

    private fun labelS2(cover: String?, winCondition: Boolean, hue: Int): String {
        val colors = if (winCondition) ColorArray.lightGreen else ColorArray.red
        val char = if (winCondition) "W" else "L"
        val random = Texts.random(6)
        val defs = "<defs><clipPath id=\"clippath-S2-$random\">" +
            PanelDraw.rect(0.0, 0.0, 40.0, 20.0, 5.0, "none") +
            "</clipPath></defs>"
        val image = "<g clip-path=\"url(#clippath-S2-$random)\">" +
            (cover?.let { Svg.image(0.0, 0.0, 40.0, 20.0, it) } ?: "") + "</g>"
        val chars = Fonts.torusBold.textPath(char, 55.0, 15.0, 14.0, "center baseline", PanelColor.base(hue))
        val rrect = PanelDraw.linearGradientRect(45.0, 0.0, 20.0, 20.0, 5.0, colors, listOf(80, 20), listOf(60, 40), 1.0)
        return "<g id=\"Label_S2\">$defs$rrect$chars$image</g>"
    }

    private fun sizedLabel(label: SizedLabel, assets: ImageAssets, width: Double): String {
        val labelLeft = 61.0
        val progressWidth = width - labelLeft
        val numberColor = "#fff"
        val iconTitle = Fonts.poppinsBold.textPath(label.spec.iconTitle, 25.0, 65.0, 14.0, "center baseline", "#fff")
        val number = Typography.multipleTextPath(
            listOf(
                TextPart(label.dataB, Fonts.poppinsBold, 36.0, numberColor),
                TextPart(label.dataM + " ", Fonts.poppinsBold, 24.0, numberColor),
                TextPart("", Fonts.poppinsBold, 16.0, numberColor),
            ),
            labelLeft, 30.0, "left baseline",
        )
        val barMin = Fonts.poppinsBold.textPath(Svg.number(label.spec.barMin), labelLeft, 65.0, 14.0, "left baseline", "#666")
        val barMid = Fonts.poppinsBold.textPath(Svg.number(label.spec.barMid), labelLeft + progressWidth / 2, 65.0, 14.0, "center baseline", "#666")
        val barMax = Fonts.poppinsBold.textPath(Svg.number(label.spec.barMax), width, 65.0, 14.0, "right baseline", "#666")
        val barWidth = Numbers.clampToInteger(label.progress * progressWidth, progressWidth, 10.0)
        val bar = if (label.spec.barColors.size == 2) {
            PanelDraw.linearGradientRect(labelLeft, 38.0, barWidth, 10.0, 5.0, label.spec.barColors, listOf(0, 100), listOf(0, 0), 1.0)
        } else {
            PanelDraw.rect(labelLeft, 38.0, barWidth, 10.0, 5.0, label.spec.barColor)
        }
        val barBase = if (label.spec.barColors.size == 2) {
            PanelDraw.linearGradientRect(labelLeft, 38.0, progressWidth, 10.0, 5.0, label.spec.barColors, listOf(0, 100), listOf(0, 0), 0.2)
        } else {
            PanelDraw.rect(labelLeft, 38.0, progressWidth, 10.0, 5.0, label.spec.barColor, 0.2)
        }
        val icon = label.spec.iconFile.takeIf { it.isNotEmpty() }?.let(assets::local)?.let {
            Svg.image(0.0, 0.0, 50.0, 50.0, it)
        }.orEmpty()
        return icon + iconTitle + number + barMin + barMid + barMax + barBase + bar
    }

    private fun roundCover(round: RoomRound, assets: ImageAssets): String? {
        val direct = round.covers?.list2x.orEmpty()
        val url = direct.takeIf { it.isNotBlank() }
            ?: if (round.beatmapsetID > 0) "https://assets.ppy.sh/beatmaps/${round.beatmapsetID}/covers/list@2x.jpg" else ""
        return url.takeIf { it.isNotBlank() }?.let(assets::remote) ?: assets.local("card-default.webp")
    }

    private fun avatarImage(x: Double, y: Double, width: Double, height: Double, id: Long?, assets: ImageAssets): String {
        val image = avatarId(id, assets) ?: return ""
        return Svg.image(x, y, width, height, image)
    }

    private fun avatarId(id: Long?, assets: ImageAssets): String? {
        val url = id?.takeIf { it > 0 }?.let { "https://a.ppy.sh/$it" } ?: return assets.local("avatar-guest.png")
        return assets.remote(url) ?: assets.local("avatar-guest.png")
    }

    private fun normalize(value: Number, max: Double, min: Double): Double {
        return Numbers.normalize(value.toDouble(), max, min)
    }

    private fun rankBackground(user: OsuUser, assets: ImageAssets): String? {
        val rank = user.globalRank
        val percent = user.statistics?.globalRankPercent ?: (rank / 1000.0)
        val file = when {
            rank in 1L..100L -> "PF"
            percent < 0.0005 -> "SSS"
            percent < 0.0025 -> "A"
            percent < 0.005 -> "B"
            percent < 0.025 -> "X"
            percent < 0.05 -> "XH"
            percent < 0.25 -> "SP"
            percent < 0.5 -> "SH"
            else -> "F"
        }
        return assets.local("object-score-backimage-$file.webp")
    }

    private data class RankedRule(val limit: Double, val name: String)

    private val RANKED_PLAY_RULES = listOf(
        RankedRule(0.01, "Radiant III"),
        RankedRule(0.025, "Radiant II"),
        RankedRule(0.05, "Radiant I"),
        RankedRule(0.1, "Rhodium III"),
        RankedRule(0.15, "Rhodium II"),
        RankedRule(0.2, "Rhodium I"),
        RankedRule(0.3, "Platinum III"),
        RankedRule(0.4, "Platinum II"),
        RankedRule(0.5, "Platinum I"),
        RankedRule(0.55, "Gold III"),
        RankedRule(0.65, "Gold II"),
        RankedRule(0.75, "Gold I"),
        RankedRule(0.8, "Silver III"),
        RankedRule(0.875, "Silver II"),
        RankedRule(0.95, "Silver I"),
        RankedRule(0.96, "Bronze III"),
        RankedRule(0.98, "Bronze II"),
        RankedRule(1.0, "Bronze I"),
    )

    private fun rankedPlayName(rank: Int, percent: Double): String {
        if (rank <= 0 && percent <= 0) return "Peppy"
        if (rank in 1..100) return "Lustrous"
        return RANKED_PLAY_RULES.firstOrNull { percent <= it.limit }?.name ?: "Peppy"
    }

    private fun roundedArcBar(
        cx: Double,
        cy: Double,
        r1: Double,
        r2: Double,
        startAngle: Double,
        endAngle: Double,
        color: String,
        opacity: Double = 1.0,
    ): String {
        val pi2 = Math.PI / 180
        val type = if (endAngle - startAngle >= 180) 1 else 0
        val lsx = cx - r2 * kotlin.math.cos(startAngle * pi2)
        val lsy = cy - r2 * kotlin.math.sin(startAngle * pi2)
        val lex = cx - r2 * kotlin.math.cos(endAngle * pi2)
        val ley = cy - r2 * kotlin.math.sin(endAngle * pi2)
        val ssx = cx - r1 * kotlin.math.cos(endAngle * pi2)
        val ssy = cy - r1 * kotlin.math.sin(endAngle * pi2)
        val sex = cx - r1 * kotlin.math.cos(startAngle * pi2)
        val sey = cy - r1 * kotlin.math.sin(startAngle * pi2)
        val rd = (r2 - r1) / 2
        return "<path d=\"M ${Svg.number(lsx)} ${Svg.number(lsy)} " +
            "A ${Svg.number(r2)} ${Svg.number(r2)} 0 $type 1 ${Svg.number(lex)} ${Svg.number(ley)} " +
            "A ${Svg.number(rd)} ${Svg.number(rd)} 0 0 1 ${Svg.number(ssx)} ${Svg.number(ssy)} " +
            "A ${Svg.number(r1)} ${Svg.number(r1)} 0 $type 0 ${Svg.number(sex)} ${Svg.number(sey)} " +
            "A ${Svg.number(rd)} ${Svg.number(rd)} 0 0 1 ${Svg.number(lsx)} ${Svg.number(lsy)} Z\" " +
            "style=\"fill: ${Svg.escape(color)}; fill-opacity: ${Svg.number(opacity)}\"/>"
    }

    private fun rotateSvgBody(body: String, cx: Double, cy: Double, angle: Double): String {
        return "<g transform=\"translate(${Svg.number(cx)}, ${Svg.number(cy)}) rotate(${Svg.number(angle)}) " +
            "translate(${Svg.number(-cx)}, ${Svg.number(-cy)})\">$body</g>"
    }

    private data class SizedLabel(
        val spec: LabelSpecS,
        val dataB: String,
        val dataM: String,
        val progress: Double,
    )

    private data class LabelSpecS(
        val iconFile: String,
        val iconTitle: String,
        val barColor: String,
        val barColors: List<String>,
        val barMin: Double,
        val barMid: Double,
        val barMax: Double,
    )

    private object LabelS {
        val PC = LabelSpecS("object-score-pp.png", "PC", "#00B7EE", ColorArray.cyan, 0.0, 100.0, 200.0)
        val MAP_PC = LabelSpecS("object-score-healthpoint.png", "MAP", "#F06EA9", ColorArray.pink, 0.0, 250.0, 500.0)
        val SR = LabelSpecS("object-score-overalldifficulty.png", "A.SR", "#A864A8", ColorArray.purple, 2.0, 5.0, 8.0)
        val PT = LabelSpecS("object-score-length.png", "PT", "#7CC576", ColorArray.green, 0.0, 24.0, 48.0)
        val COMBO = LabelSpecS("object-score-combo.png", "A.CB", "#FFF467", ColorArray.yellow, 0.0, 500.0, 1000.0)
        val ACC = LabelSpecS("object-score-accuracy.png", "A.ACC", "#F26C4F", ColorArray.red, 90.0, 95.0, 100.0)
    }
}
