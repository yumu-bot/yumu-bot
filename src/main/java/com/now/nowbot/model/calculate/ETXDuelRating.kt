package com.now.nowbot.model.calculate

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import java.time.OffsetDateTime

data class ETXDuelRating(
    @field:JsonProperty("star")
    @field:JsonAlias("osuDuelStarRating")
    var star: Double = 0.0,

    @field:JsonProperty("no_mod")
    @field:JsonAlias("osuNoModDuelStarRating")
    var noMod: Double = 0.0,

    @field:JsonProperty("hidden")
    @field:JsonAlias("osuHiddenDuelStarRating")
    var hidden: Double = 0.0,

    @field:JsonProperty("hard_rock")
    @field:JsonAlias("osuHardRockDuelStarRating")
    var hardRock: Double = 0.0,

    @field:JsonProperty("double_time")
    @field:JsonAlias("osuDoubleTimeDuelStarRating")
    var doubleTime: Double = 0.0,

    @field:JsonProperty("free_mod")
    @field:JsonAlias("osuFreeModDuelStarRating")
    var freeMod: Double = 0.0,

    @field:JsonProperty("outdated")
    @field:JsonAlias("osuDuelOutdated")
    var outdated: Boolean = true,

    @field:JsonProperty("provisional")
    @field:JsonAlias("osuDuelProvisional")
    var provisional: Boolean = true,

    @field:JsonProperty("updated_at")
    @field:JsonAlias("updatedAt")
    @field:JsonFormat(
        shape = JsonFormat.Shape.STRING,
        pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        timezone = "UTC"
    )
    var updatedAt: OffsetDateTime? = null
) {

    @JsonSetter("osuDuelStarRating")
    fun setStarFromAny(value: Any) {
        star = when (value) {
            is String -> value.toDoubleOrNull() ?: 0.0
            is Number -> value.toDouble()
            else -> 0.0
        }
    }
}