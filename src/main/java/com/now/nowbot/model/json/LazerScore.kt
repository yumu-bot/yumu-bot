package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMod
import com.now.nowbot.model.enums.OsuMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import kotlin.math.ln
import kotlin.math.roundToInt

// 这是 API v2 version header is 20220705 or higher 会返回的成绩数据。这并不支持老版本的比赛数据（stable 比赛依旧是原来那个 score
open class LazerScore {
    @JsonProperty("classic_total_score") var classicScore: Long = 0L

    @JsonProperty("preserve") var preserve: Boolean = false

    @JsonProperty("processed") var processed: Boolean = false

    @JsonProperty("ranked") var ranked: Boolean = false

    @JsonProperty("maximum_statistics") var maximumStatistics: StatisticsV2 = StatisticsV2()

    @get:JsonProperty("total_hit")
    val totalHit: Int
        get() {
            val s = this.maximumStatistics

            return when (this.mode) {
                OsuMode.OSU -> (s.great ?: 0) + (s.legacyComboIncrease ?: 0)
                OsuMode.TAIKO -> (s.great ?: 0)
                OsuMode.CATCH -> (s.great ?: 0) + (s.largeTickHit ?: 0)
                OsuMode.MANIA -> (s.perfect ?: 0)
                else -> 0
            }
        }

    @JsonProperty("mods") private val modList: List<LazerMod> = listOf()

    @JsonIgnore
    var mods: List<OsuMod> = listOf()
        get() {
            return if (this.modList.isNotEmpty()) {
                this.modList.stream().map(OsuMod::getModFromLazerMod).toList()
            } else {
                listOf()
            }
        }
        set(value) {
            field = value.ifEmpty { listOf() }
        }

    data class LazerMod(
            @JsonProperty("acronym") var acronym: String = "",
            @JsonProperty("settings") var settings: LazerModSettings? = LazerModSettings(),
    )

    // 天杀的自定义设置
    data class LazerModSettings(
            // DT HT DC NC
            @JsonProperty("speed_change") var speed: Double? = null,

            // EZ
            @JsonProperty("extra_lives") var extraLives: Int? = null,

            // DT HT AS WU WD
            @JsonProperty("adjust_pitch") var adjustPitch: Boolean? = null,

            // SD PF
            @JsonProperty("restart_on_fail") var restartOnFail: Boolean? = null,

            // AS WU WD
            @JsonProperty("initial_rate") var initialSpeed: Double? = null,

            // WU WD
            @JsonProperty("final_rate") var finalSpeed: Double? = null,

            // DA
            @JsonProperty("circle_size") var cs: Double? = null,
            @JsonProperty("approach_rate") var ar: Double? = null,
            @JsonProperty("overall_difficulty") var od: Double? = null,
            @JsonProperty("drain_rate") var hp: Double? = null,
            @JsonProperty("extended_limits") var extendedLimits: Boolean? = null,
    )

    @JsonProperty("statistics") var statistics: StatisticsV2 = StatisticsV2()

    @get:JsonProperty("score_hit")
    // 获取目前成绩进度（部分未通过成绩，这里并不是总和）
    val scoreHit: Int
        get() {
            val s = this.statistics

            return when (this.mode) {
                OsuMode.OSU -> (s.great ?: 0) + (s.ok ?: 0) + (s.meh ?: 0) + (s.miss ?: 0)
                OsuMode.TAIKO -> (s.great ?: 0) + (s.ok ?: 0) + (s.miss ?: 0)
                OsuMode.CATCH ->
                        (s.great ?: 0) +
                                (s.miss ?: 0) +
                                (s.largeTickHit ?: 0) +
                                (s.largeTickMiss ?: 0)
                OsuMode.MANIA ->
                        (s.perfect ?: 0) +
                                (s.great ?: 0) +
                                (s.good ?: 0) +
                                (s.ok ?: 0) +
                                (s.meh ?: 0) +
                                (s.miss ?: 0)
                else -> 0
            }
        }

