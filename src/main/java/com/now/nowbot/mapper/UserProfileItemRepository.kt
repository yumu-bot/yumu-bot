package com.now.nowbot.mapper

import com.now.nowbot.entity.UserProfileItem
import com.now.nowbot.entity.UserProfileKey
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface UserProfileItemRepository : JpaRepository<UserProfileItem, UserProfileKey> {
    @Query("select p from UserProfileItem p where p.verify = false")
    fun getAllUnaudited(): List<UserProfileItem>

    @Query("select p from UserProfileItem p where p.userId = :userId and p.verify = true")
    fun getAllByUserId(userId: Long): List<UserProfileItem>

    @Query("select p from UserProfileItem p where p.verify = false")
    fun getAllUnverified(): List<UserProfileItem>

    fun deleteAllByUserId(userId: Long): Int

    @Modifying
    @Transactional
    @Query("delete from UserProfileItem where userId = :#{#key.userId} and type = :#{#key.type}")
    fun deleteByUserIdAndType(key: UserProfileKey)

    @Modifying
    @Transactional
    @Query("update UserProfileItem set verify = true, audit = '' where userId = :#{#key.userId} and type = :#{#key.type}")
    fun verified(key: UserProfileKey)


    @Modifying
    @Transactional
    @Query("update UserProfileItem set verify = false, audit = :audit where userId = :#{#key.userId} and type = :#{#key.type}")
    fun unverified(key: UserProfileKey, audit: String)
}