package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.model.enums.OsuMode
import org.springframework.lang.NonNull
import org.springframework.lang.Nullable
import kotlin.math.pow
import kotlin.math.round

/**
 * 注意，成绩内的 Statistics 只有 count_xxx 的指标
 */
@JsonInclude(JsonInclude.Include.NON_NULL) @JsonIgnoreProperties(ignoreUnknown = true)
open class Statistics : Cloneable {
    @Nullable @JsonProperty("count_50")
    var count50: Int? = 0

    @Nullable @JsonProperty("count_100")
    var count100: Int? = 0

    @Nullable @JsonProperty("count_300")
    var count300: Int? = 0

    @Nullable @JsonProperty("count_geki")
    var countGeki: Int? = 0

    @Nullable @JsonProperty("count_katu")
    var countKatu: Int? = 0

    @Nullable @JsonProperty("count_miss")
    var countMiss: Int? = 0

    val countAll: Int
        get() = getCountAll(OsuMode.MANIA)

    @JvmField @JsonProperty("ranked_score")
    var rankedScore: Long? = null

    @JvmField @JsonProperty("total_score")
    var totalScore: Long? = null

    var pp: Double? = null

    @JvmField @JsonProperty("hit_accuracy")
    var accuracy: Double? = null

    @JvmField @JsonProperty("play_count")
    var playCount: Long? = null

    @JvmField @JsonProperty("play_time")
    var playTime: Long? = null

    @JvmField @JsonProperty("total_hits")
    var totalHits: Long? = null

    @JvmField @JsonProperty("maximum_combo")
    var maxCombo: Int = 0

    @JsonProperty("is_ranked")
    var ranked: Boolean? = null

    @JvmField @JsonProperty("global_rank")
    var globalRank: Long? = null

    @JsonProperty("replays_watched_by_others")
    var replaysWatchedByOthers: Int = 0

    @JvmField @JsonProperty("country_rank")
    var countryRank: Long? = null

    @get:JsonProperty("level_current")
    var levelCurrent: Int = 0

    @get:JsonProperty("level_progress")
    var levelProgress: Int = 0

    @JsonIgnoreProperties
    var countSS: Int = 0

    @JsonIgnoreProperties
    var countSSH: Int = 0

    @JsonIgnoreProperties
    var countS: Int = 0

    @JsonIgnoreProperties
    var countSH: Int = 0

    @JsonIgnoreProperties
    var countA: Int = 0
    
    @JsonProperty("level") fun setLevel(map: Map<String, Int>) {
        levelCurrent = map["current"] as Int
        levelProgress = map["progress"] as Int
    }

    @JsonProperty("grade_counts") fun setGrade(map: Map<String, Int>) {
        countSS = map["ss"] as Int
        countSSH = map["ssh"] as Int
        countS = map["s"] as Int
        countSH = map["sh"] as Int
        countA = map["a"] as Int
    }

    @get:JsonProperty("country_rank_7k") @JsonIgnore
    var countryRank7K: Int = 0

    @get:JsonProperty("country_rank_4k") @JsonIgnore
    var countryRank4K: Int = 0

    @get:JsonProperty("rank_7k") @JsonIgnore
    var rank7K: Int = 0

    @get:JsonProperty("rank_4k") @JsonIgnore
    var rank4K: Int = 0

    @get:JsonProperty("pp_7k") @JsonIgnore
    var pp7K: Double? = 0.0

    @get:JsonProperty("pp_4k") @JsonIgnore
    var pp4K: Double? = 0.0

    @JsonProperty("variants") fun setVariants(data: JsonNode?) {
        if (data != null && !data.isEmpty) {
            for (m in data) {
                if (m["variant"].asText() == "4k") {
                    countryRank4K = m["country_rank"].asInt()
                    rank4K = m["global_rank"].asInt()
                    pp4K = m["pp"].asDouble()
                } else {
                    countryRank7K = m["country_rank"].asInt()
                    rank7K = m["global_rank"].asInt()
                    pp7K = m["pp"].asDouble()
                }
            }
        }
    }

    @NonNull fun getCountAll(mode: OsuMode?): Int {
        val c300 = count300 ?: 0
        val c100 = count100 ?: 0
        val c50 = count50 ?: 0
        val cG = countGeki ?: 0
        val cK = countKatu ?: 0
        val c0 = countMiss ?: 0

        return when (mode) {
            OsuMode.OSU, OsuMode.DEFAULT -> c300 + c100 + c50 + c0
            OsuMode.TAIKO -> c300 + c100 + c0
            OsuMode.CATCH -> c300 + c100 + c50 + c0 + cK
            OsuMode.MANIA -> cG + c300 + cK + c100 + c50 + c0
            null -> cG + c300 + cK + c100 + c50 + c0
        }
    }

    fun getAccuracy(mode: OsuMode): Double {
        val c300 = count300 ?: 0
        val c100 = count100 ?: 0
        val c50 = count50 ?: 0
        val cG = countGeki ?: 0
        val cK = countKatu ?: 0
        // val c0 = countMiss ?: 0
        
        return when (mode) {
            OsuMode.OSU, OsuMode.DEFAULT -> {
                (c50 / 6.0 + c100 / 3.0 + c300) / getCountAll(OsuMode.OSU)
            }

            OsuMode.TAIKO -> {
                (c100 / 2.0 + c300) / getCountAll(OsuMode.TAIKO)
            }

            OsuMode.CATCH -> {
                (c50 + c100 + c300) * 1.0 / getCountAll(OsuMode.CATCH)
            }

            OsuMode.MANIA -> {
                (c50 / 6.0 + c100 / 3.0 + c300 + cK * 2.0 / 3.0 + cG) / getCountAll(
                    OsuMode.MANIA
                )
            }
        }
    }

    /**
     * 获得指定小数位的 acc
     * @param i 位数
     * @return acc
     */
    fun getAccuracy(i: Int = 0): Double {
        val n = 10.0.pow(i.toDouble()).toInt().toDouble()
        val c = accuracy ?: 0.0

        val out = round(c * n) / n
        return out
    }

    /**
     * 获得指定小数位的pp
     * @param x 位数
     * @return pp
     */
    fun getPP(x: Int = 0): Double {
        val n = 10.0.pow(x.toDouble()).toInt().toDouble()
        val c = pp ?: 0.0

        val out = round(c * n) / n
        return out
    }

    @get:JsonIgnoreProperties
    val nonNull: Boolean
        get() = countGeki != null && count300 != null && countKatu != null && count100 != null && count50 != null && countMiss != null

    @get:JsonIgnoreProperties
    val isNull: Boolean
        get() = this.nonNull.not()


    public override fun clone(): Statistics {
        try {
            return super.clone() as Statistics
        } catch (e: CloneNotSupportedException) {
            throw AssertionError()
        }
    }

    override fun toString(): String {
        return "Statistics(count50=$count50, count100=$count100, count300=$count300, countGeki=$countGeki, countKatu=$countKatu, countMiss=$countMiss, countAll=$countAll, rankedScore=$rankedScore, totalScore=$totalScore, pp=$pp, accuracy=$accuracy, playCount=$playCount, playTime=$playTime, totalHits=$totalHits, maxCombo=$maxCombo, ranked=$ranked, globalRank=$globalRank, replaysWatchedByOthers=$replaysWatchedByOthers, countryRank=$countryRank, countSS=$countSS, countSSH=$countSSH, countS=$countS, countSH=$countSH, countA=$countA)"
    }
}
