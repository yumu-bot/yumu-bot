package com.now.nowbot.service.sbApiService

import com.now.nowbot.model.ppysb.SBUser

interface SBUserApiService {
    fun getUserID(username: String): Long?

    fun getUserOnlineCount(): Pair<Long, Long>

    fun getUser(id: Long?, username: String?, scope: String = "all"): SBUser?

    fun getUser(id: Long?): SBUser? {
        return getUser(id = id, username = null)
    }

    fun getUser(username: String?): SBUser? {
        return getUser(id = null, username = username)
    }

    fun getUserOnlineStatus(id: Long?, username: String?): Pair<Boolean, Long>
}