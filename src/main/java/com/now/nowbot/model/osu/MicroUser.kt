package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.*
import com.now.nowbot.model.osu.OsuUser.Team
import com.now.nowbot.model.osu.OsuUser.UserGroup
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

@JsonIgnoreProperties(ignoreUnknown = true) // 允许忽略json没有的值赋为空
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY) //扫描非public的值并注入
class MicroUser : Comparable<MicroUser> {
    @JsonProperty("avatar_url")
    var avatarUrl: String? = null

    @JsonProperty("cover_url")
    var coverUrl: String? = null

    @JsonProperty("default_group")
    var defaultGroup: String? = null

    @JsonProperty("id")
    var userID: Long = 0L

    @JsonProperty("is_active")
    var isActive: Boolean = false

    @JsonProperty("is_bot")
    var isBot: Boolean = false

    @JsonProperty("is_deleted")
    var isDeleted: Boolean = false

    @JsonProperty("is_online")
    var isOnline: Boolean = false

    @JsonProperty("is_supporter")
    var isSupporter: Boolean = false

    @JsonProperty("last_visit")
    var lastVisitString: String? = null

    @get:JsonIgnoreProperties
    val lastVisitTime: LocalDateTime?
        get() {
            if (lastVisitString != null) return LocalDateTime.parse(lastVisitString!!, formatter)
            return null
        }

    @JsonProperty("pm_friends_only")
    var pmFriendsOnly: Boolean? = false

    @JsonProperty("profile_color")
    var profileColor: String? = null

    @JsonProperty("username")
    var userName: String = ""

    var cover: Cover? = null

    @set:JsonProperty("country_code") @get:JsonIgnore
    var countryCode: String = ""

    @JsonIgnore
    var country: OsuUser.Country? = null

    // 通过 LazerFriend 设置
    @JsonProperty("is_mutual")
    var isMutual: Boolean = false

    @JsonProperty("country") fun setCountry(country: Map<String, String>?) {
        if (country != null) this.country = OsuUser.Country(
            country["code"]!!,
            country["name"]!!
        )
    }

    @JsonProperty("groups")
    var groups: List<UserGroup> = listOf()

    @JsonProperty("statistics")
    var statistics: Statistics? = null

    // 只有 getUsers包含
    @JsonProperty("statistics_rulesets")
    var rulesets: UserStatisticsRulesets? = null

    data class UserStatisticsRulesets(
        @JsonProperty("osu")
        val osu: Statistics? = null,
        @JsonProperty("taiko")
        val taiko: Statistics? = null,
        @JsonProperty("fruits")
        val fruits: Statistics? = null,
        @JsonProperty("mania")
        val mania: Statistics? = null,
    )

    @JsonProperty("support_level")
    var supportLevel: Byte? = null

    @JsonProperty("team")
    var team: Team? = null

    override fun hashCode(): Int {
        return userID.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MicroUser) return false

        return userID == other.userID
    }

    override fun compareTo(other: MicroUser): Int {
        return (userID - other.userID).toInt()
    }

    override fun toString(): String {
        return "MicroUser(avatarUrl=$avatarUrl, coverUrl=$coverUrl, defaultGroup=$defaultGroup, userID=$userID, isActive=$isActive, isBot=$isBot, isDeleted=$isDeleted, isOnline=$isOnline, isSupporter=$isSupporter, lastVisitString=$lastVisitString, pmFriendsOnly=$pmFriendsOnly, profileColor=$profileColor, userName='$userName', cover=$cover, country=$country, isMutual=$isMutual, groups=$groups, statistics=$statistics, rulesets=$rulesets, supportLevel=$supportLevel, team=$team)"
    }

    companion object {
        private val formatter: DateTimeFormatter = DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .appendLiteral("T")
            .appendPattern("HH:mm:ss")
            .appendZoneId().toFormatter()
    }
}
