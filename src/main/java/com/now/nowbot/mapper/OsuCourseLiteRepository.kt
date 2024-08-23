package com.now.nowbot.mapper

import com.now.nowbot.entity.OsuCourseLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface OsuCourseLiteRepository : JpaRepository<OsuCourseLite, Int> {
    @Query(""" select o from OsuCourseLite o where o.name like :name """)
    fun getOsuCourseLitesByNameLike(name: String): List<OsuCourseLite>
}