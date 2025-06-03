package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.*
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import kotlin.math.ln
import kotlin.math.roundToInt

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
// 这是 API v2 version header is 20240529 or higher 会返回的成绩数据。
@JsonIgnoreProperties(ignoreUnknown = true)
open class LazerScore {
    @JsonProperty("classic_total_score")
    var classicScore: Long = 0L

    @JsonProperty("preserve")
    var preserve: Boolean = false

    @JsonProperty("processed")
    var processed: Boolean = false

    @JsonProperty("ranked")
    var ranked: Boolean = false

    @JsonProperty("maximum_statistics")
    var maximumStatistics: LazerStatistics = LazerStatistics()

    @JsonProperty("mods")
    var mods: List<LazerMod> = listOf()
        get() { // 如果是 stable 成绩，则这里的 Classic 模组应该去掉
            return field.filterNot { it is LazerMod.Classic && !this.isLazer }
        }

    @JsonProperty("statistics")
    var statistics: LazerStatistics = LazerStatistics()

    @JsonProperty("total_score_without_mods")
    var rawScore: Long = 0L

    @JsonProperty("beatmap_id")
    var beatmapID: Long = 0L

    @JsonProperty("best_id")
    var bestID: Long? = 0L

    @JsonProperty("id")
    var scoreID: Long = 0L

    /** 注意，这个 rank 是 lazer 计分方式计算出来的，stable 成绩放在这里会有问题 */
    @JsonProperty("rank")
    var lazerRank: String = "F"

    @get:JsonProperty("legacy_rank")
    var rank: String
        get() = _rank ?: getStableRank(this)
        set(value) {
            _rank = value
        }

    // 傻逼 Lazer
    @JsonIgnore
    private var _rank: String? = null

    @JsonProperty("type")
    var type: String = "solo_score" // solo_score 不区分是否是新老客户端

    @JsonProperty("user_id")
    var userID: Long = 0L

    /** 注意，这个 accuracy 是 lazer 计分方式计算出来的，stable 成绩放在这里会有问题 */
    @JsonProperty("accuracy")
    var lazerAccuracy: Double = 0.0

    // 傻逼 Lazer
    @get:JsonProperty("legacy_accuracy")
    val accuracy: Double
        get() = getStableAccuracy(this)

    @JsonProperty("build_id")
    var buildID: Long? = 0L

    @get:JsonProperty("is_lazer")
    val isLazer: Boolean
        get() = buildID != null && buildID!! > 0L

    @set:JsonProperty("ended_at")
    @get:JsonIgnore
    var endedTime: OffsetDateTime = OffsetDateTime.now()

    // 给 js 用
    @get:JsonProperty("ended_at")
    val endedTimeString: String
        get() = formatter.format(endedTime)

    @JsonProperty("has_replay")
    var hasReplay: Boolean = false

    @JsonProperty("is_perfect_combo")
    var perfectCombo: Boolean = false

    @JsonProperty("legacy_perfect")
    var fullCombo: Boolean = false

    @JsonProperty("legacy_score_id")
    var legacyScoreID: Long = 0L

    @JsonProperty("legacy_total_score")
    var legacyScore: Long? = 0L

    @JsonProperty("max_combo")
    var maxCombo: Int = 0

    @JsonProperty("passed")
    var passed: Boolean = false

    @JsonProperty("pp")
    var PP: Double? = 0.0

    @JsonProperty("ruleset_id")
    var ruleset: Byte = 0

    /**
     * 如果要设置，请设置 ruleset
     */
    @get:JsonProperty("mode")
    val mode: OsuMode
        get() = when (this.ruleset.toInt()) {
            0 -> OSU
            1 -> TAIKO
            2 -> CATCH
            3 -> MANIA
            else -> DEFAULT
        }

    @get:JsonIgnore
    @set:JsonProperty("started_at")
    var startedTime: OffsetDateTime? = null

    @get:JsonProperty("started_at")
    val startedTimeString: String = if (startedTime != null) {
        formatter.format(startedTime)
    } else {
        ""
    }

    @JsonProperty("total_score")
    var score: Long = 0L

    @JsonProperty("replay")
    var replay: Boolean = false

