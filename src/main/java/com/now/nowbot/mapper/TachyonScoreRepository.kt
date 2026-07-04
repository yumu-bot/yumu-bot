package com.now.nowbot.mapper

import com.now.nowbot.entity.TachyonScoreLite
import com.now.nowbot.entity.TachyonStatisticsKey
import com.now.nowbot.entity.TachyonStatisticsLite
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime
import java.time.OffsetDateTime

interface TachyonScoreRepository: JpaRepository<TachyonScoreLite, Long> {
    @Query("""
        SELECT COUNT(*)
        FROM tachyon_score s 
        WHERE s.user_id = :userID AND s.mode = :mode AND s.time BETWEEN :from AND :to
        """, nativeQuery = true)
    fun getCountBetween(userID: Long, mode: Byte, from: LocalDateTime, to: LocalDateTime): Long

    @Query("""
        select * from tachyon_score s
        where s.user_id = :userID
        and s.beatmap_id = :beatmapID
        and s.mode = :mode
        order by s.time desc
        """, nativeQuery = true)
    fun getBeatmapScores(userID: Long, beatmapID: Long, mode: Byte): List<TachyonScoreLite>

    @Query("""
        SELECT id FROM tachyon_score WHERE id IN (:ids)
    """, nativeQuery = true)
    fun exists(ids: Collection<Long>): List<Long>

    @Query(
        value = """
        SELECT s.time FROM tachyon_score s 
        WHERE s.user_id = :userID AND s.time BETWEEN :start AND :end
    """,
        countQuery = """
        SELECT COUNT(*) FROM tachyon_score s 
        WHERE s.user_id = :userID AND s.time BETWEEN :start AND :end
    """,
        nativeQuery = true
    )
    fun getUserAllScoreTime(userID: Long, start: OffsetDateTime, end: OffsetDateTime, page: Pageable): List<OffsetDateTime>

    @Query("""
        select * from tachyon_score s
        where s.user_id = :userID
        and s.time between :start and :end
        and s.mode = :mode
        and s.pp > 0
        """, nativeQuery = true)
    fun getUserRankedScore(userID: Long, mode:Byte, start: OffsetDateTime, end: OffsetDateTime): List<TachyonScoreLite>

    @Query("""
        select * from tachyon_score s
        where s.user_id in (:userIDs)
        and s.time between :start and :end
        and s.mode = :mode
        and s.pp > 0
        """, nativeQuery = true)
    fun getUsersRankedScore(userIDs: Iterable<Long>, mode:Byte, start: OffsetDateTime, end: OffsetDateTime): List<TachyonScoreLite>

    @Query("""
    SELECT t.* FROM (
        SELECT s.*, 
               ROW_NUMBER() OVER (
                   PARTITION BY s.user_id 
                   ORDER BY s.pp DESC, s.accuracy DESC, s.time DESC
               ) as rn
        FROM tachyon_score s
        WHERE s.user_id IN (:userIDs)
          AND s.beatmap_id = :beatmapID
          AND s.mode = :mode
    ) t
    WHERE t.rn = 1
""", nativeQuery = true)
    fun getUsersBestScore(userIDs: Collection<Long>, beatmapID: Long, mode: Byte): List<TachyonScoreLite>

    @Query("""
    SELECT * FROM tachyon_score 
    WHERE id IN (:scoreIDs)
""", nativeQuery = true)
    fun getScoresFromIDs(scoreIDs: Collection<Long>): List<TachyonScoreLite>
}


interface TachyonStatisticsRepository: JpaRepository<TachyonStatisticsLite, TachyonStatisticsKey> {
    @Query("""
        SELECT s FROM tachyon_statistics s WHERE s.id IN (:ids) AND s.mode = :mode
    """, nativeQuery = true)
    fun getStatistics(ids: Collection<Long>, mode: Byte = -1): List<TachyonStatisticsLite>

    @Query("SELECT EXISTS(SELECT 1 FROM tachyon_statistics WHERE id = :id AND mode = :mode)", nativeQuery = true)
    fun exists(id: Long, mode: Byte = -1): Boolean

    @Query("""
        SELECT id FROM tachyon_statistics WHERE id IN (:ids) AND mode = :mode
    """, nativeQuery = true)
    fun exists(ids: Collection<Long>, mode: Byte = -1): List<Long>
}
