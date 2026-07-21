package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.entity.IDUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.Companion.getMode
import com.now.nowbot.util.DataUtil
import tools.jackson.databind.JsonNode
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL) @JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class OsuUser(
    @get:JsonProperty("user_id")
    @set:JsonProperty("id")
    @set:JsonAlias("user_id")
    override var userID: Long = 0L
): IDUser {
    @JsonProperty("avatar_url")
    var avatarUrl: String = ""

    @JsonProperty("country_code")
    var countryCode: String = ""

    @JsonProperty("default_group")
    var defaultGroup: String? = ""

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

    @JsonProperty("is_restricted")
    var isRestricted: Boolean = false

    @JsonProperty("last_visit")
    var lastVisit: OffsetDateTime? = null

    @JsonProperty("pm_friends_only")
    var pmFriendsOnly: Boolean = false

    @JsonProperty("profile_colour")
    var profileColor: String? = null

    @JsonProperty("username")
    override var username: String = ""

    // Optional attributes
    @JsonProperty("cover_url")
    var coverUrl: String = ""

    var discord: String? = ""

    @JsonProperty("has_supported")
    var hasSupported: Boolean = false

    var interests: String? = ""

    @JsonProperty("join_date")
    var joinDate: OffsetDateTime? = null

    var location: String? = ""

    @JsonProperty("max_blocks")
    var maxBlocks: Int = 0

    @JsonProperty("max_friends")
    var maxFriends: Int = 0

    var occupation: String? = ""

    /**
     * 注意，如果以其他模式请求 OsuUser，这里依旧是玩家的默认模式。需要获得其他模式请使用 getCurrentOsuMode
     * 所以尽量不要用这个。如果你一定要用，那肯定是请求玩家的默认模式
     * 保留是因为这个类已经计入数据库。如果你能修改，请帮忙改掉
     * @return 默认游戏模式
     */
    /**
     * 不要用这个
     */
    @JsonProperty("playmode")
    var mode: String = ""

    @JsonProperty("mode", access = JsonProperty.Access.READ_ONLY)
    var currentOsuMode: OsuMode = OsuMode.DEFAULT
        get() = if (rankHistory != null) {
            getMode(rankHistory!!.mode, defaultOsuMode)
        } else {
            getMode(getMode(mode, defaultOsuMode), field)
        }
        
        set(mode) {
            if (rankHistory == null) {
                rankHistory = RankHistory(mode.shortName, listOf())
            }
            
            field = mode
        }

    @JsonProperty("playstyle")
    var playStyle: List<String>? = listOf()

    @JsonProperty("post_count")
    var postCount: Int = 0

    @JsonProperty("profile_hue")
    var profileHue: Int = 0
     // 这个很重要，是新增的撒泼特自设功能。只要面板知道你的色相，即可生成对应的面板类型。
    // 区域 0-255，可以为 null

    @JsonProperty("profile_order")
    var profileOrder: List<String>? = listOf()

    var title: String? = null

    @JsonProperty("title_url")
    var titleUrl: String? = null

    var twitter: String? = null

    var website: String? = null

    @JsonProperty("country")
    var country: Country? = null

    data class Country(val code: String, val name: String)

    @JsonProperty("cover")
    var cover: Cover? = null

    @JsonProperty("kudosu")
    var kudosu: Kudosu? = Kudosu(0, 0)

    data class Kudosu(val available: Int, val total: Int)

    @JsonProperty("account_history")
    var accountHistory: List<UserAccountHistory>? = null

    //type: note, restriction, silence.
    data class UserAccountHistory(
        val description: String?, val id: Long, val length: Int, val permanent: Boolean,
        val timestamp: OffsetDateTime, val type: String
    )

    /*

   @JsonProperty("active_tournament_banner")
   @Nullable
   @Deprecated
   ProfileBanner profileBanner;

    */
    data class ProfileBanner(
        val id: Long,
        @field:JsonProperty("tournament_id") val tournamentID: Long,
        val image: String?,
        @field:JsonProperty("image@2x") val image2x: String?
    )


    @JsonProperty("active_tournament_banners")
    var profileBanners: List<ProfileBanner> = listOf()

    var badges: List<UserBadge> = listOf()


    data class UserBadge(
        @field:JsonProperty("awarded_at") val awardAt: OffsetDateTime,
        @field:JsonProperty("description") val description: String,
        @field:JsonProperty("image@2x_url") val image2xUrl: String,
        @field:JsonProperty("image_url") val imageUrl: String,
        @field:JsonProperty("url") val url: String?
    )

    @JsonProperty("beatmap_playcounts_count")
    var beatmapPlaycount: Int = 0

    @JsonProperty("comments_count")
    var commentsCount: Int = 0

    // current_season_stats

    @JsonProperty("daily_challenge_user_stats")
    var dailyChallenge: DailyChallenge? = null

    data class DailyChallenge(
        @field:JsonProperty("daily_streak_best")
        val bestDayStreak: Int,
        @field:JsonProperty("daily_streak_current")
        val currentDayStreak: Int,
        @field:JsonProperty("last_update")
        val lastUpdate: OffsetDateTime,
        @field:JsonProperty("last_weekly_streak")
        val lastWeeklyStreak: OffsetDateTime,
        @field:JsonProperty("playcount")
        val playCount: Int,
        @field:JsonProperty("top_10p_placements")
        val top10PercentCount: Int,
        @field:JsonProperty("top_50p_placements")
        val top50PercentCount: Int,
        @field:JsonProperty("user_id")
        val userID: Int,
        @field:JsonProperty("weekly_streak_best")
        val bestWeekStreak: Int,
        @field:JsonProperty("weekly_streak_current")
        val currentWeekStreak: Int
    )

    @JsonProperty("favourite_beatmapset_count")
    var favoriteCount: Int = 0

    @JsonProperty("follower_count")
    var followerCount: Int = 0

    @JsonProperty("graveyard_beatmapset_count")
    var graveyardCount: Int = 0

    @JsonProperty("groups")
    var groups: List<UserGroup> = listOf()

    data class UserGroup(
        @field:JsonProperty("colour")
        val color: String? = "",

        @field:JsonProperty("has_listing")
        val hasListing: Boolean = false,

        @field:JsonProperty("has_playmodes")
        val hasModes: Boolean = false,

        @field:JsonProperty("id")
        val id: Int = 0,

        @field:JsonProperty("identifier")
        val identifier: String = "",

        @field:JsonProperty("is_probationary")
        val isProbationary: Boolean = false,

        @field:JsonProperty("name")
        val name: String = "",

        @field:JsonProperty("short_name")
        val shortName: String = "",

        @field:JsonProperty("playmodes")
        val modes: List<String>? = null,
    )

    @JsonProperty("guest_beatmapset_count")
    var guestCount: Int = 0

    @JsonProperty("loved_beatmapset_count")
    var lovedCount: Int = 0

    @JsonProperty("mapping_follower_count")
    var mappingFollowerCount: Int = 0

    @JsonProperty("matchmaking_stats")
    var matchmakingStats: List<MatchmakingStats> = listOf()

    @get:JsonProperty("matchmaking_highest_rank")
    val matchmakingHighestRank: Int
        get() = matchmakingStats.minOfOrNull { it.rank } ?: 0

    @get:JsonProperty("matchmaking_rating")
    val matchmakingRating: Int
        get() = matchmakingStats.firstOrNull()?.rating ?: 0

    data class MatchmakingStats(
        @field:JsonProperty("first_placements")
        var firstPlacements: Int = 0,

        @field:JsonProperty("is_rating_provisional")
        var provisional: Boolean = true,

        @field:JsonProperty("plays")
        var plays: Int = 0,

        @field:JsonProperty("pool_id")
        var poolID: Int = 0,

        @field:JsonProperty("rank")
        var rank: Int = 0,

        @field:JsonProperty("rating")
        var rating: Int = 0,

        @field:JsonProperty("total_points")
        var totalPoints: Int = 0,

        @set:JsonProperty("user_id")
        @get:JsonIgnore
        var userID: Long = 0L,

        @field:JsonProperty("pool")
        var pool: MatchmakingPool = MatchmakingPool()
    )

    data class MatchmakingPool(
        @field:JsonProperty("active")
        var active: Boolean = false,

        @field:JsonProperty("id")
        var poolID: Long = 0L,

        @field:JsonProperty("name")
        var name: String = "",

        @field:JsonProperty("ruleset_id")
        var rulesetID: Byte = 0,

        @field:JsonProperty("variant_id")
        var variantID: Byte = 0,
    )

    @get:JsonProperty("monthly_playcounts")
    @set:JsonIgnore
    var monthlyPlaycounts: List<UserMonthly> = emptyList()

    @set:JsonProperty("monthly_playcounts")
    @get:JsonIgnore
    var monthlyPlaycountsRaw: JsonNode? = null
        set(value) {
            field = value

            if (value != null && value.isArray) {
                this.monthlyPlaycounts = value.asIterable().map { node ->
                    val startDate = node.path("start_date").asString("")
                    val count = node.path("count").asInt(0)
                    UserMonthly(startDate, count)
                }
            }
        }

    data class UserMonthly(@field:JsonProperty("start_date") val startDate: String, val count: Int)

    @JsonProperty("nominated_beatmapset_count")
    var nominatedCount: Int = 0

    // 暂时不用这个类，字符太多了
    @get:JsonIgnore
    var page: Page? = null

    data class Page(val html: String, val raw: String)

    @JsonProperty("pending_beatmapset_count")
    var pendingCount: Int = 0

    @JsonProperty("previous_usernames")
    var previousNames: List<String>? = listOf()

    @JsonProperty("rank_highest")
    var highestRank: HighestRank? = HighestRank()

    data class HighestRank(
        @field:JsonProperty("rank")
        val rank: Long = 0,

        @field:JsonProperty("updated_at")
        val updatedAt: OffsetDateTime? = null
    )

    @JsonProperty("ranked_beatmapset_count")
    var rankedCount: Int = 0

    @get:JsonIgnore
    var replaysWatchedCounts: List<UserMonthly> = listOf()

    @JsonProperty("replays_watched_counts") fun setReplaysWatchedCount(dataList: List<HashMap<String, Any>>) {
        replaysWatchedCounts = dataList.map { UserMonthly(it["start_date"] as String, it["count"] as Int) }

        this.statistics?.let { stat ->
            stat.replaysWatchedByOthers =
                replaysWatchedCounts.sumOf { it.count }
        }
    }

    @JsonProperty("scores_best_count")
    var scoreBestCount: Int = 0

    @JsonProperty("scores_first_count")
    var scoreFirstCount: Int = 0

    @JsonProperty("scores_pinned_count")
    var scorePinnedCount: Int = 0

    @JsonProperty("scores_recent_count")
    var scoreRecentCount: Int = 0

    @JsonProperty("statistics")
    var statistics: Statistics? = null

    @JsonProperty("support_level")
    var supportLevel: Byte = 0

    @JsonProperty("team")
    var team: Team? = null

    @JsonProperty("user_achievements")
    var userAchievements: List<UserAchievement>? = null

    @get:JsonProperty("user_achievements_count")
    var userAchievementsCount: Int = 0
        get() = if (field > 0) field else userAchievements?.size ?: 0

    data class UserAchievement(
        @field:JsonProperty("achieved_at") val achievedAt: OffsetDateTime,
        @field:JsonProperty("achievement_id") val achievementID: Int
    )

    @JsonProperty("rank_history")
    var rankHistory: RankHistory? = null

    data class RankHistory(val mode: String, val data: List<Long>)

    // ranked 和 pending
    /*
    @JsonProperty("ranked_and_approved_beatmapset_count")
    Integer rankedAndApprovedCount;

    @JsonProperty("unranked_beatmapset_count")
    Integer unrankedCount;

     */
    //自己算

    @get:JsonProperty("pp")
    var pp: Double = 0.0
        get() = statistics?.pp ?: field

    @get:JsonProperty("estimate_pp")
    var ppEstimate: Double = 0.0

    constructor(username: String) : this() {
        this.username = username
    }

    constructor(statistics: Statistics) : this() {
        this.statistics = statistics
    }

    constructor(id: Long, pp: Double) : this() {
        this.userID = id
        this.pp = pp
    }

    constructor(username: String, pp: Double) : this() {
        this.username = username
        this.pp = pp
    }

    constructor(username: String, id: Long, pp: Double) : this() {
        this.userID = id
        this.username = username
        this.pp = pp
    }

    constructor(user: MicroUser) : this() {
        this.username = user.username
        this.userID = user.userID
        user.avatarUrl?.let { this.avatarUrl = it }
        this.countryCode = user.countryCode
        this.country = user.country
        this.cover = user.cover
        this.profileColor = user.profileColor
    }

    @get:JsonIgnore
    var defaultOsuMode: OsuMode
        get() = getMode(mode)
        set(mode) {
            this.mode = mode.shortName
        }

    @get:JsonProperty("accuracy")
    val accuracy: Double
        get() = statistics?.accuracy ?: 0.0

    @get:JsonProperty("play_count")
    val playCount: Long
        get() = statistics?.playCount ?: 0L

    @get:JsonProperty("play_time")
    val playTime: Long
        get() = statistics?.playTime ?: 0L

    @get:JsonProperty("total_hits")
    val totalHits: Long
        get() = statistics?.totalHits ?: 0L

    @get:JsonProperty("max_combo")
    val maxCombo: Int
        get() = statistics?.maxCombo ?: 0

    @get:JsonProperty("global_rank")
    val globalRank: Long
        get() = statistics?.globalRank ?: 0L

    @get:JsonProperty("country_rank")
    val countryRank: Long
        get() = statistics?.countryRank ?: 0L

    @get:JsonProperty("level_current")
    val levelCurrent: Short
        get() = statistics?.levelCurrent ?: 0

    @get:JsonProperty("level_progress")
    val levelProgress: Short
        get() = statistics?.levelProgress ?: 0

    override fun toString(): String {
        return "OsuUser(avatarUrl=$avatarUrl, countryCode=$countryCode, id=$userID, isActive=$isActive, isBot=$isBot, isDeleted=$isDeleted, isOnline=$isOnline, isSupporter=$isSupporter, isRestricted=$isRestricted, pmFriendsOnly=$pmFriendsOnly, username=$username, coverUrl=$coverUrl, discord=$discord, hasSupported=$hasSupported, interests=$interests, joinDate=$joinDate, location=$location, maxBlocks=$maxBlocks, maxFriends=$maxFriends, occupation=$occupation, mode=$mode, playStyle=$playStyle, postCount=$postCount, profileHue=$profileHue, profileOrder=$profileOrder, twitter=$twitter, website=$website, country=$country, cover=$cover, kudosu=$kudosu, profileBanners=$profileBanners, badges=$badges, beatmapPlaycount=$beatmapPlaycount, commentsCount=$commentsCount, dailyChallenge=$dailyChallenge, favoriteCount=$favoriteCount, followerCount=$followerCount, graveyardCount=$graveyardCount, groups=$groups, guestCount=$guestCount, lovedCount=$lovedCount, mappingFollowerCount=$mappingFollowerCount, monthlyPlaycounts=$monthlyPlaycounts, nominatedCount=$nominatedCount, page=$page, pendingCount=$pendingCount, previousNames=$previousNames, highestRank=$highestRank, rankedCount=$rankedCount, replaysWatchedCounts=$replaysWatchedCounts, scoreBestCount=$scoreBestCount, scoreFirstCount=$scoreFirstCount, scorePinnedCount=$scorePinnedCount, scoreRecentCount=$scoreRecentCount, statistics=$statistics, supportLevel=$supportLevel, userAchievements=$userAchievements, rankHistory=$rankHistory)"
    }

    fun toCSV(): String {
        return "${getUserName(username)},${userID},${statistics?.pp},${statistics?.pp4K},${statistics?.pp7K},${statistics?.accuracy},${statistics?.rankedScore},${statistics?.totalScore},${statistics?.playCount},${statistics?.playTime},${statistics?.totalHits},${avatarUrl},${countryCode},${defaultGroup},${isActive},${isBot},${isDeleted},${isOnline},${isSupporter},${isRestricted},${lastVisit},${pmFriendsOnly},${profileColor},${coverUrl},${replaceCommas(discord)},${hasSupported},${replaceCommas(interests)},${joinDate},${replaceCommas(location)},${maxBlocks},${maxFriends},${replaceCommas(occupation)},${mode},${playStyle?.joinToString(",")},${postCount},${profileOrder?.joinToString(",")},${title},${titleUrl},${twitter},${website},${country?.name},${cover?.custom},${kudosu?.total},${beatmapPlaycount},${commentsCount},${favoriteCount},${followerCount},${graveyardCount},${guestCount},${lovedCount},${mappingFollowerCount},${nominatedCount},${pendingCount},${previousNames?.joinToString(",")},${highestRank?.rank},${rankedCount},${replaysWatchedCounts.size},${scoreBestCount},${scoreFirstCount},${scorePinnedCount},${scoreRecentCount},${supportLevel},${userAchievements?.size}"
    }

    private fun getUserName(username: String): String {
        return if (username.startsWith("- ")) "'$username"
        else username
    }

    private fun replaceCommas(str: String?): String {
        return str?.replace(",", "/") ?: ""
    }

    data class Team(
        @field:JsonProperty("flag_url") val flag: String?,
        @field:JsonProperty("id") val id: Int,
        @field:JsonProperty("name") val name: String,
        @field:JsonProperty("short_name")val shortName: String
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OsuUser) return false

        return userID == other.userID
    }

    override fun hashCode(): Int {
        return userID.hashCode()
    }

    @JsonIgnore
    fun updateEstimatedPP(bestMap: Map<*, LazerScore>) {
        updateEstimatedPP(bestMap.values)
    }

    @JsonIgnore
    fun updateEstimatedPP(bests: Collection<LazerScore>) {
        if (this.pp < 1e-4) {
            val estimateBestsPP = DataUtil.getBestsPP(bests)
            val estimateBonusPP = DataUtil.getBonusPP(beatmapPlaycount)

            ppEstimate = estimateBestsPP + estimateBonusPP
        } else {
            ppEstimate = this.pp
        }
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
            .withZone(ZoneOffset.UTC)

        /**
         * List<OsuUser> 去重方法
         *
         * @param to   要合并进去 List
         * @param from 要用来合并的 List
         * @return 合并好的 List
        </OsuUser> */

        fun merge2OsuUserList(to: List<OsuUser>, from: List<OsuUser>): List<OsuUser> {
            if (to.isEmpty()) {
                return from
            }

            if (from.isEmpty()) {
                return to
            }

            val toSet = HashSet(to)
            val fromSet = HashSet(from)

            if (!(toSet.containsAll(fromSet) || fromSet.isEmpty())) {
                toSet.addAll(fromSet)
                return toSet.toList()
            }

            return to
        }
    }
}
