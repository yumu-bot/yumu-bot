package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.*
import com.now.nowbot.model.osu.LazerMod.Companion.containsHidden
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt

// 这是 API v2 version header is 20240529 or higher 会返回的成绩数据。
open class LazerScore(
    @field:JsonProperty("classic_total_score")
    var classicScore: Long = 0L,

    @field:JsonProperty("preserve")
    var preserve: Boolean = false,

    @field:JsonProperty("processed")
    var processed: Boolean = false,

    @field:JsonProperty("ranked")
    var ranked: Boolean = false,

    @field:JsonProperty("maximum_statistics")
    var maximumStatistics: LazerStatistics = LazerStatistics(),

    @field:JsonProperty("statistics")
    var statistics: LazerStatistics = LazerStatistics(),

    @field:JsonProperty("total_score_without_mods")
    var rawScore: Long = 0L,

    @field:JsonProperty("beatmap_id")
    var beatmapID: Long = 0L,

    @field:JsonProperty("best_id")
    var bestID: Long? = 0L,

    @field:JsonProperty("id")
    var scoreID: Long = 0L,

    /** 注意，这个 rank 是 lazer 计分方式计算出来的，stable 成绩放在这里会有问题 */
    @field:JsonProperty("rank")
    var lazerRank: String = "F",

    // solo_score 不区分是否是新老客户端
    @field:JsonProperty("type")
    var type: String = "solo_score",

    @field:JsonProperty("user_id")
    var userID: Long = 0L,

    /** 注意，这个 accuracy 是 lazer 计分方式计算出来的，stable 成绩放在这里会有问题 */
    @field:JsonProperty("accuracy")
    var lazerAccuracy: Double = 0.0,

    @field:JsonProperty("build_id")
    val buildID: Long? = 0L,

    @set:JsonProperty("ended_at")
    @get:JsonIgnore
    var endedTime: OffsetDateTime = OffsetDateTime.now(),

    @field:JsonProperty("has_replay")
    var hasReplay: Boolean = false,

    @field:JsonProperty("is_perfect_combo")
    var perfectCombo: Boolean = false,

    @field:JsonProperty("legacy_perfect")
    var fullCombo: Boolean = false,

    @field:JsonProperty("legacy_score_id")
    var legacyScoreID: Long = 0L,

    @field:JsonProperty("legacy_total_score")
    var legacyScore: Long? = 0L,

    @field:JsonProperty("max_combo")
    var maxCombo: Int = 0,

    @field:JsonProperty("passed")
    var passed: Boolean = false,

    @set:JsonProperty("pp")
    @get:JsonIgnore
    var ppDouble: Double? = 0.0,

    @field:JsonProperty("ruleset_id")
    var ruleset: Byte = 0,

    @get:JsonIgnore
    @set:JsonProperty("started_at")
    var startedTime: OffsetDateTime? = null,

    @field:JsonProperty("total_score")
    var score: Long = 0L,

    @field:JsonProperty("replay")
    var replay: Boolean = false,

    ) {

    @get:JsonProperty("pp")
    @set:JsonIgnore
    var pp: Double = 0.0
        get() = ppDouble ?: 0.0
        set(value) {
            field = value
            ppDouble = value
        }

    @JsonIgnore
    fun getPP(): Double {
        return this.pp
    }

    // @field:JsonProperty("current_user_attributes") var userAttributes: UserAttributes =
    // UserAttributes()
    // 这个有问题
    // data class UserAttributes(@field:JsonProperty("pin") var pin: String? = null)

    @field:JsonProperty("beatmap")
    var beatmap: Beatmap = Beatmap()

    @field:JsonProperty("beatmapset")
    var beatmapset: Beatmapset = Beatmapset()

    @field:JsonProperty("user")
    var user: MicroUser = MicroUser()

    // MatchScore 继承：自己设
    @field:JsonProperty("ranking")
    var ranking: Int? = null

    @field:JsonProperty("match")
    var playerStat: MatchScorePlayerStat? = null

    data class MatchScorePlayerStat(val slot: Byte, val team: String, val pass: Boolean)

    @field:JsonProperty("weight")
    var weight: Weight? = null // 只在 BP 里有

    data class Weight(
        @field:JsonProperty("percentage") var percentage: Double = 0.0,
        @field:JsonProperty("pp") var pp: Double = 0.0,
    ) {
        val index: Int = (ln((percentage / 100)) / ln(0.95)).roundToInt()
    }

    @get:JsonProperty("is_lazer")
    var isLazer: Boolean = buildID != null && buildID > 0L

    @field:JsonProperty("mods")
    var mods: List<LazerMod> = listOf()
        get() { // 如果是 stable 成绩，则这里的 Classic 模组应该去掉
            return field.filterNot { it is LazerMod.Classic && !this.isLazer }
        }

    @get:JsonProperty("legacy_rank")
    var rank: String = ""
        get() = field.ifBlank { getStableRank(this) }

    // 傻逼 Lazer
    @get:JsonProperty("legacy_accuracy")
    val accuracy: Double
        get() = getStableAccuracy(this)

    // 给 js 用
    @get:JsonProperty("ended_at")
    val endedTimeString: String
        get() = formatter.format(endedTime)

    /**
     * 如果要设置，请设置 ruleset
     */
    @get:JsonProperty("mode")
    var mode: OsuMode = DEFAULT
        get() = when (this.ruleset.toInt()) {
            0 -> OSU
            1 -> TAIKO
            2 -> CATCH
            3 -> MANIA
            else -> DEFAULT
        }
        set(value) {
            field = value
            this.ruleset = value.modeValue
        }

    @get:JsonProperty("started_at")
    val startedTimeString: String = if (startedTime != null) {
        formatter.format(startedTime)
    } else {
        ""
    }


    @get:JsonProperty("preview_name")
    val previewName: String
        get() = if (beatmapset.artist.isNotBlank() || beatmapset.title.isNotBlank() || beatmapset.creator.isNotBlank()) {
            "${beatmapset.artist} - ${beatmapset.title} (${beatmapset.creator}) [${beatmap.difficultyName}]"
        } else {
            ""
        }

    @get:JsonProperty("score_hit") // 获取目前成绩进度（部分未通过成绩，这里并不是总和）
    val scoreHit: Int
        get() {
            val s = this.statistics

            return when (this.mode) {
                OSU -> s.great + s.ok + s.meh + s.miss
                TAIKO -> s.great + s.ok + s.miss
                CATCH -> s.great + s.miss + s.largeTickHit + s.largeTickMiss

                MANIA -> s.perfect + s.great + s.good + s.ok + s.meh + s.miss

                else -> 0
            }
        }

    @get:JsonProperty("total_hit")
    val totalHit: Int
        get() {
            val m = this.maximumStatistics

            if (this.isLazer || this.beatmap.circles == null || this.beatmap.convert == true) {
                return when (this.mode) {
                    OSU -> m.great
                    TAIKO -> m.great
                    CATCH -> m.great + m.largeTickHit + m.smallTickHit
                    MANIA -> m.perfect
                    else -> 0
                }
            } else { // 稳定版内，maximumStatistics 拿到的数据只是当前成绩的完美值（特别是中途退出的成绩），并不是总数
                val b = this.beatmap

                return when (this.mode) {
                    OSU -> max(m.great + m.legacyComboIncrease, (b.circles ?: 0) + (b.sliders ?: 0))
                    TAIKO -> max(m.great + m.legacyComboIncrease, (b.circles ?: 0))
                    CATCH -> m.great + m.largeTickHit + m.smallTickHit + m.legacyComboIncrease
                    MANIA -> max(m.perfect, (b.circles ?: 0) + (b.sliders ?: 0))
                    else -> 0
                }
            }
        }

    @get:JsonProperty("total_combo")
    val totalCombo: Int
        get() {
            val m = this.maximumStatistics

            return when (this.mode) {
                OSU -> m.great + m.legacyComboIncrease
                TAIKO -> m.great + m.legacyComboIncrease
                CATCH -> m.great + m.largeTickHit + m.legacyComboIncrease
                MANIA -> m.perfect + m.ignoreHit + m.legacyComboIncrease
                else -> 0
            }
        }

    companion object {

        private val formatter: DateTimeFormatter =
            DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd").appendLiteral("T").appendPattern("HH:mm:ss")
                .toFormatter()

        private fun getStableRank(score: LazerScore): String {
            if (!score.passed && score.type !== "sb_score") return "F"
            if (score.isLazer) return score.lazerRank

            val m = score.maximumStatistics
            val s = score.statistics

            // matchScore 无法计算
            if (m.great == 0 && m.perfect == 0) {
                return score.lazerRank
            }

            val accuracy = score.accuracy
            val p300 = 1.0 * s.great / m.great
            val hasMiss = s.miss > 0

            // 浮点数比较使用 `==` 容易出bug, 一般5-7位小数就够了
            var rank = when (score.mode) {
                OSU, OSU_RELAX, OSU_AUTOPILOT -> {
                    val is50Over1P = (s.meh * 100 > score.scoreHit)
                    when {
                        p300 > 1.0 - 1e-6 -> "X"
                        p300 >= 0.9 -> if (hasMiss) "A" else if (is50Over1P) "A" else "S"
                        p300 >= 0.8 -> if (hasMiss) "B" else "A"
                        p300 >= 0.7 -> if (hasMiss) "C" else "B"
                        p300 >= 0.6 -> "C"
                        else -> "D"
                    }
                }

                TAIKO, TAIKO_RELAX -> when {
                    p300 > 1.0 - 1e-6 -> "X"
                    p300 >= 0.9 -> if (hasMiss) "A" else "S"
                    p300 >= 0.8 -> if (hasMiss) "B" else "A"
                    p300 >= 0.7 -> if (hasMiss) "C" else "B"
                    p300 >= 0.6 -> "C"
                    else -> "D"
                }

                CATCH, CATCH_RELAX -> when {
                    accuracy > 1.0 - 1e-6 -> "X"
                    accuracy > 0.98 -> "S"
                    accuracy > 0.94 -> "A"
                    accuracy > 0.90 -> "B"
                    accuracy > 0.85 -> "C"
                    else -> "D"
                }

                MANIA -> when {
                    (s.great + s.perfect) == m.perfect -> "X"
                    accuracy > 0.95 -> "S"
                    accuracy > 0.90 -> "A"
                    accuracy > 0.80 -> "B"
                    accuracy > 0.70 -> "C"
                    else -> "D"
                }

                else -> "F"
            }

            if (score.mods.containsHidden() && (rank == "S" || rank == "X")) rank += "H"

            return rank
        }

        private fun getStableAccuracy(score: LazerScore): Double {
            if (score.isLazer) return score.lazerAccuracy

            val m = score.maximumStatistics
            val s = score.statistics

            var total = m.great

            if (m.great == 0 && m.perfect == 0) return score.lazerAccuracy

            return when (score.mode) {
                OSU, OSU_RELAX, OSU_AUTOPILOT -> {
                    val hit = s.great + 1.0 / 3 * s.ok + 1.0 / 6 * s.meh
                    hit / total
                }

                TAIKO, TAIKO_RELAX -> {
                    (s.great + 1.0 / 2 * s.ok) / total
                }

                CATCH, CATCH_RELAX -> {
                    val hit = s.great + s.largeTickHit + s.smallTickHit
                    total = m.great + m.largeTickHit + m.smallTickHit

                    1.0 * hit / total
                }

                MANIA -> {
                    val hit = s.perfect + s.great + 2.0 / 3 * s.good + 1.0 / 3 * s.ok + 1.0 / 6 * s.meh
                    total = m.perfect
                    hit / total
                }

                else -> 0.0
            }
        }
    }
}