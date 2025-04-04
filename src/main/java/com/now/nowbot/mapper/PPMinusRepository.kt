package com.now.nowbot.mapper

import com.now.nowbot.entity.PPMinusLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PPMinusRepository : JpaRepository<PPMinusLite, Long> {

    @Query("""
        select * from pp_minus where user_id = :userID and mode = :modeByte
        """, nativeQuery = true)
    fun findByUserID(userID: Long, modeByte: Byte): List<PPMinusLite>?

    // 寻找同分段的玩家记录。分段为：自身上下 500pp。
    @Query("""
        SELECT DISTINCT ON (user_id) * FROM pp_minus WHERE (raw_pp BETWEEN :bottomPP AND :topPP)
            AND mode = :modeByte
            AND record_time >= :minTime
        ORDER BY user_id, record_time DESC;
        """, nativeQuery = true)
    fun findSurroundingByUserID(id: Long, bottomPP: Double, topPP: Double, modeByte: Byte, minTime: Long = 0L): List<PPMinusLite>?
}