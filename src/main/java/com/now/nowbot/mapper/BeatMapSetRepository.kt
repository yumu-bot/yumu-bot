package com.now.nowbot.mapper

import com.now.nowbot.entity.MapSetLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface BeatMapSetRepository : JpaRepository<MapSetLite, Int>, JpaSpecificationExecutor<MapSetLite>
