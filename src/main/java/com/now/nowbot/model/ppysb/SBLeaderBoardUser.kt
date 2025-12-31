package com.now.nowbot.model.ppysb

import com.fasterxml.jackson.annotation.JsonProperty

data class SBLeaderBoardUser(
    @field:JsonProperty("player_id")
    val userID: Long,

    @field:JsonProperty("name")
    val username: String,

    @field:JsonProperty("country")
    val country: String,

    @field:JsonProperty("tscore")
    val totalScore: Long,

    @field:JsonProperty("rscore")
    val rankedScore: Long,

    @field:JsonProperty("pp")
    val pp: Int,

    @field:JsonProperty("plays")
    val playCount: Long,

    @field:JsonProperty("playtime")
    val playTime: Long,

    @field:JsonProperty("acc")
    val accuracy: Double,

    @field:JsonProperty("xh_count")
    val countSSH: Int,

    @field:JsonProperty("x_count")
    val countSS: Int,

    @field:JsonProperty("sh_count")
    val countSH: Int,

    @field:JsonProperty("s_count")
    val countS: Int,

    @field:JsonProperty("a_count")
    val countA: Int,

    @field:JsonProperty("clan_id")
    val clanID: Int?,

    @field:JsonProperty("clan_name")
    val clanName: String?,

    @field:JsonProperty("clan_tag")
    val clanTag: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SBLeaderBoardUser

        return userID == other.userID
    }

    override fun hashCode(): Int {
        return userID.hashCode()
    }
}