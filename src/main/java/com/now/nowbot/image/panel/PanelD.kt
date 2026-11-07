package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.component.profile.D1NComponent
import com.now.nowbot.image.component.profile.D1NData
import com.now.nowbot.image.component.profile.D5Component
import com.now.nowbot.image.component.profile.D5Data
import com.now.nowbot.image.component.profile.D6NComponent
import com.now.nowbot.image.component.profile.D6NData
import com.now.nowbot.image.component.profile.D7Component
import com.now.nowbot.image.component.profile.D7Data
import com.now.nowbot.image.component.profile.MascotAssets
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderAssetRegistry
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.draw.randomBanner
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.GameModes
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.model.osu.OsuUser
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class PanelDParam(
    val user: OsuUser,
    val historyUser: OsuUser? = null,
    val mode: String? = null,
    val bpTimes: IntArray = intArrayOf(),
    val bonusPp: Double = 0.0,
    val day: Long = 1,
    val profileBanner: String? = null,
    val profileCard: String? = null,
)

data class DRenderResult(val svg: String, val report: RenderReport)

object PanelD {
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PD-BR);\">"
    private const val MARK_BACKGROUND = "<g style=\"clip-path: url(#clippath-PD-BG);\">"
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_CARD_A1 = "<g id=\"Card_A1\">"
    private const val MARK_CARD_F1 = "<g id=\"Card_F1\">"
    private const val MARK_CARD_F5 = "<g id=\"Card_F5\">"
    private const val MARK_CARD_F6 = "<g id=\"Card_F6\">"
    private const val MARK_CARD_F7 = "<g id=\"Card_F7\">"

    fun toSvg(
        param: PanelDParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
        bannerIndex: Int? = null,
    ): String = render(param, lease, remote, bannerIndex).svg

    fun render(
        param: PanelDParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
        bannerIndex: Int? = null,
    ): DRenderResult {
        val report = RenderReport()
        val assets = ImageAssets(
            RenderAssetRegistry { bytes -> lease.registerImage(bytes).url },
            remote,
            report = report,
        )
        return DRenderResult(render(param, assets, report, bannerIndex), report)
    }

    fun render(
        param: PanelDParam,
        assets: ImageAssets,
        report: RenderReport = RenderReport(),
        bannerIndex: Int? = null,
    ): String {
        val user = param.user
        val mode = GameModes.getGameMode(param.mode ?: user.mode.shortName, 0, user.mode.shortName)
        var svg = Svg.removeXmlDeclaration(Templates.read("Panel_D.svg"))
        val card = CardA1.render(user, param.historyUser, assets, param.profileCard, bannerIndex)
        val f1 = D1NComponent.render(d1n(user, param.historyUser, mode), assets)
        val f5 = D5Component.render(d5(user, param.bpTimes, mode), assets)
        val f6 = D6NComponent.render(d6n(user, param.historyUser, param.bonusPp, mode), assets)
        val f7 = D7Component.render(d7(user, mode))

        svg = Svg.setText(svg, Svg.group(40.0, 40.0, card), MARK_CARD_A1)
        svg = Svg.setText(svg, Svg.group(40.0, 330.0, f1), MARK_CARD_F1)
        svg = Svg.setText(svg, Svg.group(980.0, 330.0, f5), MARK_CARD_F5)
        svg = Svg.setText(svg, Svg.group(980.0, 690.0, f6), MARK_CARD_F6)
        svg = Svg.setText(svg, f7, MARK_CARD_F7)
        svg = Svg.setText(svg, panelName(param, mode), MARK_INDEX)

        val banner = assets.resolveRef(param.profileBanner) ?: assets.randomBanner("default", bannerIndex)
        svg = Svg.setText(svg, banner?.let { Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7) }, MARK_BANNER)
        svg = Svg.setText(
            svg,
            MascotAssets.rankBackground(user, assets)?.let { Svg.image(0.0, 280.0, 1920.0, 1080.0, it, 0.4) },
            MARK_BACKGROUND,
        )
        return svg
    }

    private fun panelName(param: PanelDParam, mode: String): String {
        val dayText = if (param.day >= 2) "compare time: ${param.day} days ago // " else "compare time: ${param.day} day ago // "
        val request = dayText + "request time: " + TimeFormat.nowTimestamp()
        return Fonts.torus.textPath("powered by Yumubot v0.8.5 VS // (Legacy) Information v3 (!ymi)", 20.0, 26.84, 24.0, "left baseline", "#fff") +
            Fonts.torus.textPath(request, 1900.0, 26.84, 24.0, "right baseline", "#fff") +
            Fonts.torus.textPath("IL", 607.5, 83.67, 48.0, "center baseline", "#fff")
    }

    private fun d1n(user: OsuUser, history: OsuUser?, mode: String): D1NData {
        val stats = user.statistics
        val old = history?.statistics
        return D1NData(
            playCount = stats?.playCount ?: 0,
            playTime = stats?.playTime ?: 0,
            totalHits = stats?.totalHits ?: 0,
            deltaPlayCount = (stats?.playCount ?: 0) - (old?.playCount ?: 0),
            deltaPlayTime = (stats?.playTime ?: 0) - (old?.playTime ?: 0),
            deltaTotalHits = (stats?.totalHits ?: 0) - (old?.totalHits ?: 0),
            mode = mode,
            levelCurrent = user.levelCurrent.toInt(),
            levelProgress = user.levelProgress.toInt(),
        )
    }

    private fun d5(user: OsuUser, bpTimes: IntArray, mode: String): D5Data {
        val highest = user.highestRank
        val date = highest?.updatedAt?.let {
            DateTimeFormatter.ofPattern("yyyy/MM/dd").format(it.atZoneSameInstant(ZoneOffset.ofHours(8)))
        } ?: "?"
        return D5Data(
            mode = mode,
            country = user.country?.name ?: "China",
            countryRank = user.countryRank,
            globalRank = user.globalRank,
            highestRank = highest?.rank ?: 0,
            highestDate = date,
            ranking = user.rankHistory?.data ?: emptyList(),
            bpTimes = bpTimes,
        )
    }

    private fun d6n(user: OsuUser, history: OsuUser?, bonusPp: Double, mode: String): D6NData {
        val stats = user.statistics
        val old = history?.statistics
        val start = user.monthlyPlaycounts.firstOrNull()?.startDate
        val monthly = user.monthlyPlaycounts.associate { it.startDate to it.count }
        val now = java.time.YearMonth.now(ZoneOffset.UTC)
        val firstMonth = start?.take(7)?.let { runCatching { java.time.YearMonth.parse(it) }.getOrNull() } ?: now
        val counts = mutableListOf<Int>()
        var cursor = firstMonth
        while (cursor.isBefore(now)) {
            val key = cursor.toString() + "-01"
            counts += monthly[key] ?: 0
            cursor = cursor.plusMonths(1)
        }
        val last = now
        val first = last.minusMonths(42)
        val mid = last.minusMonths(21)
        return D6NData(
            rankedScore = stats?.rankedScore ?: 0,
            totalScore = stats?.totalScore ?: 0,
            playedMap = user.beatmapPlaycount.toLong(),
            replaysWatched = (stats?.replaysWatchedByOthers ?: 0).toLong(),
            follower = user.followerCount.toLong(),
            medal = user.userAchievementsCount.toLong(),
            deltaRankedScore = (stats?.rankedScore ?: 0) - (old?.rankedScore ?: 0),
            deltaTotalScore = (stats?.totalScore ?: 0) - (old?.totalScore ?: 0),
            bonusPp = bonusPp,
            playCounts = counts.toIntArray(),
            lastYear = last.year.toString().takeLast(2),
            lastMonth = last.monthValue.toString().padStart(2, '0'),
            firstYear = first.year.toString().takeLast(2),
            firstMonth = first.monthValue.toString().padStart(2, '0'),
            midYear = mid.year.toString().takeLast(2),
            midMonth = mid.monthValue.toString().padStart(2, '0'),
            mode = mode,
        )
    }

    private fun d7(user: OsuUser, mode: String): D7Data {
        val stats = user.statistics
        return D7Data(
            mode = mode,
            country = user.country?.code ?: "UN",
            pp4k = stats?.pp4K ?: 0.0,
            pp7k = stats?.pp7K ?: 0.0,
            global4k = (stats?.rank4K ?: 0).toLong(),
            global7k = (stats?.rank7K ?: 0).toLong(),
            country4k = (stats?.countryRank4K ?: 0).toLong(),
            country7k = (stats?.countryRank7K ?: 0).toLong(),
        )
    }
}
