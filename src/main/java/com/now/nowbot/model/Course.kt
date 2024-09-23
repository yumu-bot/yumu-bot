package com.now.nowbot.model

import com.now.nowbot.entity.OsuCourseLite
import com.now.nowbot.entity.OsuCourseSingleLite
import com.now.nowbot.model.json.BeatMap

data class Course(
    // id 在数据库中生成, 初始为空
    var id: Int? = null,
    var name: String?,
    var base: String?,
    var danBid: List<Long>,
    var danCount: List<Int>,
    var dan: List<CourseSingle>,
    var beatmaps: Map<Long, BeatMap>?,
    var criteria: Criteria,
) {
    data class CourseSingle(
        // id 在数据库中生成, 初始为空
        var id: Int? = null,

        var artist: String? = null,

        var artistUnicode: String? = null,

        var title: String? = null,

        var titleUnicode: String? = null,

        var creator: String? = null,

        var version: String? = null,

        var bid: Long,

        var count: Int,
    ) {
        fun toLite(): OsuCourseSingleLite {
            val result = OsuCourseSingleLite()
            result.id = id
            result.artist = artist
            result.artistUnicode = artistUnicode
            result.title = title
            result.titleUnicode = titleUnicode
            result.creator = creator
            result.bid = bid
            result.count = count
            return result
        }
    }

    data class Criteria(
        var type: OsuCourseLite.CriteriaType,
        var target: Double,
        var score: OsuCourseLite.CriteriaScoreType,
    )

    fun toLite(singles: List<Int>): OsuCourseLite {
        val result = OsuCourseLite()
        result.id = id
        result.name = name
        result.base = base
        result.single = singles.toIntArray()
        result.criteriaType = criteria.type
        result.criteriaTarget = criteria.target
        result.criteriaScoreType = criteria.score
        return result
    }
}
