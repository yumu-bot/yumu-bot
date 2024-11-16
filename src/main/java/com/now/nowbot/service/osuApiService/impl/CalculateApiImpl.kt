package com.now.nowbot.service.osuApiService.impl

import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.enums.LazerModType
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.BeatmapDifficultyAttributes
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.LazerScore.LazerStatistics
import com.now.nowbot.service.messageServiceImpl.MapStatisticsService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.impl.CalculateApiImpl.Companion.CalculateType.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import rosu.Rosu
import rosu.parameter.JniScore
import rosu.result.JniResult
import rosu.result.ManiaResult
import rosu.result.OsuResult
import rosu.result.TaikoResult
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Service
class CalculateApiImpl(private val beatmapApiService: OsuBeatmapApiService) : OsuCalculateApiService {
    override fun getBeatMapPerfectPP(beatMap: BeatMap, mode: OsuMode, mods: List<LazerMod>): JniResult {
        return getBeatMapPP(beatMap, mode, mods, PF)
    }

    override fun getScorePerfectPP(score: LazerScore): JniResult {
        return getScorePP(score, PF)
    }

    override fun getBeatMapFullComboPP(beatMap: BeatMap, mode: OsuMode, mods: List<LazerMod>): JniResult {
        return getBeatMapPP(beatMap, mode, mods, FC)
    }

    override fun getScoreFullComboPP(score: LazerScore): JniResult {
        return getScorePP(score, FC)
    }

    override fun getBeatMapPP(beatMap: BeatMap, mode: OsuMode, mods: List<LazerMod>): JniResult {
        return getBeatMapPP(beatMap, mode, mods, DEFAULT)
    }

    override fun getScorePP(score: LazerScore): JniResult {
        return getScorePP(score, DEFAULT)
    }

    override fun getExpectedPP(beatMap: BeatMap, expected: MapStatisticsService.Expected): JniResult {
        return getExpectedPP(beatMap, expected, PF)
    }

    override fun getScoreStatistics(score: LazerScore): Map<String, Double> {
        return getStatistics(getScorePP(score, DEFAULT))
    }

    override fun applyStarToBeatMap(beatMap: BeatMap?, mode: OsuMode, mods: List<LazerMod>) {
        if (beatMap == null || beatMap.beatMapID == 0L) return

        try {
            val attr: BeatmapDifficultyAttributes =
                beatmapApiService.getAttributes(beatMap.beatMapID, mode, LazerMod.getModsValue(mods))

            if (attr.starRating != null) {
                beatMap.starRating = attr.starRating!!.toDouble()
            } else {
                log.error("给谱面应用星级：无法获取谱面 {}，无法应用 API 提供的星数！", beatMap.beatMapID)
            }
        } catch (e: Exception) {
            log.error("给谱面应用星级：无法获取谱面 {}，无法获取 API 提供的星数！", beatMap.beatMapID, e)
        }
    }

    override fun applyStarToBeatMap(beatMap: BeatMap?, expected: MapStatisticsService.Expected) {
        applyStarToBeatMap(beatMap, expected.mode, LazerMod.getModsList(expected.mods))
    }

    override fun applyStarToScore(score: LazerScore) {
        if (score.beatMapID == 0L) return

        try {
            val attr: BeatmapDifficultyAttributes =
                beatmapApiService.getAttributes(score.beatMapID, score.mode, LazerMod.getModsValue(score.mods))

            if (attr.starRating != null) {
                score.beatMap.starRating = attr.starRating!!.toDouble()
            } else {
                log.error("给成绩应用星级：无法获取谱面 {}，无法应用 API 提供的星数！", score.beatMapID)
            }
        } catch (e: Exception) {
            log.error("给成绩应用星级：无法获取谱面 {}，无法获取 API 提供的星数！", score.beatMapID, e)
        }
    }

    override fun applyStarToScores(scores: List<LazerScore>) {
        for (score in scores) {
            applyStarToScore(score)
        }
    }

    override fun applyPPToScore(score: LazerScore) {
        if (score.PP != null && score.PP!! > 0) return
        score.PP = getScorePP(score).pp
    }

    override fun applyPPToScores(scores: List<LazerScore>) {
        for (score in scores) {
            applyPPToScore(score)
        }
    }

    // 独立计算
    private fun getJniResult(score: LazerScore, map: ByteArray, jni: JniScore): JniResult {
        return getJniResult(score.statistics, score.mods, score.mode, score.accuracy, score.maxCombo, map, jni)
    }

