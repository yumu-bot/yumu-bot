package com.now.nowbot.mapper

import com.now.nowbot.entity.OsuGroupConfigLite
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface OsuGroupConfigRepository : JpaRepository<OsuGroupConfigLite, Long> {
    fun findByGroupId(id: Long): Optional<OsuGroupConfigLite>
}