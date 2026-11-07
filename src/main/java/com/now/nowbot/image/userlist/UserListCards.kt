package com.now.nowbot.image.userlist

import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.card.CardA1Data
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.image.util.duration
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.service.messageServiceImpl.GuestDifficultyService
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Locale

object UserListCards {
    fun me(
        user: OsuUser?,
        history: OsuUser? = null,
        assets: ImageAssets,
        profileCard: String? = null,
        bannerIndex: Int? = null,
    ): CardA1Data {
        return CardA1.factory(user, history, assets, profileCard, bannerIndex)
    }

    fun micro(user: MicroUser, type: String?, showMutual: Boolean, assets: ImageAssets): CardA1Data {
        val statistics = user.statistics
        val supporter = if (user.isSupporter) assets.local("object-card-supporter.png") else null
        val mutual = if (showMutual) {
            if (user.isMutual) assets.local("object-card-mutual.png") else assets.local("object-card-follower.png")
        } else {
            null
        }
        val subIcon1 = supporter ?: mutual
        val subIcon2 = if (supporter != null) mutual else null

        val country = user.countryCode.ifBlank { "CN" }
        val globalRank = statistics?.globalRank?.takeIf { it != 0L }
        val level = statistics?.levelCurrent ?: 0
        val progress = statistics?.levelProgress ?: 0
        val accuracy = Numbers.floor(statistics?.accuracy, 2).ifBlank { "0" }
        val pp = statistics?.pp?.takeIf { it != 0.0 }

        val right2 = when {
            user.isBot -> ""
            level + progress > 0 -> "$accuracy% Lv.$level($progress%)"
            else -> "$accuracy%"
        }
        val right3b = if (user.isBot || pp == null) "" else Numbers.jsRound(pp).toLong().toString()
        val right3m = when {
            user.isBot -> "Bot"
            pp != null -> "PP"
            else -> "AFK"
        }

        return CardA1Data(
            background = banner(user, assets),
            avatar = avatarOf(user.userID, user.avatarUrl, assets),
            subIcon1 = subIcon1,
            subIcon2 = subIcon2,
            country = country,
            top1 = user.username,
            left1 = globalRank?.let { "#$it" } ?: "#0",
            left2 = "$country ${user.userID}",
            right1 = typeText(user, type),
            right2 = right2,
            right3b = right3b,
            right3m = right3m,
        )
    }

    fun mapper(user: OsuUser, assets: ImageAssets, profileCard: String? = null): CardA1Data {
        val country = user.country?.code?.ifBlank { "CN" } ?: "CN"
        val background = assets.resolveRef(user.coverUrl)
            ?: assets.resolveRef(user.cover?.url)
            ?: assets.resolveRef(profileCard)
            ?: assets.localBanner()

        return CardA1Data(
            background = background,
            avatar = avatarOf(user.userID, user.avatarUrl, assets),
            subIcon1 = if (user.isSupporter) assets.local("object-card-supporter.png") else null,
            country = country,
            top1 = user.username,
            left1 = country,
            left2 = "U${user.userID}",
            right1 = "",
            right2 = "Mapping Followers",
            right3b = user.mappingFollowerCount.toString(),
            right3m = "x",
        )
    }

    fun guest(owner: GuestDifficultyService.GuestDifficultyOwner, assets: ImageAssets, bannerIndex: Int? = null): CardA1Data {
        val user = owner.user
        val rankedTotal = owner.sentRanked + owner.receivedRanked
        return CardA1Data(
            background = assets.localBanner(bannerIndex),
            avatar = avatarOf(user.userID, null, assets),
            country = null,
            top1 = user.username,
            top2 = "",
            left1 = "sent to: ${owner.sent}",
            left2 = "received from: ${owner.received}",
            right1 = "Ranked GDs:",
            right2 = "(sent [received])",
            right3b = if (rankedTotal > 0) owner.sentRanked.toString() else "",
            right3m = if (rankedTotal > 0) " [${owner.receivedRanked}]" else "-",
        )
    }

    fun avatarOf(userID: Long, avatarUrl: String?, assets: ImageAssets): String? {
        val direct = assets.resolveRef(avatarUrl)
        if (direct != null) return direct
        if (userID > 0) {
            assets.remote("https://a.ppy.sh/$userID")?.let { return it }
        }
        return assets.local("avatar-guest.png")
    }

    fun banner(user: MicroUser, assets: ImageAssets): String? {
        return assets.resolveRef(user.coverUrl) ?: assets.resolveRef(user.cover?.url) ?: assets.localBanner()
    }

    fun typeText(user: MicroUser, type: String?): String {
        return when (type) {
            "time" -> seenAt(user)
            "play_count" -> "PC: ${user.statistics?.playCount ?: 0}"
            "play_time" -> "PT: " + TimeFormat.duration((user.statistics?.playTime ?: 0L).toDouble(), false)
            "total_hits" -> "TTH: ${user.statistics?.totalHits ?: 0}"
            "online" -> if (user.isOnline) "online" else "offline"
            "uid" -> "U${user.userID}"
            else -> ""
        }
    }

    /**
     * 好友列表的 last_visit 在当前类型里是 ISO 字符串，源实现按 moment 数组读取，取不到值。
     * 这里按字符串补出同一行 Seen 文本。
     */
    private fun seenAt(user: MicroUser): String {
        val text = user.lastVisitString ?: return ""
        val time = runCatching { OffsetDateTime.parse(text) }.getOrNull()
            ?: runCatching { user.lastVisitTime }.getOrNull()?.atOffset(ZoneOffset.ofHours(8))
            ?: return ""
        return String.format(Locale.ROOT, "Seen: %02d-%d-%d", time.year % 100, time.monthValue, time.dayOfMonth)
    }
}
