package com.now.nowbot.entity

import jakarta.persistence.*

// 完全确定表结构前, 别取消注释
//@Entity
//@Table(name = "osu_course_dan")
class OsuCouresDanLite(
    @Id
    // 这个是自增id, 如果每一个都有确定的id就不用, 如果需要自动生成就保留
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    var bid: Long? = null,

    var count: Int? = null,

    @Column(columnDefinition = "text")
    var artist: String? = null,

    @Column(columnDefinition = "text")
    var artistUnicode: String? = null,

    @Column(columnDefinition = "text")
    var title: String? = null,

    @Column(columnDefinition = "text")
    var titleUnicode: String? = null,

    @Column(columnDefinition = "text")
    var creator: String? = null,

    @Column(columnDefinition = "text")
    var version: String? = null,
)
