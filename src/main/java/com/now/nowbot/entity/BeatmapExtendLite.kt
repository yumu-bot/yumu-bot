package com.now.nowbot.entity

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import java.time.LocalDateTime

/**
 * 鉴于现在的 Beatmap 各种类缺失太多太多数据，先开设一个新类，用来存可以把 Beatmap 变成 BeatmapExtended 的数据
 */

@Entity
@Table(name = "osu_extend_beatmap", indexes = [
    Index(name = "index_beatmapset_id", columnList = "beatmapset_id"),
    Index(name = "index_updated_at", columnList = "updated_at")
])
class BeatmapExtendLite(
    @Id
    @Column(name = "beatmap_id")
    val beatmapID: Long = -1,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beatmapset_id")
    val beatmapset: BeatmapsetExtendLite,

    @Type(JsonBinaryType::class)
    @Column(name = "fail_times", columnDefinition = "JSONB", nullable = true, updatable = true)
    var failTimes: String? = null,

    @Column(name = "max_combo")
    val maxCombo: Int = 0,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
    )
