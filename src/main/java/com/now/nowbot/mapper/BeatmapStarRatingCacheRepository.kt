package com.now.nowbot.mapper

import com.now.nowbot.entity.BeatmapStarRatingCache
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface BeatmapStarRatingCacheRepository : JpaRepository<BeatmapStarRatingCache, BeatmapStarRatingCache.BeatmapStarRatingKey> {
    @Query("select s.star from BeatmapStarRatingCache s where s.id = :id and s.mode = :mode and s.mods = :mods")
    fun findByKey(id: Long, mode: Byte, mods: Int): Float?

    @Transactional
    @Modifying
    @Query("""
        INSERT INTO osu_beatmap_star (id, mode, mods, star) 
        VALUES(:id, :mode, :mods, :star) 
        ON CONFLICT (id, mode, mods)
        DO UPDATE SET star = :star;
        """, nativeQuery = true)
    fun saveAndUpdate(id: Long, mode: Byte, mods: Int, star: Float)

    @Transactional
    @Modifying
    @Query("DELETE FROM BeatmapStarRatingCache s WHERE s.mode = :mode")
    fun deleteByMode(mode: Byte)
}