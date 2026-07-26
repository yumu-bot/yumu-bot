package com.now.nowbot.entity

import com.now.nowbot.model.osu.Beatmapset
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "osu_extend_beatmapset", indexes = [Index(name = "index_extend_time", columnList = "beatmapset_id,updated_at")])
class BeatmapsetExtendLite (
    @Id
    @Column(name = "beatmapset_id")
    var beatmapsetID: Long = -1,

    // 一对多关系
    @OneToMany(mappedBy = "beatmapset", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var beatmaps: List<BeatmapExtendLite> = emptyList(),

    @Column(name = "anime_cover")
    var animeCover: Boolean = false,

    @Column(name = "artist", columnDefinition = "TEXT")
    var artist: String = "",

    @Column(name = "artist_unicode", columnDefinition = "TEXT")
    var artistUnicode: String = "",

    @Column(name = "cover_cache", nullable = true)
    var coverID: Long? = null,

    @Column(name = "creator", columnDefinition = "VARCHAR(20)")
    var creator: String = "",

    @Column(name = "favourite_count")
    var favouriteCount: Long = -1,

    @Column(name = "genre_id")
    var genreID: Byte = -1,

    @Column(name = "language_id")
    var languageID: Byte = -1,

    @Column(name = "nsfw")
    var nsfw: Boolean = false,

    // PSQL 中，offset 是一个保留字
    @Column(name = "recommend_offset")
    var recommendOffset: Short = 0,

    @Column(name = "play_count")
    var playCount: Long = -1,

    @Column(name = "source", columnDefinition = "TEXT")
    var source: String = "",

    @Column(name = "spotlight")
    var spotlight: Boolean = false,

    @Column(name = "status", columnDefinition = "VARCHAR(15)")
    var status: String = "",

    @Column(name = "title", columnDefinition = "TEXT")
    var title: String = "",

    @Column(name = "title_unicode", columnDefinition = "TEXT")
    var titleUnicode: String = "",

    @Column(name = "track_id", nullable = true)
    var trackID: Int? = null,

    @Column(name = "user_id")
    var creatorID: Long = -1,

    @Column(name = "video")
    var video: Boolean = false,

    @Column(name = "bpm")
    var bpm: Float = 0f,

    @Column(name = "discussion_locked")
    var discussionLocked: Boolean = false,

    @Column(name = "last_updated")
    var lastUpdated: LocalDateTime = LocalDateTime.now(),

    @Column(name = "legacy_thread_url_id", nullable = true)
    var threadID: Long? = null,

    @Column(name = "nominations_current", nullable = true)
    var nominationsCurrent: Byte? = null,

    @Column(name = "nominations_rulesets", nullable = true)
    var nominationsRulesets: Byte? = null,

    @Column(name = "nominations_required_main", nullable = true)
    var nominationsRequiredMain: Byte? = null,

    @Column(name = "nominations_required_secondary", nullable = true)
    var nominationsRequiredSecondary: Byte? = null,

    @Column(name = "ranked")
    var ranked: Byte = 0,

    @Column(name = "ranked_date", nullable = true)
    var rankedDate: LocalDateTime? = null,

    @Column(name = "rating")
    var rating: Float = 0f,

    @Column(name = "storyboard")
    var storyboard: Boolean = false,

    @Column(name = "submitted_date")
    var submittedDate: LocalDateTime = LocalDateTime.now(),

    @Column(name = "tags", columnDefinition = "TEXT")
    var tags: String = "",

    @Column(name = "availability_download_disabled")
    var downloadDisabled: Boolean = false,

    @Column(name = "availability_more_information", nullable = true)
    var moreInformation: String? = null,

    @Column(name = "ratings", columnDefinition = "int4[]")
    var ratings: Array<Int> = arrayOf(),

    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun Beatmapset.toExtendEntity(): BeatmapsetExtendLite {
            val set = this

            val entity = BeatmapsetExtendLite(
                beatmapsetID = set.beatmapsetID,
                animeCover = set.animeCover,
                artist = set.artist,
                artistUnicode = set.artistUnicode,
                coverID = set.covers.cover
                    .split("?").getOrNull(1)?.toLongOrNull(),
                creator = set.creator,
                favouriteCount = set.favouriteCount,
                genreID = set.genreID,
                creatorID = set.creatorID,
                languageID = set.languageID,
                nsfw = set.nsfw,
                recommendOffset = set.offset,
                playCount = set.playCount,
                source = set.source,
                status = set.status,
                spotlight = set.spotlight,
                title = set.title,
                titleUnicode = set.titleUnicode,
                trackID = set.trackID,
                video = set.video,
                bpm = set.bpm,
                discussionLocked = set.discussionLocked,
                lastUpdated = set.lastUpdated.toLocalDateTime(),
                threadID = set.legacyThreadUrl?.split("/")?.lastOrNull()?.toLongOrNull(),
                nominationsCurrent = set.nominationsSummary?.current,
                nominationsRulesets = set.nominationsSummary?.mode
                    ?.map { mode ->
                        when(mode) {
                            "osu" -> 1
                            "taiko" -> 2
                            "catch", "fruits" -> 4
                            "mania" -> 8
                            else -> 0
                        }
                    }?.sum()?.toByte(),
                nominationsRequiredMain = set.nominationsSummary?.required?.main,
                nominationsRequiredSecondary = set.nominationsSummary?.required?.secondary,
                ranked = set.ranked,
                rankedDate = set.rankedDate?.toLocalDateTime(),
                rating = set.rating,
                storyboard = set.storyboard,
                submittedDate = set.submittedDate.toLocalDateTime(),
                tags = set.tags,
                downloadDisabled = set.availability.downloadDisabled,
                moreInformation = set.availability.moreInformation,
                ratings = set.ratings.toTypedArray(),
            )

            return entity
        }
    }
}