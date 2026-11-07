package com.now.nowbot.image.panel

import com.now.nowbot.entity.Team
import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.card.CardA1Data
import com.now.nowbot.image.card.CardA2
import com.now.nowbot.image.card.CardA2Data
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderAssetRegistry
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.draw.randomBanner
import com.now.nowbot.image.markdown.MarkdownRenderer
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.userlist.UserListCards
import com.now.nowbot.image.util.GameModes
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.model.osu.MicroUser
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object PanelA9 {
    private const val PAGE_SIZE = 48
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_MAIN = "<g id=\"Main_Card\">"
    private const val MARK_BODY = "<g id=\"Body_Card\">"
    private const val MARK_DESCRIPTION = "<g id=\"Description_Card\" style=\"clip-path: url(#clippath-PA9-2);\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PA9-1);\">"

    private val HTML_TAG = Regex("""</?[\s\S]*?/?>""")
    private val BBCODE_TAG = Regex("""\[/?[^\]]+]""")
    private val ZONE: ZoneOffset = ZoneOffset.ofHours(8)
    private val MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM")
    private val LAST_VISIT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun toSvg(
        team: Team,
        page: Int,
        maxPage: Int,
        lease: RenderResourceLease,
        markdownRenderer: MarkdownRenderer,
        remote: RemoteImageSource? = null,
    ): String {
        val assets = ImageAssets(
            RenderAssetRegistry { bytes -> lease.registerImage(bytes).url },
            remote,
            report = RenderReport(),
        )
        val description = descriptionImage(team, lease, markdownRenderer)
        return toSvg(
            team = team,
            page = page,
            maxPage = maxPage,
            assets = assets,
            descriptionUrl = description?.first,
            descriptionHeight = description?.second ?: 0,
        )
    }

    fun toSvg(
        payload: Map<String, Any?>,
        lease: RenderResourceLease,
        markdownRenderer: MarkdownRenderer,
        remote: RemoteImageSource? = null,
    ): String {
        val team = payload["team"] as? Team
            ?: throw IllegalArgumentException("A9 参数缺少 Team")
        val page = (payload["page"] as? Number)?.toInt() ?: 1
        val maxPage = (payload["max_page"] as? Number)?.toInt() ?: 1
        return toSvg(team, page, maxPage, lease, markdownRenderer, remote)
    }

    internal fun toSvg(
        team: Team,
        page: Int,
        maxPage: Int,
        assets: ImageAssets,
        descriptionUrl: String? = null,
        descriptionHeight: Int = 0,
    ): String {
        val totalPages = maxPage.coerceAtLeast(1)
        val currentPage = page.coerceIn(1, totalPages)
        val members = team.members
            .drop((currentPage - 1) * PAGE_SIZE)
            .take(PAGE_SIZE)

        val panelHeight = PanelBatchSupport.panelHeight(
            members.size,
            210.0,
            4,
            290.0,
            40.0,
            40.0,
        ) + 250
        val cardHeight = panelHeight - 290

        var svg = Svg.removeXmlDeclaration(Templates.read("Panel_A9.svg"))
        svg = PanelBatchSupport.applyHeights(svg, panelHeight, cardHeight)

        val mainCard = CardA2.render(teamCardData(team, assets), assets)
        val banner = assets.resolveRef(team.coverUrl)
            ?: assets.local("beatmap-DLfailBG.webp")
            ?: assets.randomBanner()

        val leaderGroup = team.leader?.let { leader ->
            Svg.group(40.0, 330.0, CardA1.render(memberCardData(leader, true, assets), assets))
        }
        val gold = assets.local("backlight-golden.png")
            ?.let { Svg.image(0.0, 290.0, 510.0, 290.0, it) }

        val memberCards = members.mapIndexed { index, member ->
            val column = index % 4
            val row = index / 4
            val x = 40.0 + 470.0 * column
            val y = 330.0 + 250.0 + 250.0 * row
            Triple(x, y, CardA1.render(memberCardData(member, false, assets), assets))
        }
        val greenImages = members.mapIndexedNotNull { index, member ->
            if (!member.isOnline) return@mapIndexedNotNull null
            val column = index % 4
            val row = index / 4
            val x = 40.0 + 470.0 * column
            val y = 330.0 + 250.0 + 250.0 * row
            assets.local("backlight-green.png")
                ?.let { Svg.image(x - 40.0, y - 40.0, 510.0, 290.0, it) }
        }

        val body = buildString {
            greenImages.forEach { append(it).append('\n') }
            memberCards.forEach { (x, y, card) ->
                append(Svg.group(x, y, card)).append('\n')
            }
            gold?.let { append(it).append('\n') }
            leaderGroup?.let { append(it).append('\n') }
            append(PanelBatchSupport.page(currentPage, totalPages, panelHeight))
        }

        svg = Svg.setText(svg, PanelBatchSupport.panelName("Team (!ymtm)", "TM"), MARK_INDEX)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, mainCard), MARK_MAIN)
        svg = Svg.setText(
            svg,
            banner?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) },
            MARK_BANNER,
        )
        if (descriptionUrl != null) {
            svg = Svg.setText(
                svg,
                Svg.image(
                    510.0,
                    320.0,
                    1410.0,
                    descriptionHeight.toDouble().coerceAtMost(240.0),
                    descriptionUrl,
                    1.0,
                    "xMidYMin slice",
                ),
                MARK_DESCRIPTION,
            )
        }
        svg = Svg.setText(svg, body, MARK_BODY)
        return svg
    }

    private fun descriptionImage(
        team: Team,
        lease: RenderResourceLease,
        markdownRenderer: MarkdownRenderer,
    ): Pair<String, Int>? {
        val markdown = team.markdownDescription
        if (markdown.isBlank()) return null
        val cleaned = cleanDescription(markdown)
        if (cleaned.isEmpty()) return null
        val rendered = markdownRenderer.render(cleaned, 1370, 0)
        val url = lease.registerImage(rendered.png).url
        val height = Math.min(
            Math.round(rendered.height * 1410.0 / 1370.0).toInt(),
            240,
        )
        return url to height
    }

    private fun cleanDescription(value: String): String {
        return value
            .replace("<br />", " ")
            .replace("<br>", " ")
            .replace(HTML_TAG, "")
            .replace(BBCODE_TAG, "")
    }

    private fun teamCardData(team: Team, assets: ImageAssets): CardA2Data {
        val stats = team.statistics
        val isOpen = if (team.open == true) "Open" else "Closed"
        val title2 = GameModes.getGameMode(team.defaultRuleset, 2) +
            " // " + isOpen +
            " // " + (team.emptySlots ?: 0) + " Slots"
        val month = team.createdAt
            ?.atZoneSameInstant(ZONE)
            ?.format(MONTH_FORMAT)
            ?: TimeFormat.nowTimestamp().take(7)
        return CardA2Data(
            background = assets.resolveRef(team.flagUrl),
            mapStatus = "",
            title1 = team.name ?: "Team",
            title2 = title2,
            left1 = "RKS: " + Numbers.round(stats?.rankedScore ?: 0L, -4, 0),
            left2 = "PC: " + Numbers.jsRound((stats?.playCount ?: 0L).toDouble()).toLong(),
            left3 = month,
            right1 = "TeamID: #" + (stats?.teamID ?: 0),
            right2 = "Rank: #" + (stats?.rank ?: 0),
            right3b = Numbers.jsRound((stats?.performance ?: 0).toDouble()).toLong().toString(),
            right3m = "PP",
        )
    }

    private fun memberCardData(user: MicroUser, leader: Boolean, assets: ImageAssets): CardA1Data {
        val secret = user.lastVisitString == null
        return CardA1Data(
            background = UserListCards.banner(user, assets),
            avatar = UserListCards.avatarOf(user.userID, user.avatarUrl, assets),
            subIcon1 = if (user.isSupporter) assets.local("object-card-supporter.png") else null,
            country = user.countryCode.ifBlank { user.country?.code.orEmpty() }.ifBlank { "CN" },
            teamUrl = user.team?.flag,
            top1 = user.username,
            left1 = "[" + (user.team?.shortName ?: "?") + "]",
            left2 = if (leader) "leader" else "member",
            right1 = "(" + (if (secret) "?" else TimeFormat.difference(user.lastVisitString)) + ")",
            right2 = "last visit: " + (if (secret) "?" else formatLastVisit(user.lastVisitString)),
            right3b = "",
            right3m = if (secret) "?" else if (user.isOnline) "Online" else "Offline",
        )
    }

    private fun formatLastVisit(value: String?): String {
        if (value == null) return "?"
        if (value.isBlank()) return "Invalid date"
        val time = runCatching { OffsetDateTime.parse(value) }.getOrNull()
            ?: return "Invalid date"
        return time.atZoneSameInstant(ZONE).format(LAST_VISIT_FORMAT)
    }
}
