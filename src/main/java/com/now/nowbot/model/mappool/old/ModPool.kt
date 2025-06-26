package com.now.nowbot.model.mappool.old

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.util.DataUtil

class ModPool(private val abbr: String, val beatmaps: List<Beatmap>) {

    @get:JsonProperty("mod")
    val mod: LazerMod?

    init {
        val m = LazerMod.getModFromAcronym(abbr)

        mod = if (m !is LazerMod.None && abbr.isNotBlank()) {
            m
        } else {
            null
        }
    }

    @get:JsonProperty("mod_color")
    val modColor: String
        get() = mod?.color ?: DataUtil.hsl2hex(abbr.hashCode() % 360, 60, 50)

    @get:JsonProperty("mod_str")
    val modStr: String
        get() = mod?.acronym ?: abbr
}
