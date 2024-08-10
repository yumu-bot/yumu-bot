package com.now.nowbot.model.JsonData

import com.now.nowbot.entity.OsuCourseSingleLite
import com.now.nowbot.entity.OsuCourseLite

data class Course(
    val id: Int,
    val name: String,
    val base: String,
    val danBid: List<Long>,
    val danCount: List<Int>,
    val dan: List<OsuCourseSingleLite>,
    val beatmap: List<BeatMap>,
    val criteria: Criteria,
) {
    companion object {
        fun create(
            course: OsuCourseLite,
            dan: List<OsuCourseSingleLite>,
            beatmaps: List<BeatMap>,
        ): Course {
            val criteria = Criteria (
                course.criteriaType!!.name,
                course.criteriaTarget!!,
                course.criteriaScoreType!!.name
            )
            return Course(
                course.id!!,
                course.name!!,
                course.base!!,
                dan.map { it.bid!! },
                dan.map { it.count!! },
                dan,
                beatmaps,
                criteria,
            )
        }
    }

    data class Criteria(
        val type: String,
        val target: Double,
        val scoreType: String
    )
}