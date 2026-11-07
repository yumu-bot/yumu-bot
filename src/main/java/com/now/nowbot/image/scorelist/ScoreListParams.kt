package com.now.nowbot.image.scorelist

import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.LazerScoreWithFcPP
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.osu.ScoreWithUserProfile
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

data class PanelA4Param(
    val user: OsuUser? = null,
    val history: OsuUser? = null,
    val scores: List<LazerScore> = emptyList(),
    val ranks: List<Int> = emptyList(),
    val panel: String = "",
    val compact: Boolean = false,
    val filtered: Int = 0,
    val createdAt: OffsetDateTime? = null,
    val bannerIndex: Int? = null,
)

data class PanelA5Param(
    val user: OsuUser? = null,
    val history: OsuUser? = null,
    val scores: List<LazerScore> = emptyList(),
    val ranks: List<Int> = emptyList(),
    val panel: String = "",
    val compact: Boolean = false,
    val matchId: Long? = null,
    val bannerIndex: Int? = null,
)

data class PanelA7Param(
    val user: OsuUser? = null,
    val scores: List<LazerScoreWithFcPP> = emptyList(),
    val pp: Double = 0.0,
    val page: Int = 1,
    val maxPage: Int = 1,
    val profileCard: String? = null,
    val banner: String? = null,
    val bannerIndex: Int? = null,
)

data class PanelA12Param(
    val user: OsuUser? = null,
    val mostPlayed: List<Beatmap> = emptyList(),
    val page: Int = 1,
    val maxPage: Int = 1,
    val bannerIndex: Int? = null,
)

data class PanelA15Param(
    val scores: List<LazerScore> = emptyList(),
    val firstScoreRank: Int = 1,
    val page: Int = 1,
    val maxPage: Int = 1,
    val banner: String? = null,
    val bannerIndex: Int? = null,
)

object ScoreListMapParams {
    fun a4(body: Map<String, Any>): PanelA4Param {
        return PanelA4Param(
            user = body["user"] as? OsuUser,
            history = body["history_user"] as? OsuUser,
            scores = body.scores(),
            ranks = body.ranks(),
            panel = body.string("panel"),
            compact = body.boolean("compact"),
            filtered = body.int("filtered") ?: 0,
            createdAt = body.time("created_at"),
        )
    }

    fun a5(body: Map<String, Any>): PanelA5Param {
        return PanelA5Param(
            user = body["user"] as? OsuUser,
            history = body["history_user"] as? OsuUser,
            scores = body.scores(),
            ranks = body.ranks(),
            panel = body.string("panel"),
            compact = body.boolean("compact"),
            matchId = body.long("match_id"),
        )
    }

    fun a7(body: Map<String, Any>): PanelA7Param {
        val scores = (body["scores"] as? Collection<*>).orEmpty().mapNotNull { it.toFcScore() }
        return PanelA7Param(
            user = body["user"] as? OsuUser,
            scores = scores,
            pp = (body["pp"] as? Number)?.toDouble() ?: 0.0,
            page = body.int("page") ?: 1,
            maxPage = body.int("max_page") ?: 1,
        )
    }

    fun a12(body: Map<String, Any>): PanelA12Param {
        return PanelA12Param(
            user = body["user"] as? OsuUser,
            mostPlayed = (body["most_played"] as? Collection<*>).orEmpty().filterIsInstance<Beatmap>(),
            page = body.int("page") ?: 1,
            maxPage = body.int("max_page") ?: 1,
        )
    }

    fun a15(body: Map<String, Any>): PanelA15Param {
        return PanelA15Param(
            scores = body.scores(),
            firstScoreRank = body.int("first_score_rank") ?: 1,
            page = body.int("page") ?: 1,
            maxPage = body.int("max_page") ?: 1,
        )
    }
}

internal fun Any?.toLazerScore(): LazerScore? = when (this) {
    is LazerScore -> this
    is ScoreWithUserProfile -> score
    is LazerScoreWithFcPP -> score
    else -> null
}

private fun Any?.toFcScore(): LazerScoreWithFcPP? = when (this) {
    is LazerScoreWithFcPP -> this
    is ScoreWithUserProfile -> LazerScoreWithFcPP(score, index = 0, indexAfter = 0)
    is LazerScore -> LazerScoreWithFcPP(this, index = 0, indexAfter = 0)
    else -> null
}

private fun Map<String, Any>.scores(): List<LazerScore> {
    return (this["scores"] as? Collection<*>).orEmpty().mapNotNull { it.toLazerScore() }
}

private fun Map<String, Any>.ranks(): List<Int> {
    return (this["rank"] as? Collection<*>).orEmpty().mapNotNull { (it as? Number)?.toInt() }
}

private fun Map<String, Any>.string(key: String): String = this[key]?.toString().orEmpty()

private fun Map<String, Any>.boolean(key: String): Boolean = this[key] as? Boolean ?: false

private fun Map<String, Any>.int(key: String): Int? = (this[key] as? Number)?.toInt()

private fun Map<String, Any>.long(key: String): Long? = (this[key] as? Number)?.toLong()

private fun Map<String, Any>.time(key: String): OffsetDateTime? {
    return when (val value = this[key]) {
        is OffsetDateTime -> value
        is LocalDateTime -> value.atOffset(ZoneOffset.UTC)
        is String -> runCatching { OffsetDateTime.parse(value) }.getOrNull()
        else -> null
    }
}
