package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.yumu.core.extensions.isNotNull
import jakarta.persistence.Column
import org.springframework.lang.Nullable
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatterBuilder
import kotlin.math.max

class Beatmapset {
    @Column(name = "anime_cover")
    var animeCover: Boolean = false

    @JsonProperty("artist")
    var artist: String = ""

    @JsonProperty("artist_unicode")
    var artistUnicode: String = ""

    @JsonProperty("covers")
    var covers: Covers = Covers()

    @JsonProperty("creator")
    var creator: String = ""

    @JsonProperty("favourite_count")
    var favouriteCount: Long = 0

    @JsonProperty("genre_id")
    var genreID: Byte = 0

    @Nullable
    var hype: Hype? = null

    data class Hype(val current: Int, val required: Int)

    @JsonProperty("id")
    var beatmapsetID: Long = 0L

    @JsonProperty("language_id")
    var languageID: Byte = 0

    @JsonProperty("nsfw")
    var nsfw: Boolean = false

    var offset: Short = 0

    @JsonProperty("play_count")
    var playCount: Long = 0

    @JsonProperty("preview_url")
    var previewUrl: String = ""

    var source: String = ""

    var status: String = ""

    @JsonProperty("spotlight")
    var spotlight: Boolean = false

    var title: String = ""

    @JsonProperty("title_unicode")
    var titleUnicode: String = ""

    @JsonProperty("track_id")
    var trackID: Int? = null

    @JsonProperty("user_id")
    var creatorID: Long = 0

    var video: Boolean = false

    @JsonProperty("bpm")
    var bpm: Float = 0f

    @JsonProperty("can_be_hyped")
    var canBeHyped: Boolean? = null

    @JsonProperty("deleted_at")
    var deletedAt: OffsetDateTime? = null

    //已经弃用 Deprecated, all beatmapsets now have discussion enabled.
 //@JsonProperty("discussion_enabled")
 //Boolean discussionEnabled;
    @JsonProperty("discussion_locked")
    var discussionLocked: Boolean = false

    @JsonProperty("is_scoreable")
    var scoreable: Boolean = false

    @JsonProperty("last_updated")
    var lastUpdated: OffsetDateTime = OffsetDateTime.now()

    @JsonProperty("legacy_thread_url")
    var legacyThreadUrl: String? = null

    @JsonProperty("nominations_summary")
    var nominationsSummary: NominationsSummary? = null
        get(){
            val s = field ?: return null

            val r = s.required

            val secondary: Byte

            if (beatmaps.isNullOrEmpty()) {
                secondary = 0
            } else {
                val formatter = DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd")
                    .appendLiteral("T")
                    .appendPattern("HH:mm:ss")
                    .appendZoneId().toFormatter()

                val changedTime = LocalDateTime.from(formatter.parse("2024-06-03T00:00:00Z"))

                // 没榜，或者最后更新时间晚于这一天的谱面才应用这次更改
                secondary = if (lastUpdated.toLocalDateTime().isAfter(changedTime) || !this.hasLeaderBoard) {
                    max(beatmaps!!.map { it.modeInt }.toSet().size - 1, 0).toByte()
                } else {
                    // 之前的，其他模式要 x2
                    max((beatmaps!!.map { it.modeInt }.toSet().size - 1) * 2, 0).toByte()
                }
            }

            return NominationsSummary(s.current, s.mode, RequiredMeta(r.main, secondary))
        }

     // https://osu.ppy.sh/home/changelog/web/2024.603.0 改了
    data class NominationsSummary (
         @field:JsonProperty("current")
         val current: Byte,

         // 只有一个元素的列表，存储模式信息

         @field:JsonProperty("eligible_main_rulesets")
         val mode: List<String>?,

         @field:JsonProperty("required_meta")
         val required: RequiredMeta

    )

    data class RequiredMeta (
        @field:JsonProperty("main_ruleset")
        val main: Byte,  // 这个不准，需要重新计算并赋值。

        @field:JsonProperty("non_main_ruleset")
        val secondary: Byte

    )

    var ranked: Byte = 0

    @JsonProperty("ranked_date")
    var rankedDate: OffsetDateTime? = null

    var storyboard: Boolean = false

    @JsonProperty("submitted_date")
    var submittedDate: OffsetDateTime = OffsetDateTime.now()

