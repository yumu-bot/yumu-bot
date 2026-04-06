package com.now.nowbot.entity
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerMod.Companion.containsHidden
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.util.JacksonUtil
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.Type
import java.io.Serializable
import java.sql.Types
import java.time.OffsetDateTime

@Entity
@Table(indexes = [
    Index(name = "idx_lazer_user_query", columnList = "user_id, mode, time DESC"),
    Index(name = "idx_lazer_user_beatmap_query", columnList = "user_id, beatmap_id, mode, time DESC"),
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

    @Column(name = "rank_byte")
    var rankByte: Byte,
) {
    constructor(score: LazerScore) : this(
        score.scoreID,
        score.legacyScoreID,
        score.userID,
        score.beatmapID,
        JacksonUtil.objectToJson(score.mods),
        (score.pp).toFloat(),
        score.accuracy.toFloat(),
        score.maxCombo,
        score.endedTime,
        score.perfectCombo,
        score.passed,
        score.classicScore.toInt(),
        score.legacyScore?.toInt() ?: 0,
        score.score.toInt(),
        score.mode.modeValue.toShort(),
        rankToByte(score.rank)
    )

    fun toLazerScore(): LazerScore {
        val lite = this

        val mods = if (lite.mods.isNullOrBlank()) {
            emptyList()
        } else {
            JacksonUtil.parseObjectList(lite.mods, LazerMod::class.java)
        }

        val rank = if (!lite.passed) {
            "F"
        } else if (lite.rankByte > 0) {
            byteToRank(lite.rankByte)
        } else {
            when(lite.mode.toInt()) {
                0, 1, 3 -> when(lite.accuracy) {
                    in 1.0..1.001 -> if (mods.containsHidden()) "XH" else "X"
                    in 0.95..1.0 -> if (mods.containsHidden()) "SH" else "S"
                    in 0.9..0.95 -> "A"
                    in 0.8..0.9 -> "B"
                    in 0.7..0.8 -> "C"
                    else -> "D"
                }

                2 -> when(lite.accuracy) {
                    in 1.0..1.001 -> if (mods.containsHidden()) "XH" else "X"
                    in 0.98..1.0 -> if (mods.containsHidden()) "SH" else "S"
                    in 0.94..0.98 -> "A"
                    in 0.9..0.94 -> "B"
                    in 0.85..0.9 -> "C"
                    else -> "D"
                }

                else -> "F"
            }
        }

        return LazerScore().apply {
            this.buildID = if (lite.legacyScore == 0) 1 else null
            this.scoreID = lite.id
            this.legacyScoreID = lite.legacyScoreId
            this.userID = lite.userId
            this.beatmapID = lite.beatmapId
            this.beatmap.beatmapID = lite.beatmapId
            this.mods = mods
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
            this.rank = rank
            this.lazerRank = rank
        }
    }

    companion object {
        fun rankToByte(rank: String): Byte {
            return when(rank.trim().uppercase()) {
                "F" -> 0
                "D" -> 1
                "C" -> 2
                "B" -> 3
                "A" -> 4
                "S" -> 5
                "SH" -> 6
                "X", "SS" -> 7
                "XH", "SSH" -> 8
                else -> 0
            }
        }

        fun byteToRank(byte: Byte): String {
            return when(byte.toInt()) {
                0 -> "F"
                1 -> "D"
                2 -> "C"
                3 -> "B"
                4 -> "A"
                5 -> "S"
                6 -> "SH"
                7 -> "X"
                8 ->"XH"
                else -> "F"
            }
        }
    }
}

@Entity
@Table(name = "score_statistic")
@IdClass(ScoreStatisticLite.ScoreStatisticKey::class)
class ScoreStatisticLite(
    @Id
    var id: Long = 0,

    @Id
    @JdbcTypeCode(Types.SMALLINT)
    @Column(name = "mode", columnDefinition = "int2")
    // -1: score, 0-3: osu, taiko, catch, mania
    var mode: Byte = -1,

    @Type(JsonBinaryType::class)
    @Column(name = "data", columnDefinition = "jsonb", nullable = false)
    var data: String = ""
) {
    fun setStatus(score: LazerScore) {
        when(this.mode.toInt()) {
            -1 -> score.statistics = JacksonUtil.parseObject(this.data)!!
            else -> score.maximumStatistics = JacksonUtil.parseObject(this.data)!!
        }
    }

    companion object {
        fun createByBeatmap(score: LazerScore) = ScoreStatisticLite(
            score.beatmapID,
            score.mode.modeValue,
            score.maximumStatistics.toShortJson()
        )

        fun createByScore(score: LazerScore) = ScoreStatisticLite(
            score.scoreID,
            -1,
            score.statistics.toShortJson()
        )
    }

    data class ScoreStatisticKey(
        var id: Long = 0L,
        var mode: Byte = -1,
    ) : Serializable {
        @Suppress("UNUSED")
        constructor() : this(0L, -1)
    }
}

