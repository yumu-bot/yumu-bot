package com.now.nowbot.image.card

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.GameModes
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.Texts
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.mappool.old.MapPoolDto
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.model.osu.BeatmapsetSearch
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class CardA2Data(
    val background: String? = null,
    val mapStatus: String = "",
    val title1: String = "",
    val title2: String = "",
    val title3: String = "",
    val left1: String = "",
    val left2: String = "",
    val left3: String = "",
    val right1: String = "",
    val right2: String = "",
    val right3b: String = "",
    val right3m: String = "",
    val right3bSize: Double = 60.0,
    val right3mSize: Double = 48.0,
)

object CardA2 {
    private const val MARK_TEXT = "<g id=\"Text\">"
    private const val MARK_BACKGROUND = "<g style=\"clip-path: url(#clippath-CA2-1);\">"
    private const val MARK_TOP_ICONS = "<g id=\"TopIcons\">"

    fun render(data: CardA2Data, assets: ImageAssets): String {
        var svg = Templates.read("Card_A2.svg")

        val title1Font = titleFont(data.title1)
        val title2Font = titleFont(data.title2)
        val title3Font = titleFont(data.title3)
        val left1Font = titleFont(data.left1)
        val left2Font = titleFont(data.left2)
        val left3Font = titleFont(data.left3)

        val title1Size = if (Texts.isAscii(data.title1)) 36.0 else 32.0
        val title2Size = if (Texts.isAscii(data.title2)) 24.0 else 22.0
        val title3Size = if (Texts.isAscii(data.title3)) 24.0 else 22.0
        val left1Size = if (Texts.isAscii(data.left1)) 24.0 else 22.0
        val left2Size = if (Texts.isAscii(data.left2)) 24.0 else 22.0
        val left3Size = if (Texts.isAscii(data.left3)) 24.0 else 22.0

        val title1MaxWidth = if (data.mapStatus.isNotEmpty()) 350.0 else 390.0
        val titleMaxWidth = 390.0
        val right3bSize = effectiveSize(data.right3bSize, 60.0)
        val right3mSize = effectiveSize(data.right3mSize, 48.0)
        val right3Width = Fonts.torus.width(data.right3b, right3bSize) +
            Fonts.torus.width(data.right3m, right3mSize)
        val leftMaxWidth = 390.0 - right3Width
        val right2Width = Fonts.torus.width(data.right2, 24.0)
        val left1MaxWidth = 390.0 - right2Width

        val texts = listOf(
            title1Font.textPath(
                title1Font.cutStringTail(data.title1, title1Size, title1MaxWidth),
                20.0, 46.6 - 1.8, title1Size, "left baseline", "#fff",
            ),
            title2Font.textPath(
                title2Font.cutStringTail(data.title2, title2Size, titleMaxWidth),
                20.0, 77.4, title2Size, "left baseline", "#fff",
            ),
            title3Font.textPath(
                title3Font.cutStringTail(data.title3, title3Size, titleMaxWidth),
                20.0, 107.4, title3Size, "left baseline", "#fff",
            ),
            Fonts.torus.textPath(data.right1, 420.0, 114.836 - 2.0, 24.0, "right baseline", "#fff"),
            Fonts.torus.textPath(data.right2, 420.0, 141.836, 24.0, "right baseline", "#fff"),
            Fonts.torus.get2SizeTextPath(
                data.right3b, data.right3m, right3bSize, right3mSize, 420.0, 191.59, "right baseline", "#fff",
            ),
            left1Font.textPath(
                Fonts.torus.cutStringTail(data.left1, left1Size, left1MaxWidth, true),
                20.0, 140.836, left1Size, "left baseline", "#fff",
            ),
            left2Font.textPath(
                Fonts.torus.cutStringTail(data.left2, 24.0, leftMaxWidth, true),
                20.0, 165.836, left2Size, "left baseline", "#fff",
            ),
            left3Font.textPath(
                Fonts.torus.cutStringTail(data.left3, 24.0, leftMaxWidth, true),
                20.0, 191.836, left3Size, "left baseline", "#fff",
            ),
        )

        val background = assets.resolveRef(data.background?.takeIf { it.isNotEmpty() })
            ?: assets.local("beatmap-DLfailBG.webp")
        svg = Svg.setText(
            svg,
            background?.let { Svg.image(0.0, 0.0, 430.0, 210.0, it, 0.6) },
            MARK_BACKGROUND,
        )

        if (data.mapStatus.isNotEmpty()) {
            val status = assets.local(mapStatusFile(data.mapStatus))
            svg = Svg.setText(svg, status?.let { Svg.image(370.0, 10.0, 50.0, 50.0, it) }, MARK_TOP_ICONS)
        }

        return Svg.setTexts(svg, texts, MARK_TEXT)
    }

