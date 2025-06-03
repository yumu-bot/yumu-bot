package com.now.nowbot.service.sbApiService.impl

import com.now.nowbot.model.ppysb.SBUser
import com.now.nowbot.service.sbApiService.SBUserApiService
import org.springframework.stereotype.Service

@Service
class UserApiServiceImpl: SBUserApiService {
    override fun getUserID(username: String): Long {
        return 0L
    }

    override fun getUserOnlineCount(): Pair<Long, Long> {
        return 0L to 0L
    }

    override fun getUser(id: Long?, username: String?, scope: String): SBUser {
        return SBUser()
    }

    override fun getUserOnlineStatus(id: Long?, username: String?): Pair<Boolean, Long> {
        return true to 0L
    }
}