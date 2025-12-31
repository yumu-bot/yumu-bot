package com.now.nowbot.model.ppysb

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class SBClan(
    @field:JsonProperty("id") val clanID: Long = 0,

    @field:JsonProperty("name") val clanName: String = "",

    @field:JsonProperty("tag") val clanTag: String = "",

    @field:JsonProperty("members") val members: List<SBClanMember> = listOf(),

    ) {
    data class SBClanMember(
        @field:JsonProperty("id") val userID: Long,

        @field:JsonProperty("name") val username: String,

        @field:JsonProperty("country") val country: String,

        /**
         * Member, Owner
         */
        @field:JsonProperty("rank") @JsonAlias("role") val role: String,
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
