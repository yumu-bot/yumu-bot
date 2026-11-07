package com.now.nowbot.image.card

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.Texts
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.model.osu.OsuUser

data class CardA1Data(
    val background: String? = null,
    val avatar: String? = null,
    val subIcon1: String? = null,
    val subIcon2: String? = null,
    val subBanner: String? = null,
    val country: String? = null,
    val teamUrl: String? = null,
    val top1: String = "",
    val top2: String = "",
    val left1: String = "",
    val left2: String = "",
    val right0: String = "",
    val right1: String = "",
    val right2: String = "",
    val right3b: String = "",
    val right3m: String = "",
    val left1Colors: List<String>? = null,
    val left1Stroke: String = "",
    val left1StrokeWidth: Double = 0.0,
)

object CardA1 {
    private const val MARK_DEFS = "<defs>"
    private const val MARK_TEXT = "<g id=\"Text\">"
    private const val MARK_BANNER = "<g id=\"Banner\" style=\"clip-path: url(#clippath-CA1-6);\">"
    private const val MARK_BACKGROUND = "<g style=\"clip-path: url(#clippath-CA1-1);\">"
    private const val MARK_AVATAR = "<g style=\"clip-path: url(#clippath-CA1-2);\">"
    private const val MARK_COUNTRY = "<g style=\"clip-path: url(#clippath-CA1-3);\">"
    private const val MARK_ICON1 = "<g style=\"clip-path: url(#clippath-CA1-4);\">"
    private const val MARK_ICON2 = "<g style=\"clip-path: url(#clippath-CA1-5);\">"
    private const val MARK_TEAM = "<g style=\"clip-path: url(#clippath-CA1-7);\">"

