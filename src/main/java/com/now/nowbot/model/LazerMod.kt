package com.now.nowbot.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.now.nowbot.model.enums.OsuMod

data class LazerMod(
    @JsonProperty("acronym")
    @JsonSerialize(using = OsuMod.OsuModSerializer::class)
    @JsonDeserialize(using = OsuMod.OsuModDeserializer::class)
    val type: OsuMod,
    @JsonProperty("settings")
    var settings: Settings?
) {
    data class Settings(
        @JsonProperty("speed_change")
        var speed: Double?,

        @JsonProperty("extra_lives")
        var extraLives: Int?,

        @JsonProperty("adjust_pitch")
        var adjustPitch: Boolean?,

        @JsonProperty("restart_on_fail")
        var restartOnFail: Boolean?,

        @JsonProperty("initial_rate")
        var initialSpeed: Double?,

        @JsonProperty("final_rate")
        var finalSpeed: Double?,

        @JsonProperty("circle_size")
        var cs: Double?,

        @JsonProperty("approach_rate")
        var ar: Double?,

        @JsonProperty("overall_difficulty")
        var od: Double?,

        @JsonProperty("drain_rate")
        var hp: Double?,

        @JsonProperty("extended_limits")
        var extendedLimits: Boolean?,
    )

    val speed: Double
        get() {
            return if (settings?.speed != null) {
                settings?.speed!!
            } else if (type == OsuMod.DoubleTime || type == OsuMod.Nightcore) {
                1.5
            } else if (type == OsuMod.HalfTime || type == OsuMod.Daycore) {
                0.75
            } else {
                1.0
            }
        }

    val finalSpeed: Double
        get() {
            return if (settings?.finalSpeed != null) {
                settings?.finalSpeed!!
            } else if (type == OsuMod.WindUp) {
                1.5
            } else if (type == OsuMod.WindDown) {
                0.75
            } else {
                1.0
            }
        }

    val cs by lazy {
        settings?.cs
    }
    val ar by lazy {
        settings?.ar
    }
    val od by lazy {
        settings?.od
    }
    val hp by lazy {
        settings?.hp
    }

    companion object {
        val speedChange = setOf(
            OsuMod.HalfTime,
            OsuMod.Daycore,
            OsuMod.DoubleTime,
            OsuMod.Nightcore,
            OsuMod.AdaptiveSpeed,
            OsuMod.WindUp,
            OsuMod.WindDown
        )

        val ratingChange = setOf(
            OsuMod.Easy,
            OsuMod.HardRock,
            OsuMod.HalfTime,
            OsuMod.Daycore,
            OsuMod.DoubleTime,
            OsuMod.Nightcore,
            OsuMod.WindUp,
            OsuMod.WindDown,
            OsuMod.Flashlight,
            OsuMod.TouchDevice,
            OsuMod.DifficultyAdjust,
            OsuMod.AdaptiveSpeed,
        )

        @JvmStatic
        fun getModSpeed(mods: List<LazerMod>?): Double {
            if (mods.isNullOrEmpty()) return 1.0
            mods.firstOrNull {
                it.type in speedChange
            }?.let {
                return it.speed
            }
            return 1.0
        }

        @JvmStatic
        fun hasChangeRating(mods: List<LazerMod>?): Boolean {
            if (mods.isNullOrEmpty()) return false
            return mods.any {
                it.type in ratingChange
            }
        }

        @JvmStatic
        fun getModsList(acronym: String?): List<LazerMod> {
            return OsuMod.getModsList(acronym).map { LazerMod(it, null) }
        }

        @JvmStatic
        fun getModsList(acronym: List<String>?): List<LazerMod> {
            return OsuMod.getModsList(acronym).map { LazerMod(it, null) }
        }

        @JvmStatic
        fun getModSpeedForStarCalculate(mods: List<LazerMod>?): Double {
            if (mods.isNullOrEmpty()) return 1.0

            mods.firstOrNull {
                it.type in speedChange
            }?.let {
                return if (it.type == OsuMod.WindUp || it.type == OsuMod.WindDown) {
                    it.finalSpeed
                } else {
                    it.speed
                }
            }
            return 1.0
        }

        @JvmStatic
        fun getModsValue(mods:Iterable<LazerMod>):Int{
            return OsuMod.getModsValue(mods.map(LazerMod::type))
        }
    }

    override fun toString() = type.acronym
}