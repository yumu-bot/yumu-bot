package com.now.nowbot.model.mappool.old

import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.json.BeatMap

class ModPool(private val abbr: String, val beatmaps: List<BeatMap>) {
    val modColor: String
        get() = "#000"

    val mod: LazerMod
        get() = LazerMod.getModFromAcronym(abbr)
}
