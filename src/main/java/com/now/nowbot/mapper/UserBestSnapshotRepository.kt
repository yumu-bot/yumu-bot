package com.now.nowbot.mapper

import com.now.nowbot.entity.UserBestSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserBestSnapshotRepository: JpaRepository<UserBestSnapshot, Long> {

    @Query("""
        SELECT s From UserBestSnapshot s WHERE s.userID = :userID AND s.mode = :modeByte ORDER BY s.createdAt DESC
    """)
    fun getLatest(userID: Long, modeByte: Byte): UserBestSnapshot?

}