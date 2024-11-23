package com.now.nowbot.service.osuApiService.impl

import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.Mod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.BeatmapDifficultyAttributes
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.LazerScore.LazerStatistics
import com.now.nowbot.model.json.RosuPerformance
import com.now.nowbot.model.json.RosuPerformance.Companion.asMap
import com.now.nowbot.service.messageServiceImpl.MapStatisticsService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.impl.CalculateApiImpl.Companion.CalculateType.*
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.JacksonUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.spring.osu.extended.rosu.*
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Service
class CalculateApiImpl(private val beatmapApiService: OsuBeatmapApiService) : OsuCalculateApiService {
    override fun getBeatMapPerfectPP(beatMap: BeatMap, mode: OsuMode, mods: List<LazerMod>): RosuPerformance {
        return getBeatMapPP(beatMap, mode, mods, PF)
    }

    override fun getScorePerfectPP(score: LazerScore): RosuPerformance {
        return getScorePP(score, PF)
    }

    override fun getBeatMapFullComboPP(beatMap: BeatMap, mode: OsuMode, mods: List<LazerMod>): RosuPerformance {
        return getBeatMapPP(beatMap, mode, mods, FC)
    }

    override fun getScoreFullComboPP(score: LazerScore): RosuPerformance {
        return getScorePP(score, FC)
    }

    override fun getBeatMapPP(beatMap: BeatMap, mode: OsuMode, mods: List<LazerMod>): RosuPerformance {
        return getBeatMapPP(beatMap, mode, mods, DEFAULT)
    }

    override fun getScorePP(score: LazerScore): RosuPerformance {
        return getScorePP(score, DEFAULT)
    }

    override fun getExpectedPP(beatMap: BeatMap, expected: MapStatisticsService.Expected): RosuPerformance {
        return getExpectedPP(beatMap, expected, DEFAULT)
    }

    override fun getScoreStatistics(score: LazerScore): Map<String, Double> {
        return getScorePP(score, DEFAULT).asMap()
    }

    override fun applyStarToBeatMap(beatMap: BeatMap?, mode: OsuMode, mods: List<LazerMod>) {
        if (beatMap == null || beatMap.beatMapID == 0L || LazerMod.noStarRatingChange(mods)) return

        applyBeatMapChanges(beatMap, mods)

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
        if (score.beatMapID == 0L || LazerMod.noStarRatingChange(score.mods)) return

        applyBeatMapChanges(score)

        val sr = getStar(score.beatMapID, score.mode, score.mods)

        if (sr > 0) {
            score.beatMap.starRating = sr
        } else {
            applyStarToScoreFromOfficial(score)
        }
    }

    override fun applyStarToScores(scores: List<LazerScore>) {
        val actions = scores.map {
            return@map AsyncMethodExecutor.Supplier<Unit> {
                applyStarToScore(it)
            }
        }

        AsyncMethodExecutor.AsyncSupplier(actions)
    }

