package com.now.nowbot.model.ppysb

import com.fasterxml.jackson.annotation.JsonProperty

data class SBLeaderBoardUser(
    @JsonProperty("player_id")
    val userID: Long,

    @JsonProperty("name")
    val username: String,

    @JsonProperty("country")
    val country: String,

    @JsonProperty("tscore")
    val totalScore: Long,

    @JsonProperty("rscore")
    val rankedScore: Long,

    @JsonProperty("pp")
    val pp: Int,

    @JsonProperty("plays")
    val playCount: Long,

    @JsonProperty("playtime")
    val playTime: Long,

    @JsonProperty("acc")
    val accuracy: Double,

    @JsonProperty("xh_count")
    val countSSH: Int,

    @JsonProperty("x_count")
    val countSS: Int,

    @JsonProperty("sh_count")
    val countSH: Int,

    @JsonProperty("s_count")
    val countS: Int,

    @JsonProperty("a_count")
    val countA: Int,

    @JsonProperty("clan_id")
    val clanID: Int?,

    @JsonProperty("clan_name")
    val clanName: String?,

    @JsonProperty("clan_tag")
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