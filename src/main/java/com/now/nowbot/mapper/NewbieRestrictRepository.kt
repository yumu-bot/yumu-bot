package com.now.nowbot.mapper

import com.now.nowbot.entity.NewbieRestrictLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface NewbieRestrictRepository : JpaRepository<NewbieRestrictLite, Long> {

    @Query("""
        select * from newbie_restrict where qq = :qq
        """, nativeQuery = true)
    fun findByQQ(qq: Long): List<NewbieRestrictLite>?
}