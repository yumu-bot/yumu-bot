package com.now.nowbot.entity

import com.now.nowbot.model.enums.OsuMode.Companion.toOsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.LazerStatistics
import com.now.nowbot.util.JacksonUtil
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.Type
import org.hibernate.type.SqlTypes
import tools.jackson.databind.node.ArrayNode
import java.io.Serializable
import java.sql.Types
import java.time.OffsetDateTime

@Entity
@Table(name = "tachyon_score",
    indexes = [
        Index(name = "idx_tachyon_user_query", columnList = "user_id, mode, time DESC"),
        Index(name = "idx_tachyon_user_beatmap_query", columnList = "user_id, beatmap_id, mode, time DESC"),

        // CREATE INDEX idx_tachyon_time_brin ON tachyon_score USING BRIN (time) WITH (pages_per_range = 32);
        // CREATE INDEX idx_tachyon_mods_gin ON public.tachyon_score USING GIN (mods);
        /*
        Index(
            name = "idx_tachyon_mods_gin",
            columnList = "mods",
            options = "USING gin"
        )
         */
    ],
)
class TachyonScoreLite(
    @Id
    @Column(name = "id", nullable = false)
    var scoreID: Long,

    @Column(name = "user_id", nullable = false)
    var userID: Long,

    @Column(name = "beatmap_id", nullable = false)
    var beatmapID: Long,

    @Column(name = "build_id")
    var buildID: Int?,

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "mods", columnDefinition = "char(2)[]", nullable = true)
    var mods: List<String>? = null,

    @Type(JsonBinaryType::class)
    @Column(name = "data", columnDefinition = "jsonb", nullable = true)
    var modsData: String? = null,

    @Column(name = "pp", nullable = false)
    var pp: Float = 0f,

    @Column(name = "accuracy", nullable = false)
    var accuracy: Float = 0f,

    @Column(name = "combo", nullable = false)
    var maxCombo: Int = 0,

    @Column(name = "time", nullable = false)
    var time: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "fc", nullable = false)
    var perfect: Boolean = false,

    @Column(name = "pass", nullable = false)
    var passed: Boolean = false,

    // legacy_total_score
    @Column(name = "legacy", nullable = true)
    var legacy: Int? = null,

    // total_score
    @Column(name = "score", nullable = false)
    var score: Int = 0,

    @Column(name = "mode", nullable = false)
    var mode: Byte = (-1).toByte(),

    @Column(name = "rank", nullable = false)
    var rank: Byte = 0.toByte(),
) {
    fun toLazerScore(): LazerScore {
        val lite = this

        val mods = (lite.mods to lite.modsData).toModel()

        val rank = if (!lite.passed) {
            "F"
        } else if (lite.rank > 0) {
            RankConverter.byteToRank(lite.rank)
        } else {
            when(lite.mode.toInt()) {
                0, 1, 3 -> when(lite.accuracy) {
                    in 1.0..1.001 -> if (LazerMod.containsHiddenAcronym(lite.mods)) "XH" else "X"
                    in 0.95..1.0 -> if (LazerMod.containsHiddenAcronym(lite.mods)) "SH" else "S"
                    in 0.9..0.95 -> "A"
                    in 0.8..0.9 -> "B"
                    in 0.7..0.8 -> "C"
                    else -> "D"
                }

                2 -> when(lite.accuracy) {
                    in 1.0..1.001 -> if (LazerMod.containsHiddenAcronym(lite.mods)) "XH" else "X"
                    in 0.98..1.0 -> if (LazerMod.containsHiddenAcronym(lite.mods)) "SH" else "S"
                    in 0.94..0.98 -> "A"
                    in 0.9..0.94 -> "B"
                    in 0.85..0.9 -> "C"
                    else -> "D"
                }

                else -> "F"
            }
        }

        return LazerScore().apply {
            this.buildID = lite.buildID?.toLong()
            this.scoreID = lite.scoreID
            this.userID = lite.userID
            this.beatmapID = lite.beatmapID
            this.beatmap.beatmapID = lite.beatmapID
            this.mods = mods
            this.pp = lite.pp.toDouble()
            this.lazerAccuracy = lite.accuracy.toDouble()
            this.maxCombo = lite.maxCombo
            this.endedTime = lite.time
            this.perfectCombo = lite.perfect
            this.passed = lite.passed
            this.legacyScore = lite.legacy?.toLong()
            this.score = lite.score.toLong()
            this.mode = lite.mode.toOsuMode()
            this.rank = rank
            this.lazerRank = rank
        }
    }

    companion object {
        fun fromScore(score: LazerScore): TachyonScoreLite {
            val (mods, modsData) = score.mods.toEntity()

            return TachyonScoreLite(
                scoreID = score.scoreID,
                userID = score.userID,
                beatmapID = score.beatmapID,
                buildID = score.buildID?.toInt()?.takeIf { it > 0 },
                mods = mods,
                modsData = modsData,
                pp = score.pp.toFloat(),
                accuracy = score.accuracy.toFloat(),
                maxCombo = score.maxCombo,
                time = score.endedTime,
                perfect = score.perfectCombo,
                passed = score.passed,
                legacy = score.legacyScore?.toInt()?.takeIf { it > 0 },
                score = score.score.toInt(),
                mode = score.mode.modeValue,
                rank = RankConverter.rankToByte(score.rank)
            )
        }

        fun List<LazerMod>.toEntity(): Pair<List<String>?, String?> {
            val processed = this.map { mod ->
                val safeAcronym = when (mod.acronym) {
                    "10K" -> "XK"
                    else -> if (mod.acronym.length > 2) mod.acronym.take(2) else mod.acronym
                }
                safeAcronym to mod.settings
            }

            val acronyms = processed.map { it.first }.takeIf { it.isNotEmpty() }

            val settings = processed
                .associate { it.first to it.second }
                .filterValues { ss -> ss != null }

            val jsonb = settings.takeIf { it.isNotEmpty() }?.let { ss -> JacksonUtil.objectToJson(ss) }

            return acronyms to jsonb
        }

        fun Pair<List<String>?, String?>.toModel(): List<LazerMod> {
            val (acronyms, jsonb) = this

            if (acronyms.isNullOrEmpty()) return emptyList()

            val mapper = JacksonUtil.mapper

            val settingsTree = if (!jsonb.isNullOrBlank()) {
                mapper.readTree(jsonb)
            } else {
                mapper.createObjectNode()
            }

            val rootArrayNode: ArrayNode = mapper.createArrayNode()

            // 3. 遍历 acronyms，直接拼装 JsonNode
            acronyms.forEach { acronym ->
                val modNode = mapper.createObjectNode()

                val acronym2 = if (acronym == "XK") {
                    "10K"
                } else {
                    acronym
                }

                modNode.put("acronym", acronym2)

                val modSettings = settingsTree.get(acronym)
                if (modSettings != null && !modSettings.isNull) {
                    modNode.set("settings", modSettings)
                }

                rootArrayNode.add(modNode)
            }

            val reader = mapper.readerForListOf(LazerMod::class.java)
            return reader.readValue(rootArrayNode)
        }

        fun transfer(legacy: LazerScoreLite): TachyonScoreLite {
            val lazerMods: List<LazerMod> = if (!legacy.mods.isNullOrBlank()) {
                try {
                    JacksonUtil.parseObjectList(legacy.mods, LazerMod::class.java)
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            val (modsList, modsDataJson) = lazerMods.toEntity()

            return TachyonScoreLite(
                scoreID = legacy.id,
                userID = legacy.userId,
                beatmapID = legacy.beatmapId,
                buildID = if (legacy.legacyScore == 0) 1 else null,
                mods = modsList,
                modsData = modsDataJson,
                pp = legacy.pp,
                accuracy = legacy.accuracy,
                maxCombo = legacy.maxCombo,
                time = legacy.time,
                perfect = legacy.perfectCombo,
                passed = legacy.passed,
                legacy = legacy.legacyScore.takeIf { it > 0 },
                score = legacy.lazerScore,
                mode = legacy.mode.toByte(),
                rank = legacy.rankByte
            )
        }
    }
}

@Entity
@Table(name = "tachyon_statistics")
@IdClass(TachyonStatisticsKey::class)
class TachyonStatisticsLite(
    @Id
    @Column(name = "id")
    // beatmapID / scoreID
    var statisticsID: Long = 0L,

    @Id
    @JdbcTypeCode(Types.SMALLINT)
    @Column(name = "mode", columnDefinition = "int2")
    // -1: score, 0-3: osu, taiko, catch, mania
    var mode: Byte = -1,

    @Embedded
    var statistics: LazerStatistics,
) {
    companion object {
        fun fromBeatmap(beatmap: Beatmap, statistics: LazerStatistics) = TachyonStatisticsLite(
            statisticsID = beatmap.beatmapID,
            mode = beatmap.mode.modeValue,
            statistics = statistics
        )

        fun fromMaximumStatistics(score: LazerScore) = TachyonStatisticsLite(
            statisticsID = score.beatmapID,
            mode = score.mode.modeValue,
            statistics = score.maximumStatistics
        )

        fun fromScore(score: LazerScore) = TachyonStatisticsLite(
            statisticsID = score.scoreID,
            mode = -1,
            statistics = score.statistics
        )

        fun transfer(legacy: ScoreStatisticLite): TachyonStatisticsLite {
            val statistics = JacksonUtil.parseObject<LazerStatistics>(legacy.data)!!

            return TachyonStatisticsLite(
                statisticsID = legacy.id,
                mode = legacy.mode,
                statistics = statistics
            )
        }
    }
}

data class TachyonStatisticsKey(
    var statisticsID: Long = 0L,
    var mode: Byte = -1,
) : Serializable {
    @Suppress("UNUSED")
    constructor() : this(0L, -1)
}

object RankConverter {
    private val rankToByteMap = hashMapOf(
        "F" to 0.toByte(), "D" to 1.toByte(), "C" to 2.toByte(), "B" to 3.toByte(), "A" to 4.toByte(),
        "S" to 5.toByte(), "SH" to 6.toByte(),
        "X" to 7.toByte(), "SS" to 7.toByte(),
        "XH" to 8.toByte(), "SSH" to 8.toByte()
    )

    private val byteToRankTable = arrayOf("F", "D", "C", "B", "A", "S", "SH", "X", "XH")

    fun rankToByte(rank: String): Byte {
        val key = rank.trim().uppercase()
        return rankToByteMap[key] ?: 0
    }

    fun byteToRank(byte: Byte): String {
        val index = byte.toInt()
        // 边界检查：确保索引在 0..8 之间
        if (index in 0..8) {
            return byteToRankTable[index]
        }
        return "F"
    }
}