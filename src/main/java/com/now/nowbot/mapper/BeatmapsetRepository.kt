package com.now.nowbot.mapper

import com.now.nowbot.entity.BeatmapsetLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface BeatmapsetRepository : JpaRepository<BeatmapsetLite, Int>, JpaSpecificationExecutor<BeatmapsetLite>
