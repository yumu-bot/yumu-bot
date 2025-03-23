package com.now.nowbot.entity

import jakarta.persistence.*

@Entity
@Table(name = "newbie_restrict")
data class NewbieRestrictLite(
    // 自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(name = "qq")
    var qq: Long? = null,

    @Column(name = "star_rating")
    var star: Double? = null,

    @Column(name = "restricted_time")
    var time: Long? = null,

    @Column(name = "restricted_duration")
    var duration: Long? = null,
)