package com.now.nowbot.mapper

import com.now.nowbot.entity.UserProfileLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component

@Component
interface UserProfileRepository : JpaRepository<UserProfileLite, Long> {
    fun findTopById(aLong: Long): UserProfileLite?
}
