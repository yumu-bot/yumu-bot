package com.now.nowbot.model.osu


import com.fasterxml.jackson.annotation.JsonIgnore
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import jakarta.persistence.Column
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatterBuilder
import kotlin.math.max

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class Beatmapset(
    @Column(name = "anime_cover")
    var animeCover: Boolean = false, 

    @field:JsonProperty("artist")
    var artist: String = "", 

    @field:JsonProperty("artist_unicode")
    var artistUnicode: String = "", 

    @field:JsonProperty("covers")
    var covers: Covers = Covers(), 

    @field:JsonProperty("creator")
    var creator: String = "", 

    @field:JsonProperty("favourite_count")
    var favouriteCount: Long = 0, 

    @field:JsonProperty("genre_id")
    var genreID: Byte = 0, 

    @field:JsonProperty("hype")
    var hype: Hype? = null, 

    @field:JsonProperty("id")
    var beatmapsetID: Long = 0L, 

    @field:JsonProperty("language_id")
    var languageID: Byte = 0, 

    @field:JsonProperty("nsfw")
    var nsfw: Boolean = false,

    @field:JsonProperty("offset")
    var offset: Short = 0, 

    @field:JsonProperty("play_count")
    var playCount: Long = 0, 

    @field:JsonProperty("preview_url")
    var previewUrl: String = "",

    @field:JsonProperty("source")
    var source: String = "",

    @field:JsonProperty("status")
    var status: String = "", 

    @field:JsonProperty("spotlight")
    var spotlight: Boolean = false,

    @field:JsonProperty("title")
    var title: String = "", 

    @field:JsonProperty("title_unicode")
    var titleUnicode: String = "", 

    @field:JsonProperty("track_id")
    var trackID: Int? = null, 

    @field:JsonProperty("user_id")
    var creatorID: Long = 0,

    @field:JsonProperty("video")
    var video: Boolean = false, 

    @field:JsonProperty("bpm")
    var bpm: Float = 0f, 

    @field:JsonProperty("can_be_hyped")
    var canBeHyped: Boolean? = null, 

    @field:JsonProperty("deleted_at")
    var deletedAt: OffsetDateTime? = null,

    @field:JsonProperty("discussion_locked")
    var discussionLocked: Boolean = false, 

    @field:JsonProperty("is_scoreable")
    var scoreAble: Boolean = false, 

    @field:JsonProperty("last_updated")
    var lastUpdated: OffsetDateTime = OffsetDateTime.now(), 

    @field:JsonProperty("legacy_thread_url")
    var legacyThreadUrl: String? = null,

    @field:JsonProperty("ranked")
    var ranked: Byte = 0, 

    @field:JsonProperty("ranked_date")
    var rankedDate: OffsetDateTime? = null,

    @field:JsonProperty("storyboard")
    var storyboard: Boolean = false, 

    @field:JsonProperty("submitted_date")
    var submittedDate: OffsetDateTime = OffsetDateTime.now(), 

    @field:JsonProperty("tags")
    var tags: String = "", 

    @field:JsonProperty("availability")
    var availability: Availability = Availability(), 

    @field:JsonProperty("converts")
    var converts: List<Beatmap>? = null, 

    @field:JsonProperty("current_nominations")
    var currentNominations: List<CurrentNominations>? = null,

    var description: Description? = null, 

    @field:JsonProperty("genre")
    var genre: Genre? = null, 

    @field:JsonProperty("language")
    var language: Language? = null, 

    @field:JsonProperty("pack_tags")
    var packTags: List<String>? = null, 

    @field:JsonProperty("ratings")
    var ratings: List<Int> = listOf(), 

    @field:JsonProperty("recent_favourites")
    var recentFavourites: List<OsuUser>? = null, 

    @field:JsonProperty("related_users")
    var relatedUsers: List<OsuUser>? = null, 

    @field:JsonProperty("user")
    var creatorData: OsuUser? = null, 

    @field:JsonProperty("related_tags")
    var relatedTags: List<Tag> = listOf()
) {

    data class Hype(val current: Int, val required: Int)
    data class Language(val id: Int, val name: String)
    data class Genre(val id: Int, val name: String)
    data class Description(val description: String)

    data class Availability(
        @field:JsonProperty("download_disabled")
        val downloadDisabled: Boolean = false,

        @field:JsonProperty("more_information")
        val moreInformation: String? = null
    )

    data class CurrentNominations(
        @field:JsonProperty("beatmapset_id")
        val beatmapsetID: Long,

        @field:JsonProperty("rulesets")
        val mode: List<String>?,

        @field:JsonProperty("reset")
        val reset: Boolean,

        @field:JsonProperty("user_id")
        val userID: Long
    )

    data class RequiredMeta(
        @field:JsonProperty("main_ruleset")
        val main: Byte,  // 这个不准，需要重新计算并赋值。

        @field:JsonProperty("non_main_ruleset")
        val secondary: Byte

    )

    // https://osu.ppy.sh/home/changelog/web/2024.603.0 改了
    data class NominationsSummary(
        @field:JsonProperty("current")
        val current: Byte,

        // 只有一个元素的列表，存储模式信息

        @field:JsonProperty("eligible_main_rulesets")
        val mode: List<String>?,

        @field:JsonProperty("required_meta")
        val required: RequiredMeta

    )

    @field:JsonProperty("nominations_summary")
    var nominationsSummary: NominationsSummary? = null
        get() {
            val s = field ?: return null

            val r = s.required

            val secondary: Byte
            val bs = beatmaps

            secondary = if (bs.isNullOrEmpty()) {
                0
            } else {
                // 没榜，或者最后更新时间晚于这一天的谱面才应用这次更改
                if (lastUpdated.toLocalDateTime().isAfter(changedTime) || !this.hasLeaderBoard) {
                    max(bs.map { it.modeInt }.toSet().size - 1, 0).toByte()
                } else {
                    // 之前的，其他模式要 x2
                    max((bs.map { it.modeInt }.toSet().size - 1) * 2, 0).toByte()
                }
            }

            return NominationsSummary(s.current, s.mode, RequiredMeta(r.main, secondary))
        }

        @field:JsonProperty("beatmaps")
        var beatmaps: List<Beatmap>? = null
            get() {
                val bs = field

                if (!bs.isNullOrEmpty()) {
                    val s = Beatmapset()

                    s.availability = this.availability
                    s.beatmapsetID = this.beatmapsetID
                    s.creator = this.creator
                    s.creatorID = this.creatorID
                    s.covers = this.covers

                    s.artist = this.artist
                    s.artistUnicode = this.artistUnicode
                    s.title = this.title
                    s.titleUnicode = this.titleUnicode

                    s.ranked = this.ranked
                    s.relatedUsers = this.relatedUsers
                    s.language = this.language
                    s.genre = this.genre
                    s.source = this.source
                    s.status = this.status
                    s.tags = this.tags

                    s.playCount = this.playCount
                    s.favouriteCount = this.favouriteCount

                    s.nsfw = this.nsfw

                    s.submittedDate = this.submittedDate
                    s.rankedDate = this.rankedDate

                    s.packTags = this.packTags

                    s.currentNominations = this.currentNominations

                    bs.forEach { it.beatmapset = s }
                }

                return bs
            }

    //自己算
    @get:JsonProperty("mappers")
    val mappers: List<OsuUser>
        get() {
            val users = relatedUsers ?: return emptyList()

            val nominatorSet = nominators.map { it.userID }.toSet()

            return users.filter { u ->
                u.userID != creatorID && u.userID !in nominatorSet
            }
        }

    //自己算
    @get:JsonProperty("nominators")
    val nominators: List<OsuUser>
        get() {
            val nominations = currentNominations ?: return emptyList()
            val users = relatedUsers ?: return emptyList()

            val userMap = users.associateBy { it.userID }

            return nominations.mapNotNull { (_, _, _, userID) ->
                userMap[userID]
            }
        }

    @field:JsonProperty("rating")
    var rating: Float = 0f
        get() = if (field != 0f) {
            field
        } else if (ratings.isEmpty()) {
            0f
        } else {
            var sum = 0f
            for (i in ratings.indices) {
                sum += i * ratings[i]
            }
            sum / ratings.sum().coerceAtLeast(1)
        }

    //自己算
    @get:JsonProperty("has_leader_board")
    val hasLeaderBoard: Boolean
        get() {
            val firstChar = status.firstOrNull()?.lowercaseChar()

            if (firstChar != null) {
                return firstChar in LEADERBOARD_CHARS
            }
            return ranked in 1..4
        }

    var fromDatabase: Boolean? = null

    @get:JsonProperty("preview_name")
    val previewName: String
        get() = "$artist - $title ($creator)"

    override fun equals(other: Any?): Boolean {
        return other is Beatmapset && other.beatmapsetID == this.beatmapsetID
    }

    override fun hashCode(): Int {
        return this.beatmapsetID.hashCode()
    }

    //获取最高难度
    @JsonIgnore
    fun getTopDiff(last: Int = 1): Beatmap {
        return beatmaps?.sortedByDescending { it.starRating }?.getOrNull(last - 1) ?: throw NoSuchElementException.BeatmapTopDiff(beatmapsetID)
    }

    companion object {
        // 匹配 'r' (ranked), 'q' (qualified), 'l' (loved), 'a' (approved)
        private val LEADERBOARD_CHARS = charArrayOf('r', 'q', 'l', 'a')

        private val formatter = DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .appendLiteral("T")
            .appendPattern("HH:mm:ss")
            .appendZoneId().toFormatter()

        private val changedTime = LocalDateTime.from(formatter.parse("2024-06-03T00:00:00Z"))
    }
}
