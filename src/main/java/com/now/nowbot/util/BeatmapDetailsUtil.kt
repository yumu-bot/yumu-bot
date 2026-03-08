package com.now.nowbot.util

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.Mod
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

object BeatmapDetailsUtil {

    fun getMillisFromAR(ar: Float): Float = when {
        ar > 11f -> 300f
        ar > 5f -> 1200 - (150 * (ar - 5))
        ar > 0f -> 1800 - (120 * ar)
        else -> 1800f
    }

    fun getARFromMillis(ms: Float): Float = when {
        ms < 300 -> 11f
        ms < 1200 -> 5 + (1200 - ms) / 150f
        ms < 2400 -> (1800 - ms) / 120f
        else -> -5f
    }

    fun applyAR(ar: Float, mods: List<LazerMod>): Float {
        var a = ar

        for (mod in mods) {
            if (mod is LazerMod.DifficultyAdjust) {
                if (mod.approachRate != null) return mod.approachRate!!.roundToDigits2()
                break
            }
        }

        if (mods.contains(LazerMod.HardRock)) {
            a = (a * 1.4f).coerceIn(0f, 10f)
        } else if (mods.contains(LazerMod.Easy)) {
            a = (a / 2f).coerceIn(0f, 10f)
        }

        val speed = LazerMod.getModSpeed(mods)

        if (speed != 1f) {
            var ms = getMillisFromAR(a)
            ms = (ms / speed)
            a = getARFromMillis(ms)
        }

        return a.roundToDigits2()
    }

    fun getMillisFromOD(od: Float, mode: OsuMode): Float = when (mode) {
        OsuMode.TAIKO -> when {
            od > 11 -> 17f
            else -> 50 - 3 * od
        }

        OsuMode.MANIA -> when {
            od > 11 -> 31f
            else -> 64 - 3 * od
        }

        else -> when {
            od > 11 -> 14f
            else -> 80 - 6 * od
        }
    }

    fun getODFromMillis(ms: Float): Float = when {
        ms < 14 -> 11f
        else -> (80 - ms) / 6f
    }

    fun applyOD(od: Float, mods: List<LazerMod>, mode: OsuMode): Float {
        var o = od
        if (mods.contains(LazerMod.HardRock)) {
            o = (o * 1.4f).coerceIn(0f, 10f)
        } else if (mods.contains(LazerMod.Easy)) {
            o = (o / 2f).coerceIn(0f, 10f)
        }

        for (mod in mods) {
            if (mod is LazerMod.DifficultyAdjust) {
                if (mod.overallDifficulty != null) return mod.overallDifficulty!!.roundToDigits2()
                break
            }
        }

        // mania 模式内，模组改变速率不影响 OD
        val speed = if (mode == OsuMode.MANIA) {
            1f
        } else {
            LazerMod.getModSpeed(mods)
        }

        if (speed != 1f) {
            var ms = getMillisFromOD(o, mode)
            ms = (ms / speed)
            o = getODFromMillis(ms)
        }

        return o.roundToDigits2()
    }

    fun applyCS(cs: Float, mods: List<LazerMod>): Float {
        var c = cs

        for (mod in mods) {
            if (mod is LazerMod.DifficultyAdjust) {
                if (mod.circleSize != null) return mod.circleSize!!.roundToDigits2()
                break
            }
        }

        if (mods.contains(LazerMod.HardRock)) {
            c *= 1.3f
        } else if (mods.contains(LazerMod.Easy)) {
            c /= 2f
        }
        return c.coerceIn(0f, 10f).roundToDigits2()
    }

    fun applyHP(hp: Float, mods: List<LazerMod>): Float {
        var h = hp

        for (mod in mods) {
            if (mod is LazerMod.DifficultyAdjust) {
                if (mod.drainRate != null) return mod.drainRate!!.roundToDigits2()
                break
            }
        }

        if (mods.contains(LazerMod.HardRock)) {
            h *= 1.4f
        } else if (mods.contains(LazerMod.Easy)) {
            h /= 2f
        }
        return h.coerceIn(0f, 10f).roundToDigits2()
    }

    fun applyBPM(bpm: Float?, mods: List<LazerMod>): Float {
        return ((bpm ?: 0f) * LazerMod.getModSpeed(mods)).roundToDigits2()
    }

    fun applyLength(length: Int?, mods: List<LazerMod>): Int {
        return ((length ?: 0).toDouble() / LazerMod.getModSpeed(mods)).roundToInt()
    }

    private fun Float.roundToDigits2() = BigDecimal(this.toDouble()).setScale(2, RoundingMode.HALF_EVEN).toFloat()

    private inline fun <reified T : Mod> List<LazerMod>.contains(type: T) = LazerMod.hasMod(this, type)
}