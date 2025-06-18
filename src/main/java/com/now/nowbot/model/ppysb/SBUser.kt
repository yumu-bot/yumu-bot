package com.now.nowbot.model.ppysb

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.osu.Statistics
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Date

data class SBUser(
    @JsonProperty("id") val userID: Long = 0,
    @JsonProperty("name") val username: String = "",
    // @JsonProperty("safe_name") val safeName: String,
    @JsonProperty("priv") val privilege: Int = 0,
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

    @get:JsonProperty("mode")
    val mode: OsuMode
        get() = OsuMode.getMode(preferredMode.toInt())

    @get:JsonProperty("current_mode")
    var currentMode: OsuMode = mode

    @get:JsonProperty("clan")
    var clan: SBClan = SBClan()

    @get:JsonProperty("statistics")
    var statistics: List<SBStatistics> = listOf()

    fun toOsuUser(mode: OsuMode?): OsuUser {
        val sb = this
        mode?.let { sb.currentMode = it }

        val code = sb.country.uppercase()

        val user = OsuUser().apply {
            id = sb.userID
            avatarUrl = "https://a.ppy.sb/" + sb.userID
            username = sb.username
            countryCode = code
            country = OsuUser.Country(code, code)
            isRestricted = sb.silenceEnd > 0L
            isSupporter = sb.donorEnd > 0L
            joinDate = Date(sb.joinDate).toInstant().atOffset(ZoneOffset.ofHours(8))
            lastVisit = Date(sb.lastVisitTime).toInstant().atOffset(ZoneOffset.ofHours(8))

            if (clan.clanID > 0L) {
                team = OsuUser.Team(null, sb.clan.clanID.toInt(), sb.clan.clanName, sb.clan.clanTag)
            }

            val currentOsuMode = if (OsuMode.isDefaultOrNull(mode)) sb.mode else mode!!

            this.defaultOsuMode = sb.mode
            this.currentOsuMode = currentOsuMode

            if (sb.customBadgeName != null && sb.customBadgeIcon != null) {
                badges = listOf(
                    OsuUser.UserBadge(
                        OffsetDateTime.MIN,
                        sb.customBadgeName,
                        sb.customBadgeIcon,
                        sb.customBadgeIcon,
                        null
                ))
            }

            val bt = sb.statistics.firstOrNull { it.modeByte == currentOsuMode.modeValue } ?: SBStatistics()

            pp = bt.pp.toDouble()

            val stat = Statistics().apply {
                totalScore = bt.totalScore
                rankedScore = bt.rankedScore
                playCount = bt.playCount
                playTime = bt.playTime
                accuracy = bt.accuracy
                countSSH = bt.countSSH
                countSS = bt.countSS
                countSH = bt.countSH
                countS = bt.countS
                countA = bt.countA
                globalRank = bt.rank.toLong()
                countryRank = bt.countryRank.toLong()
            }

            statistics = stat
        }

        return user
    }
}
