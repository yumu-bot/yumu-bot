package com.now.nowbot.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.sql.Types

@Entity
@Table(
    name = "osu_beatmap_object_count",
    indexes = [Index(name = "bid", columnList = "bid"), Index(name = "check_s", columnList = "bid, check_str")]
)
class BeatmapObjectCountLite(
    @Id
    var bid:Long?=null,

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "check_str", columnDefinition = "CHAR(32)")
    var check: String?=null,

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "timestamp_arr", columnDefinition = "int4[]")
    var timestamp: IntArray?=null,

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "density", columnDefinition = "int4[]")
    var density: IntArray?=null,
) {
    interface TimeResult{
        val bid: Long
        val times: IntArray
    }
}