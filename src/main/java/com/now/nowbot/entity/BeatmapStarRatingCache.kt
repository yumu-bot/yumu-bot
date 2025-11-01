package com.now.nowbot.entity

import com.now.nowbot.model.enums.OsuMode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable

@Entity
@Table(name = "osu_beatmap_star")
@IdClass(BeatmapStarRatingCache.BeatmapStarRatingKey::class)
class BeatmapStarRatingCache(
    @Id
    @Column("id")
    var id: Long,

    @Id
    @Column("mode")
    var mode: Byte,

    @Id
    @Column("mods")
    var mods: Int,


    @Column("star")
    var star: Float
) {
    @Suppress("UNUSED")
    constructor(bid: Long, mode: OsuMode, mods: Int, star: Float): this(bid, mode.modeValue, mods, star)

    data class BeatmapStarRatingKey(
        var id: Long,
        var mode: Byte,
        var mods: Int,
    ) : Serializable {
        @Suppress("UNUSED")
        constructor() : this(0, 0, 0)
    }
}