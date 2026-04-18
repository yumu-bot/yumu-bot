package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore

import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
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
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE, // 禁用自动扫描 getter
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.ANY
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
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

    // score_mania
    @field:JsonProperty("type")
    var type: String = "solo_score",

    @field:JsonProperty("user_id")
    var userID: Long = 0L,

    /** 注意，这个 accuracy 是 lazer 计分方式计算出来的，stable 成绩放在这里会有问题 */
    @field:JsonProperty("accuracy")
    var lazerAccuracy: Double = 0.0,

    @field:JsonProperty("build_id")
    var buildID: Long? = 0L,

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
    val isLazer: Boolean
        // 使用 get() 确保它是动态计算的
        get() = buildID != null && buildID!! > 0L

    @field:JsonProperty("mods")
    @get:JsonIgnore
    @set:JsonProperty("mods")
    private var rawMods: List<LazerMod> = listOf()

    @get:JsonProperty("mods")
    @set:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    var mods: List<LazerMod>
        get() = if (this.isLazer) {
            rawMods
        } else {
            rawMods.filterNot { it.acronym == "CL" }
        }

        set(value) {
            rawMods = value
        }

    @get:JsonProperty("legacy_rank")
    @set:JsonIgnore
    var rank: String = ""
        get() = field.ifBlank { getStandardisedRank(this) }

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
    @get:JsonProperty("mode", access = JsonProperty.Access.READ_ONLY)
    @set:JsonProperty(access = JsonProperty.Access.READ_ONLY)
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

        private fun getStandardisedRank(score: LazerScore): String {
            if (!score.passed && score.type != "sb_score") {
                return "F"
            }

            /*
            if (score.isLazer) {
                return score.lazerRank
            }

             */

            val s = score.statistics

            val mode = score.mode.toSafeModeValue().toInt()

            val total = when(mode) {
                1 -> s.great + s.ok + s.miss
                2 -> s.great + s.largeTickHit + s.smallTickHit + s.largeTickMiss + s.smallTickMiss + s.miss
                3 -> s.perfect + s.great + s.good + s.ok + s.meh + s.miss
                else -> s.great + s.ok + s.meh + s.miss
            }

            val hasMiss = s.miss > 0

            val rank = if (score.isLazer) {
                // lazer
                when (mode) {
                    2 -> {
                        val hit = s.great + s.largeTickHit + s.smallTickHit

                        when {
                            hit == total -> "X"
                            hit * 100 > total * 98 -> "S"
                            hit * 100 > total * 94 -> "A"
                            hit * 100 > total * 90 -> "B"
                            hit * 100 > total * 85 -> "C"
                            else -> "D"
                        }
                    }

                    3 -> {
                        val judgement = s.perfect * 305 + s.great * 300 + s.good * 200 + s.ok * 100 + s.meh * 50 + 0
                        val max = total * 305

                        when {
                            s.perfect + s.great == total -> "X"
                            judgement * 100 > max * 95 -> "S"
                            judgement * 100 > max * 90 -> "A"
                            judgement * 100 > max * 80 -> "B"
                            judgement * 100 > max * 70 -> "C"
                            else -> "D"
                        }
                    }

                    else -> {
                        val judgement = if (mode == 1) {
                            s.great * 300 + s.ok * 150 + 0
                        } else {
                            s.great * 300 + s.ok * 100 + s.meh * 50 + 0 + s.sliderTailHit * 150 + s.largeTickHit * 30
                        }

                        val max = if (mode == 1) {
                            total * 300
                        } else {
                            val m = score.maximumStatistics

                            m.great * 300 + m.sliderTailHit * 150 + m.largeTickHit * 30
                        }

                        when {
                            judgement == max -> "X"
                            judgement * 100 > max * 95 -> if (hasMiss) "A" else "S"

                            judgement * 100 > max * 90 -> "A"
                            judgement * 100 > max * 80 -> "B"
                            judgement * 100 > max * 70 -> "C"
                            else -> "D"
                        }
                    }
                }
            } else {
                // stable
                when (mode) {

                    1 -> when {
                        s.great == total -> "X"
                        s.great * 10 > total * 9 -> if (hasMiss) "A" else "S"
                        s.great * 10 > total * 8 -> if (hasMiss) "B" else "A"
                        s.great * 10 > total * 7 -> if (hasMiss) "C" else "B"
                        s.great * 10 > total * 6 -> "C"
                        else -> "D"
                    }

                    2 -> {
                        val hit = s.great + s.largeTickHit + s.smallTickHit

                        when {
                            hit == total -> "X"
                            hit * 100 > total * 98 -> "S"
                            hit * 100 > total * 94 -> "A"
                            hit * 100 > total * 90 -> "B"
                            hit * 100 > total * 85 -> "C"
                            else -> "D"
                        }
                    }

                    3 -> {
                        val judgement = s.perfect * 300 + s.great * 300 + s.good * 200 + s.ok * 100 + s.meh * 50 + 0

                        when {
                            judgement == total * 300 -> "X"
                            judgement * 100 > total * 300 * 95 -> "S"
                            judgement * 100 > total * 300 * 90 -> "A"
                            judgement * 100 > total * 300 * 80 -> "B"
                            judgement * 100 > total * 300 * 70 -> "C"
                            else -> "D"
                        }
                    }

                    else -> {
                        val is50Over1P = (s.meh * 100 > total)

                        when {
                            s.great == total -> "X"

                            s.great * 10 > total * 9 -> if (hasMiss || is50Over1P) {
                                "A"
                            } else {
                                "S"
                            }

                            s.great * 10 > total * 8 -> if (hasMiss) "B" else "A"
                            s.great * 10 > total * 7 -> if (hasMiss) "C" else "B"
                            s.great * 10 > total * 6 -> "C"
                            else -> "D"
                        }
                    }
                }
            }

            if (score.mods.containsHidden() && (rank == "S" || rank == "X")) return rank + "H"

            return rank
        }

        private fun getStableAccuracy(score: LazerScore): Double {
            if (score.isLazer && score.lazerAccuracy > 0.0 && score.lazerAccuracy <= 1.0) {
                return score.lazerAccuracy
            }

            val m = score.maximumStatistics
            val s = score.statistics

            val total: Double = if (score.passed) {
                when(score.mode) {
                    OSU, OSU_RELAX, OSU_AUTOPILOT -> s.great + s.ok + s.meh + s.miss
                    TAIKO, TAIKO_RELAX -> s.great + s.ok + s.miss
                    CATCH, CATCH_RELAX -> s.great + s.largeTickHit + s.smallTickHit + s.largeTickMiss + s.smallTickMiss + s.miss
                    else -> s.perfect + s.great + s.good + s.ok + s.meh + s.miss
                }
            } else {
                when(score.mode) {
                    MANIA -> m.perfect
                    else -> m.great
                }
            }.toDouble()

            if (total == 0.0 && !score.passed) return score.lazerAccuracy

            val hit: Double = when(score.mode) {
                OSU, OSU_RELAX, OSU_AUTOPILOT -> s.great + 1.0 / 3 * s.ok + 1.0 / 6 * s.meh
                TAIKO, TAIKO_RELAX -> s.great + 1.0 / 2 * s.ok
                CATCH, CATCH_RELAX -> (s.great + s.largeTickHit + s.smallTickHit) * 1.0
                MANIA -> if (score.isLazer) {
                    s.perfect + 300.0 / 305.0 * s.great + 200.0 / 305.0 * s.good + 100.0 / 305.0 * s.ok + 50.0 / 305.0 * s.meh
                } else {
                    s.perfect + s.great + 2.0 / 3 * s.good + 1.0 / 3 * s.ok + 1.0 / 6 * s.meh
                }

                else -> 0.0
            }

            return (hit / total).coerceIn(0.0..1.0)
        }
    }
}