    private fun getJniResult(
        stat: LazerStatistics,
        mods: List<LazerMod?>,
        mode: OsuMode,
        accuracy: Double?,
        combo: Int?,
        map: ByteArray,
        jni: JniScore
    ): JniResult {
        val value = LazerMod.getModsValue(mods)

        if (value != 0) jni.mods = value
        if (accuracy != null) jni.accuracy = accuracy
        if (combo != null) jni.combo = combo

        when (mode) {
            OsuMode.TAIKO -> {
                if (stat.ok != null) jni.n100 = stat.ok!!
            }

            OsuMode.CATCH -> {
                if (stat.largeTickHit != null) jni.n100 = stat.largeTickHit!!
                if (stat.smallTickHit != null) jni.n50 = stat.smallTickHit!!
            }

            OsuMode.MANIA -> {
                if (stat.perfect != null) jni.geki = stat.perfect!!
                if (stat.good != null) jni.katu = stat.good!!
                if (stat.ok != null) jni.n100 = stat.ok!!
                if (stat.meh != null) jni.n50 = stat.meh!!
            }

            else -> {
                if (stat.ok != null) jni.n100 = stat.ok!!
                if (stat.meh != null) jni.n50 = stat.meh!!
            }
        }

        if (stat.great != null) jni.n300 = stat.great!!
        if (stat.miss != null) jni.misses = stat.miss!!

        return getJniResult(map, jni)
    }

    private fun getJniResult(map: ByteArray, score: JniScore): JniResult {
        return Rosu.calculate(map, score)
    }

    // 主计算
    private fun getScorePP(s: LazerScore, type: CalculateType): JniResult {
        return getPP(
            s.beatMapID,
            s.statistics,
            s.mode,
            s.mods,
            s.accuracy,
            s.statistics.miss,
            s.maxCombo,
            s.beatMap.maxCombo,
            type,
            s.passed,
            s.totalHit
        )
    }

    private fun getBeatMapPP(b: BeatMap, mode: OsuMode, mods: List<LazerMod>, type: CalculateType): JniResult {
        val pp = getPP(b.beatMapID, null, mode, mods, null, null, null, b.maxCombo, type)
        applyBeatMapChanges(b, mods)
        return pp
    }

    private fun getExpectedPP(b: BeatMap, expected: MapStatisticsService.Expected, type: CalculateType): JniResult {
        return getPP(
            b.beatMapID,
            null,
            expected.mode,
            LazerMod.getModsList(expected.mods),
            expected.accuracy,
            expected.misses,
            expected.combo,
            b.maxCombo,
            type
        )
    }

