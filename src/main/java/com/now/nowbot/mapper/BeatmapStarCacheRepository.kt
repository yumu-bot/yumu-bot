package com.now.nowbot.mapper

import com.now.nowbot.entity.BeatmapStartCache
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface BeatmapStarCacheRepository : JpaRepository<BeatmapStartCache, BeatmapStartCache.BeatmapStartKey> {
    @Query("select s.star from BeatmapStartCache s where s.id = :id and s.mods = :mods")
    fun findByKey(id: Long, mods: Int): Optional<Double>
}