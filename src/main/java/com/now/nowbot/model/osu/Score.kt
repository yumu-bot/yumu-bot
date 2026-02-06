package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.*
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.Companion.getMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import kotlin.math.ln
import kotlin.math.roundToInt

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true, allowSetters = true, allowGetters = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class Score {
    @JsonProperty("accuracy")
    var accuracy: Double? = null

    @JsonProperty("best_id")
    var bestID: Long? = null

    @JsonProperty("max_combo")
    var maxCombo: Int? = null

    @JsonProperty("user_id")
    var userID: Long? = null

    @get:JsonProperty("create_at_str")
    @JsonAlias("created_at")
    var createTime: String? = null

    @JsonProperty("id")
    var scoreID: Long? = null

    @JsonIgnore
    var mode: OsuMode? = null

    @JsonProperty("mode_int")
    var modeInt: Int? = null

    var mods: List<String>? = null

    var passed: Boolean? = null

    var perfect: Boolean? = null

    @JsonProperty("pp")
    var pp: Float? = null
        get() {
            if (field != null) {
                return field!!
            }

            // PPY PP 有时候是 null
            if (weight != null && weight!!.percentage > 0) {
                return weight!!.weightedPP / (weight!!.percentage / 100f)
            }

            return 0f
        }

    var rank: String? = null

    var replay: Boolean? = null

    var score: Int? = null

    var statistics: Statistics? = null

    var type: String? = null

    @get:JsonIgnore
    val legacy: Boolean
        get() = type != null && type != "solo_score" //目前只看见有这个类别，mp 房也是这个类别

    // 仅查询bp时存在
    @JsonProperty("weight")
    var weight: Weight? = null

    data class Weight(
        @JsonProperty("percentage") val percentage: Float,
        @JsonProperty("pp") val weightedPP: Float
    ) {
        val index: Int
            get() = (ln((percentage / 100).toDouble()) / ln(0.95)).roundToInt()
    }

    @JsonProperty("beatmap")
    var beatmap: Beatmap? = null

    @JsonProperty("beatmapset")
    var beatmapset: Beatmapset? = null

    var user: MicroUser? = null

    @JsonProperty("mode") fun setMode(mode: String?) {
        this.mode = getMode(mode)
    }

    private val createTimePretty: LocalDateTime
        get() {
            if (createTime != null) return LocalDateTime.parse(
                createTime!!,
                formatter
            ).plusHours(8L)
            return LocalDateTime.now()
        }

    private val weightedPP: Float
        get() = if (weight != null) {
            weight!!.weightedPP
        } else {
            0f
        }

    override fun toString(): String {
        return "Score(createTime=$createTime, accuracy=$accuracy, bestID=$bestID, maxCombo=$maxCombo, uID=$userID, scoreID=$scoreID, mode=$mode, modeInt=$modeInt, mods=$mods, passed=$passed, perfect=$perfect, pp=$pp, rank=$rank, replay=$replay, score=$score, statistics=$statistics, type=$type, legacy=$legacy, weight=$weight, beatmap=$beatmap, beatmapset=$beatmapset, user=$user, createTimePretty=$createTimePretty, weightedPP=$weightedPP)"
    }

    companion object {
        val formatter: DateTimeFormatter = DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .appendLiteral("T")
            .appendPattern("HH:mm:ss")
            .appendZoneId().toFormatter()
    }
}
