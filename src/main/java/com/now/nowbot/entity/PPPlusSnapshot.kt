package com.now.nowbot.entity

import com.now.nowbot.model.osu.LazerScore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.util.DigestUtils
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_best_snapshot",
    indexes = [
        Index(
            name = "idx_user_mode_created",
            columnList = "user_id, mode, created_at DESC"
        )
    ]
)
class UserBestSnapshot(
    // 这个类是 pp+ 用的，只记录了很少的数据。
    // 如果想构建完整数据，可以使用其他表

    @Column(name = "user_id")
    var userID: Long,

    @Column(name = "mode")
    var mode: Byte,

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "beatmap_ids", columnDefinition = "int8[]")
    var beatmapIDs: LongArray,

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "score_ids", columnDefinition = "int8[]")
    var scoreIDs: LongArray,

    @Column(name = "content_hash")
    var contentHash: String,

    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    companion object {
        fun fromBests(bests: Collection<LazerScore>): UserBestSnapshot? {
            // 没数据你创个棒槌
            val f = bests.firstOrNull() ?: return null

            val bids = bests.map { it.beatmapID }.toLongArray()
            val sids = bests.map { it.scoreID }.toLongArray()

            val currentDataString = bests.joinToString(",") { "${it.beatmapID}:${it.scoreID}" }

            val hash = DigestUtils.md5DigestAsHex(currentDataString.toByteArray())

            return UserBestSnapshot(
                userID = f.userID,
                mode = f.mode.modeValue,
                beatmapIDs = bids,
                scoreIDs = sids,
                contentHash = hash,
                createdAt = LocalDateTime.now()
            )
        }
    }
}