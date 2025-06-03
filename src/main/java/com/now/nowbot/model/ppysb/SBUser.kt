package com.now.nowbot.model.ppysb

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMode

data class SBUser(
    @JsonProperty("id") val userID: Long = 0,
    @JsonProperty("name") val username: String = "",
    // @JsonProperty("safe_name") val safeName: String,
    @JsonProperty("priv") val privilege: Byte = 0,
    @JsonProperty("country") val country: String = "",

    @JsonProperty("silence_end") val silenceEnd: Long = 0,
    @JsonProperty("donor_end") val donorEnd: Long = 0,
    @JsonProperty("creation_time") val joinDate: Long = 0,
    @JsonProperty("latest_activity") val lastVisitTime: Long = 0,
    @JsonProperty("clan_id") val clanID: Long = 0,
    @JsonProperty("clan_priv") val clanPrivilege: Byte = 0,
    @JsonProperty("preferred_mode") val preferredMode: Byte = 0,
    @JsonProperty("play_style") val playStyle: Byte = 0,

    @JsonProperty("custom_badge_name") val customBadgeName: String? = null,
    @JsonProperty("custom_badge_icon") val customBadgeIcon: String? = null,
    @JsonProperty("userpage_content") val userpageContent: String? = null,
) {

    @get:JsonProperty("mode") val mode: OsuMode
        get() = OsuMode.getMode(preferredMode.toInt())

}
