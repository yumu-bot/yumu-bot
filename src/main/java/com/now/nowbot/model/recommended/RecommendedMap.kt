package com.now.nowbot.model.recommended

import com.now.nowbot.model.osu.Beatmap

class RecommendedMap private constructor() {
    var bid: Long? = null
    var bgUrl: String? = null
    var star: Double? = null
    var oD: Float? = null
    var aR: Float? = null
    var hP: Float? = null
    var cS: Float? = null
    var bpm: Float? = null

    var objects: Int? = null
    var length: Int? = null

    var title: String? = null
    var version: String? = null

    var introduction: String? = null

    var minPlayerPP: Float? = null
    var maxPlayerPP: Float? = null
    fun setPlayerPP() {
        if (star!! < 1.5) {
            minPlayerPP = 0f
            maxPlayerPP = 500f
        } else if (star!! < 3) {
            minPlayerPP = 300f
            maxPlayerPP = 800f
        } else if (star!! < 4.5) {
            minPlayerPP = 300f
            maxPlayerPP = 1000f
        } else if (star!! < 5.3) {
            minPlayerPP = 1000f
            maxPlayerPP = 2500f
        } else if (star!! < 5.9) {
            minPlayerPP = 2000f
            maxPlayerPP = 3000f
        } else if (star!! < 6.4) {
            minPlayerPP = 2500f
            maxPlayerPP = 5000f
        } else {
            minPlayerPP = 3500f
            maxPlayerPP = -1f
        }
    }

    companion object {
        fun getMap(b: Beatmap): RecommendedMap {
            val data = RecommendedMap()
            data.bid = b.beatmapID
            if (b.beatmapset != null) {
                data.bgUrl = b.beatmapset!!.covers.cover2x
            }
            data.star = b.starRating
            data.oD = b.OD
            data.aR = b.AR
            data.hP = b.HP
            data.cS = b.CS
            data.bpm = b.BPM
            data.objects = b.spinners!! + b.circles!! + b.sliders!!
            data.length = b.totalLength
            data.title = b.beatmapset!!.titleUnicode
            data.version = b.difficultyName
            data.setPlayerPP()
            return data
        }
    }
}

