package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonProperty
import com.yumu.core.extensions.isNotNull
import org.springframework.lang.Nullable
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatterBuilder
import java.util.*
import kotlin.math.max

class BeatMapSet {
    @JsonProperty("artist")
    var artist: String = ""

    @JsonProperty("artist_unicode")
    var artistUnicode: String = ""

    @JsonProperty("covers")
    var covers: Covers = Covers()

    @JsonProperty("creator")
    var creator: String = ""

    @JsonProperty("favourite_count")
    var favouriteCount: Int = 0

    @Nullable
    var hype: Hype? = null

    @JvmRecord
    data class Hype(val current: Int, val required: Int)

    @JsonProperty("id")
    var beatMapSetID: Long = 0

    @JsonProperty("nsfw")
    var nsfw: Boolean = false

    var offset: Int = 0

    @JsonProperty("play_count")
    var playCount: Long = 0

    @JsonProperty("preview_url")
    var previewUrl: String = ""

    var source: String = ""

    var status: String = ""

    val statusByte: Byte
        get() = when(status) {
            "wip" -> -1
            "pending" -> 0
            "ranked" -> 1
            "approved" -> 2
            "qualified" -> 3
            "loved" -> 4
            else -> -2
        }

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
    var BPM: Double? = null

    @JsonProperty("can_be_hyped")
    var canBeHyped: Boolean? = null

    @JsonProperty("deleted_at")
    var deletedAt: OffsetDateTime? = null

    //已经弃用 Deprecated, all beatmapsets now have discussion enabled.
 //@JsonProperty("discussion_enabled")
 //Boolean discussionEnabled;
    @JsonProperty("discussion_locked")
    var discussionLocked: Boolean? = null

    @JsonProperty("is_scoreable")
    var scoreable: Boolean? = null

    @JsonProperty("last_updated")
    var lastUpdated: OffsetDateTime? = null

    @JsonProperty("legacy_thread_url")
    var legacyThreadUrl: String? = null

    @JsonProperty("nominations_summary")
    val nominationsSummary: NominationsSummary? = null
        get(){
            val s = field ?: return null

            val r = s.required

            val secondary: Int

            if (beatMaps.isNullOrEmpty()) {
                secondary = 0
            } else {
                val formatter = DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd")
                    .appendLiteral("T")
                    .appendPattern("HH:mm:ss")
                    .appendZoneId().toFormatter()

                val changedTime = LocalDateTime.from(formatter.parse("2024-06-03T00:00:00Z"))

                // 没榜，或者最后更新时间晚于这一天的谱面才应用这次更改
                secondary = if (lastUpdated!!.toLocalDateTime().isAfter(changedTime) || !this.hasLeaderBoard) {
                    max(beatMaps!!.map { it.modeInt }.toSet().size - 1, 0)
                } else {
                    // 之前的，其他模式要 x2
                    max((beatMaps!!.map { it.modeInt }.toSet().size - 1) * 2, 0)
                }
            }

            return NominationsSummary(s.current, s.mode, RequiredMeta(r.main, secondary))
        }

     // https://osu.ppy.sh/home/changelog/web/2024.603.0 改了
    @JvmRecord
    data class NominationsSummary (
         val current: Int,  // 只有一个元素的列表，存储模式信息

         @JsonProperty("eligible_main_rulesets")
         val mode: List<String>?,

         @JsonProperty("required_meta")
         val required: RequiredMeta

    )

    @JvmRecord
    data class RequiredMeta (
        @JsonProperty("main_ruleset")
        val main: Int,  // 这个不准，需要重新计算并赋值。

        @JsonProperty("non_main_ruleset")
        val secondary: Int

    )

    var ranked: Int = 0

    @JsonProperty("ranked_date")
    var rankedDate: OffsetDateTime? = null

    var storyboard: Boolean? = null

    @JsonProperty("submitted_date")
    var submittedDate: OffsetDateTime? = null

    var tags: String? = null

    @JsonProperty("availability")
    var availability: Availability? = null

    @JvmRecord
    data class Availability (
        @JsonProperty("download_disabled")
        val downloadDisabled: Boolean,

        @JsonProperty("more_information")
        val moreInformation: String?
    )

    @JsonProperty("beatmaps")
    // 这里的 beatMaps 类内的 BeatMapSet 是半残品，要什么属性自己写在下面
    var beatMaps: List<BeatMap>? = null
        get() {
            if (!field.isNullOrEmpty()) {

                val s = BeatMapSet()

                s.availability = this.availability
                s.beatMapSetID = this.beatMapSetID
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

                field!!.forEach { it.beatMapSet = s }
            }

            return field
        }

    @JsonProperty("converts")
    var converts: List<BeatMap>? = null

    @JsonProperty("current_nominations")
    var currentNominations: List<CurrentNominations>? = null

    data class CurrentNominations (
        @JsonProperty("beatmapset_id")
        val beatMapSetID: Long,

        @JsonProperty("rulesets")
        val mode: List<String>?,

        val reset: Boolean,

        @JsonProperty("user_id")
        val userID: Long
    )

    var description: Description? = null

    @JvmRecord
    data class Description (
        val description: String
    )

    var genre: Genre? = null

    data class Genre (val id: Int, val name: String)

    @JsonProperty("language")
    var language: Language? = null

    @JvmRecord
    data class Language (val id: Int, val name: String)

    @JsonProperty("pack_tags")
    var packTags: List<String>? = null

    @JsonProperty("ratings")
    var ratings: List<Int>? = null

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


    //自己算
    @get:JsonProperty("public_rating")
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

    //自己算
    @get:JsonProperty("has_leader_board")
    val hasLeaderBoard: Boolean
         get() {
             return if (Objects.nonNull(status)) {
                 (status == "ranked" || status == "qualified" || status == "loved" || status == "approved")
             } else {
                 when (ranked) {
                     1, 2, 3, 4 -> true
                     else -> false
                 }
             }
         }

    var fromDatabase: Boolean? = null
    //获取最高难度
    @get:JsonProperty("top_diff")
    val topDiff: BeatMap?
        get() {
            if (beatMaps.isNullOrEmpty()) return null
            if (beatMaps!!.size == 1) return beatMaps!!.first()

            val starComparator =
                Comparator { o1: BeatMap?, o2: BeatMap? -> (o1!!.starRating * 100f - o2!!.starRating * 100f).toInt() }

            return Collections.max(beatMaps!!, starComparator)
        }

    @get:JsonProperty("preview_name")
    val previewName: String
        get() = this.artist + " - " + this.title + " (" + this.creator + ") [" + this.beatMapSetID + "]"

    override fun equals(other: Any?): Boolean {
        if (other !is BeatMapSet) return false

        return other.beatMapSetID == this.beatMapSetID
    }

    override fun hashCode(): Int {
        return this.beatMapSetID.hashCode()
    }
}
