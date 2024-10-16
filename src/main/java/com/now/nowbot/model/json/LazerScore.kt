package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMod
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

// 这是 API v2 version header is 20220705 or higher 会返回的成绩数据。这并不支持老版本的比赛数据（stable 比赛依旧是原来那个 score
class LazerScore {
    private val formatter: DateTimeFormatter =
            DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd")
                    .appendLiteral("T")
                    .appendPattern("HH:mm:ss")
                    .appendZoneId()
                    .toFormatter()
    @JsonProperty("classic_total_score") var classicScore: Long = 0L

    @JsonProperty("preserve") var preserve: Boolean = false

    @JsonProperty("processed") var processed: Boolean = false

    @JsonProperty("ranked") var ranked: Boolean = false

    @JsonProperty("classic_total_score")
    var maximumStatistics: MaximumStatistics = MaximumStatistics()

    data class MaximumStatistics(
            @JsonProperty("great") var great: Int = 0,

            // 以下是 stable 独有
            @JsonProperty("legacy_combo_increase") var legacyComboIncrease: Int? = 0,

            // 以下是 lazer 独有
            @JsonProperty("ignore_hit") var ignoreHit: Int? = 0,
            @JsonProperty("large_tick_hit") var largeTickHit: Int? = 0,
            @JsonProperty("slider_tail_hit") var sliderTailHit: Int? = 0,
    )

    @JsonProperty("mods") private val modList: List<ScoreMod> = listOf()

    @JsonIgnoreProperties
    var mods: List<OsuMod> = listOf()
        get() {
            return if (this.modList.isNotEmpty()) {
                this.modList
                        .stream()
                        .map(ScoreMod::acronym)
                        .map(OsuMod::getModFromAbbreviation)
                        .toList()
            } else {
                listOf()
            }
        }
        set(value) {
            field = value.ifEmpty { listOf() }
        }

    data class ScoreMod(@JsonProperty("acronym") var acronym: String)

    @JsonProperty("statistics") var statistics: StatisticsV2 = StatisticsV2()

    data class StatisticsV2(
            @JsonProperty("great") var great: Int = 0,
            @JsonProperty("ok") var ok: Int = 0,
            @JsonProperty("miss") var miss: Int = 0,

            // 以下是 stable 独有
            @JsonProperty("meh") var meh: Int? = 0,

            // 以下是 lazer 独有
            @JsonProperty("ignore_hit") var ignoreHit: Int? = 0,
            @JsonProperty("large_tick_hit") var largeTickHit: Int? = 0,
            @JsonProperty("large_tick_miss") var largeTickMiss: Int? = 0,
            @JsonProperty("slider_tail_hit") var sliderTailHit: Int? = 0,
    )

    @JsonProperty("total_score_without_mods") var rawScore: Long = 0L

    @JsonProperty("beatmap_id") var beatmapID: Long = 0L

    @JsonProperty("best_id") var bestID: Long? = 0L

    @JsonProperty("id") var scoreID: Long = 0L

    @JsonProperty("rank") var rank: String = "F"

    @JsonProperty("type") var type: String = "solo_score" // solo_score 不区分是否是新老客户端

    @JsonProperty("user_id") var userID: Long = 0L

    @JsonProperty("accuracy") var accuracy: Double = 0.0

    @JsonProperty("build_id") var buildID: Long? = 0L

    @JsonProperty("ended_at") var endedTime: String = ""

    // @JsonProperty("has_replay") var replay: Boolean = false

    @JsonProperty("is_perfect_combo") var perfectCombo: Boolean = false

    @JsonProperty("legacy_perfect") var fullCombo: Boolean = false

    @JsonProperty("legacy_score_id") var legacyScoreID: Long? = 0L

    @JsonProperty("legacy_total_score") var legacyScore: Long = 0L

    @JsonProperty("max_combo") var maxCombo: Long = 0L

    @JsonProperty("passed") var passed: Boolean = false

    @JsonProperty("pp") var pp: Double? = 0.0

    @JsonProperty("ruleset_id") var ruleset: Byte = 0

    @JsonProperty("started_at") var startedTime: String? = ""

    @JsonProperty("total_score") var score: Long = 0L

    @JsonProperty("replay") var replay: Boolean = false

    @JsonProperty("current_user_attributes") var userAttributes: UserAttributes = UserAttributes()

    data class UserAttributes(@JsonProperty("replay") var pin: String? = null)

    @JsonProperty("beatmap") var beatMap: BeatMap = BeatMap()

    @JsonProperty("beatmapset") var beatMapSet: BeatMapSet = BeatMapSet()

    @JsonProperty("user") var user: MicroUser = MicroUser()
}
