package com.now.nowbot.mapper

import com.now.nowbot.entity.BeatmapStarRatingCache
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.*

interface BeatmapStarRatingCacheRepository : JpaRepository<BeatmapStarRatingCache, BeatmapStarRatingCache.BeatmapStarRatingKey> {
    @Query("select s.star from BeatmapStarRatingCache s where s.id = :id and s.mods = :mods")
    fun findByKey(id: Long, mods: Int): Optional<Double>

    @Transactional
    @Modifying
    @Query("""
        INSERT INTO osu_beatmap_star_rating_cache (id, mode, mods, star) 
        VALUES(:id, :mode, :mods, :star) 
        ON CONFLICT (id, mode, mods)
        DO UPDATE SET star = :star;
        """, nativeQuery = true)
    fun saveAndUpdate(id: Long, mode: Short, mods: Int, star: Double)
}