package com.now.nowbot.mapper

import com.now.nowbot.entity.UserProfileLite
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component

@Component
interface UserProfileRepository : JpaRepository<UserProfileLite?, Long?> {
    @Cacheable(value = ["user_profile"], key = "#aLong")
    fun findTopById(aLong: Long): UserProfileLite?
}
