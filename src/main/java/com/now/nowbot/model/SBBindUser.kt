package com.now.nowbot.model

import com.now.nowbot.entity.SBBindUserLite
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.ppysb.SBUser
import java.time.OffsetDateTime

data class SBBindUser(
    val id: Long? = null,

    val userID: Long,

    val username: String,

    val mode: OsuMode,

    val time: Long = 0L,

    val joinDate: OffsetDateTime = OffsetDateTime.now(),
) {
    constructor(sbUser: SBUser): this (null, sbUser.userID, sbUser.username, sbUser.mode)
    constructor(userID: Long, username: String): this (null, userID, username, OsuMode.DEFAULT, 0L)
    constructor(id: Long?, userID: Long, username: String, mode: OsuMode): this (id, userID, username, mode, 0L)

    fun toSBBindUserLite(): SBBindUserLite {
        return SBBindUserLite(null, this.userID, this.username, System.currentTimeMillis(), OffsetDateTime.now(), mode)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SBBindUser

        return userID == other.userID
    }

    override fun hashCode(): Int {
        return userID.hashCode()
    }

}
