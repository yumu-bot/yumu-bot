package com.now.nowbot.image.scorelist

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.draw.randomBanner
import com.now.nowbot.image.util.Rankings
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.model.osu.OsuUser

internal object ScoreListImages {
    private const val DEFAULT_AVATAR = "avatar-guest.png"

    fun avatar(user: MicroUser?, assets: ImageAssets): String? {
        val url = user?.avatarUrl?.takeIf { it.isNotBlank() }
            ?: user?.userID?.takeIf { it > 0 }?.let { "https://a.ppy.sh/$it" }
        return url?.let(assets::remote) ?: assets.local(DEFAULT_AVATAR)
    }

    fun avatar(user: OsuUser?, assets: ImageAssets): String? {
        if (user == null) return assets.local(DEFAULT_AVATAR)
        val url = user.avatarUrl.takeIf { it.isNotBlank() }
            ?: user.userID.takeIf { it > 0 }?.let { "https://a.ppy.sh/$it" }
        return url?.let(assets::remote) ?: assets.local(DEFAULT_AVATAR)
    }

    fun mapBackground(beatmap: Beatmap, type: String, assets: ImageAssets): String? {
        return assets.mapBackground(beatmap, type)
    }

    fun mapBackground(beatmapset: Beatmapset?, type: String, assets: ImageAssets): String? {
        return assets.mapBackground(beatmapset, type)
    }

    fun scoreBackground(score: LazerScore, type: String, assets: ImageAssets): String? {
        return assets.mapBackground(score, type)
    }

    fun banner(custom: String?, assets: ImageAssets, bannerIndex: Int? = null): String? {
        if (!custom.isNullOrBlank()) return assets.resolveRef(custom)
        return randomBanner(assets, bannerIndex)
    }

    fun randomBanner(assets: ImageAssets, bannerIndex: Int? = null): String? {
        return assets.randomBanner("default", bannerIndex)
    }

    fun scoreType(score: LazerScore, assets: ImageAssets): String? {
        if (score.type == "sb_score") return assets.local("object-type-ppysb.png")
        return assets.local(if (score.isLazer) "object-type-lazer.png" else "object-type-stable.png")
    }

    fun rankImage(rank: String?, assets: ImageAssets): String? {
        return assets.local("object-score-${Rankings.rankSS2X(rank ?: "F")}-small2.png")
    }
}