    // @JsonProperty("current_user_attributes") var userAttributes: UserAttributes =
    // UserAttributes()
    // TODO 这个有问题
    // data class UserAttributes(@JsonProperty("pin") var pin: String? = null)

    // MatchScore 继承：自己设
    @JsonProperty("ranking")
    var ranking: Int? = null

    @JsonProperty("match")
    var playerStat: MatchScorePlayerStat? = null

    data class MatchScorePlayerStat(val slot: Byte, val team: String, val pass: Boolean)

    @JsonProperty("weight")
    var weight: Weight? = null // 只在 BP 里有
        get() = field ?: Weight()
        set(value) {
            field = value ?: Weight()
        }

    data class Weight(
        @JsonProperty("percentage") var percentage: Double = 0.0,
        @JsonProperty("pp") var PP: Double = 0.0,
    ) {
        val index: Int = (ln((percentage / 100)) / ln(0.95)).roundToInt()
    }

    @JsonProperty("beatmap")
    var beatmap: Beatmap = Beatmap()

    @JsonProperty("beatmapset")
    var beatmapset: Beatmapset = Beatmapset()

    @JsonProperty("user")
    var user: MicroUser = MicroUser()

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

            if (this.isLazer || this.beatmap.circles == null) {
                return when (this.mode) {
                    OSU -> m.great
                    TAIKO -> m.great
                    CATCH -> m.great + m.largeTickHit
                    MANIA -> m.perfect
                    else -> 0
                }
            } else { // 稳定版内，maximumStatistics 拿到的数据只是当前成绩的完美值（特别是中途退出的成绩），并不是总数
                val b = this.beatmap

                return when (this.mode) {
                    OSU -> (b.circles ?: 0) + (b.sliders ?: 0)
                    TAIKO -> (b.circles ?: 0)
                    CATCH -> m.great + m.largeTickHit + m.legacyComboIncrease
                    MANIA -> (b.circles ?: 0) + (b.sliders ?: 0)
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
            if (!score.passed) return "F"
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
                OSU -> {
                    val is50Over1P = s.meh / m.great > 0.01
                    when {
                        p300 > 1.0 - 1e-6 -> "X"
                        p300 >= 0.9 -> if (hasMiss) "A" else if (is50Over1P) "A" else "S"
                        p300 >= 0.8 -> if (hasMiss) "B" else "A"
                        p300 >= 0.7 -> if (hasMiss) "C" else "B"
                        p300 >= 0.6 -> "C"
                        else -> "D"
                    }
                }

                TAIKO -> when {
                    p300 > 1.0 - 1e-6 -> "X"
                    p300 >= 0.9 -> if (hasMiss) "A" else "S"
                    p300 >= 0.8 -> if (hasMiss) "B" else "A"
                    p300 >= 0.7 -> if (hasMiss) "C" else "B"
                    p300 >= 0.6 -> "C"
                    else -> "D"
                }

                CATCH -> when {
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

                DEFAULT -> "F"
            }

            if (LazerMod.containsHidden(score.mods) && (rank == "S" || rank == "X")) rank += "H"

            return rank
        }

        private fun getStableAccuracy(score: LazerScore): Double {
            if (score.isLazer) return score.lazerAccuracy

            val m = score.maximumStatistics
            val s = score.statistics

            var total = m.great

            if (total <= 0) return score.lazerAccuracy

            return when (score.mode) {
                OSU -> {
                    val hit = s.great + 1.0 / 3 * s.ok + 1.0 / 6 * s.meh
                    hit / total
                }

                TAIKO -> {
                    (s.great + 1.0 / 2 * s.ok) / total
                }

                CATCH -> {
                    val hit = s.great + s.largeTickHit + s.smallTickHit
                    total = m.great + m.largeTickHit + m.smallTickHit

                    1.0 * hit / total
                }

                MANIA -> {
                    val hit = s.perfect + s.great + 2.0 / 3 * s.good + 1.0 / 3 * s.ok + 1.0 / 6 * s.meh
                    total = m.perfect
                    hit / total
                }

                DEFAULT -> 0.0
            }
        }
    }
}