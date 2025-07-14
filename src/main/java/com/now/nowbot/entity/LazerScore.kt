package com.now.nowbot.entity

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.LazerStatistics
import com.now.nowbot.util.JacksonUtil
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcType
import org.hibernate.annotations.Type
import org.hibernate.dialect.PostgreSQLEnumJdbcType
import java.io.Serializable
import java.time.OffsetDateTime

@Entity
@Table(indexes = [
    Index(name = "s_uid", columnList = "user_id"),
    Index(name = "s_bid", columnList = "beatmap_id"),
    Index(name = "s_time", columnList = "time"),
    Index(name = "s_mode", columnList = "mode"),
])
class LazerScoreLite(
    @Id
    var id: Long,

    var legacyScoreId: Long,

    var userId: Long,

    var beatmapId: Long,

    @Type(JsonBinaryType::class)
    @Column(columnDefinition = "jsonb", nullable = true)
    var mods: String?,

    var pp: Float,

    var accuracy: Float,

    var maxCombo: Int,

    var time: OffsetDateTime,

    var perfectCombo: Boolean,

    var passed: Boolean,

    // classic_total_score
    var classicScore: Int,

    // legacy_total_score
    var legacyScore: Int,

    // total_score
    var lazerScore: Int,

    var mode: Short,

    @Enumerated
    @JdbcType(value = PostgreSQLEnumJdbcType::class)
    var rank: Rank?,
) {
    constructor(score: LazerScore) : this(
        score.scoreID,
        score.legacyScoreID,
        score.userID,
        score.beatmapID,
        JacksonUtil.objectToJson(score.mods),
        (score.pp).toFloat(),
        score.lazerAccuracy.toFloat(),
        score.maxCombo,
        score.endedTime,
        score.perfectCombo,
        score.passed,
        score.classicScore.toInt(),
        score.legacyScore?.toInt() ?: 0,
        score.score.toInt(),
        score.mode.modeValue.toShort(),
        Rank.fromString(score.lazerRank)
    )

    fun toLazerScore(): LazerScore {
        val lite = this

        return LazerScore().apply {
            this.scoreID = lite.id
            this.legacyScoreID = lite.legacyScoreId
            this.userID = lite.userId
            this.beatmapID = lite.beatmapId
            this.mods = JacksonUtil.parseObjectList(lite.mods, LazerMod::class.java)
            this.pp = lite.pp.toDouble()
            this.lazerAccuracy = lite.accuracy.toDouble()
            this.maxCombo = lite.maxCombo
            this.endedTime = lite.time
            this.perfectCombo = lite.perfectCombo
            this.passed = lite.passed
            this.classicScore = lite.classicScore.toLong()
            this.legacyScore = lite.legacyScore.toLong()
            this.score = lite.lazerScore.toLong()
            this.mode = OsuMode.getMode(lite.mode)
            this.rank = lite.rank?.toString() ?: "F"
        }
    }

    enum class Rank {
        F, D, C, B, A, S, SH, X, XH, ;

        override fun toString() = when (this) {
            F -> "F"
            D -> "D"
            C -> "C"
            B -> "B"
            A -> "A"
            S -> "S"
            SH -> "SH"
            X -> "X"
            XH -> "XH"
        }

        companion object {
            fun fromString(str: String) = when (str.uppercase()) {
                "F" -> F
                "D" -> D
                "C" -> C
                "B" -> B
                "A" -> A
                "S" -> S
                "SH" -> SH
                "X" -> X
                "XH" -> XH
                else -> F
            }
        }
    }
}

@Entity
@Table(name = "score_statistic")
@IdClass(ScoreStatisticLite.ScoreStatisticKey::class)
class ScoreStatisticLite(
    @Id
    var id: Long,

    @Id
    // -1: score, 0-3: osu, taiko, catch, mania
    var status: Int,

    @Type(JsonBinaryType::class)
    @Column(columnDefinition = "json", nullable = false)
    var data: String
) {
    fun getMode(): OsuMode?  = when(status){
        0 -> OsuMode.OSU
        1 -> OsuMode.TAIKO
        2 -> OsuMode.CATCH
        3 -> OsuMode.MANIA
        else -> null
    }

    fun setStatus(score: LazerScore) {
        when(this.status) {
            -1 -> score.statistics = JacksonUtil.parseObject(this.data, LazerStatistics::class.java)
            else -> score.maximumStatistics = JacksonUtil.parseObject(this.data, LazerStatistics::class.java)
        }
    }

    companion object {
        fun createByBeatmap(score: LazerScore) = ScoreStatisticLite(
            score.beatmapID,
            score.mode.modeValue.toInt(),
            score.maximumStatistics.toShortJson()
        )

        fun createByScore(score: LazerScore) = ScoreStatisticLite(
            score.scoreID,
            -1,
            score.statistics.toShortJson()
        )
    }

    data class ScoreStatisticKey(
        var id: Long,
        var status: Int,
    ) : Serializable {
        constructor() : this(0, -1)
    }
}

