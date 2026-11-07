package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.component.profile.CardD2
import com.now.nowbot.image.component.profile.CardD2Data
import com.now.nowbot.image.component.profile.D3Components
import com.now.nowbot.image.component.profile.D3RankChartData
import com.now.nowbot.image.component.profile.D3RankInterval
import com.now.nowbot.image.component.profile.ProfileTime
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderAssetRegistry
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.draw.randomBanner
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.GameModes
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.PanelColor
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class PanelD3Param(
    val user: OsuUser,
    val bests: List<LazerScore> = emptyList(),
    val historyUser: OsuUser? = null,
    val percentiles: Map<String, Double> = emptyMap(),
    val bonusPp: Double = 0.0,
    val historyDay: Long = 1,
    val rankingTop: Long? = null,
    val rankingBottom: Long? = null,
    val rankingImprovement: Double? = null,
    val rankingIntervals: List<D3RankInterval> = emptyList(),
    val profileBanner: String? = null,
    val profileCard: String? = null,
)

data class D3RenderResult(val svg: String, val report: RenderReport)

object PanelD3 {
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PD2-BR);\">"
    private const val MARK_BACKGROUND = "<g filter=\"url(#blur-PD2-BG)\" style=\"clip-path: url(#clippath-PD2-BG);\">"
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_COMPONENT = "<g id=\"Component\">"
    private const val MARK_CARD_A1 = "<g id=\"Card_A1\">"

    fun toSvg(param: PanelD3Param, lease: RenderResourceLease, remote: RemoteImageSource? = null, bannerIndex: Int? = null): String =
        render(param, lease, remote, bannerIndex).svg

    fun render(param: PanelD3Param, lease: RenderResourceLease, remote: RemoteImageSource? = null, bannerIndex: Int? = null): D3RenderResult {
        val report = RenderReport()
        val assets = ImageAssets(RenderAssetRegistry { bytes -> lease.registerImage(bytes).url }, remote, report = report)
        return D3RenderResult(render(param, assets, report, bannerIndex), report)
    }

    fun render(param: PanelD3Param, assets: ImageAssets, report: RenderReport = RenderReport(), bannerIndex: Int? = null): String {
        val user = param.user
        val hue = user.profileHue.takeIf { it != 0 } ?: 342
        var svg = template(hue)
        val card = CardA1.render(user, param.historyUser, assets, param.profileCard, bannerIndex)
        val rank = rankChartData(user, param)
        val bests = bestCards(param.bests, assets)
        val bestPp = maxOf((user.pp - param.bonusPp), 0.0)
        val stats = user.statistics
        val old = param.historyUser?.statistics
        val rankCounts = listOf(stats?.countSSH ?: 0, stats?.countSS ?: 0, stats?.countSH ?: 0, stats?.countS ?: 0, stats?.countA ?: 0)
        val rankDeltas = listOf(
            delta((stats?.countSSH ?: 0).toLong(), (old?.countSSH ?: 0).toLong()),
            delta((stats?.countSS ?: 0).toLong(), (old?.countSS ?: 0).toLong()),
            delta((stats?.countSH ?: 0).toLong(), (old?.countSH ?: 0).toLong()),
            delta((stats?.countS ?: 0).toLong(), (old?.countS ?: 0).toLong()),
            delta((stats?.countA ?: 0).toLong(), (old?.countA ?: 0).toLong()),
        )
        val bestArray = bestArray(param.bests)
        val pcArray = pcArray(user)
        val components = listOf(
            Svg.group(40.0, 330.0, D3Components.rankChart(rank, hue)),
            Svg.group(40.0, 500.0, D3Components.level(user.levelCurrent.toInt(), user.levelProgress.toInt(), hue, assets)),
            Svg.group(40.0, 620.0, D3Components.bests(bests, bestPp, param.bonusPp, hue)),
            Svg.group(40.0, 935.0, D3Components.rankCounts(rankCounts, rankDeltas, maxOf(1, rankCounts.sum()), param.percentiles["rank_count_score"] ?: 0.0, hue, assets)),
            Svg.group(1450.0, 330.0, D3Components.bestMatrix(bestArray.counts, bestArray.week0, bestArray.week4, bestArray.week8, bestArray.max, bestArray.time, hue)),
            Svg.group(1675.0, 330.0, D3Components.pcMatrix(pcArray.counts, pcArray.years, pcArray.max, pcArray.time, hue)),
            Svg.group(550.0, 500.0, D3Components.profileCard(user, param.historyUser, param.percentiles, hue, assets)),
        )
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, card), MARK_CARD_A1)
        svg = Svg.setTexts(svg, components, MARK_COMPONENT)
        val day = if (param.historyDay >= 2) "compare time: ${param.historyDay} days ago // " else "compare time: ${param.historyDay} day ago // "
        val request = day + "request time: " + TimeFormat.nowTimestamp()
        svg = Svg.setText(
            svg,
            Fonts.torus.textPath("powered by Yumubot v0.8.5 VS // Information v5 (!ymi)", 20.0, 26.84, 24.0, "left baseline", "#fff") +
                Fonts.torus.textPath(request, 1900.0, 26.84, 24.0, "right baseline", "#fff") +
                Fonts.torus.textPath("I", 607.5, 83.67, 48.0, "center baseline", "#fff"),
            MARK_INDEX,
        )
        val banner = assets.resolveRef(param.profileBanner) ?: assets.randomBanner("default", bannerIndex)
        svg = Svg.setText(svg, banner?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) }, MARK_BANNER)
        svg = Svg.setText(svg, com.now.nowbot.image.component.profile.MascotAssets.rankBackground(user, assets)?.let { Svg.image(0.0, 280.0, 1920.0, 1080.0, it, 0.4) }, MARK_BACKGROUND)
        return svg
    }

    private fun template(hue: Int): String = """
        <svg xmlns="http://www.w3.org/2000/svg" width="1920" height="1080" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 1920 1080">
            <defs>
                <clipPath id="clippath-PD2-BR"><rect width="1920" height="320" rx="20" ry="20" style="fill: none;"/></clipPath>
                <clipPath id="clippath-PD2-BG"><rect y="290" width="1920" height="790" rx="30" ry="30" style="fill: none;"/></clipPath>
                <filter id="blur-PD2-BG" height="110%" width="110%" x="-5%" y="-5%" filterUnits="userSpaceOnUse"><feGaussianBlur in="userSpaceOnUse" stdDeviation="15" result="blur"/></filter>
            </defs>
            <g id="BannerBase"><rect width="1920" height="320" rx="20" ry="20" style="fill: ${PanelColor.base(hue)}"/></g>
            <g id="Banner"><g style="clip-path: url(#clippath-PD2-BR);"></g></g>
            <g id="Background"><rect y="290" width="1920" height="790" rx="30" ry="30" style="fill: ${PanelColor.bottom(hue)};"/><g filter="url(#blur-PD2-BG)" style="clip-path: url(#clippath-PD2-BG);"></g></g>
            <g id="Component"></g>
            <g id="Card_A1"></g>
            <g id="IndexBase"><rect x="510" y="40" width="195" height="60" rx="15" ry="15" style="fill: ${PanelColor.middle(hue)};"/></g>
            <g id="Index"></g>
        </svg>
    """.trimIndent()

    private fun rankChartData(user: OsuUser, param: PanelD3Param): D3RankChartData {
        val ranking = user.rankHistory?.data ?: emptyList()
        val top = param.rankingTop ?: (ranking.minOrNull() ?: 0)
        val bottom = param.rankingBottom ?: (ranking.maxOrNull() ?: 0)
        val improvement = param.rankingImprovement
            ?: ((ranking.firstOrNull() ?: 0) - (ranking.lastOrNull() ?: 0)).toDouble()
        val intervals = param.rankingIntervals.ifEmpty {
            if (improvement != 0.0) listOf(D3RankInterval((ranking.size - 1).coerceAtLeast(0), improvement)) else emptyList()
        }
        return D3RankChartData(ranking, top, bottom, improvement, intervals)
    }

    private fun bestCards(scores: List<LazerScore>, assets: ImageAssets): List<String> {
        return scores.take(6).map { score ->
            val star = score.beatmap.starRating
            val starColor = Colors.getStarRatingColor(star)
            val starTextColor = if (star < 4) "#1c1719" else "#fff"
            val rank = score.rank
            val rankColor = Colors.getRankColor(rank)
            val rankTextColor = if (rank == "X" || rank == "XH") "#1c1719" else "#fff"
            val total = score.beatmap.totalLength
            val time = "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
            val background = assets.mapBackground(score, "list@2x")
            CardD2.render(
                CardD2Data(background, null, Numbers.jsRound(score.pp).toLong().toString(), "PP", "",
                    Numbers.floor(star, 2), starTextColor, starColor, rank, rankTextColor, rankColor,
                    score.beatmap.beatmapID.toString(), time),
                background,
            )
        }
    }

    private data class BestArray(val counts: List<Int>, val max: Int, val time: String, val week0: String, val week4: String, val week8: String)

    private fun bestArray(scores: List<LazerScore>): BestArray {
        val today = LocalDate.now()
        val end = today.with(DayOfWeek.SUNDAY)
        val start = end.minusDays(90)
        val counts = mutableListOf<Int>()
        var cursor = start
        while (!cursor.isAfter(end)) {
            val count = scores.count { it.endedTime.atZoneSameInstant(ZoneOffset.UTC).toLocalDate() == cursor }
            counts += count
            cursor = cursor.plusDays(1)
        }
        val max = counts.maxOrNull() ?: 0
        val index = counts.indexOf(max)
        val time = if (max > 0) start.plusDays(index.toLong()).format(DateTimeFormatter.ofPattern("M-d")) else "-"
        return BestArray(counts, max, time, end.format(DateTimeFormatter.ofPattern("M-d")), end.minusWeeks(4).format(DateTimeFormatter.ofPattern("M-d")), end.minusWeeks(8).format(DateTimeFormatter.ofPattern("M-d")))
    }

    private data class PcArray(val counts: List<Int>, val max: Int, val time: String, val years: List<String>)

    private fun pcArray(user: OsuUser): PcArray {
        val values = user.monthlyPlaycounts.takeLast(39).map { it.count }
        val max = values.maxOrNull() ?: 0
        val index = values.indexOf(max)
        val time = user.monthlyPlaycounts.takeLast(39).getOrNull(index)?.startDate?.take(7) ?: "-"
        val now = java.time.YearMonth.now(ZoneOffset.ofHours(8))
        val years = (3 downTo 0).map { now.minusYears(it.toLong()).year.toString().takeLast(2) }
        return PcArray(values, max, time, years)
    }

    private fun delta(now: Long, old: Long): Long = now - old
}
