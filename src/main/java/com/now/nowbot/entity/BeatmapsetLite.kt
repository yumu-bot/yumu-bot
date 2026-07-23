package com.now.nowbot.entity

import jakarta.persistence.*

@Entity
@Table(name = "osu_mapset", indexes = [Index(name = "raw", columnList = "map_id")])
class BeatmapsetLite {
    @Id
    @Column(name = "map_id")
    var beatmapsetID: Int = 0

    @Column(columnDefinition = "TEXT")
    var artist: String = ""

    @Column(name = "artist_unicode", columnDefinition = "TEXT")
    var artistUnicode: String = ""

    //四种 covers:{}
    @Column(columnDefinition = "TEXT")
    var cover: String = ""

    @Column(columnDefinition = "TEXT")
    var card: String = ""

    @Column(columnDefinition = "TEXT")
    var list: String = ""

    @Column(columnDefinition = "TEXT")
    var slimcover: String = ""

    //属性
    @Column(columnDefinition = "TEXT")
    var creator: String = ""

    @Column(name = "favourite_count")
    var favourite: Int = 0

    @Column(name = "nsfw")
    var nsfw: Boolean = false

    @Column(name = "play_count")
    var playCount: Long = 0

    @Column(name = "preview_url", columnDefinition = "TEXT")
    var previewUrl: String = ""

    @Column(name = "legacy_thread_url", columnDefinition = "TEXT")
    var legacyUrl: String? = null

    @Column(columnDefinition = "TEXT")
    var status: String = ""

    @Column(columnDefinition = "TEXT")
    var source: String = ""

    // 好像没有写存取哦
    @Column(columnDefinition = "TEXT")
    var tags: String = ""

    @Column(columnDefinition = "TEXT")
    var title: String = ""

    @Column(name = "title_unicode", columnDefinition = "TEXT")
    var titleUnicode: String = ""

    @Column(name = "user_id")
    var mapperID: Int = 0

    var storyboard: Boolean? = null

    @Column(name = "download_disabled")
    var availabilityDownloadDisabled: Boolean? = false
}
