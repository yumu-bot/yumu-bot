package com.now.nowbot.model.mappool.old

import com.now.nowbot.model.json.BeatMap

class ModPool(val modStr: String, val beatmaps: List<BeatMap>) {
    val modColor: String
        get() = Mod.getColor(modStr)

    val mod: Mod
        get() = Mod.getModFromAbbreviation(modStr)
}
