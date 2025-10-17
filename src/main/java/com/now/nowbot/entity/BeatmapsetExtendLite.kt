package com.now.nowbot.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "osu_extend_beatmapset")
class BeatmapsetExtendLite (
    @Id
    @Column(name = "beatmapset_id")
    val beatmapsetID: Long = -1,

    // 一对多关系
    @OneToMany(mappedBy = "beatmapset", fetch = FetchType.LAZY)
    val beatmaps: List<BeatmapExtendLite> = emptyList(),

    @Column(name = "artist", columnDefinition = "TEXT")
    val artist: String = "",

    @Column(name = "artist_unicode", columnDefinition = "TEXT")
    val artistUnicode: String = "",

    @Column(name = "cover_cache", nullable = true)
    val coverID: Long? = null,

    @Column(name = "creator", columnDefinition = "VARCHAR(20)")
    val creator: String = "",

    @Column(name = "favourite_count")
    var favouriteCount: Long = -1,

    @Column(name = "genre_id")
    val genreID: Byte = -1,

    @Column(name = "language_id")
    val languageID: Byte = -1,

    @Column(name = "nsfw")
    val nsfw: Boolean = false,

    // PSQL 中，offset 是一个保留字
    @Column(name = "recommend_offset")
    var recommendOffset: Short = 0,

    @Column(name = "play_count")
    var playCount: Long = -1,

    @Column(name = "source", columnDefinition = "TEXT")
    val source: String = "",

    @Column(name = "spotlight")
    var spotlight: Boolean = false,

    @Column(name = "status", columnDefinition = "VARCHAR(15)")
    val status: String = "",

    @Column(name = "title", columnDefinition = "TEXT")
    val title: String = "",

    @Column(name = "title_unicode", columnDefinition = "TEXT")
    val titleUnicode: String = "",

    @Column(name = "track_id", nullable = true)
    var trackID: Int? = null,

    @Column(name = "user_id")
    val creatorID: Long = -1,

    @Column(name = "video")
    val video: Boolean = false,

    @Column(name = "bpm")
    val bpm: Float = 0f,

    @Column(name = "discussion_locked")
    var discussionLocked: Boolean = false,

    @Column(name = "last_updated")
    val lastUpdated: LocalDateTime = LocalDateTime.now(),

    @Column(name = "legacy_thread_url_id", nullable = true)
    val threadID: Long? = null,

    @Column(name = "nominations_current", nullable = true)
    val nominationsCurrent: Byte? = null,

    @Column(name = "nominations_rulesets", nullable = true)
    val nominationsRulesets: Byte? = null,

    @Column(name = "nominations_required_main", nullable = true)
    val nominationsRequiredMain: Byte? = null,

    @Column(name = "nominations_required_secondary", nullable = true)
    val nominationsRequiredSecondary: Byte? = null,

    @Column(name = "ranked")
    val ranked: Byte = 0,

    @Column(name = "ranked_date", nullable = true)
    val rankedDate: LocalDateTime? = null,

    @Column(name = "rating")
    var rating: Float = 0f,

    @Column(name = "storyboard")
    val storyboard: Boolean = false,

    @Column(name = "submitted_date")
    val submittedDate: LocalDateTime = LocalDateTime.now(),

    @Column(name = "tags", columnDefinition = "TEXT")
    val tags: String = "",

    @Column(name = "availability_download_disabled")
    val downloadDisabled: Boolean = false,

    @Column(name = "availability_more_information", nullable = true)
    val moreInformation: String? = null,

    @Column(name = "ratings", columnDefinition = "INTEGER[]")
    var ratings: Array<Int> = arrayOf(),

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)