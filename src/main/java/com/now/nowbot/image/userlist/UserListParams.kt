package com.now.nowbot.image.userlist

import com.now.nowbot.entity.UserProfileLite
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.osu.OsuUserPlus
import com.now.nowbot.service.messageServiceImpl.FriendService
import com.now.nowbot.service.messageServiceImpl.GuestDifficultyService

data class PanelA1Param(
    val me: OsuUser? = null,
    val profile: UserProfileLite? = null,
    val friends: List<MicroUser> = emptyList(),
    val sortType: String = "",
    val bannerIndex: Int? = null,
)

data class PanelA10Param(
    val user: OsuUser? = null,
    val profile: UserProfileLite? = null,
    val badges: List<OsuUser.UserBadge> = emptyList(),
    val banner: String? = null,
    val bannerIndex: Int? = null,
)

data class PanelA11Param(
    val user: OsuUser? = null,
    val profile: UserProfileLite? = null,
    val guests: List<GuestDifficultyService.GuestDifficultyOwner> = emptyList(),
    val page: Int = 1,
    val maxPage: Int = 1,
    val banner: String? = null,
    val bannerIndex: Int? = null,
)

data class PanelUParam(
    val user: OsuUser? = null,
    val partner: OsuUser? = null,
    val statistics: FriendService.FriendPairStatistics? = null,
    val userProfile: UserProfileLite? = null,
    val partnerProfile: UserProfileLite? = null,
    val bannerIndex: Int? = null,
)

object UserListMapParams {
    fun a1(body: Map<String, Any?>): PanelA1Param {
        val me = body["me"]
        return PanelA1Param(
            me = osuUser(me),
            profile = osuProfile(me),
            friends = (body["friends"] as? Collection<*>).orEmpty().filterIsInstance<MicroUser>(),
            sortType = body["type"]?.toString().orEmpty(),
            bannerIndex = int(body["banner_index"]),
        )
    }

    fun a10(body: Map<String, Any?>): PanelA10Param {
        val user = body["user"]
        return PanelA10Param(
            user = osuUser(user),
            profile = osuProfile(user),
            badges = (body["badges"] as? Collection<*>).orEmpty()
                .filterIsInstance<OsuUser.UserBadge>(),
            banner = string(body["banner"]),
            bannerIndex = int(body["banner_index"]),
        )
    }

    fun a11(body: Map<String, Any?>): PanelA11Param {
        val user = body["user"]
        return PanelA11Param(
            user = osuUser(user),
            profile = osuProfile(user),
            guests = (body["guest_differs"] as? Collection<*>).orEmpty()
                .filterIsInstance<GuestDifficultyService.GuestDifficultyOwner>(),
            page = int(body["page"]) ?: 1,
            maxPage = int(body["max_page"]) ?: 1,
            banner = string(body["banner"]),
            bannerIndex = int(body["banner_index"]),
        )
    }

    fun u(body: Map<String, Any?>): PanelUParam {
        val user = body["user"]
        val partner = body["partner"]
        return PanelUParam(
            user = osuUser(user),
            partner = osuUser(partner),
            statistics = statistics(body["statistics"]),
            userProfile = osuProfile(user),
            partnerProfile = osuProfile(partner),
            bannerIndex = int(body["banner_index"]),
        )
    }

    private fun osuUser(value: Any?): OsuUser? {
        return when (value) {
            is OsuUser -> value
            is OsuUserPlus -> value.user
            else -> null
        }
    }

    private fun osuProfile(value: Any?): UserProfileLite? {
        return (value as? OsuUserPlus)?.profile
    }

    private fun statistics(value: Any?): FriendService.FriendPairStatistics? {
        return when (value) {
            is FriendService.FriendPairStatistics -> value
            is Map<*, *> -> FriendService.FriendPairStatistics(
                userBind = value["user_bind"] as? Boolean ?: false,
                partnerBind = value["partner_bind"] as? Boolean ?: false,
                isFollowing = value["is_following"] as? Boolean ?: false,
                isFollowed = value["is_followed"] as? Boolean,
                userFollowing = (value["user_following"] as? Number)?.toInt() ?: 0,
                partnerFollowing = (value["partner_following"] as? Number)?.toInt() ?: 0,
            )

            else -> null
        }
    }

    private fun int(value: Any?): Int? = (value as? Number)?.toInt()

    private fun string(value: Any?): String? = value?.toString()?.takeIf { it.isNotBlank() }
}
