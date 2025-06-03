package com.now.nowbot.entity

import com.now.nowbot.model.osu.Beatmapset
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity @Table(name = "beat_map_set_info")
class BeatMapSetInfoLite {
    @Id @Column(name = "id", nullable = false)
    private var sid: Int? = null

    //@Lob
    @Column(columnDefinition = "TEXT")
    private var artistUnicode: String? = null

    //@Lob
    @Column(columnDefinition = "TEXT")
    private var artist: String? = null

    //@Lob
    @Column(columnDefinition = "TEXT") var titleUnicode: String? = null

    //@Lob
    @Column(columnDefinition = "TEXT")
    private var title: String? = null

    //@Lob
    @Column(columnDefinition = "TEXT")
    private var mapper: String? = null

    var mapperId: Int? = null

    //@Lob
    @Column(columnDefinition = "TEXT")
    private var status: String? = null

    var video: Boolean? = null

    var nsfw: Boolean? = null

    //@Lob
    @Column(columnDefinition = "TEXT")
    private var cover: String? = null

    //@Lob
    @Column(columnDefinition = "TEXT")
    var card: String? = null

    //@Lob
    @Column(columnDefinition = "TEXT")
    var list: String? = null

    //@Lob
    @Column(columnDefinition = "TEXT")
    var slimcover: String? = null

    companion object {
        fun from(s: Beatmapset): BeatMapSetInfoLite {
            val t = BeatMapSetInfoLite()
            t.sid = Math.toIntExact(s.beatmapsetID)

            t.artistUnicode = s.artistUnicode
            t.artist = s.artist
            t.title = s.title

            t.mapper = s.creator
            t.mapperId = Math.toIntExact(s.creatorID)

            t.nsfw = s.nsfw
            t.video = s.video
            t.status = s.status

            var url = s.covers.cover2x
            if (url.isNotBlank()) {
                t.cover = url
            } else {
                t.cover = s.covers.cover
            }

            url = s.covers.card2x
            if (url.isNotBlank()) {
                t.card = url
            } else {
                t.card = s.covers.card
            }

            url = s.covers.list2x
            if (url.isNotBlank()) {
                t.list = url
            } else {
                t.list = s.covers.list
            }

            url = s.covers.slimcover2x
            if (url.isNotBlank()) {
                t.slimcover = url
            } else {
                t.slimcover = s.covers.slimcover
            }
            return t
        }
    }
}