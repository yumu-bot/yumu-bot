package com.now.nowbot.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.LocalDateTime

/**
 * - 这个数据表是用来给每个玩家的 info 设一个当前所在位置的百分比：
 * - 比如在前 20%
 * - 这个数据表每天更新，不需要大批量查询 osu_user_info_archive
 */
@Entity
@Table(
    name = "osu_user_info_percent",
    )
@IdClass(OsuUserInfoPercentilesKey::class)
class OsuUserInfoPercentilesLite(
    @Id
    @Column(name = "user_id")
    var userID: Long,

    @Id
    @Column(name = "mode")
    var mode: Byte,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "global_rank", nullable = true)
    var globalRank: Long? = null,

    @Column(name = "country_rank", nullable = true)
    var countryRank: Long? = null,

    /**
     * 这个数据的等级是在百分位开始，十分位和个分位是进度
     */
    @Column(name = "level")
    var level: Int = 0,

    /**
     * 这个分数：SS 视作 3 分，S 视作 2 分，A 视作 1 分
     */
    @Column(name = "rank_count_score")
    var rankCountScore: Int = 0,

    @Column(name = "play_count")
    var playCount: Long = 0L,

    @Column(name = "total_hits")
    var totalHits: Long = 0L,

    @Column(name = "play_time")
    var playTime: Long = 0L,

    @Column(name = "ranked_score")
    var rankedScore: Long = 0L,

    @Column(name = "total_score")
    var totalScore: Long = 0L,

    @Column(name = "beatmap_playcount")
    var beatmapPlaycount: Int = 0,

    @Column(name = "replays_watched")
    var replaysWatched: Int = 0,

    @Column(name = "maximum_combo")
    var maximumCombo: Int = 0,

    @Column(name = "achievements_count")
    var achievementsCount: Int = 0

    ) {

    @Suppress("UNUSED")
    constructor() : this(0, 0, LocalDateTime.now())
}

data class OsuUserInfoPercentilesKey(
    var userID: Long,
    var mode: Byte,
) : Serializable {
    @Suppress("UNUSED")
    constructor() : this(0, 0)
}
