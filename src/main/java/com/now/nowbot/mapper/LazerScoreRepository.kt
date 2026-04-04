package com.now.nowbot.mapper

import com.now.nowbot.entity.LazerScoreLite
import com.now.nowbot.entity.ScoreStatisticLite
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime
import java.time.OffsetDateTime

interface LazerScoreRepository : JpaRepository<LazerScoreLite, Long> {
    @Query("""
        SELECT COUNT(*)
        FROM lazer_score_lite s 
        WHERE s.user_id = :userID AND s.mode = :mode AND s.time BETWEEN :from AND :to
        """, nativeQuery = true)
    fun getCountBetween(userID: Long, mode: Byte, from: LocalDateTime, to: LocalDateTime): Long

    @Query("""
        select * from lazer_score_lite s
        where s.user_id = :userID
        and s.beatmap_id = :beatmapID
        and s.mode = :mode
        order by s.time desc
        """, nativeQuery = true)
    fun getBeatmapScores(userID: Long, beatmapID: Long, mode: Byte): List<LazerScoreLite>

    @Query("select s.id from LazerScoreLite s where s.id in (:id)")
    fun getSavedScoreIDs(id: Collection<Long>): Set<Long>

    @Query("""
        SELECT EXISTS(SELECT 1 FROM lazer_score_lite WHERE id = :id);
    """, nativeQuery = true)
    fun exists(id: Long): Boolean

    @Query("""
        SELECT id FROM lazer_score_lite WHERE id IN (:ids)
    """, nativeQuery = true)
    fun exists(ids: Collection<Long>): List<Long>

    @Query("select s.time from LazerScoreLite s where s.userId = :userID and s.time between :start and :end")
    fun getUserAllScoreTime(userID: Long, start: OffsetDateTime, end: OffsetDateTime, page: Pageable): List<OffsetDateTime>

    // and s.beatmap_id in (select bid from osu_ranked_beatmap_id)
    @Query("""
        select * from lazer_score_lite s
        where s.user_id = :userID
        and s.time between :start and :end
        and s.mode = :mode
        and s.pp > 0
        """, nativeQuery = true)
    fun getUserRankedScore(userID: Long, mode:Byte, start: OffsetDateTime, end: OffsetDateTime): List<LazerScoreLite>

    @Query("""
        select * from lazer_score_lite s
        where s.user_id in (:userIDs)
        and s.time between :start and :end
        and s.mode = :mode
        and s.pp > 0
        """, nativeQuery = true)
    fun getUsersRankedScore(userIDs: Iterable<Long>, mode:Byte, start: OffsetDateTime, end: OffsetDateTime): List<LazerScoreLite>

    // TODO 可能需要优化
    @Query("""
    SELECT t.* FROM (
        SELECT s.*, 
               ROW_NUMBER() OVER (
                   PARTITION BY s.user_id 
                   ORDER BY s.pp DESC, s.accuracy DESC, s.time DESC
               ) as rn
        FROM lazer_score_lite s
        WHERE s.user_id IN (:userIDs)
          AND s.beatmap_id = :beatmapID
          AND s.mode = :mode
    ) t
    WHERE t.rn = 1
""", nativeQuery = true)
    fun getUsersBestScore(userIDs: Collection<Long>, beatmapID: Long, mode: Byte): List<LazerScoreLite>
}

interface LazerScoreStatisticRepository : JpaRepository<ScoreStatisticLite, ScoreStatisticLite.ScoreStatisticKey> {

    @Query("""
        SELECT s FROM ScoreStatisticLite s WHERE s.id IN (:ids) AND s.mode = :mode
    """)
    fun getStatistics(ids: Collection<Long>, mode: Byte = -1): List<ScoreStatisticLite>

    @Query("SELECT EXISTS(SELECT 1 FROM score_statistic WHERE id = :id AND mode = :mode)", nativeQuery = true)
    fun exists(id: Long, mode: Byte = -1): Boolean

    @Query("""
        SELECT id FROM score_statistic WHERE id IN (:ids) AND mode = :mode
    """, nativeQuery = true)
    fun exists(ids: Collection<Long>, mode: Byte = -1): List<Long>
}