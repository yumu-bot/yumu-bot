package com.now.nowbot.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.now.nowbot.entity.BeatmapsetLite.Companion.toEntity
import com.now.nowbot.entity.BeatmapsetLite.Companion.toModel
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
    var beatmapID: Long = -1

    @Column(name = "map_id", insertable = false, updatable = false)
    var beatmapsetID: Int? = null

    @Column(name = "mapper_id")
    var mapperID: Long = -1

    @Column(name = "is_convert")
    var convert: Boolean? = null

    @Column(name = "version", columnDefinition = "text")
    var difficultyName: String = ""

    @Column(columnDefinition = "text")
    var status: String = ""

    var playcount: Int = 0

    var passcount: Int? = null

    //四维
    //accuracy值
    var od: Float? = null
    var cs: Float? = null
    var ar: Float? = null

    //drain值
    var hp: Float? = null

    @Column(name = "difficulty_rating")
    var difficultyRating: Float = 0f

    @Column(name = "bpm")
    var bpm: Float = 0f

    @Column(name = "max_combo")
    var maxCombo: Int? = null

    //物件数
    var circles: Int? = null
    var sliders: Int? = null
    var spinners: Int? = null

    //秒
    @Column(name = "total_length")
    var totalLength: Int = 0

    @Column(name = "hit_length")
    var hitLength: Int? = null

    //mode_init 0->osu ...
    @Column(name = "mode_int")
    var modeInt: Int? = null

    // 空列
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


    interface BeatmapHitLengthResult {
        val id: Long
        val length: Int
    }

    companion object {
        fun BeatmapLite.toModel(): Beatmap {
            val lite = this

            return Beatmap().apply {
                this.beatmapID = lite.beatmapID
                this.beatmapsetID = lite.beatmapsetID?.toLong() ?: -1L
                this.convert = lite.convert
                this.difficultyName = lite.difficultyName
                this.playCount = lite.playcount
                this.passCount = lite.passcount
                this.od = lite.od
                this.cs = lite.cs
                this.ar = lite.ar
                this.hp = lite.hp
                this.starRating = lite.difficultyRating.toDouble()
                this.bpm = lite.bpm
                this.maxCombo = lite.maxCombo
                this.status = lite.status
                this.circles = lite.circles
                this.sliders = lite.sliders
                this.spinners = lite.spinners
                this.totalLength = lite.totalLength
                this.hitLength = lite.hitLength
                this.modeInt = lite.modeInt
                this.mapperID = lite.mapperID
                this.md5 = lite.md5
                this.beatmapset = lite.mapSet!!.toModel()
            }
        }

        fun Beatmap.toEntity(): BeatmapLite {
            val s = BeatmapLite(this)
            val set = this.beatmapset

            var mapSet: BeatmapsetLite? = null

            if (set != null) {
                mapSet = set.toEntity()
                s.beatmapsetID = set.beatmapsetID.toInt()
            }

            s.mapSet = mapSet

            return s
        }
    }
}