    fun fromBeatmap(beatmap: Beatmap, assets: ImageAssets): CardA2Data {
        val star = Numbers.floorParts(beatmap.starRating, 2)
        return CardA2Data(
            background = assets.mapBackground(beatmap, "list@2x"),
            mapStatus = beatmap.status,
            title1 = beatmap.beatmapset?.title.orEmpty(),
            title2 = beatmap.beatmapset?.artist.orEmpty(),
            title3 = beatmap.difficultyName,
            left1 = "",
            left2 = beatmap.beatmapset?.creator.orEmpty(),
            left3 = if (beatmap.beatmapID != 0L) "B${beatmap.beatmapID}" else "B0",
            right1 = "",
            right2 = beatmapStats(beatmap),
            right3b = star.integer,
            right3m = star.decimal + "*",
        )
    }

    fun fromSearchResult(
        search: BeatmapsetSearch,
        resultCount: Int,
        rule: String,
        first: Beatmapset?,
        last: Beatmapset?,
        assets: ImageAssets,
    ): CardA2Data {
        val background = assets.remote(first?.covers?.list2x)
            ?: assets.remote(last?.covers?.list2x)
            ?: assets.local("beatmap-DLfailBG.webp")
        val cursorDate = search.cursor?.approvedDate?.toLongOrNull()?.let {
            Instant.ofEpochMilli(it).atZone(ZONE).format(SEARCH_CURSOR_FORMAT)
        }
        val left2 = cursorDate ?: first?.rankedDate?.let { formatRankedShort(it) } ?: "null"
        val left3 = last?.rankedDate?.let { formatRankedShort(it) } ?: "null"
        return CardA2Data(
            background = background,
            mapStatus = rule,
            title1 = "Search:",
            title2 = if (search.info != null) "Sort: ${search.info?.sort.orEmpty()}" else "Sort: Default",
            title3 = "",
            left1 = "time duration:",
            left2 = left2,
            left3 = left3,
            right1 = "total ${search.total}x",
            right2 = "results:",
            right3b = resultCount.toString(),
            right3m = "x",
        )
    }

    fun fromSearchMap(beatmapset: Beatmapset, rank: Int, assets: ImageAssets): CardA2Data {
        val background = assets.mapBackground(beatmapset, "list@2x")
        val ranked = beatmapset.ranked.toInt()
        val hasLeaderBoard = GameModes.hasLeaderBoard(ranked)
        val isQualified = ranked == 3
        val isRanked = hasLeaderBoard && !isQualified
        val right3b: String
        val right3m: String
        if (isQualified) {
            val now = Instant.now()
            val expected = beatmapset.rankedDate
            if (expected == null || expected.toInstant().isBefore(now)) {
                right3b = "..."
                right3m = ""
            } else {
                val days = ChronoUnit.DAYS.between(now, expected.toInstant())
                val hours = ChronoUnit.HOURS.between(now, expected.toInstant()) % 24
                val minutes = ChronoUnit.MINUTES.between(now, expected.toInstant()) % 60
                when {
                    days > 0 -> {
                        right3b = days.toString()
                        right3m = "d${hours}h"
                    }
                    hours > 0 -> {
                        right3b = hours.toString()
                        right3m = "h${minutes}m"
                    }
                    minutes > 0 -> {
                        right3b = minutes.toString()
                        right3m = "m"
                    }
                    else -> {
                        right3b = "..."
                        right3m = ""
                    }
                }
            }
        } else {
            right3b = "-"
            right3m = ""
        }
        val date = if (isQualified || isRanked) beatmapset.rankedDate else beatmapset.submittedDate
        return CardA2Data(
            background = background,
            mapStatus = beatmapset.status.ifBlank { "graveyard" },
            title1 = beatmapset.titleUnicode.ifBlank { "Unknown Title" },
            title2 = beatmapset.artistUnicode.ifBlank { "Unknown Artist" },
            title3 = beatmapset.creator.ifBlank { "Unknown Mapper" },
            left1 = "",
            left2 = "#$rank",
            left3 = "s${beatmapset.beatmapsetID}",
            right1 = if (isQualified) "Expected:" else if (isRanked) "Ranked:" else "Submitted:",
            right2 = formatCardDate(date, "Invalid date"),
            right3b = right3b,
            right3m = right3m,
        )
    }