    fun render(data: CardA1Data, assets: ImageAssets): String {
        var svg = Templates.read("Card_A1.svg")

        val top1 = data.top1
        val top1Ascii = Texts.isAscii(top1)
        val topFont = if (top1Ascii) Fonts.torus else Fonts.puHuiTi
        var topSize = if (top1.length > 8 && Fonts.torus.width(top1, 42.0) > 290.0) 36.0 else 42.0
        if (!top1Ascii) topSize -= 4.0

        val top2Font = if (Texts.isAscii(data.top2)) Fonts.torus else Fonts.puHuiTi
        val left1Ascii = Texts.isAscii(data.left1)
        val left2Ascii = Texts.isAscii(data.left2)
        val left1Size = if (left1Ascii) 24.0 else 22.0
        val left2Size = if (left2Ascii) 24.0 else 22.0
        val left1Font = if (left1Ascii) Fonts.torus else Fonts.puHuiTi
        val left2Font = if (left2Ascii) Fonts.torus else Fonts.puHuiTi
        val right0Ascii = Texts.isAscii(data.right0)
        val right0Font = if (right0Ascii) Fonts.torus else Fonts.puHuiTi
        val right0Size = if (right0Ascii) 24.0 else 22.0

        val rightWidth = Fonts.torus.width(data.right3b, 60.0) + Fonts.torus.width(data.right3m, 48.0)
        val rightMaxWidth = 410.0
        val colors = data.left1Colors?.takeIf { it.size > 1 } ?: listOf("#fff", "#fff")
        val gradientId = "grad_CA1_" + Texts.random(6)
        val gradient = """
            <linearGradient id="$gradientId" x1="60%" y1="90%" x2="40%" y2="10%">
                <stop offset="0%" stop-color="${colors[0]}" />
                <stop offset="100%" stop-color="${colors[1]}" />
            </linearGradient>
        """.trimIndent()

        val left1Path = left1Font.cutStringTail(data.left1, left1Size, 390.0 - rightWidth)
        val paths = listOf(
            topFont.textPath(topFont.cutStringTail(top1, topSize, 290.0), 130.0, 53.672, topSize, "left baseline", "#fff"),
            top2Font.textPath(Fonts.torus.cutStringTail(data.top2, 24.0, 290.0), 130.0, 85.836, 24.0, "left baseline", "#fff"),
            left1Font.textPath(left1Path, 20.0, 165.836, left1Size, "left baseline", "url(#$gradientId)"),
            // 原 card_A1 位置参数错位：stroke 收到 2、stroke-width 为 0，保留实际可见输出
            left1Font.textPath(
                left1Path, 20.0, 165.836, left1Size, "left baseline",
                data.left1Stroke, 1.0, data.left1Stroke.isNotEmpty(),
                Svg.number(data.left1StrokeWidth), 0.0,
            ),
            left2Font.textPath(left2Font.cutStringTail(data.left2, left2Size, 390.0 - rightWidth), 20.0, 191.836, left2Size, "left baseline", "#fff"),
            if (data.right0.isNotBlank()) {
                right0Font.textPath(right0Font.cutStringTail(data.right0, right0Size, rightMaxWidth), 420.0, 83.836, right0Size, "right baseline", "#fff")
            } else {
                ""
            },
            Fonts.torus.textPath(Fonts.torus.cutStringTail(data.right1, 24.0, rightMaxWidth), 420.0, 112.836, 24.0, "right baseline", "#fff"),
            Fonts.torus.textPath(Fonts.torus.cutStringTail(data.right2, 24.0, rightMaxWidth), 420.0, 141.836, 24.0, "right baseline", "#fff"),
            Fonts.torus.get2SizeTextPath(data.right3b, data.right3m, 60.0, 48.0, 420.0, 191.59, "right baseline", "#fff"),
        )

        val background = assets.resolveRef(data.background)
        val avatar = assets.resolveRef(data.avatar)
        val team = assets.resolveRef(data.teamUrl)
        val icon1 = assets.resolveRef(data.subIcon1)
        val icon2 = assets.resolveRef(data.subIcon2)
        val banner = assets.resolveRef(data.subBanner)
        val iconOffset = if (data.teamUrl != null) 90.0 else 0.0

        svg = Svg.setText(svg, gradient, MARK_DEFS)
        svg = Svg.setText(svg, assets.countryFlag(data.country, 135.0, 64.0, 44.0), MARK_COUNTRY)
        svg = Svg.setTexts(svg, paths, MARK_TEXT)
        svg = Svg.setText(svg, background?.let { Svg.image(0.0, 0.0, 430.0, 210.0, it, 0.6) }, MARK_BACKGROUND)
        svg = Svg.setText(svg, avatar?.let { Svg.image(20.0, 20.0, 100.0, 100.0, it) }, MARK_AVATAR)
        svg = Svg.setText(svg, team?.let { Svg.image(200.0, 72.0, 76.0, 38.0, it, 1.0, "xMidYMid meet") }, MARK_TEAM)
        svg = Svg.setText(svg, icon1?.let { Svg.image(200.0 + iconOffset, 70.0, 40.0, 40.0, it) }, MARK_ICON1)
        svg = Svg.setText(svg, icon2?.let { Svg.image(250.0 + iconOffset, 70.0, 40.0, 40.0, it) }, MARK_ICON2)
        svg = Svg.setText(svg, banner?.let { Svg.image(70.0, 68.0, 320.0, 52.0, it) }, MARK_BANNER)
        return svg
    }

    fun render(
        user: OsuUser?,
        history: OsuUser?,
        assets: ImageAssets,
        profileCard: String? = null,
        bannerIndex: Int? = null,
    ): String {
        return render(factory(user, history, assets, profileCard, bannerIndex), assets)
    }

