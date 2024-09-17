package com.now.nowbot.mapper

import com.now.nowbot.entity.MaiChartLite
import com.now.nowbot.entity.MaiSongLite
import org.springframework.data.jpa.repository.JpaRepository

interface MaiSongLiteRepository : JpaRepository<MaiSongLite, Int>
interface MaiChartLiteRepository : JpaRepository<MaiChartLite, Int>