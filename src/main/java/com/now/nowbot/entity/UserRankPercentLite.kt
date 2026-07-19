package com.now.nowbot.entity

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.osu.Statistics
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.LocalDate

@Entity
@Table(name = "user_rank_percent")
@IdClass(UserRankPercentKey::class)
class UserRankPercentLite(
    @Id
    @Column(name = "user_id", columnDefinition = "int8", nullable = false)
    var userID: Long = 0L,

    @Column(name = "rank_percent", columnDefinition = "double precision", nullable = false)
    var globalRankPercent: Double = 1.0,

    @Column(name = "country_rank", columnDefinition = "int8", nullable = false)
    var countryRank: Long = 0L,

    @Id
    @Column(name = "mode", columnDefinition = "int2", nullable = false)
    var mode: Byte = (-1).toByte(),

    @Id
    @Column(name = "date", columnDefinition = "date", nullable = false)
    var date: LocalDate = LocalDate.now(),
) {
    companion object {
        fun UserRankPercentLite.updateFrom(user: OsuUser): UserRankPercentLite? {
            val percent = user.statistics?.globalRankPercent ?: return null

            return this.apply {
                this.userID = user.userID
                this.mode = user.currentOsuMode.modeValue
                this.countryRank = user.countryRank
                this.globalRankPercent = percent
            }
        }

        fun UserRankPercentLite.updateFrom(userID: Long, mode: OsuMode, countryRank: Long, statistics: Statistics): UserRankPercentLite {
            return this.apply {
                this.userID = userID
                this.mode = mode.modeValue
                this.countryRank = countryRank
                this.globalRankPercent = statistics.globalRankPercent ?: 1.0
            }
        }
    }
}

data class UserRankPercentKey(
    var userID: Long,
    var mode: Byte,
    var date: LocalDate,
) : Serializable {
    @Suppress("UNUSED")
    constructor() : this(0, 0, LocalDate.now())
}

interface UserRankModeProjection {
    val userID: Long
    val mode: Byte
    val countryRank: Long
}