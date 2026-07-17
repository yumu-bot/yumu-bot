package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.entity.IDUser

// BeatmapOwner
class NanoUser(
    @field:JsonProperty("id")
    override var userID: Long = 0L,

    @field:JsonProperty("username")
    override var username: String = "",
): IDUser {
    override fun hashCode(): Int {
        return userID.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is NanoUser) return userID == other.userID
        if (other is MicroUser) return userID == other.userID
        if (other is OsuUser) return userID == other.userID
        return false
    }
}
