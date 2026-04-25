package com.now.nowbot.mapper

import com.now.nowbot.entity.UserBestSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface UserBestSnapshotRepository: JpaRepository<UserBestSnapshot, Long> {

    @Query("""
        SELECT s From UserBestSnapshot s WHERE s.userID = :userID AND s.mode = :modeByte ORDER BY s.createdAt DESC LIMIT 1
    """)
    fun getLatest(userID: Long, modeByte: Byte): UserBestSnapshot?

    @Query("""
        SELECT COUNT(*) From UserBestSnapshot s WHERE s.userID = :userID AND s.mode = :modeByte 
    """)
    fun getCount(userID: Long, modeByte: Byte): Long

    @Query("""
        SELECT s.createdAt From UserBestSnapshot s WHERE s.userID = :userID AND s.mode = :modeByte ORDER BY s.createdAt DESC LIMIT :limit OFFSET :offset
    """)
    fun getCreatedAt(userID: Long, modeByte: Byte, offset: Int, limit: Int): List<LocalDateTime>

    @Query("""
        SELECT s From UserBestSnapshot s WHERE s.userID = :userID AND s.mode = :modeByte 
    ORDER BY s.createdAt DESC 
    OFFSET :offset
    """)
    fun getWithOffset(userID: Long, modeByte: Byte, offset: Int): List<UserBestSnapshot>
}