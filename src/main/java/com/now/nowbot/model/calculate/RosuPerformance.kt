package com.now.nowbot.model.calculate

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMode
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
open class RosuPerformance(result: me.aloic.rosupp.PerformanceResult? = null) : CalculatePerformance {
    @JsonProperty("pp")
    override var pp: Double = 0.0

    @JsonProperty("pp_aim")
    var ppAim: Double? = null

    @JsonProperty("pp_speed")
    var ppSpeed: Double? = null

    @JsonProperty("pp_acc")
    var ppAcc: Double? = null

    @JsonProperty("pp_flashlight")
    var ppFlashlight: Double? = null

    @JsonProperty("pp_difficulty")
    var ppDifficulty: Double? = null

    @JsonProperty("effective_miss_count")
    var effectiveMissCount: Double? = null

    @JsonProperty("stars")
    var stars: Double? = null

    init {
        when (result?.mode?.toOsuMode()) {
            OsuMode.OSU -> {
                ppAim = result.ppAim
                ppSpeed = result.ppSpeed
                ppAcc = result.ppAccuracy
                ppFlashlight = result.ppFlashlight
                effectiveMissCount = result.effectiveMissCount
                stars = result.difficulty.stars
                pp = result.pp
            }

            OsuMode.TAIKO -> {
                ppAcc = result.ppAccuracy
                ppDifficulty = result.ppDifficulty
                effectiveMissCount = result.effectiveMissCount
                stars = result.difficulty.stars
                pp = result.pp
            }

            OsuMode.MANIA -> {
                ppDifficulty = result.ppDifficulty
                stars = result.difficulty.stars
                pp = result.pp
            }

            OsuMode.CATCH -> {
                stars = result.difficulty.stars
                pp = result.pp
            }

            else -> {}
        }
    }

    companion object {
        fun me.aloic.rosupp.GameMode.toOsuMode(): OsuMode {
            return when (this) {
                me.aloic.rosupp.GameMode.OSU -> OsuMode.OSU
                me.aloic.rosupp.GameMode.TAIKO -> OsuMode.TAIKO
                me.aloic.rosupp.GameMode.CATCH -> OsuMode.CATCH
                me.aloic.rosupp.GameMode.MANIA -> OsuMode.MANIA
            }
        }
    }

    class FullRosuPerformance(result: me.aloic.rosupp.PerformanceResult): RosuPerformance(result), FullCalculatePerformance {
        @JsonProperty("full_pp")
        override var fullPP: Double = 0.0

        @JsonProperty("perfect_pp")
        override var perfectPP: Double = 0.0
    }
}