package com.now.nowbot.entity

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.osu.Statistics
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.ZoneOffset

@Entity
@Table(
    name = "user_statistics", 
    indexes = [
        Index(name = "idx_user_statistics_updated_at", columnList = "user_id, mode, updated_at DESC"),
    ]
)
class UserStatisticsLite(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", columnDefinition = "int8", nullable = false)
    var userID: Long = 0L,

    @Column(name = "ranked_score", columnDefinition = "int8", nullable = false)
    var rankedScore: Long = 0L,

    @Column(name = "total_score", columnDefinition = "int8", nullable = false)
    var totalScore: Long = 0L,

    @Column(name = "total_hits", columnDefinition = "int8", nullable = false)
    var totalHits: Long = 0L,

    @Column(name = "play_time", columnDefinition = "int8", nullable = false)
    var playTime: Long = 0L,

    @Column(name = "play_count", columnDefinition = "int8", nullable = false)
    var playCount: Long = 0L,

    @Column(name = "created_at", columnDefinition = "date", nullable = false, updatable = false)
    var createdAt: LocalDate = LocalDate.now(ZoneOffset.UTC),

    @Column(name = "updated_at", columnDefinition = "date", nullable = false)
    var updatedAt: LocalDate = LocalDate.now(ZoneOffset.UTC),

    @Column(name = "pp", columnDefinition = "real", nullable = false)
    var pp: Float = 0f,

    @Column(name = "accuracy", columnDefinition = "real", nullable = false)
    var accuracy: Float = 0f,

    @Column(name = "grade_ssh", columnDefinition = "int4", nullable = false)
    var countSSH: Int = 0,

    @Column(name = "grade_ss", columnDefinition = "int4", nullable = false)
    var countSS: Int = 0,

    @Column(name = "grade_sh", columnDefinition = "int4", nullable = false)
    var countSH: Int = 0,

    @Column(name = "grade_s", columnDefinition = "int4", nullable = false)
    var countS: Int = 0,

    @Column(name = "grade_a", columnDefinition = "int4", nullable = false)
    var countA: Int = 0,

    @Column(name = "replays_watched", columnDefinition = "int4", nullable = false)
    var replaysWatched: Int = 0,

    @Column(name = "max_combo", columnDefinition = "int4", nullable = false)
    var maxCombo: Int = 0,

    @Column(name = "level_current", columnDefinition = "int2", nullable = false)
    var levelCurrent: Short = 0,

    @Column(name = "level_progress", columnDefinition = "int2", nullable = false)
    var levelProgress: Short = 0,

    @Column(name = "mode", columnDefinition = "int2", nullable = false)
    var mode: Byte = (-1).toByte(),
) {
    companion object {
        fun UserStatisticsLite.updateFrom(userID: Long, mode: OsuMode, statistics: Statistics): UserStatisticsLite {
            return this.apply {
                this.userID = userID
                this.rankedScore = statistics.rankedScore ?: 0L
                this.totalScore = statistics.totalScore ?: 0L
                this.totalHits = statistics.totalHits ?: 0L
                this.playTime = statistics.playTime ?: 0L
                this.playCount = statistics.playCount ?: 0L
                this.pp = statistics.pp?.toFloat() ?: 0f
                this.accuracy = statistics.accuracy?.toFloat() ?: 0f
                this.countSSH = statistics.countSSH
                this.countSS = statistics.countSS
                this.countSH = statistics.countSH
                this.countS = statistics.countS
                this.countA = statistics.countA
                this.replaysWatched = statistics.replaysWatchedByOthers
                this.maxCombo = statistics.maxCombo
                this.levelCurrent = statistics.levelCurrent
                this.levelProgress = statistics.levelProgress
                this.mode = mode.modeValue
            }
        }

        fun UserStatisticsLite.updateFrom(user: OsuUser): UserStatisticsLite? {
            val statistics = user.statistics ?: return null

            if ((statistics.totalHits ?: 0L) == 0L) return null

            if (user.mode.isDefault) return null

            return this.updateFrom(user.userID, user.mode, statistics)
        }
    }
}

interface UserStatisticsProjection {
    val id: Long
    val userID: Long
    val mode: Byte
    val updatedAt: LocalDate
}


interface UserPlayCountProjection {
    val userID: Long
    val playCount: Long
    val mode: Byte
}