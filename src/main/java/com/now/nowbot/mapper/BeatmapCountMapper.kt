package com.now.nowbot.mapper

import com.now.nowbot.entity.BeatmapCountLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BeatmapCountMapper : JpaRepository<BeatmapCountLite, Long> {

    @Query("""
         select density from beatmap_count where id = :beatmapID and hash = :hash limit 1
    """, nativeQuery = true)
    fun getDensityByBeatmapIDAndHash(
        @Param("beatmapID") beatmapID: Long, @Param("hash") hash: String
    ): IntArray?

    @Query("select density from beatmap_count where id = :beatmapID limit 1", nativeQuery = true)
    fun getDensityByBeatmapID(@Param("bid") bid: Long): IntArray?

    @Query("""
        select id as beatmapID, delta as delta from beatmap_count where id in (:bid)
    """, nativeQuery = true)
    fun getTimeStampByBeatmapIDs(@Param("bid") bid: Collection<Long>): List<BeatmapCountLite.TimeResult>
}