package com.now.nowbot.model.mappool.old

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.json.BeatMap

class ModPool(abbr: String, val beatmaps: List<BeatMap>) {

    @get:JsonProperty("mod")
    val mod = LazerMod.getModFromAcronym(abbr)

    @get:JsonProperty("mod_color")
    val modColor: String
        get() = mod.color

    @get:JsonProperty("mod_str")
    val modStr: String
        get() = mod.acronym
}
