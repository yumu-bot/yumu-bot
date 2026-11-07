package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA2
import com.now.nowbot.image.card.CardC
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.scorelist.ScoreListGenerate
import com.now.nowbot.image.scorelist.ScoreListFormat
import com.now.nowbot.image.scorelist.ScoreListImages
import com.now.nowbot.image.scorelist.ScoreListLayout
import com.now.nowbot.image.scorelist.ScoreListOutputFormat
import com.now.nowbot.image.scorelist.ScoreListRenderResult
import com.now.nowbot.image.scorelist.renderScoreList
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.model.match.MatchRating
import com.now.nowbot.model.osu.Beatmapset
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

object PanelC {
    val outputFormat: ScoreListOutputFormat = ScoreListFormat.fixedAdvanced()
    private val ZONE = ZoneOffset.ofHours(8)
    private val DATE_TIME = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
    private val TIME = DateTimeFormatter.ofPattern("HH:mm")

    fun toSvg(rating: MatchRating, assets: ImageAssets): String {
        val skip = (rating.skipIgnoreMap["skip"] as? Number)?.toInt() ?: 0
        val ignore = (rating.skipIgnoreMap["ignore"] as? Number)?.toInt() ?: 0
        val skipText = if (skip > 0) "skip: $skip // " else ""
        val ignoreText = if (ignore > 0) "ignore: $ignore // " else ""
        val requestTime = "$skipText${ignoreText}match time: ${matchDuration(rating)} // request time: ${TimeFormat.nowTimestamp()}"
        val banner = ScoreListImages.randomBanner(assets)

        val firstSet = firstBeatmapset(rating)
        val background = assets.mapBackground(firstSet, "list@2x")
        val statA2 = CardA2.render(ScoreListGenerate.matchRating2CardA2(rating, assets, background), assets)

        val red = rating.playerDataList.filter { it.team == "red" }
        val blue = rating.playerDataList.filter { it.team == "blue" }
        val none = rating.playerDataList.filter { it.team != "red" && it.team != "blue" }.toMutableList()

        val vsRow = max(red.size, blue.size)
        val totalRow = vsRow + ceil(none.size.toDouble() / 2).toInt()
        val cards = mutableListOf<String>()

        if (rating.isTeamVs) {
            red.forEachIndexed { index, player ->
                cards += playerCard(player, true, assets, index + 1, 1, 2)
            }
            blue.forEachIndexed { index, player ->
                cards += playerCard(player, true, assets, index + 1, 2, 2)
            }
        } else {
            val noneRow = floor(none.size.toDouble() / 2).toInt() + 1
            val luckyDog = if (none.size % 2 == 1) none.removeAt(none.size - 1) else null
            var index = 0
            while (index < none.size) {
                for (column in 0..1) {
                    cards += playerCard(none[index + column], false, assets, vsRow + index / 2 + 1, column + 1, 2)
                }
                index += 2
            }
            if (luckyDog != null) {
                cards += playerCard(luckyDog, false, assets, vsRow + noneRow, 1, 1)
            }
        }

        val panelHeight = ScoreListLayout.panelHeight(totalRow * 2, 110, 2, interval = 40, spacing = 40)
        var svg = ScoreListLayout.panelC(panelHeight, panelHeight - 290)
        svg = Svg.setText(
            svg,
            ScoreListLayout.panelName("Yumu Rating v3.5 (!ymra)", "RA", requestTime),
            ScoreListLayout.MARK_INDEX,
        )
        svg = Svg.setText(
            svg,
            ScoreListLayout.bannerSvg(banner),
            ScoreListLayout.MARK_BANNER_C,
        )
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, statA2), ScoreListLayout.MARK_MAIN)
        svg = Svg.setText(svg, cards.joinToString("\n"), ScoreListLayout.MARK_MAIN)
        return svg
    }

    fun render(
        rating: MatchRating,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
        report: RenderReport = RenderReport(),
    ): ScoreListRenderResult {
        return renderScoreList(lease, remote, report) { assets -> toSvg(rating, assets) }
    }

    private fun playerCard(
        player: MatchRating.PlayerData,
        isTeamVs: Boolean,
        assets: ImageAssets,
        row: Int,
        column: Int,
        maxColumn: Int,
    ): String {
        val xBase = if (maxColumn == 2) 40.0 else 510.0
        val x = xBase + 940.0 * (column - 1)
        val y = 330.0 + 150.0 * (row - 1)
        val card = ScoreListGenerate.matchPlayer2CardC(player, isTeamVs, assets)
        return Svg.group(x, y, CardC.render(card, assets))
    }

    private fun firstBeatmapset(rating: MatchRating): Beatmapset? {
        return rating.match.events
            .mapNotNull { it.round }
            .firstOrNull { it.beatmapID != 0L }
            ?.beatmap
            ?.beatmapset
    }

    private fun matchDuration(rating: MatchRating): String {
        val match = rating.match
        val start = match.startTime.atZoneSameInstant(ZONE).format(DATE_TIME)
        if (!match.isMatchEnd) return "$start - in progress"
        val endTime = match.endTime
        if (endTime == null) return "$start - in progress"
        val end = endTime.atZoneSameInstant(ZONE).format(TIME)
        return "$start - $end (${TimeFormat.difference(endTime, 0)})"
    }
}
