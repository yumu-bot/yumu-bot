package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA2
import com.now.nowbot.image.card.CardA2Data
import com.now.nowbot.image.component.IM1Component
import com.now.nowbot.image.component.IM1Data
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.model.osu.DiscussionDetails
import com.now.nowbot.model.osu.OsuUser
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class NominationMore(
    val hostCount: Int = 0,
    val guestCount: Int = 0,
    val totalCount: Int = 0,
    val maxStar: String = "",
    val minStar: String = "",
    val totalLength: Int = 0,
    val tags: List<String> = emptyList(),
    val problemCount: Int = 0,
    val suggestCount: Int = 0,
    val notSolvedCount: Int = 0,
    val hypeCount: Int = 0,
    val praiseCount: Int = 0,
) {
    companion object {
        fun fromMap(map: Map<*, *>?): NominationMore {
            if (map == null) return NominationMore()
            fun int(key: String): Int = (map[key] as? Number)?.toInt() ?: 0
            fun text(key: String): String = map[key]?.toString().orEmpty()
            val tags = when (val value = map["tags"]) {
                is Array<*> -> value.mapNotNull { it?.toString() }
                is List<*> -> value.mapNotNull { it?.toString() }
                else -> emptyList()
            }
            return NominationMore(
                hostCount = int("host_count"),
                guestCount = int("guest_count"),
                totalCount = int("total_count"),
                maxStar = text("max_star"),
                minStar = text("min_star"),
                totalLength = int("total_length"),
                tags = tags,
                problemCount = int("problem_count"),
                suggestCount = int("suggest_count"),
                notSolvedCount = int("not_solved_count"),
                hypeCount = int("hype_count"),
                praiseCount = int("praise_count"),
            )
        }
    }
}

data class PanelNData(
    val beatmapset: Beatmapset = Beatmapset(),
    val discussions: List<DiscussionDetails> = emptyList(),
    val hype: List<DiscussionDetails> = emptyList(),
    val more: NominationMore = NominationMore(),
    val users: List<OsuUser> = emptyList(),
) {
    companion object {
        fun fromMap(map: Map<*, *>): PanelNData {
            return PanelNData(
                beatmapset = map["beatmapset"] as? Beatmapset ?: Beatmapset(),
                discussions = (map["discussion"] as? List<*>)?.filterIsInstance<DiscussionDetails>() ?: emptyList(),
                hype = (map["hype"] as? List<*>)?.filterIsInstance<DiscussionDetails>() ?: emptyList(),
                more = NominationMore.fromMap(map["more"] as? Map<*, *>),
                users = (map["users"] as? List<*>)?.filterIsInstance<OsuUser>() ?: emptyList(),
            )
        }
    }
}

object PanelN {
    private const val MARK_INDEX = "<g id=\"Index_PN\">"
    private const val MARK_BEATMAP_A2 = "<g id=\"BeatMap_A2\">"
    private const val MARK_ME = "<g id=\"Me_PN\">"
    private const val MARK_GUEST = "<g id=\"Guest_PN\">"
    private const val MARK_PROGRESS = "<g id=\"Progress_PN\">"
    private const val MARK_TAG = "<g id=\"Tag_PN\">"
    private const val MARK_DISCUSSION = "<g id=\"Discussion_PN\">"
    private const val MARK_FAVORITE = "<g id=\"Favorite_PN\">"
    private const val MARK_GENRE = "<g id=\"Genre_PN\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PN1-1);\">"

