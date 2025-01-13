package com.now.nowbot.entity

import com.now.nowbot.model.enums.OsuMode
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable

@Entity
@Table(name = "osu_beatmap_start_cache")
@IdClass(BeatmapStartCache.BeatmapStartKey::class)
class BeatmapStartCache(
    @Id
    var id: Long,

    @Id
    var mode: Short,

    @Id
    var mods: Int,

    var star: Double
) {
    constructor(bid:Long, mode: OsuMode, mods: Int, star:Double): this(bid, mode.modeValue, mods, star)
    data class BeatmapStartKey(
        var id: Long,
        var mode: Short,
        var mods: Int,
    ) : Serializable {
        constructor() : this(0, 0, 0)
    }
}