    private fun getPP(
        beatMapID: Long,
        t: LazerStatistics?,
        mode: OsuMode,
        mods: List<LazerMod>,
        accuracy: Double?,
        misses: Int?,
        combo: Int?,
        maxCombo: Int?,
        type: CalculateType,
        passed: Boolean = false,
        totalHit: Int? = null
    ): JniResult {
        val b: ByteArray = beatmapApiService.getBeatMapFileByte(beatMapID)

        val js = JniScore()
        var max = maxCombo

        //js.setLazer(s.isLazer());
        js.applyDifficultyAdjustMod(mods)
        js.mods = LazerMod.getModsValue(mods)
        if (combo != null) js.combo = combo
        js.speed = LazerMod.getModSpeedForStarCalculate(mods)
        js.mode = mode.toRosuMode()

        return when (type) {
            PF -> {
                if (max == null) try {
                    max = beatmapApiService.getBeatMap(beatMapID).maxCombo
                } catch (ignored: Exception) {

                }

                js.combo = max ?: 0
                js.misses = 0

                if (totalHit != null) {
                    when (mode) {
                        OsuMode.MANIA -> js.geki = totalHit
                        else -> js.n300 = totalHit
                    }
                }

                getJniResult(b, js)
            }

            FC -> {
                if (max == null) try {
                    max = beatmapApiService.getBeatMap(beatMapID).maxCombo
                } catch (ignored: Exception) {

                }

                if (t != null && totalHit != null) {
                    when (mode) {
                        OsuMode.TAIKO -> {
                            if (t.ok != null) js.n100 = t.ok!!
                            if (t.meh != null) js.n50 = t.meh!!
                            js.n300 = (t.great ?: 0) + (t.miss ?: 0)
                        }

                        OsuMode.CATCH -> {
                            if (t.largeTickHit != null) js.n100 = t.largeTickHit!!
                            if (t.smallTickHit != null) js.n50 = t.smallTickHit!!
                            js.n300 = (t.great ?: 0) + (t.miss ?: 0) + (t.largeTickMiss ?: 0)
                        }

                        OsuMode.MANIA -> { // mania 的无 miss fc pp 没有意义，故使用当前彩率下，不含 100 及以下判定（bad）的 fc
                            val p = (t.perfect ?: 0)
                            val g = (t.great ?: 0)
                            val bad = (t.ok ?: 0) + (t.meh ?: 0) + (t.miss ?: 0)

                            val rate = t.getPerfectRate()
                            val pb = min(max((bad * rate).roundToInt(), 0), bad)
                            val gb = min(max((bad - pb), 0), bad)

                            if (p + pb > 0) js.geki = p + pb
                            if (g + gb > 0) js.n300 = g + gb

                            if (t.good != null) js.katu = t.good!!
                            js.n100 = 0
                            js.n50 = 0
                        }

                        else -> {
                            if (t.ok != null) js.n100 = t.ok!!
                            if (t.meh != null) js.n50 = t.meh!!

                            js.n300 = (t.great ?: 0) + (t.miss ?: 0)
                        }
                    }
                }

                if (max != null) js.combo = max
                js.misses = 0

                getJniResult(b, js)
            }

            DEFAULT -> {
                if (passed) {
                    if (misses != null) js.misses = misses

                    getJniResult(t!!, mods, mode, accuracy, combo, b, js)
                } else { // 没 pass 不给 300, acc 跟 combo
                    if (t == null) return getJniResult(b, js)

                    when (mode) {
                        OsuMode.TAIKO -> {
                            if (t.ok != null) js.n100 = t.ok!!
                            if (t.meh != null) js.n50 = t.meh!!
                        }

                        OsuMode.CATCH -> {
                            if (t.largeTickHit != null) js.n100 = t.largeTickHit!!
                            if (t.smallTickHit != null) js.n50 = t.smallTickHit!!
                        }

                        OsuMode.MANIA -> {
                            if (t.good != null) js.katu = t.good!!
                            if (t.ok != null) js.n100 = t.ok!!
                            if (t.meh != null) js.n50 = t.meh!!
                        }

                        else -> {
                            if (t.ok != null) js.n100 = t.ok!!
                            if (t.meh != null) js.n50 = t.meh!!
                        }
                    }

                    if (misses != null) js.misses = misses
                    else if (t.miss != null) js.misses = t.miss!!

                    getJniResult(b, js)
                }
            }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(Companion::class.java)

        enum class CalculateType {
            PF, FC, DEFAULT
        }

        // 应用四维的变化 4 dimensions
        @JvmStatic fun applyBeatMapChanges(beatMap: BeatMap?, mods: List<LazerMod>) {
            if (beatMap == null) return

            if (LazerMod.hasStarRatingChange(mods)) {
                beatMap.BPM = applyBPM(beatMap.BPM ?: 0f, mods)
                beatMap.AR = applyAR(beatMap.AR ?: 0f, mods)
                beatMap.CS = applyCS(beatMap.CS ?: 0f, mods)
                beatMap.OD = applyOD(beatMap.OD ?: 0f, mods)
                beatMap.HP = applyHP(beatMap.HP ?: 0f, mods)
                beatMap.totalLength = applyLength(beatMap.totalLength, mods)
                beatMap.hitLength = applyLength(beatMap.hitLength, mods)
            }
        }

        @JvmStatic fun getMillisFromAR(ar: Float): Float = when {
            ar > 11f -> 300f
            ar > 5f -> 1200 - (150 * (ar - 5))
            ar > 0f -> 1800 - (120 * ar)
            else -> 1800f
        }

        @JvmStatic fun getARFromMillis(ms: Float): Float = when {
            ms < 300 -> 11f
            ms < 1200 -> 5 + (1200 - ms) / 150f
            ms < 2400 -> (1800 - ms) / 120f
            else -> -5f
        }

        @JvmStatic fun applyAR(ar: Float, mods: List<LazerMod>): Float {
            var a = ar

            val d = mods.stream().filter { it.type == LazerModType.DifficultyAdjust }.toList().firstOrNull()?.ar
            if (d != null) return d.toFloat().roundToDigits2()

            if (mods.contains(LazerModType.HardRock)) {
                a = (a * 1.4f).clamp()
            } else if (mods.contains(LazerModType.Easy)) {
                a = (a / 2f).clamp()
            }

            val speed = LazerMod.getModSpeed(mods)

            if (speed != 1.0) {
                var ms = getMillisFromAR(a.clamp())
                ms = (ms / speed).toFloat()
                a = getARFromMillis(ms)
            }

            return a.roundToDigits2()
        }

        @JvmStatic fun getMillisFromOD(od: Float): Float = when {
            od > 11 -> 14f
            else -> 80 - 6 * od
        }

        @JvmStatic fun getODFromMillis(ms: Float): Float = when {
            ms < 14 -> 11f
            else -> (80 - ms) / 6f
        }

        @JvmStatic fun applyOD(od: Float, mods: List<LazerMod>): Float {
            var o = od
            if (mods.contains(LazerModType.HardRock)) {
                o = (o * 1.4f).clamp()
            } else if (mods.contains(LazerModType.Easy)) {
                o = (o / 2f).clamp()
            }

            val d = mods.stream().filter { it.type == LazerModType.DifficultyAdjust }.toList().firstOrNull()?.od
            if (d != null) return d.toFloat().roundToDigits2()

            val speed = LazerMod.getModSpeed(mods)

            if (speed != 1.0) {
                var ms = getMillisFromOD(od.clamp())
                ms = (ms / speed).toFloat()
                o = getODFromMillis(ms)
            }

            return o.roundToDigits2()
        }

        @JvmStatic fun applyCS(cs: Float, mods: List<LazerMod>): Float {
            var c = cs

            val d = mods.stream().filter { it.type == LazerModType.DifficultyAdjust }.toList().firstOrNull()?.cs
            if (d != null) return d.toFloat().roundToDigits2()

            if (mods.contains(LazerModType.HardRock)) {
                c *= 1.3f
            } else if (mods.contains(LazerModType.Easy)) {
                c /= 2f
            }
            return c.clamp().roundToDigits2()
        }

        @JvmStatic fun applyHP(hp: Float, mods: List<LazerMod>): Float {
            var h = hp

            val d = mods.stream().filter { it.type == LazerModType.DifficultyAdjust }.toList().firstOrNull()?.hp
            if (d != null) return d.toFloat().roundToDigits2()

            if (mods.contains(LazerModType.HardRock)) {
                h *= 1.4f
            } else if (mods.contains(LazerModType.Easy)) {
                h /= 2f
            }
            return h.clamp().roundToDigits2()
        }

        @JvmStatic fun applyBPM(bpm: Float?, mods: List<LazerMod>): Float {
            return ((bpm ?: 0f) * LazerMod.getModSpeed(mods)).toFloat().roundToDigits2()
        }

        @JvmStatic fun applyLength(length: Int?, mods: List<LazerMod>): Int {
            return ((length ?: 0).toDouble() / LazerMod.getModSpeed(mods)).roundToInt()
        }

        private fun getStatistics(result: JniResult): Map<String, Double> {
            val out = HashMap<String, Double>(6)

            when (result) {
                (result as? OsuResult) -> {
                    out["aim_pp"] = result.ppAim
                    out["spd_pp"] = result.ppSpeed
                    out["acc_pp"] = result.ppAcc
                    out["fl_pp"] = result.ppFlashlight
                }

                (result as? TaikoResult) -> {
                    out["acc_pp"] = result.ppAcc
                    out["diff_pp"] = result.ppDifficulty
                }

                (result as? ManiaResult) -> out["diff_pp"] = result.ppDifficulty

                else -> {}
            }

            return out
        }

        private fun Float.clamp() = if ((0f..10f).contains(this)) {
            this
        } else if (this > 10f) {
            10f
        } else {
            0f
        }

        private fun Float.roundToDigits2() = BigDecimal(this.toDouble()).setScale(2, RoundingMode.HALF_EVEN).toFloat()

        private fun List<LazerMod>.contains(type: LazerModType) = LazerMod.hasMod(this, type)

        private fun JniScore.applyDifficultyAdjustMod(mods: List<LazerMod>) {
            for (m in mods) {
                if (m.type.acronym == "DA") {
                    if (m.cs != null) {
                        this.cs = m.cs!!
                    }
                    if (m.ar != null) {
                        this.ar = m.ar!!
                    }
                    if (m.od != null) {
                        this.od = m.od!!
                    }
                    if (m.hp != null) {
                        this.hp = m.hp!!
                    }
                }
            }
        }

        private fun LazerStatistics.getPerfectRate(): Double {
            return if (this.perfect == null || this.perfect == 0) 0.0
            else if (this.great == null || this.great == 0) Double.MAX_VALUE
            else 1.0 * this.perfect!! / this.great!!
        }
    }
}