    @JsonProperty("tags")
    var tags: String = ""

    @JsonProperty("availability")
    var availability: Availability = Availability()

    data class Availability (
        @field:JsonProperty("download_disabled")
        val downloadDisabled: Boolean = false,

        @field:JsonProperty("more_information")
        val moreInformation: String? = null
    )

    @JsonProperty("beatmaps")
    // 这里的 beatmaps 类内的 BeatMapSet 是半残品，要什么属性自己写在下面
    var beatmaps: List<Beatmap>? = null
        get() {
            if (!field.isNullOrEmpty()) {

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

                field!!.forEach { it.beatmapset = s }
            }

            return field
        }

    @JsonProperty("converts")
    var converts: List<Beatmap>? = null

    @JsonProperty("current_nominations")
    var currentNominations: List<CurrentNominations>? = null

    data class CurrentNominations (
        @field:JsonProperty("beatmapset_id")
        val beatmapsetID: Long,

        @field:JsonProperty("rulesets")
        val mode: List<String>?,

        val reset: Boolean,

        @field:JsonProperty("user_id")
        val userID: Long
    )

    var description: Description? = null

    data class Description (
        val description: String
    )

    @JsonProperty("genre")
    var genre: Genre? = null

    data class Genre (val id: Int, val name: String)

    @JsonProperty("language")
    var language: Language? = null

    data class Language (val id: Int, val name: String)

    @JsonProperty("pack_tags")
    var packTags: List<String>? = null

    @JsonProperty("ratings")
    var ratings: List<Int> = listOf()

    @JsonProperty("recent_favourites")
    var recentFavourites: List<OsuUser>? = null

    @JsonProperty("related_users")
    var relatedUsers: List<OsuUser>? = null

    @JsonProperty("user")
    var creatorData: OsuUser? = null

    //自己算
    @get:JsonProperty("mappers")
    val mappers: MutableList<OsuUser>
        get(){
            val m = mutableListOf<OsuUser>()

            if (relatedUsers.isNullOrEmpty().not()) {
                for (u in relatedUsers!!) {
                    if ((nominators.isEmpty() || nominators.contains(u).not()) && u.userID != creatorID) {
                        m.add(u)
                    }
                }
            }

            return m
        }

    //自己算
    @get:JsonProperty("nominators")
    val nominators: List<OsuUser>
         get(){
             val n = mutableListOf<OsuUser>()

             if (currentNominations.isNotNull() && relatedUsers.isNotNull()) {
                 for ((_, _, _, userID) in currentNominations!!) {
                     for (u in relatedUsers!!) {
                         if (u.userID == userID) {
                             n.add(u)
                             break
                         }
                     }
                 }
             }

             return n.toList()
         }

    @JsonProperty("rating")
    var rating: Float = 0f

    //不用自己算
    @get:JsonProperty("public_rating")
    val publicRating: Float
        get() = rating
    /*
    val publicRating: Double
        get(){
            if (ratings.isNullOrEmpty()) return 0.0

            var r = 0.0
            val sum = ratings!!.sumOf { it }.toDouble()

            if (sum == 0.0) {
                return 0.0
            }

            for (j in 0 .. 10) {
                r += (j * ratings!![j] / sum)
            }

            return r
        }

     */

    //自己算
    @get:JsonProperty("has_leader_board")
    val hasLeaderBoard: Boolean
         get() {
             return if (status.isNotBlank()) {
                 (status == "ranked" || status == "qualified" || status == "loved" || status == "approved")
             } else {
                 when (ranked.toInt()) {
                     1, 2, 3, 4 -> true
                     else -> false
                 }
             }
         }

    var fromDatabase: Boolean? = null

    @get:JsonProperty("preview_name")
    val previewName: String
        get() = this.artist + " - " + this.title + " (" + this.creator + ") [" + this.beatmapsetID + "]"

    override fun equals(other: Any?): Boolean {
        if (other !is Beatmapset) return false

        return other.beatmapsetID == this.beatmapsetID
    }

    override fun hashCode(): Int {
        return this.beatmapsetID.hashCode()
    }

    //获取最高难度
    @JsonIgnore
    fun getTopDiff(last: Int = 1): Beatmap? {
        return beatmaps?.sortedByDescending { it.starRating }?.getOrNull(last - 1)
    }
}
