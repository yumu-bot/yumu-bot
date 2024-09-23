package com.now.nowbot.model.jsonData

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.jsonData.Score.Weight
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Objects
import kotlin.math.ln
import kotlin.math.roundToInt

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true, allowSetters = true, allowGetters = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
open class Score {
    //@JsonProperty("statistics")
    var accuracy: Double? = null

    @JsonProperty("best_id") var bestID: Long? = null

    @JsonProperty("max_combo") var maxCombo: Int? = null

    @JsonProperty("user_id") var UID: Long? = null

    @JsonAlias("created_at") var createTime: String? = null

    @JsonProperty("id") var scoreID: Long? = null

    @JsonIgnoreProperties var mode: OsuMode? = null

    @JsonProperty("mode_int") var modeInt: Int? = null

    var osuMods: MutableList<String?>? = null

    var passed: Boolean? = null

    var perfect: Boolean? = null

    @JsonProperty("pp") var PP: Float? = null

    var rank: String? = null

    var replay: Boolean? = null

    var score: Int? = null

    var statistics: Statistics? = null

    var type: String? = null

    @JsonIgnoreProperties var legacy: Boolean = false

    // 仅查询bp时存在
    @JsonProperty("weight") var weight: Weight? = null

    @JvmRecord
    data class Weight(
        @JsonProperty("percentage") val percentage: Float?,
        @JsonProperty("pp") val weightedPP: Float,
    ) {
        fun getIndex(): Int {
            val i = ln((percentage!! / 100).toDouble()) / ln(0.95)
            return i.roundToInt()
        }
    }

    @JsonProperty("beatmap") var beatMap: BeatMap? = null

    @JsonProperty("beatmapset") var beatMapSet: BeatMapSet? = null

    var user: MicroUser? = null

    @JsonProperty("mode")
    fun setMode(mode: String?) {
        this.mode = OsuMode.getMode(mode)
    }

    fun getAccuracy(): Double? {
        return accuracy
    }

    fun setAccuracy(accuracy: Double?) {
        this.accuracy = accuracy
    }

    fun getBestID(): Long? {
        return bestID
    }

    fun setBestID(bestID: Long?) {
        this.bestID = bestID
    }

    fun getUID(): Long? {
        return UID
    }

    fun setUID(UID: Long?) {
        this.UID = UID
    }

    fun getCreateTimePretty(): LocalDateTime {
        if (createTime == null) return LocalDateTime.now()
        else return LocalDateTime.parse(createTime!!, formatter).plusHours(8L)
    }

    @JsonProperty("create_at_str")
    fun getCreateTime(): String? {
        return createTime
    }

    fun setCreateTime(createTime: String?) {
        this.createTime = createTime
    }

    fun getScoreID(): Long? {
        return scoreID
    }

    fun setScoreID(scoreID: Long?) {
        this.scoreID = scoreID
    }

    fun getMode(): OsuMode? {
        return mode
    }

    fun setMode(mode: OsuMode?) {
        this.mode = mode
    }

    fun getModeInt(): Int? {
        return modeInt
    }

    fun setModeInt(modeInt: Int?) {
        this.modeInt = modeInt
    }

    fun getMods(): MutableList<String?>? {
        return osuMods
    }

    fun setMods(osuMods: MutableList<String?>?) {
        this.osuMods = osuMods
    }

    fun getPassed(): Boolean? {
        return passed
    }

    fun setPassed(passed: Boolean?) {
        this.passed = passed
    }

    fun getPerfect(): Boolean? {
        return perfect
    }

    fun setPerfect(perfect: Boolean?) {
        this.perfect = perfect
    }

    fun getPP(): Float {
        if (Objects.nonNull(PP)) {
            return PP!!
        }

        // PPY PP 有时候是 null
        if (
            Objects.nonNull(weight) &&
                Objects.nonNull(weight!!.percentage) &&
                Objects.nonNull(weight!!.weightedPP) &&
                weight!!.percentage!! > 0
        ) {
            return weight!!.weightedPP / (weight!!.percentage!! / 100f)
        }

        return 0f
    }

    fun setPP(pp: Float) {
        this.PP = pp
    }

    fun getWeightedPP(): Float {
        if (Objects.nonNull(weight) && Objects.nonNull(weight!!.weightedPP)) {
            return weight!!.weightedPP
        }

        return 0f
    }

    fun getRank(): String? {
        return rank
    }

    fun setRank(rank: String?) {
        this.rank = rank
    }

    fun getReplay(): Boolean? {
        return replay
    }

    fun setReplay(replay: Boolean?) {
        this.replay = replay
    }

    fun getScore(): Int? {
        return score
    }

    fun setScore(score: Int?) {
        this.score = score
    }

    fun getType(): String? {
        return type
    }

    fun setType(type: String?) {
        this.type = type
    }

    fun isLegacy(): Boolean {
        legacy = Objects.nonNull(type) && type != "solo_score" // 目前只看见有这个类别，mp 房也是这个类别
        return legacy
    }

    fun setLegacy(legacy: Boolean) {
        this.legacy = legacy
    }

    fun getStatistics(): Statistics? {
        return statistics
    }

    fun setStatistics(statistics: Statistics?) {
        this.statistics = statistics
    }

    fun getBeatMap(): BeatMap? {
        return beatMap
    }

    fun setBeatMap(beatMap: BeatMap?) {
        this.beatMap = beatMap
    }

    fun getBeatMapSet(): BeatMapSet? {
        return beatMapSet
    }

    fun setBeatMapSet(beatMapSet: BeatMapSet?) {
        this.beatMapSet = beatMapSet
    }

    fun getUser(): MicroUser? {
        return user
    }

    fun setUser(user: MicroUser?) {
        this.user = user
    }

    fun getMaxCombo(): Int? {
        return maxCombo
    }

    fun setMaxCombo(maxCombo: Int?) {
        this.maxCombo = maxCombo
    }

    fun isPerfect(): Boolean? {
        return perfect
    }

    fun getWeight(): Weight? {
        return weight
    }

    fun setWeight(weight: Weight?) {
        this.weight = weight
    }

    override fun toString(): String {
        return "Score{accuracy=${accuracy}, bestID=${bestID}, maxCombo=${maxCombo}, UID=${UID}, createTime='${createTime}${'\''}, scoreID=${scoreID}, mode=${mode}, modeInt=${modeInt}, osuMods=${osuMods}, passed=${passed}, perfect=${perfect}, PP=${PP}, rank='${rank}${'\''}, replay=${replay}, score=${score}, statistics=${statistics}, type='${type}${'\''}, legacy=${legacy}, weight=${weight}, beatMap=${beatMap}, beatMapSet=${beatMapSet}, user=${user}${'}'}"
    }

    companion object {
        @JvmField
        val formatter: DateTimeFormatter =
            DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd")
                .appendLiteral("T")
                .appendPattern("HH:mm:ss")
                .appendZoneId()
                .toFormatter()
    }
}
