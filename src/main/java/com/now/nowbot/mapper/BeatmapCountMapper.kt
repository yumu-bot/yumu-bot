package com.now.nowbot.mapper

import com.now.nowbot.entity.BeatmapCountLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BeatmapCountMapper : JpaRepository<BeatmapCountLite, Long> {

    @Query("""
        select density, hash from beatmap_count 
        where id = :beatmapID
        limit 1
        """, nativeQuery = true)
    fun getDensityResultByBeatmapID(
        @Param("beatmapID") beatmapID: Long
    ): BeatmapCountLite.DensityResult?

    @Query("""
        select density from beatmap_count 
        where id = :beatmapID 
        limit 1
        """, nativeQuery = true)
    fun getDensityByBeatmapID(@Param("beatmapID") beatmapID: Long): IntArray?

    @Query("""
        SELECT b.beatmapID as beatmapID, b.delta as delta 
        FROM BeatmapCountLite b 
        WHERE b.beatmapID IN :beatmapIDs
    """)
    fun getTimeStampByBeatmapIDs(@Param("beatmapIDs") beatmapIDs: Collection<Long>): List<BeatmapCountLite.TimeResult>
}