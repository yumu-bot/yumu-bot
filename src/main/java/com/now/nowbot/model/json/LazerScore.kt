package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.*
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

// 这是 API v2 version header is 20220705 or higher 会返回的成绩数据。这并不支持老版本的比赛数据（stable 比赛依旧是原来那个 score
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

    @get:JsonProperty("total_hit")
    val totalHit: Int
        get() {
            val m = this.maximumStatistics

            if (this.isLazer || this.beatMap.circles == null) {
                return when (this.mode) {
                    OSU -> (m.great ?: 0)
                    TAIKO -> (m.great ?: 0)
                    CATCH -> (m.great ?: 0) + (m.largeTickHit ?: 0)
                    MANIA -> (m.perfect ?: 0)
                    else -> 0
                }
            } else {
                // 稳定版内，maximumStatistics 拿到的数据只是当前成绩的完美值（特别是中途退出的成绩），并不是总数
                val b = this.beatMap

                return when (this.mode) {
                    OSU -> (b.circles ?: 0) + (b.sliders ?: 0)
                    TAIKO -> (b.circles ?: 0)
                    CATCH -> (m.great ?: 0) + (m.largeTickHit ?: 0) + (m.legacyComboIncrease ?: 0)
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
                OSU -> (m.great ?: 0) + (m.legacyComboIncrease ?: 0)
                TAIKO -> (m.great ?: 0) + (m.legacyComboIncrease ?: 0)
                CATCH -> (m.great ?: 0) + (m.largeTickHit ?: 0) + (m.legacyComboIncrease ?: 0)
                MANIA -> (m.perfect ?: 0) + (m.ignoreHit ?: 0) + (m.legacyComboIncrease ?: 0)
                else -> 0
            }
        }

    @JsonProperty("mods")
    var modList: List<LazerMod> = listOf()

    @JsonIgnore
    var mods = modList

    @JsonProperty("statistics")
    var statistics: LazerStatistics = LazerStatistics()

    @get:JsonProperty("score_hit")
    // 获取目前成绩进度（部分未通过成绩，这里并不是总和）
    val scoreHit: Int
        get() {
            val s = this.statistics

            return when (this.mode) {
                OSU -> (s.great ?: 0) + (s.ok ?: 0) + (s.meh ?: 0) + (s.miss ?: 0)
                TAIKO -> (s.great ?: 0) + (s.ok ?: 0) + (s.miss ?: 0)
                CATCH ->
                    (s.great ?: 0) +
                            (s.miss ?: 0) +
                            (s.largeTickHit ?: 0) +
                            (s.largeTickMiss ?: 0)

                MANIA ->
                    (s.perfect ?: 0) +
                            (s.great ?: 0) +
                            (s.good ?: 0) +
                            (s.ok ?: 0) +
                            (s.meh ?: 0) +
                            (s.miss ?: 0)

                else -> 0
            }
        }

    data class LazerStatistics(
        // M 320
        @JsonProperty("perfect") var perfect: Int? = 0,

        // O、T、C、M 300
        @JsonProperty("great") var great: Int? = 0,

        // M 200
        @JsonProperty("good") var good: Int? = 0,

        // T、M 100
        @JsonProperty("ok") var ok: Int? = 0,

        // O、M 50
        @JsonProperty("meh") var meh: Int? = 0,

        // O、T、C、M 0
        @JsonProperty("miss") var miss: Int? = 0,

        // ?
        @JsonProperty("ignore_hit") var ignoreHit: Int? = 0,
        @JsonProperty("ignore_miss") var ignoreMiss: Int? = 0,

        // O SliderTick、C Large Droplet (medium)
        @JsonProperty("large_tick_hit") var largeTickHit: Int? = 0,
        @JsonProperty("large_tick_miss") var largeTickMiss: Int? = 0,

        // C Small Droplet (small)
        @JsonProperty("small_tick_hit") var smallTickHit: Int? = 0,
        @JsonProperty("small_tick_miss") var smallTickMiss: Int? = 0,

        // O SliderTail
        @JsonProperty("slider_tail_hit") var sliderTailHit: Int? = 0,

        // O Spinner Bonus、T Spinner Drumroll、C Banana
        @JsonProperty("large_bonus") var largeBonus: Int? = 0,

        // O Spinner Base
        @JsonProperty("small_bonus") var smallBonus: Int? = 0,

        // 仅 MAX 有
        @JsonProperty("legacy_combo_increase") var legacyComboIncrease: Int? = 0,
    ) : Cloneable {
        public override fun clone(): LazerStatistics {
            try {
                return super.clone() as LazerStatistics
            } catch (e: CloneNotSupportedException) {
                throw AssertionError()
            }
        }
    }

    @JsonProperty("total_score_without_mods")
    var rawScore: Long = 0L

    @JsonProperty("beatmap_id")
    var beatMapID: Long = 0L

    @JvmField
    @JsonProperty("best_id")
    var bestID: Long? = 0L

    @JsonProperty("id")
    var scoreID: Long = 0L

    /** 注意，这个 rank 是 lazer 计分方式计算出来的，stable 成绩放在这里会有问题 */
    @JsonProperty("rank")
    var lazerRank: String = "F"

    @get:JsonIgnore
    @get:JvmName("getRankString")
    val rank: String
        get() {
            return getRank()
        }

    // 傻逼 Lazer
    @JsonIgnore
    private var _rank: String? = null

    @JsonIgnore
    fun setRank(String: String) {
        _rank = String
    }

    @JsonProperty("legacy_rank")
    fun getRank(): String {
        return _rank ?: getStableRank(this)
    }

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
        get() {
            return getStableAccuracy(this)
        }

    @JvmField
    @JsonProperty("build_id")
    var buildID: Long? = 0L

    @get:JsonProperty("is_lazer")
    val isLazer: Boolean
        get() {
            return buildID != null && buildID!! > 0
        }

    @set:JsonProperty("ended_at")
    @get:JsonIgnore
    var endedTime: OffsetDateTime = OffsetDateTime.now()

    // 给 js 用
    @get:JsonProperty("ended_at")
    val endedTimeString: String
        get() {
            return formatter.format(endedTime)
        }

    // @JsonProperty("has_replay") var replay: Boolean = false

    @JsonProperty("is_perfect_combo")
    var perfectCombo: Boolean = false

    @JsonProperty("legacy_perfect")
    var fullCombo: Boolean = false

    @JsonProperty("legacy_score_id")
    var legacyScoreID: Long = 0L

    @JvmField
    @JsonProperty("legacy_total_score")
    var legacyScore: Long? = 0L

    @JsonProperty("max_combo")
    var maxCombo: Int = 0

    @JsonProperty("passed")
    var passed: Boolean = false

    @JvmField
    @JsonProperty("pp")
    var PP: Double? = 0.0

    @JsonProperty("ruleset_id")
    var ruleset: Byte = 0

    @get:JsonProperty("mode")
    val mode: OsuMode
        get() {
            return when (this.ruleset.toInt()) {
                0 -> OSU
                1 -> TAIKO
                2 -> CATCH
                3 -> MANIA
                else -> DEFAULT
            }
        }
//    怎么这里 default 是 min
//    @get:JsonIgnore
//    val startedTimePretty: LocalDateTime
//        get() {
//            return if (startedTime != null && startedTime!!.isNotBlank()) {
//                LocalDateTime.parse(startedTime!!, formatter).plusHours(8L)
//            } else {
//                LocalDateTime.MIN
//            }
//        }

    @JvmField
    @set:JsonProperty("started_at")
    @get:JsonIgnore
    var startedTime: OffsetDateTime? = OffsetDateTime.MIN

    @get:JsonProperty("started_at")
    val startedTimeString: String
        get() {
            return formatter.format(startedTime)
        }

    @JsonProperty("total_score")
    var score: Long = 0L

    @JsonProperty("replay")
    var replay: Boolean = false

    // @JsonProperty("current_user_attributes") var userAttributes: UserAttributes =
    // UserAttributes()
    // TODO 这个有问题
    // data class UserAttributes(@JsonProperty("pin") var pin: String? = null)

    @JsonProperty("beatmap")
    var beatMap: BeatMap = BeatMap()

    @JsonProperty("beatmapset")
    var beatMapSet: BeatMapSet = BeatMapSet()

    @JsonProperty("user")
    var user: MicroUser = MicroUser()

    @JsonProperty("weight")
    var weight: Weight? = Weight() // 只在 BP 里有
        get() {
            return field ?: Weight()
        }
        set(value) {
            field = value ?: Weight()
        }

    data class Weight(
        @JsonProperty("percentage") var percentage: Double = 0.0,
        @JsonProperty("pp") var PP: Double = 0.0,
    ) {
        val index: Int
            get() {
                val i = ln((percentage / 100)) / ln(0.95)
                return i.roundToInt()
            }
    }

    fun getPP(): Double {
        return PP!!
    }

    fun setPP(pp: Double?) {
        PP = pp ?: PP
    }

    companion object {
        private val formatter: DateTimeFormatter =
            DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd")
                .appendLiteral("T")
                .appendPattern("HH:mm:ss")
                .toFormatter()

        fun getWeightPP(score: LazerScore, position: Int = 0): Double {
            return score.weight?.PP ?: ((score.PP ?: 0.0) * (0.95).pow(position.toDouble()))
        }

        @JvmStatic
        private fun getStableRank(score: LazerScore): String {
            if (!score.passed) return "F"
            if (score.isLazer) return score.lazerRank

            val m = score.maximumStatistics
            val s = score.statistics

            val accuracy = score.accuracy
            val p300 = 1.0 * (s.great ?: 0) / (m.great ?: m.perfect ?: 0)
            val hasMiss = (s.miss ?: 0) > 0

            // 浮点数比较使用 `==` 容易出bug, 一般5-7位小数就够了
            var rank = when (score.mode) {
                OSU -> {
                    val is50Over1P = (s.meh ?: 0) / (m.great ?: m.perfect ?: 0) > 0.01
                    when {
                        p300 > 0.99999 -> "X"
                        p300 >= 0.9 -> if (hasMiss) "A" else if (is50Over1P) "A" else "S"
                        p300 >= 0.8 -> if (hasMiss) "B" else "A"
                        p300 >= 0.7 -> if (hasMiss) "C" else "B"
                        p300 >= 0.6 -> "C"
                        else -> "D"
                    }
                }

                TAIKO -> when {
                    p300 > 0.99999 -> "X"
                    p300 >= 0.9 -> if (hasMiss) "A" else "S"
                    p300 >= 0.8 -> if (hasMiss) "B" else "A"
                    p300 >= 0.7 -> if (hasMiss) "C" else "B"
                    p300 >= 0.6 -> "C"
                    else -> "D"
                }

                CATCH -> when {
                    accuracy > 0.99999 -> "X"
                    accuracy > 0.98 -> "S"
                    accuracy > 0.94 -> "A"
                    accuracy > 0.90 -> "B"
                    accuracy > 0.85 -> "C"
                    else -> "D"
                }

                MANIA -> when {
                    accuracy > 0.99999 -> "X"
                    accuracy > 0.95 -> "S"
                    accuracy > 0.90 -> "A"
                    accuracy > 0.80 -> "B"
                    accuracy > 0.70 -> "C"
                    else -> "D"
                }

                DEFAULT -> "F"
            }

            if (LazerMod.isHidden(score.modList) && (rank == "S" || rank == "X")) rank += "H"

            return rank
        }

        @JvmStatic
        private fun getStableAccuracy(score: LazerScore): Double {
            if (score.isLazer) return score.lazerAccuracy

            val m = score.maximumStatistics
            val s = score.statistics

            var total = m.great ?: 0

            return when (score.mode) {
                OSU -> {
                    val hit = (s.great ?: 0) + 1.0 / 3 * (s.ok ?: 0) + 1.0 / 6 * (s.meh ?: 0)
                    hit / total
                }

                TAIKO -> {
                    ((s.great ?: 0) + 1.0 / 2 * (s.ok ?: 0)) / total
                }

                CATCH -> {
                    val hit = (s.great ?: 0) + (s.largeTickHit ?: 0) + (s.smallTickHit ?: 0)
                    total = (m.great ?: 0) + (m.largeTickHit ?: 0) + (m.smallTickHit ?: 0)

                    1.0 * hit / total
                }

                MANIA -> {
                    val hit =
                        (s.perfect ?: 0) +
                                (s.great ?: 0) +
                                2.0 / 3 * (s.good ?: 0) +
                                1.0 / 3 * (s.ok ?: 0) +
                                1.0 / 6 * (s.meh ?: 0)
                    total = (m.perfect ?: 0)
                    hit / total
                }

                DEFAULT -> 0.0
            }
        }
    }
}