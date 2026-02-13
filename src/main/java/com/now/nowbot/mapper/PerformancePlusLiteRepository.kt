package com.now.nowbot.mapper

import com.now.nowbot.entity.PerformancePlusLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query

interface PerformancePlusLiteRepository : JpaRepository<PerformancePlusLite, Long>,
    JpaSpecificationExecutor<PerformancePlusLite> {
    // 通过id批量查询已有的 PerformancePlusLite
    @Query("select p from PerformancePlusLite p where p.id in (:ids) and p.type = 0")
    fun findScorePPPlus(ids: Collection<Long>): List<PerformancePlusLite>

    @Query("select p from PerformancePlusLite p where p.id in (:ids) and p.type = 1")
    fun findBeatmapPPPlus(ids: Collection<Long>): List<PerformancePlusLite>

    @Query("select p from PerformancePlusLite p where p.id = :id and p.type = 1")
    fun findBeatmapPPPlusByBeatmapID(id: Long?): PerformancePlusLite?
}