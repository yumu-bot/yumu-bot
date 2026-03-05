package com.now.nowbot.mapper

import com.now.nowbot.entity.BeatmapLite
import com.now.nowbot.entity.BeatmapLite.BeatmapHitLengthResult
import com.now.nowbot.entity.BeatmapsetLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BeatmapRepository : JpaRepository<BeatmapLite, Long>, JpaSpecificationExecutor<BeatmapLite> {
    @Query("select b.mapSet from BeatmapLite b where b.id = :bid")
    fun getBeatmapsetByBid(bid: Long): BeatmapsetLite?

    @Query("select id as id, hit_length as length from osu_beatmap where id in (:id)", nativeQuery = true
    ) fun getBeatmapHitLength(id: Collection<Long>): List<BeatmapHitLengthResult>

    @Query("SELECT b.id FROM BeatmapLite b WHERE b.id IN :ids")
    fun findExistingIds(@Param("ids") ids: Collection<Int>): List<Int>
}
