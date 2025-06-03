package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.*
import com.now.nowbot.util.JacksonUtil
import kotlin.math.roundToInt

data class LazerStatistics(
    // M 320
    @JsonProperty("perfect") var perfect: Int = 0,

    // O、T、C、M 300
    @JsonProperty("great") var great: Int = 0,

    // M 200
    @JsonProperty("good") var good: Int = 0,

    // T、M 100
    @JsonProperty("ok") var ok: Int = 0,

    // O、M 50
    @JsonProperty("meh") var meh: Int = 0,

    // O、T、C、M 0
    @JsonProperty("miss") var miss: Int = 0,

    // ?
    @JsonProperty("ignore_hit") var ignoreHit: Int = 0,
    @JsonProperty("ignore_miss") var ignoreMiss: Int = 0,

    // O SliderTick、C Large Droplet (medium)
    @JsonProperty("large_tick_hit") var largeTickHit: Int = 0,
    @JsonProperty("large_tick_miss") var largeTickMiss: Int = 0,

    // C Small Droplet (small)
    @JsonProperty("small_tick_hit") var smallTickHit: Int = 0,
    @JsonProperty("small_tick_miss") var smallTickMiss: Int = 0,

    // O SliderTail
    @JsonProperty("slider_tail_hit") var sliderTailHit: Int = 0,

    // O Spinner Bonus、T Spinner Drumroll、C Banana
    @JsonProperty("large_bonus") var largeBonus: Int = 0,

    // O Spinner Base
    @JsonProperty("small_bonus") var smallBonus: Int = 0,

    // 仅 MAX 有
    @JsonProperty("legacy_combo_increase") var legacyComboIncrease: Int = 0,
) {
    fun getTotalHits(mode: OsuMode) = when (mode) {
        OSU -> great + ok + meh + miss
        TAIKO -> great + ok + miss
        CATCH -> great + largeTickHit + smallTickHit + miss
        MANIA -> perfect + great + good + ok + meh + miss
        else -> 0
    }

    fun accuracy(mode: OsuMode): Float {
        val numerator: Int
        val denominator: Int
        when (mode) {
            OSU -> {
                numerator = great * 300 + ok * 100 + meh * 50
                denominator = getTotalHits(mode) * 300
            }

            TAIKO -> {
                numerator = great * 2 + ok * 1
                denominator = getTotalHits(mode) * 2
            }

            CATCH -> {
                numerator = great + largeTickHit + smallTickHit
                denominator = getTotalHits(mode)
            }

            MANIA -> {
                numerator = (perfect + great) * 300 + good * 200 + ok * 100 + meh * 50
                denominator = getTotalHits(mode) * 300
            }

            else -> return 0f
        }

        return (10000f * numerator / denominator).roundToInt() / 100f
    }

    fun toScoreStatistics(mode: OsuMode): Statistics {
        var geki = 0
        var katu = 0
        val n300 = great
        val misses = miss
        val n100: Int
        val n50: Int
        when (mode) {
            OSU -> {
                n100 = ok
                n50 = meh
            }

            TAIKO -> {
                n100 = ok
                n50 = 0
            }

            CATCH -> {
                n100 = largeTickHit
                n50 = smallTickHit
                katu = smallTickMiss
            }

            MANIA -> {
                geki = perfect
                katu = good
                n100 = ok
                n50 = meh
            }

            else -> return Statistics()
        }
        return Statistics().apply {
            count300 = n300
            count100 = n100
            count50 = n50
            countMiss = misses
            countGeki = geki
            countKatu = katu
        }
    }

    fun toShortJson(): String {
        val map = mutableMapOf<String, Int>()
        perfect.takeIf { it > 0 }?.let { map["perfect"] = it }
        great.takeIf { it > 0 }?.let { map["great"] = it }
        good.takeIf { it > 0 }?.let { map["good"] = it }
        ok.takeIf { it > 0 }?.let { map["ok"] = it }
        meh.takeIf { it > 0 }?.let { map["meh"] = it }
        miss.takeIf { it > 0 }?.let { map["miss"] = it }
        ignoreHit.takeIf { it > 0 }?.let { map["ignore_hit"] = it }
        ignoreMiss.takeIf { it > 0 }?.let { map["ignore_miss"] = it }
        largeTickHit.takeIf { it > 0 }?.let { map["large_tick_hit"] = it }
        largeTickMiss.takeIf { it > 0 }?.let { map["large_tick_miss"] = it }
        smallTickHit.takeIf { it > 0 }?.let { map["small_tick_hit"] = it }
        smallTickMiss.takeIf { it > 0 }?.let { map["small_tick_miss"] = it }
        sliderTailHit.takeIf { it > 0 }?.let { map["slider_tail_hit"] = it }
        largeBonus.takeIf { it > 0 }?.let { map["large_bonus"] = it }
        smallBonus.takeIf { it > 0 }?.let { map["small_bonus"] = it }
        legacyComboIncrease.takeIf { it > 0 }?.let { map["legacy_combo_increase"] = it }
        return JacksonUtil.toJson(map)
    }
}