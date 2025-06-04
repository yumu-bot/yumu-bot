package com.now.nowbot.model.ppysb

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class SBClan(
    @JsonProperty("id") val clanID: Long = 0,

    @JsonProperty("name") val clanName: String = "",

    @JsonProperty("tag") val clanTag: String = "",

    @JsonProperty("members") val members: List<SBClanMember> = listOf(),

    ) {
    data class SBClanMember(
        @JsonProperty("id") val userID: Long,

        @JsonProperty("name") val username: String,

        @JsonProperty("country") val country: String,

        /**
         * Member, Owner
         */
        @JsonProperty("rank") @JsonAlias("role") val role: String,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SBClan

        return clanID == other.clanID
    }

    override fun hashCode(): Int {
        return clanID.hashCode()
    }
}
