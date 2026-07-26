package com.now.nowbot.mapper

import com.now.nowbot.entity.OsuGroupConfigLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface OsuGroupConfigRepository : JpaRepository<OsuGroupConfigLite, Long> {
    @Query("SELECT g FROM OsuGroupConfigLite g WHERE g.groupID = :groupID")
    fun findByGroupID(groupID: Long): OsuGroupConfigLite?
}