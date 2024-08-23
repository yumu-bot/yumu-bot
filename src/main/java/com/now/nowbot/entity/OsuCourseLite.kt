package com.now.nowbot.entity

import com.now.nowbot.model.Course
import com.now.nowbot.model.Course.CourseSingle
import io.hypersistence.utils.hibernate.type.array.IntArrayType
import jakarta.persistence.*
import org.hibernate.annotations.Type

@Entity
@Table(name = "osu_course", indexes = [Index(name = "name_index", columnList = "course_name_all")])
class OsuCourseLite(
    @Id
    // 这个是自增id, 如果每一个都有确定的id就不用, 如果需要自动生成就保留
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(name = "course_name_all", columnDefinition = "text")
    var name: String? = null,

    @Column(name = "course_name_base", columnDefinition = "text")
    var base: String? = null,

    /**
     * 存储 osu_course_dan 的 id
     */
    @Type(IntArrayType::class)
    @Column(name = "course_single", columnDefinition = "integer[]")
    var single: IntArray? = null,

    @Enumerated(EnumType.STRING)
    var criteriaType: CriteriaType? = null,

    @Enumerated(EnumType.STRING)
    var criteriaTrend: CriteriaTrend? = null,

    var criteriaTarget: Double? = null,

    @Enumerated(EnumType.STRING)
    var criteriaScoreType: CriteriaScoreType? = null,
) {
    enum class CriteriaTrend {
        MORE, MORE_EQUAL, LESS_EQUAL, LESS, EQUAL, NOT_EQUAL
    }

    enum class CriteriaType {
        ACC, COMBO, SCORE
    }

    enum class CriteriaScoreType {
        SCORE, SCORE_V2
    }

    fun toCourse(single: List<CourseSingle>) = Course(
        id = id,
        name = name,
        base = base,
        dan = single,
        danBid = single.map { it.bid },
        danCount = single.map { it.count },
        beatmaps = emptyMap(),
        criteria = Course.Criteria(
            type = criteriaType ?: CriteriaType.ACC,
            target = criteriaTarget ?: 0.0,
            score = criteriaScoreType ?: CriteriaScoreType.SCORE
        ),
    )
}