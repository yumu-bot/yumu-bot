package com.now.nowbot.service.sbApiService

import com.now.nowbot.model.ppysb.SBUser

interface SBUserApiService {
    fun getUserID(username: String): Long?

    fun getUserOnlineCount(): Pair<Long, Long>

    fun getUser(id: Long? = null, username: String? = null, scope: String = "all"): SBUser?

    fun getUserOnlineStatus(id: Long?, username: String?): Pair<Boolean, Long>
}