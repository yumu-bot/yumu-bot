package com.now.nowbot.mapper

import com.now.nowbot.entity.BeatmapsetLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BeatmapsetRepository : JpaRepository<BeatmapsetLite, Int>, JpaSpecificationExecutor<BeatmapsetLite> {
    @Query("SELECT b.id FROM BeatmapsetLite b WHERE b.id IN :ids")
    fun findExistingIds(@Param("ids") ids: Collection<Int>): List<Int>
}
