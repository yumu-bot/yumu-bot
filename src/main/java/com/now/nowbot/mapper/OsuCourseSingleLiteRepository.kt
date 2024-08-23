package com.now.nowbot.mapper

import com.now.nowbot.entity.OsuCourseSingleLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface OsuCourseSingleLiteRepository : JpaRepository<OsuCourseSingleLite, Int> {
    @Query("select u from OsuCourseSingleLite u where u.id in :ids")
    fun findAllByCourse(ids: IntArray) : List<OsuCourseSingleLite>
}