package com.now.nowbot.entity

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.osu.NanoUser

class NanoUserLite(
    @field:JsonProperty("i")
    var userID: Long = 0L,

    @field:JsonProperty("u")
    var username: String = "",
) {
    override fun hashCode(): Int {
        return userID.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is NanoUserLite && userID == other.userID
    }

    fun toNanoUser(): NanoUser {
        return NanoUser(userID, username)
    }

    companion object {
        fun NanoUser.toNanoUserLite(): NanoUserLite {
            return NanoUserLite(this.userID, this.username)
        }
    }
}