    private fun applyStarToScoreFromOfficial(score: LazerScore) {
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

    override fun applyPPToScore(score: LazerScore) {
        if (score.PP != null && score.PP!! > 0) return
        score.PP = getScorePP(score).pp
    }

    override fun applyPPToScores(scores: List<LazerScore>) {
        val actions = scores.map {
            return@map AsyncMethodExecutor.Supplier<Unit> {
                applyPPToScore(it)
            }
        }

        AsyncMethodExecutor.AsyncSupplier(actions)
    }

    private fun getScorePP(s: LazerScore, type: CalculateType): RosuPerformance {
        return getPP(
            s.beatMapID,
            s.statistics,
            s.isLazer,
            s.mode,
            s.mods,
            s.accuracy,
            s.statistics.miss,
            s.maxCombo,
            s.beatMap.maxCombo,
            type,
            s.passed,
            s.totalHit
        ).toRosuPerformance()
    }

    private fun getBeatMapPP(b: BeatMap, mode: OsuMode, mods: List<LazerMod>, type: CalculateType): RosuPerformance {
        val pp = getPP(b.beatMapID, lazer = true, mode = mode, mods = mods, maxCombo = b.maxCombo, type = type)
        applyBeatMapChanges(b, mods)
        return pp.toRosuPerformance()
    }

    private fun getExpectedPP(b: BeatMap, expected: MapStatisticsService.Expected, type: CalculateType): RosuPerformance {
        return getPP(
            b.beatMapID,
            null,
            true,
            expected.mode,
            LazerMod.getModsList(expected.mods),
            expected.accuracy,
            expected.misses,
            expected.combo,
            b.maxCombo,
            type
        ).toRosuPerformance()
    }

    private fun getStar(beatMapID: Long, mode: OsuMode, mods: List<LazerMod>): Double {
        val map: ByteArray = beatmapApiService.getBeatMapFileByte(beatMapID)

        val performance = calculate(map, mods = mods, mode = mode)
        return performance.getStarRating()
    }

    private fun getPP(
        id: Long,
        stat: LazerStatistics? = null,
        lazer: Boolean = false,
        mode: OsuMode,
        mods: List<LazerMod>,
        accuracy: Double? = null,
        misses: Int? = null,
        combo: Int? = null,
        maxCombo: Int? = null,
        type: CalculateType,
        passed: Boolean = false,
        totalHit: Int? = null
    ): JniPerformanceAttributes {
        val map: ByteArray = beatmapApiService.getBeatMapFileByte(id)

        val score = JniScoreStat()
        var max = maxCombo
        var combo = combo

        return when (type) {
            PF -> {
                if (max == null) try {
                    max = beatmapApiService.getBeatMap(id).maxCombo
                } catch (ignored: Exception) {

                }

                combo = max ?: 0

                if (totalHit != null) {
                    when (mode) {
                        OsuMode.MANIA -> score.geki = totalHit
                        else -> score.n300 = totalHit
                    }
                }

                calculate(map, lazer, score, mods, mode, combo, 1.0)
            }

            FC -> {
                if (max == null) try {
                    max = beatmapApiService.getBeatMap(id).maxCombo
                } catch (ignored: Exception) {

                }

                if (stat != null && totalHit != null) {
                    when (mode) {
                        OsuMode.TAIKO -> {
                            if (stat.ok != null) score.n100 = stat.ok!!
                            if (stat.meh != null) score.n50 = stat.meh!!
                            score.n300 = (stat.great ?: 0) + (stat.miss ?: 0)
                        }

                        OsuMode.CATCH -> {
                            if (stat.largeTickHit != null) score.n100 = stat.largeTickHit!!
                            if (stat.smallTickHit != null) score.n50 = stat.smallTickHit!!
                            score.n300 = (stat.great ?: 0) + (stat.miss ?: 0) + (stat.largeTickMiss ?: 0)
                        }

                        OsuMode.MANIA -> { // mania 的无 miss fc pp 没有意义，故使用当前彩率下，不含 100 及以下判定（bad）的 fc
                            val p = (stat.perfect ?: 0)
                            val g = (stat.great ?: 0)
                            val bad = (stat.ok ?: 0) + (stat.meh ?: 0) + (stat.miss ?: 0)

                            val rate = stat.getPerfectRate()
                            val pb = min(max((bad * rate).roundToInt(), 0), bad)
                            val gb = min(max((bad - pb), 0), bad)

                            if (p + pb > 0) score.geki = p + pb
                            if (g + gb > 0) score.n300 = g + gb

                            if (stat.good != null) score.katu = stat.good!!
                            score.n100 = 0
                            score.n50 = 0
                        }

                        else -> {
                            if (stat.ok != null) score.n100 = stat.ok!!
                            if (stat.meh != null) score.n50 = stat.meh!!

                            score.n300 = (stat.great ?: 0) + (stat.miss ?: 0)
                        }
                    }
                }

                if (max != null) combo = max
                score.misses = 0

                calculate(map, lazer, score, mods, mode, combo, accuracy)
            }

            DEFAULT -> {
                if (stat == null) return calculate(map, lazer, null, mods, mode, combo, accuracy)

                if (passed) {
                    when (mode) {
                        OsuMode.TAIKO -> {
                            score.n100 = stat.ok ?: 0
                        }

                        OsuMode.CATCH -> {
                            score.n100 = stat.largeTickHit ?: 0
                            score.n50 = stat.smallTickHit ?: 0
                        }

                        OsuMode.MANIA -> {
                            score.geki = stat.perfect ?: 0
                            score.katu = stat.good ?: 0
                            score.n100 = stat.ok ?: 0
                            score.n50 = stat.meh ?: 0
                        }

                        else -> {
                            score.n100 = stat.ok ?: 0
                            score.n50 = stat.meh ?: 0
                        }
                    }

                    score.maxCombo = combo ?: 0
                    score.n300 = stat.great ?: 0
                    score.misses = stat.miss ?: 0

                    calculate(map, lazer, score, mods, mode, combo, accuracy)
                } else { // 没 pass 不给 300, acc 跟 combo
                    when (mode) {
                        OsuMode.TAIKO -> {
                            score.n100 = stat.ok ?: 0
                        }

                        OsuMode.CATCH -> {
                            score.n100 = stat.largeTickHit ?: 0
                            score.n50 = stat.smallTickHit ?: 0
                        }

                        OsuMode.MANIA -> {
                            score.katu = stat.good ?: 0
                            score.n100 = stat.ok ?: 0
                            score.n50 = stat.meh ?: 0
                        }

                        else -> {
                            score.n100 = stat.ok ?: 0
                            score.n50 = stat.meh ?: 0
                        }
                    }

                    score.misses = if (stat.miss != null)  {
                        stat.miss!!
                    } else {
                        misses ?: 0
                    }

                    calculate(map, lazer, score, mods, mode, combo, accuracy)
                }
            }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(Companion::class.java)

        enum class CalculateType {
            PF, FC, DEFAULT
        }

        // 真正的主计算
        private fun calculate(
            map: ByteArray,
            lazer: Boolean = false,
            score: JniScoreStat? = null,
            mods: List<LazerMod>? = null,
            mode: OsuMode? = null,
            combo: Int? = null,
            accuracy: Double? = null,
        ): JniPerformanceAttributes {
            val rosuBeatmap = JniBeatmap(map)
            val rosuMode = mode?.toRosuMode()

            // 转谱
            if (rosuMode != null && rosuBeatmap.mode == org.spring.osu.OsuMode.Osu) {
                rosuBeatmap.convertInPlace(rosuMode)
            }

            val rosuDifficulty = rosuBeatmap.createDifficulty()
            rosuDifficulty.isLazer(lazer)

            val rosuPerformance = if (score == null) {
                rosuBeatmap.createPerformance()
            } else {
                rosuBeatmap.createPerformance(score.toState())
            }

            if (! mods.isNullOrEmpty()) {
                rosuDifficulty.applyDifficultyAdjustMod(mods)

                val modsJson = JacksonUtil.toJson(mods)
                rosuDifficulty.setMods(modsJson)
                rosuPerformance.setMods(modsJson)

                // rosuPerformance.setClockRate(LazerMod.getModSpeed(mods).toDouble())
                rosuPerformance.setDifficulty(rosuDifficulty)
            }
            if (combo != null) rosuPerformance.setCombo(combo)
            if (accuracy != null) {
                if (accuracy <= 1.0) {
                    rosuPerformance.setAcc(accuracy * 100)
                } else {
                    rosuPerformance.setAcc(accuracy)
                }
            }

            return rosuPerformance.calculate()
        }

        // 应用四维的变化 4 dimensions
        @JvmStatic fun applyBeatMapChanges(beatMap: BeatMap?, mods: List<LazerMod>) {
            if (beatMap == null || beatMap.beatMapID == 0L) return

            val mode = beatMap.mode

            if (LazerMod.hasStarRatingChange(mods)) {
                beatMap.BPM = applyBPM(beatMap.BPM ?: 0f, mods)
                beatMap.AR = applyAR(beatMap.AR ?: 0f, mods)
                beatMap.CS = applyCS(beatMap.CS ?: 0f, mods)
                beatMap.OD = applyOD(beatMap.OD ?: 0f, mods, mode)
                beatMap.HP = applyHP(beatMap.HP ?: 0f, mods)
                beatMap.totalLength = applyLength(beatMap.totalLength, mods)
                beatMap.hitLength = applyLength(beatMap.hitLength, mods)
            }
        }
        @JvmStatic fun applyBeatMapChanges(score: LazerScore) {
            applyBeatMapChanges(score.beatMap, score.mods)
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

            for (mod in mods) {
                if (mod is LazerMod.DifficultyAdjust) {
                    if (mod.approachRate != null) return mod.approachRate!!.roundToDigits2()
                    break
                }
            }

            if (mods.contains(LazerMod.HardRock)) {
                a = (a * 1.4f).clamp()
            } else if (mods.contains(LazerMod.Easy)) {
                a = (a / 2f).clamp()
            }

            val speed = LazerMod.getModSpeed(mods)

            if (speed != 1f) {
                var ms = getMillisFromAR(a)
                ms = (ms / speed)
                a = getARFromMillis(ms)
            }

            return a.roundToDigits2()
        }

        @JvmStatic fun getMillisFromOD(od: Float, mode: OsuMode): Float = when(mode) {
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

        @JvmStatic fun getODFromMillis(ms: Float): Float = when {
            ms < 14 -> 11f
            else -> (80 - ms) / 6f
        }

        // 只有在仅计算主模式的时候才能使用这个方法
        @JvmStatic fun applyOD(od: Float, mods: List<LazerMod>): Float {
            return applyOD(od, mods, OsuMode.OSU)
        }

        @JvmStatic fun applyOD(od: Float, mods: List<LazerMod>, mode: OsuMode): Float {
            var o = od
            if (mods.contains(LazerMod.HardRock)) {
                o = (o * 1.4f).clamp()
            } else if (mods.contains(LazerMod.Easy)) {
                o = (o / 2f).clamp()
            }

            for (mod in mods) {
                if (mod is LazerMod.DifficultyAdjust) {
                    if (mod.overallDifficulty != null) return mod.overallDifficulty!!.roundToDigits2()
                    break
                }
            }
            val speed = LazerMod.getModSpeed(mods)

            if (speed != 1f) {
                var ms = getMillisFromOD(od, mode)
                ms = (ms / speed)
                o = getODFromMillis(ms)
            }

            return o.roundToDigits2()
        }

        @JvmStatic fun applyCS(cs: Float, mods: List<LazerMod>): Float {
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
            return c.clamp().roundToDigits2()
        }

        @JvmStatic fun applyHP(hp: Float, mods: List<LazerMod>): Float {
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
            return h.clamp().roundToDigits2()
        }

        @JvmStatic fun applyBPM(bpm: Float?, mods: List<LazerMod>): Float {
            return ((bpm ?: 0f) * LazerMod.getModSpeed(mods)).roundToDigits2()
        }

        @JvmStatic fun applyLength(length: Int?, mods: List<LazerMod>): Int {
            return ((length ?: 0).toDouble() / LazerMod.getModSpeed(mods)).roundToInt()
        }

        private fun Float.clamp() = if ((0f..10f).contains(this)) {
            this
        } else if (this > 10f) {
            10f
        } else {
            0f
        }

        private fun Float.roundToDigits2() = BigDecimal(this.toDouble()).setScale(2, RoundingMode.HALF_EVEN).toFloat()

        private inline fun<reified T : Mod> List<LazerMod>.contains(type: T) = LazerMod.hasMod(this, type)

        private fun JniDifficulty.applyDifficultyAdjustMod(mods: List<LazerMod>) {
            for (m in mods) {
                if (m is LazerMod.DifficultyAdjust) {
                    m.circleSize?.let { this.setCs(it, false) }
                    m.approachRate?.let { this.setAr(it, false) }
                    m.overallDifficulty?.let { this.setOd(it, false) }
                    m.drainRate?.let { this.setHp(it, false) }
                }
            }
        }

        private fun LazerStatistics.getPerfectRate(): Double {
            return if (this.perfect == null || this.perfect == 0) 0.0
            else if (this.great == null || this.great == 0) Double.MAX_VALUE
            else 1.0 * this.perfect!! / this.great!!
        }

        // JniScoreState 只能一次性设定，所以写了个中转类方便赋值
        private data class JniScoreStat(
            var maxCombo: Int = 0,
            var largeTickHits: Int = 0,
            var sliderEndHits: Int = 0,
            var geki: Int = 0,
            var katu: Int = 0,
            var n300: Int = 0,
            var n100: Int = 0,
            var n50: Int = 0,
            var misses: Int = 0,
        )

        private fun JniScoreStat.toState(): JniScoreState {
            val score = JniScoreState(this.maxCombo, this.largeTickHits, this.sliderEndHits, this.geki, this.katu, this.n300, this.n100, this.n50, this.misses)
            return score
        }

        private fun JniPerformanceAttributes.toRosuPerformance(): RosuPerformance {
            return RosuPerformance(this)
        }
    }
}
