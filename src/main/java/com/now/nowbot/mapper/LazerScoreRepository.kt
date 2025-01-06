package com.now.nowbot.mapper

import com.now.nowbot.entity.LazerScoreLite
import com.now.nowbot.entity.ScoreStatisticLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface LazerScoreRepository : JpaRepository<LazerScoreLite, Long> {
    @Query("select s.id from LazerScoreLite s where s.id in (:id)")
    fun getRecordId(id: Collection<Long>): Set<Long>
}

interface LazerScoreStatisticRepository : JpaRepository<ScoreStatisticLite, ScoreStatisticLite.ScoreStatisticKey> {
    @Query("select s.id from ScoreStatisticLite s where s.id in (:bid) and s.status = :mode ")
    fun getRecordBeatmapId(bid: Collection<Long>, mode: Int): Set<Long>
}