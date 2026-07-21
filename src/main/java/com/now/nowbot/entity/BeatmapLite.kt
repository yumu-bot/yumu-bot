package com.now.nowbot.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.Companion.toOsuMode
import com.now.nowbot.model.osu.Beatmap
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import java.sql.Types

@Entity
@Table(
    name = "osu_beatmap",
    indexes = [Index(name = "map_find", columnList = "map_id"), Index(name = "sid", columnList = "map_id, id")]
)
class BeatmapLite {
    @Id
    @Column(name = "id")
    var beatmapID: Long? = null

    @Column(name = "map_id", insertable = false, updatable = false)
    var beatmapsetID: Int? = null

    @Column(name = "mapper_id")
    var mapperID: Long? = null

    @Column(name = "is_convert")
    var convert: Boolean? = null

    @Column(name = "version", columnDefinition = "text")
    var difficultyName: String? = null

    @Column(columnDefinition = "text")
    var status: String? = null

    var playcount: Int? = null

    var passcount: Int? = null

    //四维
    //accuracy值
    var od: Float? = null
    var cs: Float? = null
    var ar: Float? = null

    //drain值
    var hp: Float? = null

    @Column(name = "difficulty_rating")
    var difficultyRating: Float? = null
    @Column(name = "bpm")
    var bpm: Float? = null
    @Column(name = "max_combo")
    var maxCombo: Int? = null

    //物件数
    var circles: Int? = null
    var sliders: Int? = null
    var spinners: Int? = null

    //秒
    @Column(name = "total_length")
    var totalLength: Int? = null
    @Column(name = "hit_length")
    var hitLength: Int? = null

    //mode_init 0->osu ...
    @Column(name = "mode_int")
    var modeInt: Int? = null

    var ranked: Int? = null

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "check_str", columnDefinition = "char(32)")
    var md5: String? = null

    @ManyToOne
    @JoinColumn(name = "map_id")
    var mapSet: BeatmapsetLite? = null

    constructor(beatmap: Beatmap) {
        this.beatmapID = beatmap.beatmapID
        this.beatmapsetID = beatmap.beatmapsetID.toInt()
        this.convert = beatmap.convert
        this.difficultyName = beatmap.difficultyName
        this.playcount = beatmap.playCount
        this.passcount = beatmap.passCount
        this.od = beatmap.od
        this.cs = beatmap.cs
        this.ar = beatmap.ar
        this.hp = beatmap.hp
        this.difficultyRating = beatmap.starRating.toFloat()
        this.bpm = beatmap.bpm
        this.maxCombo = beatmap.maxCombo
        this.status = beatmap.status
        this.circles = beatmap.circles
        this.sliders = beatmap.sliders
        this.spinners = beatmap.spinners
        this.totalLength = beatmap.totalLength
        this.hitLength = beatmap.hitLength
        this.modeInt = beatmap.modeInt
        this.mapperID = beatmap.mapperID
        this.md5 = beatmap.md5
    }

    @get:JsonIgnore
    val mode: OsuMode
        get() = modeInt.toOsuMode()

    fun toBeatmap(): Beatmap {
        val b = Beatmap()
        b.beatmapID = this.beatmapID!!
        b.beatmapsetID = this.beatmapsetID!!.toLong()
        b.convert = this.convert
        b.difficultyName = this.difficultyName!!
        b.playCount = this.playcount!!
        b.passCount = this.passcount!!
        b.od = this.od
        b.cs = this.cs
        b.ar = this.ar
        b.hp = this.hp
        b.starRating = this.difficultyRating!!.toDouble()
        b.bpm = this.bpm!!
        b.maxCombo = this.maxCombo
        b.status = this.status!!
        b.circles = this.circles
        b.sliders = this.sliders
        b.spinners = this.spinners
        b.totalLength = this.totalLength!!
        b.hitLength = this.hitLength
        b.modeInt = this.modeInt
        b.mapperID = this.mapperID!!
        b.md5 = this.md5
        return b
    }

    interface BeatmapHitLengthResult {
        val id: Long
        val length: Int
    }
}
