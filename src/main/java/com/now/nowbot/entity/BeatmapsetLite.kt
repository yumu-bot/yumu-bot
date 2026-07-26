package com.now.nowbot.entity

import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.model.osu.Covers
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

    companion object {
        fun BeatmapsetLite.toModel(): Beatmapset {
            val set = this

            return Beatmapset().apply {
                beatmapsetID = set.beatmapsetID.toLong()
                creatorID = set.mapperID.toLong()
                creator = set.creator
                covers = Covers(set.cover, set.cover, set.card, set.card, set.list, set.list, set.slimcover, set.slimcover)

                nsfw = set.nsfw
                storyboard = set.storyboard ?: false
                source = set.source
                status = set.status
                playCount = set.playCount
                favouriteCount = set.favourite.toLong()
                title = set.title
                titleUnicode = set.titleUnicode
                artist = set.artist
                artistUnicode = set.artistUnicode
                legacyThreadUrl = set.legacyUrl

                fromDatabase = false
            }
        }

        fun Beatmapset.toEntity(): BeatmapsetLite {
            val mapSet = this

            return BeatmapsetLite().apply {
                beatmapsetID = mapSet.beatmapsetID.toInt()
                card = mapSet.covers.card2x
                cover = mapSet.covers.cover2x
                list = mapSet.covers.list2x
                slimcover = mapSet.covers.slimcover2x

                availabilityDownloadDisabled = mapSet.availability.downloadDisabled
                nsfw = mapSet.nsfw
                storyboard = mapSet.storyboard
                legacyUrl = mapSet.legacyThreadUrl

                mapperID = mapSet.creatorID.toInt()
                creator = mapSet.creator
                source = mapSet.source
                status = mapSet.status
                playCount = mapSet.playCount
                favourite = mapSet.favouriteCount.toInt()
                title = mapSet.title
                titleUnicode = mapSet.titleUnicode
                artist = mapSet.artist
                artistUnicode = mapSet.artistUnicode
            }
        }
    }
}
