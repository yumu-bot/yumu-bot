package com.now.nowbot.util

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerMod.Companion.contains
import com.now.nowbot.model.osu.LazerMod.Companion.getClockRate
import com.now.nowbot.model.osu.LazerMod.Companion.getKey
import com.now.nowbot.model.osu.LazerMod.Companion.isAffectStarRating
import com.now.nowbot.model.osu.LazerScore
import kotlin.collections.forEach
import kotlin.math.roundToInt

object BeatmapUtil {

    // 获取谱面的原信息，方便成绩面板使用。请在 applyBeatmapExtend 和 applyStarAndPP 之前用。
    fun getDetailMap(beatmap: Beatmap): Map<String, Any> {
        if (beatmap.cs == null) return mapOf()

        return mapOf(
            "cs" to beatmap.cs!!,
            "ar" to beatmap.ar!!,
            "od" to beatmap.od!!,
            "hp" to beatmap.hp!!,
            "bpm" to beatmap.bpm,
            "drain" to beatmap.hitLength!!,
            "total" to beatmap.totalLength,
        )
    }

    fun applyBeatmapChanges(scores: Collection<LazerScore>) {
        scores.forEach {
            applyBeatmapChanges(it)
        }
    }

    fun applyBeatmapChanges(scoresMap: Map<*, LazerScore>) {
        scoresMap.values.forEach {
            applyBeatmapChanges(it)
        }
    }

    fun applyBeatmapChanges(score: LazerScore) {
        applyBeatmapChanges(score.beatmap, score.mods)
    }

    fun applyBeatmapChanges(
        beatmap: Beatmap?,
        mods: List<LazerMod>
    ) {
        if (beatmap == null || beatmap.beatmapID == 0L) return

        val mode = beatmap.mode

        // 在修改之前锁定原始数据
        beatmap.captureOriginalDetails()

        if (mods.isAffectStarRating()) {
            beatmap.bpm = applyBPM(beatmap.bpm, mods)
            beatmap.ar = applyAR(beatmap.ar ?: 0f, mods)
            beatmap.cs = applyCS(beatmap.cs ?: 0f, mods, mode)
            beatmap.od = applyOD(beatmap.od ?: 0f, mods, mode)
            beatmap.hp = applyHP(beatmap.hp ?: 0f, mods)
            beatmap.totalLength = applyLength(beatmap.totalLength, mods)
            beatmap.hitLength = applyLength(beatmap.hitLength, mods)
        }
    }


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

        mods.filterIsInstance<LazerMod.DifficultyAdjust>()
            .firstNotNullOfOrNull { it.approachRate }
            ?.roundToDigits2()
            ?.let { return it }

        if (mods.contains(LazerMod.HardRock)) {
            a = (a * 1.4f).coerceIn(0f, 10f)
        } else if (mods.contains(LazerMod.Easy)) {
            a = (a / 2f).coerceIn(0f, 10f)
        }

        val speed = mods.getClockRate()

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

    fun applyOD(od: Float, mods: List<LazerMod>, mode: OsuMode = OsuMode.OSU): Float {
        var o = od
        if (mods.contains(LazerMod.HardRock)) {
            o = (o * 1.4f).coerceIn(0f, 10f)
        } else if (mods.contains(LazerMod.Easy)) {
            o = (o / 2f).coerceIn(0f, 10f)
        }

        mods.filterIsInstance<LazerMod.DifficultyAdjust>()
            .firstNotNullOfOrNull { it.overallDifficulty }
            ?.roundToDigits2()
            ?.let { return it }

        // mania 模式内，模组改变速率不影响 OD
        val speed = if (mode == OsuMode.MANIA) {
            1f
        } else {
            mods.getClockRate()
        }

        if (speed != 1f) {
            var ms = getMillisFromOD(o, mode)
            ms = (ms / speed)
            o = getODFromMillis(ms)
        }

        return o.roundToDigits2()
    }

    fun applyCS(cs: Float, mods: List<LazerMod>, mode: OsuMode = OsuMode.OSU): Float {
        var c = cs

        mods.filterIsInstance<LazerMod.DifficultyAdjust>()
            .firstNotNullOfOrNull { it.circleSize }
            ?.roundToDigits2()
            ?.let { return it }

        if (mode.safeModeValue == 0.toByte()) {
            c = mods.getKey(cs)
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

        mods.filterIsInstance<LazerMod.DifficultyAdjust>()
            .firstNotNullOfOrNull { it.drainRate }
            ?.roundToDigits2()
            ?.let { return it }

        if (mods.contains(LazerMod.HardRock)) {
            h *= 1.4f
        } else if (mods.contains(LazerMod.Easy)) {
            h /= 2f
        }
        return h.coerceIn(0f, 10f).roundToDigits2()
    }

    fun applyBPM(bpm: Float?, mods: List<LazerMod>): Float {
        return ((bpm ?: 0f) * mods.getClockRate()).roundToDigits2()
    }

    fun applyLength(length: Int?, mods: List<LazerMod>): Int {
        return ((length ?: 0).toDouble() / mods.getClockRate().coerceAtLeast(0.01f)).roundToInt()
    }

    private fun Float.roundToDigits2(): Float = kotlin.math.round(this * 100f) / 100f
}