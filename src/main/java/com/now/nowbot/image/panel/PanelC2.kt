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
import com.now.nowbot.model.match.SeriesRating
import com.now.nowbot.model.osu.Beatmapset
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.floor

object PanelC2 {
    val outputFormat: ScoreListOutputFormat = ScoreListFormat.fixedAdvanced()
    private val ZONE = ZoneOffset.ofHours(8)
    private val DATE_TIME = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")

    fun toSvg(series: SeriesRating, assets: ImageAssets): String {
        val statistics = series.statistics
        val start = statistics.startTime.atZoneSameInstant(ZONE).format(DATE_TIME)
        val end = statistics.endTime?.atZoneSameInstant(ZONE)?.format(DATE_TIME).orEmpty()
        val requestTime = "series duration: $start - $end // request time: ${TimeFormat.nowTimestamp()}"

        val firstSet = firstBeatmapset(series)
        val background = assets.mapBackground(firstSet, "list@2x")
        val seriesA2 = CardA2.render(ScoreListGenerate.seriesRating2CardA2(series, assets, background), assets)

        val arr = series.playerDataList.toMutableList()
        val noneRow = floor(arr.size.toDouble() / 2).toInt() + 1
        val luckyDog = if (arr.size % 2 == 1) arr.removeAt(arr.size - 1) else null
        val cards = mutableListOf<String>()
        var index = 0
        while (index < arr.size) {
            for (column in 0..1) {
                cards += playerCard(arr[index + column], assets, index / 2 + 1, column + 1, 2)
            }
            index += 2
        }
        if (luckyDog != null) {
            cards += playerCard(luckyDog, assets, noneRow, 1, 1)
        }

        val panelHeight = ScoreListLayout.panelHeight(arr.size, 110, 2, interval = 40, spacing = 40)
        var svg = ScoreListLayout.panelC(panelHeight, panelHeight - 290)
        svg = Svg.setText(
            svg,
            ScoreListLayout.panelName("Yumu Series Rating v3.5 (!ymsa)", "SA", requestTime),
            ScoreListLayout.MARK_INDEX,
        )
        svg = Svg.setText(
            svg,
            ScoreListLayout.bannerSvg(ScoreListImages.randomBanner(assets)),
            ScoreListLayout.MARK_BANNER_C,
        )
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, seriesA2), ScoreListLayout.MARK_MAIN)
        svg = Svg.setText(svg, cards.joinToString("\n"), ScoreListLayout.MARK_MAIN)
        return svg
    }

    fun render(
        series: SeriesRating,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
        report: RenderReport = RenderReport(),
    ): ScoreListRenderResult {
        return renderScoreList(lease, remote, report) { assets -> toSvg(series, assets) }
    }

    private fun playerCard(
        player: MatchRating.PlayerData,
        assets: ImageAssets,
        row: Int,
        column: Int,
        maxColumn: Int,
    ): String {
        val xBase = if (maxColumn == 2) 40.0 else 510.0
        val x = xBase + 940.0 * (column - 1)
        val y = 330.0 + 150.0 * (row - 1)
        val card = ScoreListGenerate.matchPlayer2CardC(
            player,
            isTeamVs = true,
            assets = assets,
            useTeamColor = false,
            showAssociatedRound = true,
        )
        return Svg.group(x, y, CardC.render(card, assets))
    }

    private fun firstBeatmapset(series: SeriesRating): Beatmapset? {
        return series.matches.firstOrNull()
            ?.events
            ?.mapNotNull { it.round }
            ?.firstOrNull { it.beatmapID != 0L }
            ?.beatmap
            ?.beatmapset
    }
}
