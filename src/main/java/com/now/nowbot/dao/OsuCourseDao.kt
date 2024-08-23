package com.now.nowbot.dao

import com.now.nowbot.entity.OsuCourseLite
import com.now.nowbot.mapper.OsuCourseLiteRepository
import com.now.nowbot.mapper.OsuCourseSingleLiteRepository
import com.now.nowbot.model.Course
import org.springframework.stereotype.Component

@Component
class OsuCourseDao(
    val osuCourseLiteRepository: OsuCourseLiteRepository,
    val osuCourseSingleLiteRepository: OsuCourseSingleLiteRepository,
) {
    fun saveCourse(course: Course) {
        val items = course.dan.map { it.toLite() }
        val data = osuCourseSingleLiteRepository.saveAll(items)
        val allID = data.map { it.id }.filterNotNull()
        val courseLite = course.toLite(allID)
        osuCourseLiteRepository.save(courseLite)
    }

    fun queryByID(id: Int): Course {
        val courseLite = osuCourseLiteRepository.findById(id)
        if (courseLite.isEmpty) {
            throw IllegalArgumentException("Course not found")
        }
        return liteToCourse(courseLite.get())
    }

    fun queryByName(name: String): Course {
        val courseLites = osuCourseLiteRepository.getOsuCourseLitesByNameLike("%$name%")
        if (courseLites.size != 1) {
            throw IllegalArgumentException("Course not found or not unique")
        }
        return liteToCourse(courseLites.first())
    }

    fun liteToCourse(lite: OsuCourseLite): Course {
        val itemsID = lite.single ?: IntArray(0)
        val items = osuCourseSingleLiteRepository.findAllByCourse(itemsID)
        return lite.toCourse(items.map { it.toCourseSingle() })
    }
}