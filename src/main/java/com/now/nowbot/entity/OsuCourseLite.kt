package com.now.nowbot.entity

import com.now.nowbot.model.JsonData.BeatMap
import io.hypersistence.utils.hibernate.type.array.IntArrayType
import jakarta.persistence.*
import org.hibernate.annotations.Type
// 完全确定表结构前, 别取消注释
// @Entity
// @Table(name = "osu_course", indexes = [Index(name = "name_index", columnList = "course_name_all")])
class OsuCourseLite(
    @Id
    // 这个是自增id, 如果每一个都有确定的id就不用, 如果需要自动生成就保留
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(name = "course_name_all", columnDefinition = "text")
    var name: String? = null,

    @Column(name = "course_name_base", columnDefinition = "text")
    var base: String? = null,

    @Type(IntArrayType::class)
    @Column(name = "course_children", columnDefinition = "integer[]")
    var children: IntArray? = null,

    @Enumerated(EnumType.STRING)
    var criteriaType: CriteriaType? = null,

    var criteriaTarget: Double? = null,

    @Enumerated(EnumType.STRING)
    var criteriaScoreType: CriteriaScoreType? = null,
) {
    enum class CriteriaType {
        ACC,
    }
    enum class CriteriaScoreType {
        V2
    }
}