    data class StatisticsV2(
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
        public override fun clone(): StatisticsV2 {
            try {
                return super.clone() as StatisticsV2
            } catch (e: CloneNotSupportedException) {
                throw AssertionError()
            }
        }
    }

    @JsonProperty("total_score_without_mods") var rawScore: Long = 0L

    @JsonProperty("beatmap_id") var beatMapID: Long = 0L

    @JsonProperty("best_id")
    var bestID: Long? = 0L
        get() {
            return field ?: 0L
        }
        set(value) {
            field = value ?: 0L
        }

    @JsonProperty("id") var scoreID: Long = 0L

    @JsonProperty("rank") var rank: String = "F"

    @JsonProperty("type") var type: String = "solo_score" // solo_score 不区分是否是新老客户端

    @JsonProperty("user_id") var userID: Long = 0L

    @JsonProperty("accuracy") var accuracy: Double = 0.0

    @JsonProperty("build_id")
    var buildID: Long? = 0L
        get() {
            return field ?: 0L
        }
        set(value) {
            field = value ?: 0L
        }

    @JsonProperty("ended_at") var endedTime: String = ""

    @get:JsonIgnore
    val endedTimePretty: LocalDateTime
        get() {
            return if (endedTime.isNotBlank())
                    LocalDateTime.parse(endedTime, formatter).plusHours(8L)
            else LocalDateTime.now()
        }

    // @JsonProperty("has_replay") var replay: Boolean = false

    @JsonProperty("is_perfect_combo") var perfectCombo: Boolean = false

    @JsonProperty("legacy_perfect") var fullCombo: Boolean = false

    @JsonProperty("legacy_score_id")
    var legacyScoreID: Long? = 0L
        get() {
            return field ?: 0L
        }
        set(value) {
            field = value ?: 0L
        }

    @JsonProperty("legacy_total_score") var legacyScore: Long = 0L

    @JsonProperty("max_combo") var maxCombo: Int = 0

    @JsonProperty("passed") var passed: Boolean = false

    @JsonProperty("pp")
    var PP: Double? = 0.0
        get() {
            return field ?: 0.0
        }
        set(value) {
            field = value ?: 0.0
        }

    @JsonProperty("ruleset_id") var ruleset: Byte = 0

    @get:JsonIgnore
    val mode: OsuMode
        get() {
            return when (this.ruleset.toInt()) {
                0 -> OsuMode.OSU
                1 -> OsuMode.TAIKO
                2 -> OsuMode.CATCH
                3 -> OsuMode.MANIA
                else -> OsuMode.DEFAULT
            }
        }

    @JsonProperty("started_at") var startedTime: String? = ""

    @get:JsonIgnore
    val startedTimePretty: LocalDateTime
        get() {
            return if (startedTime != null && startedTime!!.isNotBlank()) {
                LocalDateTime.parse(startedTime!!, formatter).plusHours(8L)
            } else {
                LocalDateTime.MIN
            }
        }

    @JsonProperty("total_score") var score: Long = 0L

    @JsonProperty("replay") var replay: Boolean = false

    // @JsonProperty("current_user_attributes") var userAttributes: UserAttributes =
    // UserAttributes()
    // TODO 这个有问题
    // data class UserAttributes(@JsonProperty("pin") var pin: String? = null)

    @JsonProperty("beatmap") var beatMap: BeatMap = BeatMap()

    @JsonProperty("beatmapset") var beatMapSet: BeatMapSet = BeatMapSet()

    @JsonProperty("user") var user: MicroUser = MicroUser()

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

    companion object {
        private val formatter: DateTimeFormatter =
                DateTimeFormatterBuilder()
                        .appendPattern("yyyy-MM-dd")
                        .appendLiteral("T")
                        .appendPattern("HH:mm:ss")
                        .appendZoneId()
                        .toFormatter()
    }
}
