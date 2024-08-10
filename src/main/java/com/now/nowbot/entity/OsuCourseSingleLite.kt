package com.now.nowbot.entity

import jakarta.persistence.*

// 完全确定表结构前, 别取消注释
//@Entity
//@Table(name = "osu_course_dan")
class OsuCourseSingleLite(
    @Id
    // 这个是自增id, 如果每一个都有确定的id就不用, 如果需要自动生成就保留
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null, //可以自己生成，只用于索引，无需和 bid 对应

    var bid: Long? = null, //可以为负数，负数则采取独特的命名逻辑

    var count: Int? = null, //大于等于 0

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
