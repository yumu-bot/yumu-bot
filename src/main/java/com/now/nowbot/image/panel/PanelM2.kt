package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.component.profile.CardD2
import com.now.nowbot.image.component.profile.CardD2Data
import com.now.nowbot.image.component.j.PanelJSupport
import com.now.nowbot.image.component.j2.J2LabelData
import com.now.nowbot.image.component.j2.PanelJ2Components
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.randomBanner
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.maimai.maiImageAssets
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.PanelColor
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.model.enums.OsuGenre
import com.now.nowbot.model.enums.OsuLanguage
import com.now.nowbot.model.osu.ActivityEvent
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.service.messageServiceImpl.GuestDifficultyService
import com.now.nowbot.service.messageServiceImpl.IMapperService
import kotlin.math.max
import kotlin.math.min

data class M2RenderResult(val svg: String, val report: RenderReport)

object PanelM2 {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PM2-1);\">"
    private const val MARK_COMPONENT = "<g id=\"Component\">"
    private const val MARK_CARD_A1 = "<g id=\"Card_A1\">"
    private const val MARK_BACKGROUND = "<g filter=\"url(#blur-PM2-BG)\" style=\"clip-path: url(#clippath-PM2-BG);\">"

    fun toSvg(body: Map<String, Any?>, lease: RenderResourceLease, remote: RemoteImageSource? = null): String =
        render(body, lease, remote).svg

    fun render(body: Map<String, Any?>, lease: RenderResourceLease, remote: RemoteImageSource? = null): M2RenderResult {
        val report = RenderReport()
        val assets = maiImageAssets(lease, remote, report)
        return M2RenderResult(render(body, assets, report), report)
    }

    fun render(body: Map<String, Any?>, assets: ImageAssets, report: RenderReport = RenderReport()): String {
        val user = body["user"] as? OsuUser ?: throw IllegalArgumentException("M2 需要 user")
        val popular = readList<IMapperService.MostPopularBeatmapset>(body, "most_popular_beatmapset")
        val recentRanked = readList<IMapperService.RecentlyRankedBeatmapset>(body, "most_recent_ranked_beatmapset")
        val guests = readList<GuestDifficultyService.GuestDifficultyOwner>(body, "guest_owners")
        val activities = readList<ActivityEvent>(body, "recent_activity")
        val genres = readIntArray(body, "genre")
        val languages = readIntArray(body, "language")
        val difficultyArr = readIntArray(body, "difficulty_arr")
        val lengthArr = readIntArray(body, "length_arr")
        val favorite = (body["favorite"] as? Number)?.toLong() ?: 0L
        val playcount = (body["playcount"] as? Number)?.toLong() ?: 0L
        val totalGuest = (body["total_guest"] as? Number)?.toInt() ?: 0
        val totalGuestRanked = (body["total_guest_ranked"] as? Number)?.toInt() ?: 0
        val totalDiff = (body["total_diff"] as? Number)?.toInt() ?: 0
        val averageRating = (body["average_rating"] as? Number)?.toDouble() ?: 0.0
        val averageLength = (body["average_length"] as? Number)?.toDouble() ?: 0.0
        val averageDifficulty = (body["average_difficulty"] as? Number)?.toDouble() ?: 0.0
        val hue = user.profileHue.takeIf { it != 0 } ?: 342

        var svg = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1920\" height=\"1080\" viewBox=\"0 0 1920 1080\">")
            append("<defs>")
            append("<clipPath id=\"clippath-PM2-1\"><rect width=\"1920\" height=\"320\" rx=\"20\" ry=\"20\"/></clipPath>")
            append("<clipPath id=\"clippath-PM2-BG\"><rect y=\"290\" width=\"1920\" height=\"790\" rx=\"30\" ry=\"30\"/></clipPath>")
            append("<filter id=\"blur-PM2-BG\" height=\"110%\" width=\"110%\" x=\"-5%\" y=\"-5%\" filterUnits=\"userSpaceOnUse\">")
            append("<feGaussianBlur stdDeviation=\"15\"/></filter></defs>")
            append("<g><rect width=\"1920\" height=\"320\" rx=\"20\" ry=\"20\" style=\"fill:${PanelColor.base(hue)}\"/></g>")
            append("<g><g style=\"clip-path: url(#clippath-PM2-1);\"></g></g>")
            append("<g><rect y=\"290\" width=\"1920\" height=\"790\" rx=\"30\" ry=\"30\" style=\"fill:${PanelColor.bottom(hue)}\"/>")
            append("<g filter=\"url(#blur-PM2-BG)\" style=\"clip-path: url(#clippath-PM2-BG);\"></g></g>")
            append("<g id=\"Component\"></g><g id=\"Card_A1\"></g>")
            append("<g><rect x=\"510\" y=\"40\" width=\"195\" height=\"60\" rx=\"15\" ry=\"15\" style=\"fill:${PanelColor.middle(hue)}\"/></g>")
            append("<g id=\"Index\"></g></svg>")
        }

        val pending = user.pendingCount
        val slot = if (user.isSupporter) 8 + min(user.rankedCount, 12) else 4 + min(user.rankedCount, 4)
        val m1Rows = listOf(
            row("Rank", "#${user.globalRank}", 1.0, Colors.getRankColor("S")),
            row("Pending", "$pending [$slot]", if (slot > 0) pending.toDouble() / slot else 0.0, Colors.getRankColor("A")),
            row("Guest", "$totalGuestRanked [$totalGuest]", if (totalGuest > 0) totalGuestRanked.toDouble() / totalGuest else 0.0, Colors.getRankColor("S")),
            row("Difficulties", totalDiff.toString(), min(totalDiff / 300.0, 1.0), Colors.getRankColor("A")),
            row("Favourites", favorite.toString(), min(favorite / 5000.0, 1.0), Colors.getRankColor("S")),
            row("Play Count", playcount.toString(), min(playcount / 1_000_000.0, 1.0), Colors.getRankColor("A")),
        )
        val m1 = buildString {
            append(PanelDraw.rect(0.0, 0.0, 490.0, 390.0, 20.0, PanelColor.middle(hue)))
            append(Fonts.poppinsBold.textPath("Mapper Statistics", 15.0, 27.0, 18.0, "left baseline", "#fff"))
            m1Rows.forEachIndexed { index, text -> append(Svg.group(20.0, 45.0 + index * 48.0, text)) }
            append(PanelDraw.rect(20.0, 368.0, 450.0, 10.0, 5.0, PanelColor.top(hue)))
            append(PanelDraw.rect(20.0, 368.0, 450.0 * (averageRating / 10.0).coerceIn(0.0, 1.0), 10.0, 5.0, "#006899"))
            append(Fonts.poppinsBold.textPath("Player Rating: ${Numbers.floor(averageRating, 1)}", 475.0, 355.0, 18.0, "right baseline", "#fff"))
        }

        val m2 = buildString {
            append(PanelDraw.rect(0.0, 0.0, 490.0, 300.0, 20.0, PanelColor.middle(hue)))
            append(Fonts.poppinsBold.textPath("Most Popular Beatmaps", 15.0, 27.0, 18.0, "left baseline", "#fff"))
            popular.take(6).forEachIndexed { index, item ->
                val card = CardD2Data(
                    background = assets.remote(item.covers.list2x),
                    title = Numbers.jsRound(item.playCount.toDouble()).toLong().toString(),
                    titleMid = "PC",
                    subTitle = "♥ ${item.favouriteCount}",
                    left = "#${item.beatmapsetID}",
                    leftRrectColor = PanelColor.top(hue),
                    right = Numbers.floor(item.rating.toDouble(), 1) + "*",
                    rightRrectColor = Colors.getStarRatingColor(item.rating.toDouble()),
                    bottomLeft = if (item.ranked > 0) "ranked" else "pending",
                    bottomRight = "SID",
                )
                val x = 10.0 + (index % 3) * 160.0
                val y = 40.0 + (index / 3) * 130.0
                append(Svg.group(x, y, CardD2.render(card, card.background)))
            }
        }

        val m3 = piePanel("Genres", genres, OsuGenre.entries.map { it.formalName }, hue, 400.0)
        val m4 = piePanel("Languages", languages, OsuLanguage.entries.map { it.formalName }, hue, 400.0)

        val m5 = buildString {
            append(PanelDraw.rect(0.0, 0.0, 470.0, 300.0, 20.0, PanelColor.middle(hue)))
            append(Fonts.poppinsBold.textPath("Recent Activities", 15.0, 27.0, 18.0, "left baseline", "#fff"))
            activities.take(5).forEachIndexed { index, event ->
                val title = event.beatmapSet?.title ?: event.beatmap?.title ?: event.achievement?.name ?: event.user?.name ?: event.type?.name ?: "Activity"
                val detail = when {
                    event.approval != null -> event.approval.orEmpty()
                    event.scoreRank != null -> event.scoreRank.orEmpty()
                    event.count != null -> "${event.count}x"
                    event.repeated > 1 -> "${event.repeated}x"
                    else -> ""
                }
                val time = event.createdAt?.let { TimeFormat.difference(it) } ?: ""
                append(Svg.group(10.0, 45.0 + index * 50.0, row(Fonts.poppinsBold.cutStringTail(title, 16.0, 300.0), "$detail $time", 0.5, PanelColor.top(hue), 450.0)))
            }
        }

        val m6 = buildString {
            append(PanelDraw.rect(0.0, 0.0, 330.0, 300.0, 20.0, PanelColor.middle(hue)))
            append(Fonts.poppinsBold.textPath("Recently Ranked Maps", 15.0, 27.0, 18.0, "left baseline", "#fff"))
            recentRanked.take(5).forEachIndexed { index, set ->
                val stars = set.beatmaps.joinToString(" / ") { Numbers.floor(it.starRating, 1) }
                append(Svg.group(15.0, 40.0 + index * 52.0, row(set.title, stars, 0.7, PanelColor.top(hue), 300.0)))
            }
        }

        val m7 = buildString {
            append(PanelDraw.rect(0.0, 0.0, 490.0, 185.0, 20.0, PanelColor.middle(hue)))
            append(Fonts.poppinsBold.textPath("Difficulty", 15.0, 27.0, 18.0, "left baseline", "#fff"))
            append(Fonts.poppinsBold.textPath("Average: ${Numbers.floor(averageDifficulty, 2)}*", 475.0, 27.0, 18.0, "right baseline", "#fff"))
            append(PanelJSupport.barChart(difficultyArr.toList(), max(difficultyArr.maxOrNull() ?: 1, 1).toDouble(), 40.0, 40.0, 460.0, 115.0, starColors(difficultyArr.size), 4.0))
        }

        val m8 = buildString {
            append(PanelDraw.rect(0.0, 0.0, 490.0, 185.0, 20.0, PanelColor.middle(hue)))
            append(Fonts.poppinsBold.textPath("Length", 15.0, 27.0, 18.0, "left baseline", "#fff"))
            val lengthText = "${(averageLength / 60).toInt()}:${(averageLength % 60).toInt().toString().padStart(2, '0')}"
            append(Fonts.poppinsBold.textPath("Average: $lengthText", 475.0, 27.0, 18.0, "right baseline", "#fff"))
            append(PanelJSupport.barChart(lengthArr.toList(), max(lengthArr.maxOrNull() ?: 1, 1).toDouble(), 40.0, 40.0, 460.0, 115.0, starColors(lengthArr.size), 4.0))
        }

        val m9 = buildString {
            append(PanelDraw.rect(0.0, 0.0, 490.0, 300.0, 20.0, PanelColor.middle(hue)))
            append(Fonts.poppinsBold.textPath("Guest Difficulties", 15.0, 27.0, 18.0, "left baseline", "#fff"))
            guests.take(6).forEachIndexed { index, owner ->
                val avatar = assets.remote("https://a.ppy.sh/${owner.user.userID}")
                val id = "guest-${owner.user.userID}-$index"
                val y = 45.0 + index * 41.0
                append(PanelDraw.rect(10.0, y, 470.0, 36.0, 8.0, PanelColor.base(hue)))
                append(Svg.image(14.0, y + 3.0, 30.0, 30.0, avatar.orEmpty()))
                append(Fonts.poppinsBold.textPath(Fonts.poppinsBold.cutStringTail(owner.user.username, 16.0, 200.0), 52.0, y + 24.0, 16.0, "left baseline", "#fff"))
                append(Fonts.poppinsBold.textPath("sent ${owner.sent} [${owner.sentRanked}]", 300.0, y + 24.0, 14.0, "left baseline", "#aaa"))
                append(Fonts.poppinsBold.textPath("recv ${owner.received} [${owner.receivedRanked}]", 472.0, y + 24.0, 14.0, "right baseline", "#aaa"))
            }
        }

        svg = Svg.setText(svg, PanelBatchSupport.panelName("I'm Mapper v2", "IM"), MARK_INDEX)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, CardA1.render(user, null, assets)), MARK_CARD_A1)
        svg = Svg.setText(
            svg,
            Svg.group(40.0, 330.0, m1) + Svg.group(40.0, 740.0, m2) + Svg.group(550.0, 330.0, m3) +
                Svg.group(970.0, 330.0, m4) + Svg.group(550.0, 740.0, m5) + Svg.group(1040.0, 740.0, m6) +
                Svg.group(1390.0, 330.0, m7) + Svg.group(1390.0, 535.0, m8) + Svg.group(1390.0, 740.0, m9),
            MARK_COMPONENT,
        )
        val banner = assets.resolveRef(user.profileBanners.firstOrNull()?.image) ?: assets.randomBanner()
        svg = Svg.setText(svg, banner?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) }, MARK_BANNER)
        val background = assets.resolveRef(user.coverUrl) ?: assets.localBanner()
        svg = Svg.setText(svg, background?.let { Svg.image(0.0, 280.0, 1920.0, 1080.0, it, 0.6) }, MARK_BACKGROUND)
        return svg
    }

    private fun row(title: String, value: String, progress: Double, color: String, width: Double = 450.0): String {
        val clamped = progress.coerceIn(0.0, 1.0)
        return buildString {
            append(PanelDraw.rect(0.0, 0.0, width, 40.0, 10.0, "#46393f"))
            append(PanelDraw.rect(0.0, 0.0, width * clamped, 40.0, 10.0, color, 0.2))
            append(PanelDraw.rect(width - 8.0, 5.0, 3.0, 30.0, 1.0, color))
            append(Fonts.poppinsBold.textPath(title, 10.0, 26.0, 16.0, "left baseline", "#fff"))
            append(Fonts.poppinsBold.textPath(value, width - 15.0, 26.0, 16.0, "right baseline", "#fff"))
        }
    }

    private fun piePanel(title: String, values: IntArray, names: List<String>, hue: Int, width: Double): String {
        val total = values.sum().takeIf { it > 0 } ?: 1
        val entries = values.indices
            .map { it to values[it] }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(6)
        return buildString {
            append(PanelDraw.rect(0.0, 0.0, width, 390.0, 20.0, PanelColor.middle(hue)))
            append(Fonts.poppinsBold.textPath(title, 15.0, 27.0, 18.0, "left baseline", "#fff"))
            var start = 0.0
            entries.forEach { (index, count) ->
                val end = start + count.toDouble() / total
                append(PanelJ2Components.pie(200.0, 136.0, 95.0, start, end, Colors.getStarRatingColor(index.toDouble())))
                start = end
            }
            entries.forEachIndexed { position, (index, count) ->
                val x = 10.0 + (position % 2) * 195.0
                val y = 250.0 + (position / 2) * 65.0
                append(
                    PanelJ2Components.labelJ4(
                        J2LabelData(names.getOrElse(index) { "?" }, names.getOrElse(index) { "?" }, "${count}", count.toString(), count.toDouble() / total, Colors.getStarRatingColor(index.toDouble()), 185.0),
                        x, y,
                    ),
                )
            }
        }
    }

    private fun starColors(size: Int): List<String> = List(size) { Colors.getStarRatingColor(it * 1.0) }

    private fun readIntArray(body: Map<String, Any?>, key: String): IntArray {
        return when (val value = body[key]) {
            null -> IntArray(0)
            is IntArray -> value
            is List<*> -> value.mapNotNull { (it as? Number)?.toInt() }.toIntArray()
            is Array<*> -> value.mapNotNull { (it as? Number)?.toInt() }.toIntArray()
            else -> throw IllegalArgumentException("M2 字段 $key 类型不正确: ${value.javaClass.name}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> readList(body: Map<String, Any?>, key: String): List<T> {
        return when (val value = body[key]) {
            null -> emptyList()
            is List<*> -> value as List<T>
            is Array<*> -> value.toList() as List<T>
            else -> throw IllegalArgumentException("M2 字段 $key 类型不正确: ${value.javaClass.name}")
        }
    }
}
