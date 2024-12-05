package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.spring.osu.extended.rosu.*
import kotlin.reflect.full.memberProperties

@JsonInclude(JsonInclude.Include.NON_NULL)
open class RosuPerformance(result: JniPerformanceAttributes? = null) {
    @JsonProperty("pp")
    var pp: Double = 0.0

    @JsonProperty("aim_pp")
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
        when (result) {
            is OsuPerformanceAttributes -> {
                ppAim = result.ppAim
                ppSpeed = result.ppSpeed
                ppAcc = result.ppAcc
                ppFlashlight = result.ppFlashlight
                effectiveMissCount = result.effectiveMissCount
                stars = result.difficulty.stars
                pp = result.pp
            }

            is TaikoPerformanceAttributes -> {
                ppAcc = result.ppAcc
                ppDifficulty = result.ppDifficulty
                effectiveMissCount = result.effectiveMissCount
                stars = result.difficulty.stars
                pp = result.pp
            }

            is ManiaPerformanceAttributes -> {
                ppDifficulty = result.ppDifficulty
                stars = result.difficulty.stars
                pp = result.pp
            }

            is CatchPerformanceAttributes -> {
                stars = result.difficulty.stars
                pp = result.pp
            }

            else -> {}
        }
    }

    companion object {
        inline fun <reified T : Any> T.asMap() : Map<String, Double> {
            val props = T::class.memberProperties.associateBy { it.name }

            return props.keys
                .associateWith { props[it]?.get(this) }
                .filterNot { it.value == null }
                .mapKeys { it.key.toUnderline() }
                .mapValues { it.value.toString().toDouble() }
        }

        fun String.toUnderline(): String {
            return this.replace("([a-z])([A-Z])".toRegex(), "$1_$2").lowercase()
        }
    }
    class FullRosuPerformance(result: JniPerformanceAttributes): RosuPerformance(result) {
        @JsonProperty("full_pp")
        var fullPP:Double?= null

        @JsonProperty("perfect_pp")
        var perfectPP:Double?= null
    }
}