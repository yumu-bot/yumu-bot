package com.now.nowbot.entity

import com.now.nowbot.model.enums.OsuMode
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable

@Entity
@Table(name = "osu_beatmap_star_rating_cache")
@IdClass(BeatmapStarRatingCache.BeatmapStarRatingKey::class)
class BeatmapStarRatingCache(
    @Id
    var id: Long,

    @Id
    var mode: Short,

    @Id
    var mods: Int,

    var star: Double
) {
    constructor(bid:Long, mode: OsuMode, mods: Int, star:Double): this(bid, mode.modeValue.toShort(), mods, star)
    data class BeatmapStarRatingKey(
        var id: Long,
        var mode: Short,
        var mods: Int,
    ) : Serializable {
        constructor() : this(0, 0, 0)
    }
}