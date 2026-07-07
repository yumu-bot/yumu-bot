package com.now.nowbot.entity

import com.now.nowbot.model.osu.OsuUser
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
    name = "user_info",
    indexes = [
        Index(name = "idx_user_info_updated_at", columnList = "user_id, mode, updated_at DESC"),
        // CREATE INDEX idx_user_info_updated_at_brin
        //ON user_info USING BRIN (updated_at)
        //WITH (pages_per_range = 32);
    ]
)
class UserInfoLite(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", columnDefinition = "int8", nullable = false)
    var userID: Long = 0L,

    @Column(name = "beatmap_playcount", columnDefinition = "int4", nullable = false)
    var beatmapPlaycount: Int = 0,

    @Column(name = "achievements_count", columnDefinition = "int4", nullable = false)
    var achievementsCount: Int = 0,

    @Column(name = "created_at", columnDefinition = "date", nullable = false, updatable = false)
    var createdAt: LocalDate = LocalDate.now(ZoneOffset.UTC),

    @Column(name = "updated_at", columnDefinition = "date", nullable = false)
    var updatedAt: LocalDate = LocalDate.now(ZoneOffset.UTC),

    @Column(name = "mode", columnDefinition = "int2", nullable = false)
    var mode: Byte = (-1).toByte(),
) {
    companion object {
        fun UserInfoLite.updateFrom(user: OsuUser): UserInfoLite {
            return this.apply {
                this.userID = user.userID
                this.beatmapPlaycount = user.beatmapPlaycount
                this.achievementsCount = user.userAchievementsCount
                this.mode = user.currentOsuMode.modeValue
            }
        }
    }
}