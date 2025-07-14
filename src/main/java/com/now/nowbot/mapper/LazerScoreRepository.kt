package com.now.nowbot.mapper

import com.now.nowbot.entity.LazerScoreLite
import com.now.nowbot.entity.ScoreStatisticLite
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.OffsetDateTime
import java.util.*

interface LazerScoreRepository : JpaRepository<LazerScoreLite, Long> {
    @Query("select s.id from LazerScoreLite s where s.id in (:id)")
    fun getRecordId(id: Collection<Long>): Set<Long>

    @Query("select id from lazer_score_lite where id = :id limit 1", nativeQuery = true)
    fun checkIdExists(id: Long): Optional<Long>

    @Query("select s.time from LazerScoreLite s where s.userId = :id and s.time between :start and :end")
    fun getUserAllScoreTime(id:Long, start: OffsetDateTime, end: OffsetDateTime, page: Pageable): List<OffsetDateTime>

    @Query("""
        select * from lazer_score_lite s
        where s.user_id = :id
        and s.time between :start and :end
        and s.mode = :mode
        and s.beatmap_id in (select bid from osu_ranked_beatmap_id)
        """, nativeQuery = true)
    fun getUserRankedScore(id: Long, mode:Byte, start: OffsetDateTime, end: OffsetDateTime): List<LazerScoreLite>

    @Query("""
        select * from lazer_score_lite s
        where s.user_id in (:ids)
        and s.time between :start and :end
        and s.mode = :mode
        and s.beatmap_id in (select bid from osu_ranked_beatmap_id)
        """, nativeQuery = true)
    fun getUsersRankedScore(ids: Iterable<Long>, mode:Byte, start: OffsetDateTime, end: OffsetDateTime): List<LazerScoreLite>
}

interface LazerScoreStatisticRepository : JpaRepository<ScoreStatisticLite, ScoreStatisticLite.ScoreStatisticKey> {
    @Query("select s.id from ScoreStatisticLite s where s.id in (:bid) and s.status = :mode ")
    fun getRecordBeatmapId(bid: Collection<Long>, mode: Int): Set<Long>


    @Query("select id from score_statistic where id = :id limit 1", nativeQuery = true)
    fun checkIdExists(id: Long): Optional<Long>

    @Query("select s from ScoreStatisticLite s where s.id in (:sid) and s.status=-1")
    fun getByScoreId(sid: Collection<Long>): List<ScoreStatisticLite>
}