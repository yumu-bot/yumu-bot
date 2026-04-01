package com.now.nowbot.model.cosu

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.osu.Statistics

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class CosuStatistics(
    @field:JsonProperty("none")
    val none: Int = 0,

    @field:JsonProperty("miss")
    var miss: Int = 0,

    @field:JsonProperty("meh")
    var meh: Int = 0,

    @field:JsonProperty("ok")
    var ok: Int = 0,

    @field:JsonProperty("good")
    var good: Int = 0,

    @field:JsonProperty("great")
    var great: Int = 0,

    @field:JsonProperty("perfect")
    var perfect: Int = 0,

    @field:JsonProperty("small_tick_miss")
    var smallTickMiss: Int = 0,

    @field:JsonProperty("small_tick_hit")
    var smallTickHit: Int = 0,

    @field:JsonProperty("large_tick_miss")
    var largeTickMiss: Int = 0,

    @field:JsonProperty("large_tick_hit")
    var largeTickHit: Int = 0,

    @field:JsonProperty("small_bonus")
    var smallBonus: Int = 0,

    @field:JsonProperty("large_bonus")
    var largeBonus: Int = 0,

    @field:JsonProperty("ignore_miss")
    var ignoreMiss: Int = 0,

    @field:JsonProperty("ignore_hit")
    var ignoreHit: Int = 0,

    @field:JsonProperty("combo_break")
    var comboBreak: Int = 0,

    @field:JsonProperty("slider_tail_hit")
    var sliderTailHit: Int = 0,

    @field:JsonProperty("legacy_combo_increase")
    var legacyComboIncrease: Int = 0,
) {
    companion object {
        // 我操, 骂娘转换还跟 osu 不一样
        fun create(s: Statistics): CosuStatistics = CosuStatistics(
            great = s.count300 ?: 0,
            ok = s.count100 ?: 0,
            meh = s.count50 ?: 0,
            miss = s.countMiss ?: 0,
        )
    }
}

