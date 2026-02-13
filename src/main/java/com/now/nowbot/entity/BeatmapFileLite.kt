package com.now.nowbot.entity

import com.now.nowbot.model.enums.OsuMode
import jakarta.persistence.*

@Entity
@Table(name = "beatmapfile", indexes = [Index(name = "index_file_sid", columnList = "file_sid")])
class BeatmapFileLite {
    @Id
    @Column(name = "file_bid")
    var bid: Long? = null

    @Column(name = "file_sid")
    var sid: Long? = null

    var background: String? = null

    var audio: String? = null

    @Column(name = "mode")
    var modeInt: Int? = null

    @get:Transient
    val mode: OsuMode
        get() = OsuMode.getMode(modeInt)
}
