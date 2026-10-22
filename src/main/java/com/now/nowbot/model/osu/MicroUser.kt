package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.*
import com.now.nowbot.entity.IDUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.Companion.orElse
import com.now.nowbot.model.osu.OsuUser.Team
import com.now.nowbot.model.osu.OsuUser.UserGroup
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

@JsonIgnoreProperties(ignoreUnknown = true) // 允许忽略json没有的值赋为空
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY) //扫描非public的值并注入
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
class MicroUser: Comparable<MicroUser>, IDUser {
    @JsonProperty("avatar_url")
    var avatarUrl: String? = null

    @JsonProperty("cover_url")
    var coverUrl: String? = null

    @JsonProperty("default_group")
    var defaultGroup: String? = null

    @JsonProperty("id")
    override var userID: Long = 0L

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

    @get:JsonIgnore
    val lastVisitTime: LocalDateTime?
        get() {
            if (lastVisitString != null) return LocalDateTime.parse(lastVisitString!!, formatter)
            return null
        }

    @JsonProperty("pm_friends_only")
    var pmFriendsOnly: Boolean? = false

    @JsonProperty("profile_colour")
    var profileColor: String? = null

    @JsonProperty("username")
    override var username: String = ""

    @JsonProperty("cover")
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
        @field:JsonProperty("osu")
        val osu: Statistics? = null,
        @field:JsonProperty("taiko")
        val taiko: Statistics? = null,
        @field:JsonProperty("fruits")
        val fruits: Statistics? = null,
        @field:JsonProperty("mania")
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
        return "MicroUser(avatarUrl=$avatarUrl, coverUrl=$coverUrl, defaultGroup=$defaultGroup, userID=$userID, isActive=$isActive, isBot=$isBot, isDeleted=$isDeleted, isOnline=$isOnline, isSupporter=$isSupporter, lastVisitString=$lastVisitString, pmFriendsOnly=$pmFriendsOnly, profileColor=$profileColor, userName='$username', cover=$cover, country=$country, isMutual=$isMutual, groups=$groups, statistics=$statistics, rulesets=$rulesets, supportLevel=$supportLevel, team=$team)"
    }

    fun toOsuUser(mode: OsuMode): OsuUser {
        val micro = this

        val m = mode.orElse(OsuMode.OSU)

        // 批量接口用 statistics_rulesets 携带四个模式的统计，单用户接口的 statistics 作为兜底。
        // safeModeValue 会把 relax / 7K 归一到主模式，再去 rulesets 里取对应统计。
        // 分键（4K / 7K）的 pp 和排名保留在 Statistics.pp4K / pp7K 等字段里，主 pp 仍然是总 pp。
        val rulesets = micro.rulesets
        val statistics = rulesets?.let {
            when (m.safeModeValue) {
                0.toByte() -> it.osu
                1.toByte() -> it.taiko
                2.toByte() -> it.fruits
                else -> it.mania
            }
        } ?: micro.statistics

        return OsuUser(userID).apply {
            micro.avatarUrl?.let { avatarUrl = it }
            micro.coverUrl?.let { coverUrl = it }
            micro.cover?.url?.let { coverUrl = it }

            defaultGroup = micro.defaultGroup
            isActive = micro.isActive
            isBot = micro.isBot
            isDeleted = micro.isDeleted
            isOnline = micro.isOnline
            isSupporter = micro.isSupporter
            lastVisit = micro.lastVisitTime?.atOffset(ZoneOffset.ofHours(8))
            pmFriendsOnly = micro.pmFriendsOnly == true
            profileColor = micro.profileColor
            username = micro.username
            cover = micro.cover
            countryCode = micro.countryCode.ifBlank { micro.country?.code.orEmpty() }
            country = micro.country
            groups = micro.groups

            this.statistics = statistics

            supportLevel = micro.supportLevel ?: 0.toByte()
            team = micro.team

            // 批量接口没有 playmode，把请求的模式当作默认模式，保证 user.mode 和请求一致。
            defaultMode = m
            this.mode = m
        }
    }

    companion object {
        private val formatter: DateTimeFormatter = DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .appendLiteral("T")
            .appendPattern("HH:mm:ss")
            .appendZoneId()
            .toFormatter()
    }
}