    fun fromPool(
        pool: MapPoolDto,
        mode: OsuMode,
        mapCount: Int,
        modCount: Int,
        assets: ImageAssets,
        fallbackBanner: String? = null,
    ): CardA2Data {
        val background = assets.remote("https://assets.ppy.sh/beatmaps/${pool.firstMapSID}/covers/cover.jpg")
            ?: fallbackBanner
            ?: assets.localBanner()
        return CardA2Data(
            background = background,
            mapStatus = "",
            title1 = pool.name.orEmpty(),
            title2 = "",
            title3 = "",
            left1 = "",
            left2 = "mode: " + GameModes.getGameMode(mode.shortName, 0),
            left3 = if (mapCount > 0 && modCount > 0) "P$modCount M$mapCount" else "",
            right1 = "",
            right2 = "Pool ID:",
            right3b = pool.id.toString(),
            right3m = "",
        )
    }

    private fun mapStatusFile(status: String): String {
        return when (status.lowercase().trim()) {
            "-2", "-1", "0" -> "object-beatmap-unranked.png"
            "1", "2", "ranked", "approved" -> "object-beatmap-ranked.png"
            "3", "qualified" -> "object-beatmap-qualified.png"
            "4", "loved" -> "object-beatmap-loved.png"
            "" -> "error.png"
            else -> "object-beatmap-unranked.png"
        }
    }

    fun fromMatchBeatmap(beatmap: Beatmap, assets: ImageAssets): CardA2Data {
        val set = beatmap.beatmapset
        val mode = GameModes.getGameMode(beatmap.modeInt ?: beatmap.mode.safeModeValue.toInt(), 1)
        val right2 = when (mode) {
            "o" -> "CS${beatmap.cs ?: 0} AR${beatmap.ar ?: 0} OD${beatmap.od ?: 0}"
            "t" -> "OD${beatmap.od ?: 0}"
            "c" -> "CS${beatmap.cs ?: 0} AR${beatmap.ar ?: 0}"
            "m" -> "${beatmap.cs ?: 0}Key OD${beatmap.od ?: 0}"
            else -> "-"
        }
        val star = Numbers.floorParts(beatmap.starRating, 2)
        return CardA2Data(
            background = assets.mapBackground(beatmap, "list@2x"),
            mapStatus = beatmap.status,
            title1 = set?.title?.ifBlank { "Deleted Beatmap" } ?: "Deleted Beatmap",
            title2 = set?.artist?.ifBlank { "-" } ?: "-",
            title3 = set?.creator?.ifBlank { "-" } ?: "-",
            left1 = "",
            left2 = beatmap.difficultyName.ifBlank { "-" },
            left3 = "b${beatmap.beatmapID}",
            right1 = "",
            right2 = right2,
            right3b = star.integer,
            right3m = star.decimal + "*",
        )
    }

    private fun beatmapStats(beatmap: Beatmap): String {
        val cs = Numbers.round(beatmap.cs, 1)
        val ar = Numbers.round(beatmap.ar, 1)
        val od = Numbers.round(beatmap.od, 1)
        val hp = Numbers.round(beatmap.hp, 1)
        val mode = GameModes.getGameMode(beatmap.modeInt ?: beatmap.mode.safeModeValue.toInt(), 1)
        return when (mode) {
            "o" -> "CS$cs AR$ar OD$od"
            "t" -> "OD$od HP$hp"
            "c" -> "CS$cs AR$ar HP$hp"
            "m" -> "${cs}Keys OD$od HP$hp"
            else -> ""
        }
    }

    private fun formatRankedShort(time: OffsetDateTime): String {
        return time.atZoneSameInstant(ZONE).format(RANKED_SHORT_FORMAT)
    }

    private fun formatCardDate(time: OffsetDateTime?, fallback: String): String {
        return time?.atZoneSameInstant(ZONE)?.format(CARD_DATE_FORMAT) ?: fallback
    }

    private val ZONE = ZoneOffset.ofHours(8)
    private val SEARCH_CURSOR_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")
    private val RANKED_SHORT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val CARD_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss '[+8]'")

    private fun titleFont(text: String) = if (Texts.isAscii(text)) Fonts.torus else Fonts.puHuiTi

    private fun effectiveSize(value: Double, fallback: Double): Double {
        return if (value == 0.0 || value.isNaN()) fallback else value
    }
}
