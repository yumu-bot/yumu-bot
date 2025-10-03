package com.now.nowbot.entity

import jakarta.persistence.*
import java.time.LocalDateTime

//@Entity
//@Table(name = "service_call_stat")
data class ServiceCallStatisticLite(
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    var id: Long = 0,

    @Column(name = "name")
    var name: String = "",

    @Column(name = "user_id")
    var userID: Long = -1L,

    @Column(name = "group_id")
    var groupID: Long = -1L,

    @Column(name = "time")
    var createTime: LocalDateTime = LocalDateTime.now(),

    @Column(name = "duration")
    var duration: Long = -1L,

    @Column(name = "beatmap_id")
    var beatmapID: Long = -1L,

    @Column(name = "beatmapset_id")
    var beatmapsetID: Long = -1L,

    @Column(name = "mode")
    var modeInt: Byte = -1,

    @Column(name = "offset")
    var offset: Int = 0,

    @Column(name = "limit")
    var limit: Int = 1,

    )
