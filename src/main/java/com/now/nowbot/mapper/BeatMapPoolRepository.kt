package com.now.nowbot.mapper

import com.now.nowbot.entity.BeatMap4Pool
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface BeatMapPoolRepository : JpaRepository<BeatMap4Pool, Int>, JpaSpecificationExecutor<BeatMapRepository> {
    fun findAllById(id: Int): BeatMap4Pool
}