    fun factory(
        user: OsuUser?,
        history: OsuUser?,
        assets: ImageAssets,
        profileCard: String? = null,
        bannerIndex: Int? = null,
    ): CardA1Data {
        if (user == null || (user.userID == 0L && user.username.isBlank())) {
            return CardA1Data(
                background = assets.local("card-default.webp"),
                avatar = assets.local("sticker_qiqi_secretly_observing.png"),
                country = "CN",
                top1 = "YumuBot",
                left1 = "#0",
                left2 = "CN#0",
                right3b = "Bot",
            )
        }

        val coverUrl = user.coverUrl.takeIf { it.isNotBlank() }
        val cover = user.cover?.url?.takeIf { it.isNotBlank() }
        val background = profileCard ?: coverUrl ?: cover ?: assets.localBanner(bannerIndex)
        val country = user.country?.code?.ifBlank { user.countryCode } ?: user.countryCode.ifBlank { "CN" }
        val right = rightValues(user, history)

        return CardA1Data(
            background = background,
            avatar = user.avatarUrl.takeIf { it.isNotBlank() } ?: "avatar-guest.png",
            subIcon1 = if (user.isSupporter) "object-card-supporter.png" else null,
            country = country,
            teamUrl = user.team?.flag,
            top1 = user.username,
            top2 = "",
            left1 = left1(user),
            left2 = left2(user, country),
            right0 = right.right0,
            right1 = right.right1,
            right2 = right.right2,
            right3b = right.right3b,
            right3m = right.right3m,
            left1Colors = Colors.getGlobalRankPercentColor(user.globalRank, user.statistics?.globalRankPercent ?: 0.0),
            left1Stroke = "#fff",
            left1StrokeWidth = 2.0,
        )
    }

    private fun left1(user: OsuUser): String {
        val globalRank = user.globalRank
        if (globalRank > 0) return "#$globalRank"
        val highest = user.highestRank
        if (highest != null && highest.rank > 0) {
            return "#${highest.rank}^ (${TimeFormat.difference(highest.updatedAt)})"
        }
        return "#0"
    }

    private fun left2(user: OsuUser, country: String): String {
        val rank = user.countryRank
        return country + if (rank > 0) "#$rank" else ""
    }

    private data class RightValues(
        val right0: String,
        val right1: String,
        val right2: String,
        val right3b: String,
        val right3m: String,
    )

    private fun rightValues(user: OsuUser, history: OsuUser?): RightValues {
        val isBot = user.isBot
        val stats = user.statistics
        val level = (stats?.levelCurrent ?: 0).toInt()
        val progress = (stats?.levelProgress ?: 0).toInt()
        val isNotPlayed = stats != null && level <= 1 && progress == 0
        val accuracy = Numbers.floor(stats?.accuracy, 2).ifBlank { "0" }
        val right2 = if (isBot) {
            ""
        } else if (isNotPlayed) {
            "$accuracy%"
        } else {
            "$accuracy% Lv.$level($progress%)"
        }

        val status = when {
            isBot -> "BOT"
            user.pp > 0 -> "HAS_PP"
            isNotPlayed -> "NOT_PLAYED"
            user.ppEstimate > 0 -> "ESTIMATED"
            else -> "AFK"
        }
        val config = when (status) {
            "BOT" -> "" to "BOT"
            "HAS_PP" -> Numbers.jsRound(user.pp).toLong().toString() to "PP"
            "NOT_PLAYED" -> "" to "NOT_PLAYED"
            "ESTIMATED" -> Numbers.jsRound(user.ppEstimate).toLong().toString() to "?"
            else -> "" to "AFK"
        }

        val teamLabel = user.team?.shortName.orEmpty()
        var right1 = if (history != null) {
            val difference = (user.pp - (history.statistics?.pp ?: 0.0)) * 100
            val ppDifference = Numbers.jsRound(difference) / 100.0
            when {
                ppDifference > 0 -> "+${Svg.number(ppDifference)}PP"
                ppDifference < 0 -> "${Svg.number(ppDifference)}PP"
                user.team != null -> "[$teamLabel]"
                else -> ""
            }
        } else if (user.team != null) {
            "[$teamLabel]"
        } else {
            ""
        }

        var right0 = ""
        if (history != null) {
            val current = user.globalRank
            val previous = history.globalRank
            if (current > 0 && previous > 0 && previous > current) {
                right0 = "+#${previous - current}"
            }
        }
        if (right0.isNotBlank() && right1.isBlank()) {
            right1 = right0
            right0 = ""
        }

        return RightValues(right0, right1, right2, config.first, config.second)
    }
}
