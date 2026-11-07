package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.component.profile.CardD2
import com.now.nowbot.image.component.profile.CardD2Data
import com.now.nowbot.image.component.profile.D2Components
import com.now.nowbot.image.component.profile.D2HistoryData
import com.now.nowbot.image.component.profile.D2MainData
import com.now.nowbot.image.component.profile.D2RankData
import com.now.nowbot.image.component.profile.D2RankHistoryData
import com.now.nowbot.image.component.profile.MascotAssets
import com.now.nowbot.image.component.profile.ProfileTime
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
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
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class PanelD2Param(
    val user: OsuUser,
    val historyUser: OsuUser? = null,
    val scores: List<LazerScore> = emptyList(),
    val bestTime: IntArray = intArrayOf(),
    val mode: String? = null,
    val historyDay: Long = 1,
    val profileBanner: String? = null,
    val profileCard: String? = null,
)

data class D2RenderResult(val svg: String, val report: RenderReport)

object PanelD2 {
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PD2-BR);\">"
    private const val MARK_BACKGROUND = "<g filter=\"url(#blur-PD2-BG)\" style=\"clip-path: url(#clippath-PD2-BG);\">"
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_COMPONENT = "<g id=\"Component\">"
    private const val MARK_CARD_A1 = "<g id=\"Card_A1\">"

    fun toSvg(param: PanelD2Param, lease: RenderResourceLease, remote: RemoteImageSource? = null, bannerIndex: Int? = null): String =
        render(param, lease, remote, bannerIndex).svg

    fun render(param: PanelD2Param, lease: RenderResourceLease, remote: RemoteImageSource? = null, bannerIndex: Int? = null): D2RenderResult {
        val report = RenderReport()
        val assets = ImageAssets(RenderAssetRegistry { bytes -> lease.registerImage(bytes).url }, remote, report = report)
        return D2RenderResult(render(param, assets, report, bannerIndex), report)
    }

    fun render(param: PanelD2Param, assets: ImageAssets, report: RenderReport = RenderReport(), bannerIndex: Int? = null): String {
        val user = param.user
        val history = param.historyUser
        val hue = user.profileHue.takeIf { it != 0 } ?: 342
        var svg = """
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
        val mode = GameModes.getGameMode(param.mode ?: user.mode.shortName, 0, user.mode.shortName)
        val card = CardA1.render(user, history, assets, param.profileCard, bannerIndex)
        val components = listOf(
            Svg.group(40.0, 330.0, D2Components.main(mainData(user, history), assets, hue)),
            Svg.group(40.0, 620.0, D2Components.bests(bestCards(param.scores, assets), hue)),
            Svg.group(40.0, 940.0, D2Components.ranks(rankData(user, history), assets, hue)),
            Svg.group(1390.0, 330.0, D2Components.rankHistory(rankHistoryData(user), hue)),
            Svg.group(1390.0, 530.0, D2Components.bpHistory(bpHistoryData(param.bestTime), hue)),
            Svg.group(1390.0, 650.0, D2Components.pcHistory(pcHistoryData(user), hue)),
            Svg.group(1390.0, 770.0, D2Components.secondary(
                user.statistics?.rankedScore ?: 0, user.statistics?.totalScore ?: 0,
                user.userAchievementsCount.toLong(), user.beatmapPlaycount.toLong(),
                (user.statistics?.maxCombo ?: 0).toLong(), user.followerCount.toLong(),
                delta(user.statistics?.rankedScore, history?.statistics?.rankedScore),
                delta(user.statistics?.totalScore, history?.statistics?.totalScore),
                delta((user.statistics?.maxCombo ?: 0).toLong(), (history?.statistics?.maxCombo ?: 0).toLong()),
                assets, hue,
            )),
            D2Components.mascot(MascotAssets.transparentPath(MascotAssets.name(mode), assets), assets, hue, true),
        )
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, card), MARK_CARD_A1)
        svg = Svg.setTexts(svg, components, MARK_COMPONENT)
        val day = if (param.historyDay >= 2) "compare time: ${param.historyDay} days ago // " else "compare time: ${param.historyDay} day ago // "
        val request = day + "request time: " + TimeFormat.nowTimestamp()
        svg = Svg.setText(
            svg,
            Fonts.torus.textPath("powered by Yumubot v0.8.5 VS // Waifu Information v4 (!ymiw)", 20.0, 26.84, 24.0, "left baseline", "#fff") +
                Fonts.torus.textPath(request, 1900.0, 26.84, 24.0, "right baseline", "#fff") +
                Fonts.torus.textPath("IW", 607.5, 83.67, 48.0, "center baseline", "#fff"),
            MARK_INDEX,
        )
        val banner = assets.resolveRef(param.profileBanner) ?: assets.randomBanner("default", bannerIndex)
        svg = Svg.setText(svg, banner?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) }, MARK_BANNER)
        svg = Svg.setText(svg, MascotAssets.rankBackground(user, assets)?.let { Svg.image(0.0, 280.0, 1920.0, 1080.0, it, 0.6) }, MARK_BACKGROUND)
        return svg
    }

    private fun mainData(user: OsuUser, history: OsuUser?): D2MainData {
        val s = user.statistics
        val h = history?.statistics
        val join = user.joinDate?.let {
            DateTimeFormatter.ofPattern("yyyy-MM-dd").format(it.atZoneSameInstant(ZoneOffset.ofHours(8))) + " [" + TimeFormat.difference(it) + "]"
        } ?: ""
        return D2MainData(
            s?.playCount ?: 0, s?.playTime ?: 0, s?.totalHits ?: 0,
            delta(s?.playCount, h?.playCount), delta(s?.playTime, h?.playTime), delta(s?.totalHits, h?.totalHits), join,
        )
    }

    private fun rankData(user: OsuUser, history: OsuUser?): D2RankData {
        val s = user.statistics
        val h = history?.statistics
        val counts = listOf(s?.countSSH ?: 0, s?.countSS ?: 0, s?.countSH ?: 0, s?.countS ?: 0, s?.countA ?: 0)
        val deltas = listOf(
            delta((s?.countSSH ?: 0).toLong(), (h?.countSSH ?: 0).toLong()),
            delta((s?.countSS ?: 0).toLong(), (h?.countSS ?: 0).toLong()),
            delta((s?.countSH ?: 0).toLong(), (h?.countSH ?: 0).toLong()),
            delta((s?.countS ?: 0).toLong(), (h?.countS ?: 0).toLong()),
            delta((s?.countA ?: 0).toLong(), (h?.countA ?: 0).toLong()),
        )
        return D2RankData(counts, deltas, maxOf(1, counts.sum()))
    }

    private fun rankHistoryData(user: OsuUser): D2RankHistoryData {
        val highest = user.highestRank
        val difference = highest?.updatedAt?.let { TimeFormat.difference(it) } ?: "0d"
        return D2RankHistoryData(user.rankHistory?.data ?: emptyList(), highest?.rank ?: 0, difference)
    }

    private fun bpHistoryData(bestTime: IntArray): D2HistoryData {
        val values = bestTime.map { it.toDouble() }
        val max = values.maxOrNull() ?: 0.0
        val index = values.indexOf(max).coerceAtLeast(0)
        return D2HistoryData(values, "${-(90 - index)}d", listOf("-90d", "-45d", "-0d"))
    }

    private fun pcHistoryData(user: OsuUser): D2HistoryData {
        val monthlies = user.monthlyPlaycounts
        val values = monthlies.map { it.count.toDouble() }
        val max = values.maxOrNull() ?: 0.0
        val maxIndex = values.indexOf(max).coerceAtLeast(0)
        val time = monthlies.getOrNull(maxIndex)?.startDate?.let { TimeFormat.difference(it, 0) } ?: "0d"
        val earliest = monthlies.firstOrNull()?.startDate?.take(7)?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        val latest = monthlies.lastOrNull()?.startDate?.take(7)?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        val axis = if (earliest != null && latest != null) {
            val period = kotlin.math.ceil(earliest.until(latest, java.time.temporal.ChronoUnit.MONTHS) / 2.0).toLong()
            listOf(earliest.toString(), latest.minusMonths(kotlin.math.abs(period)).toString(), latest.toString())
        } else {
            val now = YearMonth.now(ZoneOffset.ofHours(8))
            listOf(now.minusMonths(42).toString(), now.minusMonths(21).toString(), now.toString())
        }
        return D2HistoryData(values, time, axis)
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
                CardD2Data(
                    background, null, Numbers.jsRound(score.pp).toLong().toString(), "PP",
                    "", Numbers.floor(star, 2), starTextColor, starColor,
                    rank, rankTextColor, rankColor,
                    score.beatmap.beatmapID.toString(), time,
                ),
                background,
            )
        }
    }

    private fun delta(now: Long?, old: Long?): Long = (now ?: 0) - (old ?: 0)
}
