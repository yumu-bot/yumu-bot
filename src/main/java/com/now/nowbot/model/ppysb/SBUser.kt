package com.now.nowbot.model.ppysb

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMode

data class SBUser(
    @JsonProperty("id") val userID: Long,
    @JsonProperty("name") val username: String,
    // @JsonProperty("safe_name") val safeName: String,
    @JsonProperty("priv") val privilege: Byte,
    @JsonProperty("country") val country: String,

    @JsonProperty("silence_end") val silenceEnd: Long,
    @JsonProperty("donor_end") val donorEnd: Long,
    @JsonProperty("creation_time") val joinDate: Long,
    @JsonProperty("latest_activity") val lastVisitTime: Long,
    @JsonProperty("clan_id") val clanID: Long,
    @JsonProperty("clan_priv") val clanPrivilege: Byte,
    @JsonProperty("preferred_mode") val preferredMode: Byte,
    @JsonProperty("play_style") val playStyle: Byte,

    @JsonProperty("custom_badge_name") val customBadgeName: String?,
    @JsonProperty("custom_badge_icon") val customBadgeIcon: String?,
    @JsonProperty("userpage_content") val userpageContent: String?,
) {

    @get:JsonProperty("mode") val mode: OsuMode
        get() = OsuMode.getMode(preferredMode.toInt())

}
