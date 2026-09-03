package com.now.nowbot.restrict

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

@Entity
@Table(
    name = "restrict_record",
    indexes = [
        Index(
            name = "idx_restrict_active",
            columnList = "target_type, target_id, service",
            options = "WHERE enabled = true"
        )
    ]
)
class RestrictRecordEntity(
    // 8B
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "target_id", nullable = false)
    var targetID: Long,

    @Column(name = "operator_id", nullable = false)
    var operatorID: Long,

    @Column(name = "duration")
    var duration: Long? = null, // NULL 代表无期限（永久）

    // 4B
    @Column(name = "start_time", nullable = false)
    var startTime: Instant = Clock.System.now(),

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null,

    // 1B
    @Column(name = "target_type", nullable = false)
    var targetType: Byte,

    @Column(name = "source_type", nullable = false)
    var sourceType: Byte,

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true,

    // VAR

    @Column(name = "service", nullable = false, length = 64)
    var service: String = "",

    @Column(name = "reason")
    var reason: String? = null

    ) {
    /**
     * 判断当前时间该记录是否依然处于有效封禁状态
     */
    fun isCurrentlyActive(now: Instant = Clock.System.now()): Boolean {
        if (!enabled) return false
        if (now < startTime) return false // 未到生效时间

        // 如果 durationMs 为 null，代表永久有效；否则检查是否超过过期时间
        return duration?.let { ms ->
            now < (startTime + ms.milliseconds)
        } ?: true
    }
}
