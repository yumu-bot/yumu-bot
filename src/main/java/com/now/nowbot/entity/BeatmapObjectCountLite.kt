package com.now.nowbot.entity

import io.hypersistence.utils.hibernate.type.array.IntArrayType
import jakarta.persistence.*
import org.hibernate.annotations.Type

@Entity
@Table(
    name = "osu_beatmap_object_count",
    indexes = [Index(name = "bid", columnList = "bid"), Index(name = "check_s", columnList = "bid, check_str")]
)
class BeatmapObjectCountLite(
    @Id
    var bid:Long?=null,
    @Column(name = "check_str", columnDefinition = "CHAR(32)")
    var check: String?=null,

    @Type(IntArrayType::class)
    @Column(name = "timestamp_arr", columnDefinition = "integer[]")
    var timestamp: IntArray?=null,

    @Type(IntArrayType::class)
    @Column(name = "density", columnDefinition = "integer[]")
    var density: IntArray?=null,
)