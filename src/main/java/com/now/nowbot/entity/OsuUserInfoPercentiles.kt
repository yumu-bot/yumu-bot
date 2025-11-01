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
class OsuUserInfoPercentiles(
    @Id
    @Column(name = "user_id")
    val userID: Long,

    @Id
    @Column(name = "mode")
    val mode: Byte,

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "global_rank", nullable = true)
    val globalRank: Long? = null,

    @Column(name = "country_rank", nullable = true)
    val countryRank: Long? = null,

    /**
     * 这个数据的等级是在百分位开始，十分位和个分位是进度
     */
    @Column(name = "level")
    val level: Int = 0,

    /**
     * 这个分数：SS 视作 3 分，S 视作 2 分，A 视作 1 分
     */
    @Column(name = "rank_count_score")
    val rankCountScore: Int = 0,

    @Column(name = "play_count")
    val playCount: Long = 0L,

    @Column(name = "total_hits")
    val totalHits: Long = 0L,

    @Column(name = "play_time")
    val playTime: Long = 0L,

    @Column(name = "ranked_score")
    val rankedScore: Long = 0L,

    @Column(name = "total_score")
    val totalScore: Long = 0L,

    @Column(name = "achievement_count")
    val achievementCount: Int = 0,

    @Column(name = "beatmap_playcount")
    val beatmapPlaycount: Long = 0L,

    @Column(name = "replays_watched")
    val replaysWatched: Int = 0,

    @Column(name = "maximum_combo")
    val maximumCombo: Int = 0,

    )


data class OsuUserInfoPercentilesKey(
    var userID: Long,
    var mode: Byte,
) : Serializable {
    @Suppress("UNUSED")
    constructor() : this(0, 0)
}
