package com.now.nowbot.mapper

import com.now.nowbot.entity.PerformancePlusStatsLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface PerformancePlusLiteRepository: JpaRepository<PerformancePlusStatsLite, PerformancePlusStatsLite.PerformancePlusStatsKey> {


    @Transactional
    @Modifying
    @Query("""
        INSERT INTO pp_plus (id, score_id, user_id, aim, jump, flow, precision, speed, stamina, accuracy, total) 
        VALUES (:#{#s.beatmapID}, :#{#s.scoreID}, :#{#s.userID}, :#{#s.aim}, :#{#s.jump}, :#{#s.flow}, :#{#s.precision}, :#{#s.speed}, :#{#s.stamina}, :#{#s.accuracy}, :#{#s.total}) 
        ON CONFLICT (id, score_id) 
        DO UPDATE SET 
            aim = EXCLUDED.aim, 
            jump = EXCLUDED.jump, 
            flow = EXCLUDED.flow, 
            precision = EXCLUDED.precision, 
            speed = EXCLUDED.speed, 
            stamina = EXCLUDED.stamina, 
            accuracy = EXCLUDED.accuracy, 
            total = EXCLUDED.total
    """, nativeQuery = true)
    fun saveAndUpdate(@Param("s") stats: PerformancePlusStatsLite)

    @Query("""
        SELECT p from PerformancePlusStatsLite p WHERE p.scoreID IN (-1, -2) AND p.beatmapID = :beatmapID
    """)
    fun findDetailsByBeatmapID(
        @Param("beatmapID") beatmapID: Long
    ): List<PerformancePlusStatsLite>

    @Query("""
        SELECT p from PerformancePlusStatsLite p WHERE p.scoreID = :scoreID AND p.userID = :userID
    """)
    fun findDetailsByScoreID(
        @Param("scoreID") scoreID: Long = 0L,
        @Param("userID") userID: Long
    ): PerformancePlusStatsLite?

}