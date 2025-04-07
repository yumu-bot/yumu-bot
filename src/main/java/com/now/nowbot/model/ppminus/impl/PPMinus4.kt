package com.now.nowbot.model.ppminus.impl

import com.now.nowbot.entity.PPMinusLite
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.util.DataUtil
import kotlin.math.*

abstract class PPMinus4 {
    abstract var values: List<Double>

    companion object {
        fun getInstance(user: OsuUser, bests: List<LazerScore>, surrounding: List<PPMinusLite>, mode: OsuMode = OsuMode.DEFAULT): PPMinus4? {
            return when (OsuMode.getMode(mode, user.currentOsuMode)) {
                OsuMode.OSU -> PPMinus4Osu(user, bests, surrounding)
                OsuMode.TAIKO -> PPMinus4Osu(user, bests, surrounding)
                OsuMode.CATCH -> PPMinus4Osu(user, bests, surrounding)
                OsuMode.MANIA -> PPMinus4Osu(user, bests, surrounding)
                else -> null
            }
        }
    }
}

class PPMinus4Osu(user: OsuUser, bests: List<LazerScore>, surrounding: List<PPMinusLite>): PPMinus4() {
    override var values: List<Double>

    init {
        val me = PPMinusLite().toLite(user, bests)

        val fa = getRelativePPMinus(me.accuracy, surrounding.map { it.accuracy})
        val ba = getRelativePPMinus(me.topAccuracy, surrounding.map { it.topAccuracy })

        val bir = getRelativePPMinus((me.topPP ?: 0.0) / (me.rawPP ?: 1.0), surrounding.map { (it.topPP ?: 0.0) / (it.rawPP ?: 1.0) })
        val bdr = getRelativePPMinus((me.topPP ?: 0.0) / ((me.bottomPP ?: 0.0) + 1.0), surrounding.map { (it.topPP ?: 0.0) / ((it.bottomPP ?: 0.0) + 1.0) })

        val spt = getRelativePPMinus((me.playTime ?: 0L) / ((me.playCount ?: 0L) + 1L), surrounding.map { (it.playTime ?: 0L) / ((it.playCount ?: 0L) + 1L) })
        val bpt = getRelativePPMinus((me.topLength ?: 0) * 0.7 + (me.middleLength ?: 0) * 0.2 + (me.bottomLength ?: 0) * 0.1, surrounding.map { (it.topLength ?: 0) * 0.7 + (it.middleLength ?: 0) * 0.2 + (it.bottomLength ?: 0) * 0.1 })

        val fcr = getRelativePPMinus((me.countFC ?: 0) * 1.0 / (me.count ?: 1), surrounding.map { (it.countFC ?: 0) * 1.0 / (it.count ?: 1) })
        val grs = getRelativePPMinus((me.countSS ?: 0) * 3 + (me.countS ?: 0) * 2 + (me.countA ?: 0) * 1 - (me.countC ?: 0) * 1 - (me.countD ?: 0) * 2 + (me.count ?: 0) * 2, surrounding.map { (it.countSS ?: 0) * 3 + (it.countS ?: 0) * 2 + (it.countA ?: 0) * 1 - (it.countC ?: 0) * 1 - (it.countD ?: 0) * 2 + (it.count ?: 0) * 2 })

        val lnh = getRelativePPMinus(ln1p((me.totalHits ?: 0).toDouble()), surrounding.map { ln1p((it.totalHits ?: 0).toDouble()) })
        val lnt = getRelativePPMinus(ln1p((me.playTime ?: 0).toDouble()), surrounding.map { ln1p((it.playTime ?: 0).toDouble()) })

        val hpt = getRelativePPMinus((me.totalHits ?: 0) * 1.0 / ((me.playTime ?: 0) + 1.0), surrounding.map { (it.totalHits ?: 0) * 1.0 / ((it.playTime ?: 0) + 1.0) })
        val lnx = getRelativePPMinus(ln1p((me.topPP ?: 0.0) * (me.topLength ?: 0)), surrounding.map { ln1p((it.topPP ?: 0.0) * (me.topLength ?: 0)) })

        val total = (fa + ba) / 2 * 0.2 + (bir + bdr) / 2 * 0.1 + (spt + bpt) / 2 * 0.2 + (fcr + grs) / 2 * 0.2 + (lnh + lnt) / 2 * 0.1 + (hpt + lnx) / 2 * 0.2 * 100.0

        val san = 101.0

        values = listOf((fa + ba) / 2, (bir + bdr) / 2, (spt + bpt) / 2, (fcr + grs) / 2, (lnh + lnt) / 2, (hpt + lnx) / 2, total, san)
    }
}

/**
 * 获取相对 PPM 值。
 * 逻辑：>=μ+3δ或最大值：101。>=μ+2δ：100。>=μ+δ：90。>=μ：80。>=μ-δ：70。>=μ-2δ：60。等于 0：0
 *
 */
private fun <T: Number, U: Number> getRelativePPMinus(compare: T?, to: List<U?>) : Double {
    if (to.size <= 4) return 0.8 // 数据太少

    val too = to.map { it?.toDouble() ?: 0.0 }
    val coo = compare?.toDouble() ?: 0.0

    val normal = DataUtil.getNormalDistribution(too)
    val u = normal.first
    val o = sqrt(normal.second)

    val max = too.max()

    return if ((coo >= max - 1e-4) || coo >= u + 3*o) {
        1.01
    } else if (coo >= u + 2*o) {
        1.0 + 0.01 * (coo - (u + 2*o)) / o
    } else if (coo >= u + o) {
        0.9 + 0.1 * (coo - (u + o)) / o
    } else if (coo >= u) {
        0.8 + 0.1 * (coo - (u)) / o
    } else if (coo >= u - o) {
        0.7 + 0.1 * (coo - (u - o)) / o
    } else if (coo >= u - 2*o) {
        0.6 + 0.1 * (coo - (u - 2*o)) / o
    } else {
        max(0.6 * (coo) / (u - 2*o), 0.0)
    }
}

private fun Double.clamp(max: Double = 1.2, min: Double = 0.0): Double {
    return min(max, max(this, min))
}