    fun toSvg(data: PanelNData, assets: ImageAssets): String {
        var svg = Svg.removeXmlDeclaration(Templates.read("Panel_N.svg"))
        val set = data.beatmapset
        val more = data.more

        svg = Svg.setText(svg, bPanelName("Nomination (!ymn)", "N"), MARK_INDEX)

        val cardA2 = cardA2(set, assets)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, cardA2), MARK_BEATMAP_A2)

        val host = set.creatorData
        svg = Svg.setText(svg, Svg.group(60.0, 350.0, IM1Component.render(IM1Data.fromUser(host, assets), assets)), MARK_ME)

        val guestTitle = Fonts.torus.textPath("Guest Mappers", 60.0, 730.0, 30.0, "left baseline", "#fff")
        val tagTitle = Fonts.torus.textPath("Tags", 60.0, 910.0, 30.0, "left baseline", "#fff")
        val progressTitle = Fonts.torus.textPath("Ranking Progress", 510.0, 365.0, 30.0, "left baseline", "#fff")
        val discussionTitle = Fonts.torus.textPath("Modding Discussion", 510.0, 645.0, 30.0, "left baseline", "#fff")
        val favoriteTitle = Fonts.torus.textPath("Favorites", 1630.0, 645.0, 30.0, "left baseline", "#fff")
        val genreTitle = Fonts.torus.textPath("G/L", 1630.0, 925.0, 30.0, "left baseline", "#fff")
        svg = Svg.setTexts(
            svg,
            listOf(favoriteTitle, tagTitle, progressTitle, discussionTitle, guestTitle, genreTitle),
            MARK_INDEX,
        )

        val totalLength = more.totalLength
        val diffB = more.hostCount.toString()
        val diffM = more.totalCount.toString()
        val starB = more.minStar.ifEmpty { "0" }
        val starM = more.maxStar.ifEmpty { "0" }
        val lengthB = (totalLength / 60).toString()
        val lengthM = (totalLength % 60).toString().padStart(2, '0')

        val diff = Fonts.torus.get2SizeTextPath(diffB, "/$diffM", 42.0, 30.0, 120.0, 616.0, "center baseline", "#fff")
        val star = if (more.minStar.isNotEmpty()) {
            Fonts.torus.get2SizeTextPath(starB, "~$starM", 42.0, 30.0, 255.0, 616.0, "center baseline", "#fff")
        } else {
            Fonts.torus.textPath(starM, 255.0, 616.0, 42.0, "center baseline", "#fff")
        }
        val length = Fonts.torus.get2SizeTextPath(lengthB, ":$lengthM", 42.0, 30.0, 390.0, 616.0, "center baseline", "#fff")
        val diffIndex = Fonts.torus.textPath("Diff", 120.0, 648.0, 24.0, "center baseline", "#aaa")
        val starIndex = Fonts.torus.textPath("Star", 255.0, 648.0, 24.0, "center baseline", "#aaa")
        val lengthIndex = Fonts.torus.textPath("Length", 390.0, 648.0, 24.0, "center baseline", "#aaa")

        svg = Svg.setTexts(svg, listOf(diff, star, length), MARK_INDEX)
        svg = Svg.setTexts(svg, listOf(diffIndex, starIndex, lengthIndex), MARK_INDEX)

        val pack = set.packTags?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "none"
        val statStr = "(?) Not Solved: ${more.notSolvedCount}"
            .plus(" // (!) Problem: ${more.problemCount}")
            .plus(" // ( ) Suggestion: ${more.suggestCount}")
            .plus(" // (*) Praise: ${more.praiseCount}")
            .plus(" // Pack: $pack")
            .plus(" // User Rating: ${Numbers.floor(set.rating, 1)}")
        svg = Svg.setText(svg, Fonts.torus.textPath(statStr, 1570.0, 645.0, 18.0, "right baseline", "#aaa"), MARK_DISCUSSION)

        svg = Svg.setText(svg, tagPanel(more.tags, 60.0, 940.0), MARK_TAG)
        svg = Svg.setText(svg, guestPanel(set.mappers, assets, 54.0, 745.0), MARK_GUEST)
        svg = Svg.setText(
            svg,
            rankingProgressPanel(set, data.hype, data.users, assets, more, 490.0, 330.0),
            MARK_PROGRESS,
        )
        svg = Svg.setText(svg, favoritePanel(set.recentFavourites.orEmpty(), assets, 1620.0, 660.0), MARK_FAVORITE)
        svg = Svg.setText(svg, discussionPanel(data.discussions, data.users, assets, 510.0, 660.0), MARK_DISCUSSION)
        svg = Svg.setText(svg, genrePanel(set, assets), MARK_GENRE)

        svg = Svg.setText(
            svg,
            assets.mapBackground(set, "cover")?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) },
            MARK_BANNER,
        )
        return svg
    }

    private fun cardA2(set: Beatmapset, assets: ImageAssets): String {
        val playCount = Numbers.floorParts(set.playCount, 2)
        val data = CardA2Data(
            background = assets.mapBackground(set, "cover"),
            mapStatus = set.status,
            title1 = set.titleUnicode,
            title2 = set.artistUnicode,
            title3 = set.creator,
            left1 = "",
            left2 = mapStatusLabel(set.ranked),
            left3 = if (set.beatmapsetID != 0L) "S${set.beatmapsetID}" else "S0",
            right1 = "Favorite ${set.favouriteCount}",
            right2 = "Play Counts",
            right3b = playCount.integer,
            right3m = playCount.decimal,
        )
        return CardA2.render(data, assets)
    }

    private fun mapStatusLabel(ranked: Byte): String {
        return when (ranked.toInt()) {
            -2 -> "Graveyard"
            -1 -> "WIP"
            0 -> "Pending"
            1 -> "Ranked"
            2 -> "Approved"
            3 -> "Qualified"
            4 -> "Loved"
            else -> ""
        }
    }

    private fun tagPanel(tags: List<String>, x: Double, y: Double, size: Double = 18.0, color: String = "#3399CC"): String {
        val maxWidth = 390.0
        val maxRow = 4
        var line = ""
        var row = 1
        val out = StringBuilder()
        val blankWidth = Fonts.puHuiTi.width(" ", size)

        for (tag in tags) {
            val width = Fonts.puHuiTi.width(line, size)
            val tagWidth = Fonts.puHuiTi.width(tag, size)
            if (width < maxWidth && width + tagWidth + blankWidth >= maxWidth) {
                out.append(Fonts.puHuiTi.textPath(line, x, y + 26 * (row - 1), size, "left baseline", color))
                row++
                line = tag
            } else {
                line = if (line.isNotEmpty()) "$line $tag" else tag
            }
            if (row > maxRow) break
        }
        return out.toString()
    }

    private fun guestPanel(guest: List<OsuUser>, assets: ImageAssets, x: Double, y: Double): String {
        val out = mutableListOf<String>()
        if (guest.isEmpty()) {
            assets.local("sticker_qiqi_oh.png")?.let { out += Svg.image(180.0, -5.0, 80.0, 80.0, it) }
            out += Fonts.torus.textPath("Solo Set!", 215.0, 100.0, 18.0, "center baseline", "#fff")
        } else if (guest.size > 4) {
            for (index in guest.indices) {
                if (index >= 16) break
                val user = guest[index]
                val dx = (index % 8) * 51.0
                val dy = (index / 8) * 51.0
                out += Svg.group(dx, dy, labelN3(user, userAvatar(assets, user)))
            }
        } else {
            for (index in guest.indices) {
                val user = guest[index]
                out += Svg.group(index * 102.0, 0.0, labelN4(user, userAvatar(assets, user)))
            }
        }
        return Svg.group(x, y, out.joinToString("\n"))
    }

    private fun rankingProgressPanel(
        set: Beatmapset,
        hype: List<DiscussionDetails>,
        users: List<OsuUser>,
        assets: ImageAssets,
        more: NominationMore,
        x: Double,
        y: Double,
    ): String {
        val hypeCount = set.hype?.current?.takeIf { it != 0 } ?: more.hypeCount
        var hypeSlot = 615.0 / 185.0
        val summary = set.nominationsSummary
        val nomCount = summary?.current?.toInt() ?: 0
        val nomSlot = ((summary?.required?.main?.toInt() ?: 0) + (summary?.required?.secondary?.toInt() ?: 0)).takeIf { it != 0 } ?: 2
        val quaCount = if (nomCount == nomSlot && nomCount != 0) 1 else 0
        val rnkCount = if (set.ranked.toInt() == 1) 1 else 0
        val specialRanked = set.ranked.toInt() == 2 || set.ranked.toInt() == 4

        val totalLength = 1350.0
        val slot = nomSlot + if (specialRanked) 1 else 2
        var slotLength = totalLength / (hypeSlot + slot)
        if (slotLength <= 105) slotLength = 105.0
        hypeSlot = totalLength - slotLength * slot

        val xHype = 20 + x
        val xNom = hypeSlot + 5 + 20 + x
        val xQua = hypeSlot + 5 + nomSlot * slotLength + 20 + x
        val xRnk = hypeSlot + 5 + (nomSlot + 1) * slotLength + 20 + x
        val yIndex = 67 + y
        val yRrect = 78 + y
        val yNominator = 110 + y

        val index = StringBuilder()
        index.append(Fonts.torus.textPath("Hype $hypeCount/5", xHype, yIndex, 24.0, "left baseline"))
        index.append(Fonts.torus.textPath("Nomination $nomCount/$nomSlot", xNom, yIndex, 24.0, "left baseline"))

        val rrect = StringBuilder()
        val icon = StringBuilder()
        val hypeWidth = if (specialRanked) 1.0 else min(1.0, hypeCount / 5.0)
        rrect.append(drawHype(xHype, yRrect, hypeSlot * hypeWidth, hypeSlot))
        rrect.append(drawRRect(nomCount, nomSlot, xNom, yRrect, slotLength - 5, "#B3FD66"))

        val nominator = drawNominators(xNom, yNominator, set.nominators, assets, slotLength)

        if (set.ranked.toInt() == 2) {
            index.append(Fonts.torus.textPath("Approved", xQua, yIndex, 24.0, "left baseline"))
            rrect.append(drawRRect(1, 1, xQua, yRrect, slotLength - 5, "#D7FEA6"))
            icon.append(drawIcons(true, xQua + (slotLength - 5) / 2 - 25, yNominator, "object-beatmap-qualified.png", assets))
        } else if (set.ranked.toInt() == 4) {
            index.append(Fonts.torus.textPath("Loved", xQua, yIndex, 24.0, "left baseline"))
            rrect.append(drawRRect(1, 1, xQua, yRrect, slotLength - 5, "#FF66AA"))
            icon.append(drawIcons(true, xQua + (slotLength - 5) / 2 - 25, yNominator, "object-beatmap-loved.png", assets))
        } else {
            index.append(Fonts.torus.textPath("Qualified", xQua, yIndex, 24.0, "left baseline"))
            index.append(Fonts.torus.textPath("Ranked", xRnk, yIndex, 24.0, "left baseline"))
            rrect.append(drawRRect(quaCount, 1, xQua, yRrect, slotLength - 5, "#D7FEA6"))
            rrect.append(drawRRect(rnkCount, 1, xRnk, yRrect, slotLength - 5, "#fff"))
            icon.append(drawIcons(quaCount >= 1, xQua + (slotLength - 5) / 2 - 25, yNominator, "object-beatmap-qualified.png", assets))
            icon.append(drawIcons(rnkCount >= 1, xRnk + (slotLength - 5) / 2 - 25, yNominator, "object-beatmap-ranked.png", assets))
        }

        val hypeArray = renderDiscussion(hype, users, assets, 510.0, 435.0, 1, 145.0, hypeSlot)
        return index.toString() + rrect.toString() + nominator + icon.toString() + hypeArray
    }

    private fun discussionPanel(
        discussion: List<DiscussionDetails>,
        users: List<OsuUser>,
        assets: ImageAssets,
        x: Double,
        y: Double,
    ): String {
        return renderDiscussion(discussion, users, assets, x, y, 4, 360.0, 1060.0)
    }

    private fun favoritePanel(favourites: List<OsuUser>, assets: ImageAssets, x: Double, y: Double): String {
        val n3s = mutableListOf<String>()
        for (index in favourites.indices) {
            if (index >= 20) break
            val user = favourites[index]
            val dx = (index % 5) * 51.0 + x
            val dy = (index / 5) * 51.0 + y
            n3s += Svg.group(dx, dy, labelN3(user, userAvatar(assets, user)))
        }
        return "<g>" + n3s.joinToString("\n") + "</g>"
    }

    private fun genrePanel(set: Beatmapset, assets: ImageAssets): String {
        val genre = genreName(set.genre?.id)
        val language = languageName(set.language?.id)
        return labelN6(1625.0, 940.0, "genre", genre, assets) + labelN6(1745.0, 940.0, "language", language, assets)
    }

    private fun labelN1(x: Double, y: Double, user: OsuUser?, avatar: String?, maxWidth: Double, assets: ImageAssets): String {
        var svg = """
  <defs>
    <clipPath id="clippath-LN1">
      <circle cx="50" cy="50" r="50" style="fill: none;"/>
    </clipPath>
  </defs>
  <g id="RRect_LN1">
  </g>
  <g id="Avatar_LN1">
    <circle cx="50" cy="50" r="50" style="fill: #46393f;"/>
    <g style="clip-path: url(#clippath-LN1);">
    </g>
  </g>
  <g id="Label_LN1">
  </g>
  <g id="Text_LN1">
  </g>
        """.trimIndent()
        val textMark = "<g id=\"Text_LN1\">"
        val labelMark = "<g id=\"Label_LN1\">"
        val avatarMark = "<g style=\"clip-path: url(#clippath-LN1);\">"

        val abbr = Fonts.torus.textPath(groupAbbr(user?.defaultGroup), 15.0, 15.877, 18.0, "center baseline", "#fff")
        val abbrColor = user?.profileColor?.takeIf { it.isNotBlank() } ?: "none"
        val abbrRect = PanelDraw.rect(0.0, 0.0, 30.0, 20.0, 10.0, abbrColor)
        val name = Fonts.torus.textPath(
            Fonts.torus.cutStringTail(user?.username.orEmpty(), 18.0, max(maxWidth, 100.0), true),
            50.0, 118.0, 18.0, "center baseline", "#fff",
        )
        val uid = Fonts.torus.textPath(user?.userID?.toString() ?: "0", 50.0, 138.0, 16.0, "center baseline", "#fff")

        val image = avatar ?: userAvatar(assets, user)
        svg = Svg.setText(svg, image?.let { Svg.image(0.0, 0.0, 100.0, 100.0, it) }, avatarMark)
        svg = Svg.setText(svg, abbr, textMark)
        svg = Svg.setText(svg, abbrRect, labelMark)
        svg = Svg.setText(svg, name, textMark)
        svg = Svg.setText(svg, uid, textMark)
        return Svg.group(x, y, svg)
    }

    private fun labelN2(
        user: OsuUser?,
        avatar: String?,
        post: DiscussionDetails,
        x: Double,
        y: Double,
        maxWidth: Double,
        lines: List<String>,
        row: Int,
        assets: ImageAssets,
    ): String {
        var svg = """
  <defs>
    <clipPath id="clippath-LN2-1">
      <circle cx="20" cy="20" r="20" style="fill: none;"/>
    </clipPath>
  </defs>
  <g id="Avatar_LN2">
    <circle cx="20" cy="20" r="20" style="fill: #46393f;"/>
    <g style="clip-path: url(#clippath-LN2-1);">
    </g>
  </g>
  <g id="Label_LN2">
  </g>
  <g id="Text_LN2">
  </g>
        """.trimIndent()
        val textMark = "<g id=\"Text_LN2\">"
        val labelMark = "<g id=\"Label_LN2\">"
        val avatarMark = "<g style=\"clip-path: url(#clippath-LN2-1);\">"

        val nameSource = user?.username?.takeIf { it.isNotBlank() } ?: "ID:${user?.userID ?: 0}"
        val nameStr = Fonts.torus.cutStringTail(nameSource, 18.0, max(maxWidth - 46 - 15, 40.0))
        val nameLength = Fonts.torus.width(nameStr, 18.0)
        val name = Fonts.torus.textPath(nameStr, 46.0, 14.0, 18.0, "left baseline", "#fff")

        val type = if (post.resolved && post.canBeResolved) "resolved" else post.messageTypeStr
        val commentColor = if (type == "resolved") "#888" else "#fff"
        val comment = renderMessage(46.0, 34.0, 18.0, lines, row, commentColor)

        val diffSource = if (post.canBeResolved) (post.difficulty ?: "General") else ""
        val diffStr = Fonts.torus.cutStringTail(diffSource, 18.0, max(maxWidth - 46 - 30 - nameLength, 0.0), true)
        val diff = Fonts.torus.textPath(diffStr, 46.0 + 30 + nameLength, 14.0, 14.0, "left baseline", "#fff")
        val typeImage = assets.local("object-type-$type.png")

        svg = Svg.setText(svg, name, textMark)
        svg = Svg.setText(svg, diff, textMark)
        svg = Svg.setText(svg, comment, textMark)
        svg = Svg.setText(svg, typeImage?.let { Svg.image(46.0 + nameLength + 5, -2.0, 20.0, 20.0, it) }, labelMark)
        svg = Svg.setText(svg, Svg.image(0.0, 0.0, 40.0, 40.0, (avatar ?: "").orEmpty()), avatarMark)
        return Svg.group(x, y, svg)
    }

    private fun labelN3(user: OsuUser?, avatar: String?): String {
        var svg = """
  <defs>
    <clipPath id="clippath-LN3-1">
      <circle cx="22.5" cy="22.5" r="22.5" style="fill: none;"/>
    </clipPath>
  </defs>
  <g id="Avatar_LN3">
    <circle cx="22.5" cy="22.5" r="22.5" style="fill: #46393f;"/>
    <g style="clip-path: url(#clippath-LN3-1);">
    </g>
  </g>
  <g id="Label_LN3">
  </g>
        """.trimIndent()
        val labelMark = "<g id=\"Label_LN3\">"
        val avatarMark = "<g style=\"clip-path: url(#clippath-LN3-1);\">"
        val color = user?.profileColor?.takeIf { it.isNotBlank() } ?: "none"
        svg = Svg.setText(svg, PanelDraw.circle(37.5, 37.5, 7.5, color), labelMark)
        svg = Svg.setText(svg, Svg.image(0.0, 0.0, 45.0, 45.0, avatar.orEmpty()), avatarMark)
        return svg
    }

    private fun labelN4(user: OsuUser?, avatar: String?): String {
        var svg = """
  <defs>
    <clipPath id="clippath-LN4-1">
      <circle cx="48" cy="35" r="35" style="fill: none;"/>
    </clipPath>
  </defs>
  <g id="Avatar_LN4">
    <circle cx="48" cy="35" r="35" style="fill: #46393f;"/>
    <g style="clip-path: url(#clippath-LN4-1);">
    </g>
  </g>
  <g id="Label_LN4">
  </g>
  <g id="Text_LN4">
  </g>
        """.trimIndent()
        val textMark = "<g id=\"Text_LN4\">"
        val labelMark = "<g id=\"Label_LN4\">"
        val avatarMark = "<g style=\"clip-path: url(#clippath-LN4-1);\">"
        val name = Fonts.torus.textPath(
            Fonts.torus.cutStringTail(user?.username.orEmpty(), 18.0, 96.0),
            48.0, 88.0, 18.0, "center baseline", "#fff",
        )
        val color = user?.profileColor?.takeIf { it.isNotBlank() } ?: "none"
        svg = Svg.setText(svg, name, textMark)
        svg = Svg.setText(svg, PanelDraw.circle(73.0, 60.0, 10.0, color), labelMark)
        svg = Svg.setText(svg, Svg.image(13.0, 0.0, 70.0, 70.0, avatar.orEmpty()), avatarMark)
        return svg
    }

    private fun labelN6(x: Double, y: Double, type: String, name: String, assets: ImageAssets): String {
        var svg = """
  <g id="Label_LN6">
  </g>
  <g id="Text_LN6">
  </g>
        """.trimIndent()
        val textMark = "<g id=\"Text_LN6\">"
        val labelMark = "<g id=\"Label_LN6\">"
        val text = Fonts.torus.textPath(name, 60.0, 84.0, 18.0, "center baseline", "#fff")
        val file = name.replace(" ", "_").lowercase().ifEmpty { "unspecified" }
        val label = assets.local("object-$type-$file.png")
        svg = Svg.setText(svg, text, textMark)
        svg = Svg.setText(svg, label?.let { Svg.image(28.0, 0.0, 64.0, 64.0, it) }, labelMark)
        return Svg.group(x, y, svg)
    }

    private fun splitMessage(comment: String, size: Double = 18.0, maxWidth: Double = 100.0, maxRow: Int = 4): SplitMessage {
        val lines = mutableListOf<String>()
        var row = 0
        val newComment = comment.replace(Regex("[\\n\\r\\u000B\\u000C\\u2028\\u2029]+"), ", ")
        var fontWidth = 0.0
        var line = ""

        for (index in newComment.indices) {
            val c = newComment[index]
            val width = Fonts.puHuiTi.width(line, size)
            val charWidth = Fonts.puHuiTi.width(c, size)
            if (width < maxWidth && width + charWidth >= maxWidth) {
                row++
                if (row > maxRow) {
                    val last = lines.removeLastOrNull().orEmpty()
                    lines += last.dropLast(3) + "..."
                    row--
                    break
                }
                lines += line
                line = c.toString()
            } else {
                line += c
            }
            fontWidth = max(width, fontWidth)
        }

        if (row < 1) {
            lines += comment
            row = 1
            fontWidth = Fonts.puHuiTi.width(comment, 18.0)
        } else if (row < maxRow && line.isNotEmpty()) {
            lines += line
            row++
            fontWidth = max(Fonts.puHuiTi.width(line, 18.0), fontWidth)
        } else if (row >= maxRow && line.isNotEmpty()) {
            val last = lines.removeLastOrNull().orEmpty()
            lines += last.dropLast(3) + "..."
        }
        return SplitMessage(lines, row, fontWidth)
    }

    private fun renderMessage(x: Double, y: Double, size: Double, lines: List<String>, row: Int, color: String): String {
        val out = StringBuilder()
        for (index in 0 until row) {
            val line = lines.getOrNull(index).orEmpty()
            out.append(Fonts.puHuiTi.textPath(line, x, y + 2 + index * 20, size, "left baseline", color))
        }
        return out.toString()
    }

    private fun renderDiscussion(
        discussion: List<DiscussionDetails>,
        users: List<OsuUser>,
        assets: ImageAssets,
        x: Double,
        y: Double,
        maxRow: Int,
        maxHeight: Double,
        maxWidth: Double,
    ): String {
        val out = StringBuilder()
        var sumX = 0.0
        var sumY = 0.0
        var column = 1
        var labelMaxWidth = 0.0

        for (detail in discussion) {
            val user = users.firstOrNull { it.userID == detail.userID }
            val remainHeight = maxHeight - sumY
            val remainRow = height2Row(remainHeight, maxRow)
            val availableWidth = maxWidth / 2 - 25
            val remainingWidth = maxWidth - sumX - column * 25
            val targetWidth = Numbers.clampToInteger(remainingWidth, availableWidth, 0.0)
            val split = splitMessage(detail.post?.message.orEmpty(), 18.0, targetWidth, remainRow)
            labelMaxWidth = max(labelMaxWidth, split.fontWidth + 46 + 15)
            out.append(labelN2(user, userAvatar(assets, user), detail, sumX, sumY, labelMaxWidth, split.lines, remainRow, assets))
            sumY += if (split.row <= 2) 35 + 15 * split.row else 25 + 20 * split.row
            if (sumY >= maxHeight - 40) {
                sumX += labelMaxWidth + 10
                sumY = 0.0
                column++
                labelMaxWidth = 0.0
            }
            if (maxWidth - sumX <= 100) break
        }
        return Svg.group(x, y, out.toString())
    }

    private fun height2Row(height: Double, maxRow: Int): Int {
        val row = when {
            height <= 65 -> 1
            height <= 85 -> 2
            height <= 105 -> 3
            else -> 4
        }
        return min(row, maxRow)
    }

    private fun drawNominators(x: Double, y: Double, nominators: List<OsuUser>, assets: ImageAssets, slotLength: Double): String {
        val out = mutableListOf<String>()
        val half = (slotLength - 5) / 2
        nominators.forEachIndexed { index, user ->
            val dx = x + index * slotLength + half - 50
            out += labelN1(dx, y, user, userAvatar(assets, user), slotLength, assets)
        }
        return out.joinToString("\n")
    }

    private fun drawIcons(draw: Boolean, x: Double, y: Double, link: String, assets: ImageAssets): String {
        if (!draw) return ""
        val image = assets.local(link) ?: return ""
        return Svg.image(x, y, 50.0, 50.0, image)
    }

    private fun drawHype(
        x: Double,
        y: Double,
        length: Double,
        blankLength: Double = length,
        color: String = "#82BA44",
        fullColor: String = "#46393F",
    ): String {
        return PanelDraw.rect(x, y, blankLength, 15.0, 7.5, fullColor) +
            PanelDraw.rect(x, y, length, 15.0, 7.5, color)
    }

    private fun drawRRect(count: Int, slot: Int, x: Double, y: Double, length: Double, color: String = "#fff"): String {
        val out = StringBuilder()
        for (index in 0 until slot) {
            out.append(PanelDraw.rect(x + (length + 5) * index, y, length, 15.0, 7.5, "#46393F"))
        }
        for (index in 0 until count) {
            out.append(PanelDraw.rect(x + (length + 5) * index, y, length, 15.0, 7.5, color))
        }
        return out.toString()
    }

    private fun groupAbbr(group: String?): String {
        return when (group) {
            "bng" -> "B"
            "bng_limited" -> "P"
            "nat" -> "N"
            "gmt" -> "G"
            "alm", "alumni" -> "A"
            "ppy" -> "Y"
            "bot" -> "T"
            "dev" -> "D"
            "spt", "support" -> "S"
            "lvd" -> "L"
            "bsc" -> "C"
            else -> ""
        }
    }

    private fun genreName(id: Int?): String {
        return when (id) {
            2 -> "video game"
            3 -> "anime"
            4 -> "rock"
            5 -> "pop"
            6 -> "other"
            7 -> "novelty"
            9 -> "hip hop"
            10 -> "electronic"
            11 -> "metal"
            12 -> "classical"
            13 -> "folk"
            14 -> "jazz"
            else -> "unspecified"
        }
    }

    private fun languageName(id: Int?): String {
        return when (id) {
            2 -> "english"
            3 -> "japanese"
            4 -> "chinese"
            5 -> "instrumental"
            6 -> "korean"
            7 -> "french"
            8 -> "german"
            9 -> "swedish"
            10 -> "spanish"
            11 -> "italian"
            12 -> "russian"
            13 -> "polish"
            14 -> "other"
            else -> "unspecified"
        }
    }

    private fun userAvatar(assets: ImageAssets, user: OsuUser?): String? {
        return assets.remote(user?.avatarUrl) ?: assets.local("avatar-guest.png")
    }

    private data class SplitMessage(val lines: List<String>, val row: Int, val fontWidth: Double)
}
