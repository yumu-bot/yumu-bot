package com.now.nowbot.model.ppysb

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.osu.Statistics
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

data class SBUser(
    @field:JsonProperty("id") val userID: Long = 0,
    @field:JsonProperty("name") val username: String = "",
    // @field:JsonProperty("safe_name") val safeName: String,
    @field:JsonAlias("support_level")
    @field:JsonProperty("priv") val privilege: Int = 0,
    @field:JsonProperty("country") val country: String = "",

    @field:JsonProperty("silence_end") val silenceEnd: Long = 0,
    @field:JsonProperty("donor_end") val donorEnd: Long = 0,
    @field:JsonProperty("creation_time") val joinDate: Long = 0,
    @field:JsonProperty("latest_activity") val lastVisitTime: Long = 0,
    @field:JsonProperty("clan_id") val clanID: Long = 0,
    @field:JsonProperty("clan_priv") val clanPrivilege: Byte = 0,
    @field:JsonProperty("preferred_mode") val preferredMode: Byte = 0,
    @field:JsonProperty("play_style") val playStyle: Byte = 0,

    @field:JsonProperty("custom_badge_name") val customBadgeName: String? = null,
    @field:JsonProperty("custom_badge_icon") val customBadgeIcon: String? = null,
    @field:JsonProperty("userpage_content") val userpageContent: String? = null,
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

    @get:JsonProperty("avatar_url")
    var avatarUrl = "https://a.ppy.sb/" + this.userID

    fun toOsuUser(mode: OsuMode?): OsuUser {
        val sb = this

        if (OsuMode.isNotDefaultOrNull(mode)) {
            sb.currentMode = mode!!
        }

        val code = sb.country.uppercase()

        val bt = sb.statistics.firstOrNull { it.modeByte == sb.mode.modeValue } ?: SBStatistics()

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
            globalRank = bt.globalRank
            countryRank = bt.countryRank
            maxCombo = bt.maxCombo
            totalHits = bt.totalHits
            replaysWatchedByOthers = bt.replayWatchedByOthers
        }

        val user = OsuUser(stat).apply {
            supportLevel = privilege.toByte()
            hasSupported = privilege > 0

            id = sb.userID
            avatarUrl = sb.avatarUrl
            username = sb.username
            countryCode = code
            country = OsuUser.Country(code, code)
            isRestricted = sb.silenceEnd > 0L
            isSupporter = sb.donorEnd > 0L
            joinDate = Instant.ofEpochSecond(sb.joinDate).atOffset(ZoneOffset.ofHours(8))
            lastVisit = Instant.ofEpochSecond(sb.lastVisitTime).atOffset(ZoneOffset.ofHours(8))

            if (clan.clanID > 0L) {
                team = OsuUser.Team(null, sb.clan.clanID.toInt(), sb.clan.clanName, sb.clan.clanTag)
            }

            defaultOsuMode = sb.mode
            currentOsuMode = sb.currentMode

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

            pp = bt.pp.toDouble()

            statistics = stat
        }

        return user
    }
}
