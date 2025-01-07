package com.now.nowbot.mapper

import com.now.nowbot.entity.LazerScoreLite
import com.now.nowbot.entity.ScoreStatisticLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface LazerScoreRepository : JpaRepository<LazerScoreLite, Long> {
    @Query("select s.id from LazerScoreLite s where s.id in (:id)")
    fun getRecordId(id: Collection<Long>): Set<Long>

    @Query("select id from lazer_score_lite where id = :id limit 1", nativeQuery = true)
    fun checkIdExists(id: Long): Optional<Long>
}

interface LazerScoreStatisticRepository : JpaRepository<ScoreStatisticLite, ScoreStatisticLite.ScoreStatisticKey> {
    @Query("select s.id from ScoreStatisticLite s where s.id in (:bid) and s.status = :mode ")
    fun getRecordBeatmapId(bid: Collection<Long>, mode: Int): Set<Long>


    @Query("select id from score_statistic where id = :id limit 1", nativeQuery = true)
    fun checkIdExists(id: Long): Optional<Long>
}