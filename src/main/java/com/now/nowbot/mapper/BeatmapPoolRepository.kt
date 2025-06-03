package com.now.nowbot.mapper

import com.now.nowbot.entity.Beatmap4Pool
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface BeatMapPoolRepository : JpaRepository<Beatmap4Pool, Int>, JpaSpecificationExecutor<BeatmapRepository> {
    fun findAllById(id: Int): Beatmap4Pool
}
