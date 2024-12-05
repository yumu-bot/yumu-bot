package com.now.nowbot.service.osuApiService.impl

import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.Mod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.BeatmapDifficultyAttributes
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.RosuPerformance
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.JacksonUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.spring.osu.extended.rosu.JniBeatmap
import org.spring.osu.extended.rosu.JniPerformanceAttributes
import org.spring.osu.extended.rosu.JniScoreState
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

@Service
class CalculateApiImpl(private val beatmapApiService: OsuBeatmapApiService) : OsuCalculateApiService {

    override fun getScorePerfectPP(score: LazerScore): RosuPerformance {
        val mode = score.mode.toRosuMode()
        val mods = score.mods
        val beatmapID = score.beatMapID
        val lazer = score.isLazer

        val closable = ArrayList<AutoCloseable>(1)
        return try {
            val (beatmap, change) = getBeatmap(beatmapID, mode) { closable.add(it) }
            val objects = beatmap.objects
            val performance = beatmap.createPerformance().apply {
                isLazer(lazer)
                setPassedObjects(objects)
                if (change) this.setGameMode(mode)
                if (mods.isNotEmpty()) setMods(JacksonUtil.toJson(mods))
            }
            performance.calculate()
        } finally {
            closable.forEach { it.close() }
        }.toRosuPerformance()
    }

    /* 3 */
    override fun getScoreFullComboPP(score: LazerScore): RosuPerformance {
        val mode = score.mode.toRosuMode()
        val mods = score.mods
        val state = with(score.statistics) {
            val state = toScoreStatistics(score.mode)
            JniScoreState.create(
                maxCombo = 99999,
                largeTickHits = largeTickHit,
                smallTickHits = smallTickHit,
                sliderEndHits = sliderTailHit,
                geki = state.countGeki,
                katu = state.countKatu,
                n300 = state.count300 + state.countMiss,
                n100 = state.count100,
                n50 = state.count50,
            )
        }

        val beatmapID = score.beatMapID
        val lazer = score.isLazer

        val closable = ArrayList<AutoCloseable>(1)
        return try {
            val (beatmap, change) = getBeatmap(beatmapID, mode) { closable.add(it) }
            beatmap.createPerformance(state).apply {
                setLazer(lazer)
                setPassedObjects(beatmap.objects)
                if (change) this.setGameMode(mode)
                if (mods.isNotEmpty()) setMods(JacksonUtil.toJson(mods))
            }.calculate()
        } finally {
            closable.forEach { it.close() }
        }.toRosuPerformance()
    }

    private fun getScorePP(score: LazerScore): RosuPerformance {
        val mode = score.mode.toRosuMode()
        val mods = score.mods
        val isNotPass = score.passed.not()
        val state = with(score.statistics) {
            val state = toScoreStatistics(score.mode)
            JniScoreState.create(
                maxCombo = score.maxCombo,
                largeTickHits = largeTickHit,
                smallTickHits = smallTickHit,
                sliderEndHits = sliderTailHit,
                geki = state.countGeki,
                katu = state.countKatu,
                n300 = state.count300,
                n100 = state.count100,
                n50 = state.count50,
                misses = state.countMiss,
            )
        }
        if (isNotPass) {
            state.n300 = 0
        }

        val beatmapID = score.beatMapID
        val lazer = score.isLazer

        val closable = ArrayList<AutoCloseable>(1)
        return try {
            val (beatmap, change) = getBeatmap(beatmapID, mode) { closable.add(it) }
            beatmap.createPerformance(state).apply {
                setLazer(lazer)
                setPassedObjects(beatmap.objects)
                if (isNotPass) setHitResultPriority(true)
                if (change) this.setGameMode(mode)
                if (mods.isNotEmpty()) setMods(JacksonUtil.toJson(mods))
            }.calculate()
        } finally {
            closable.forEach { it.close() }
        }.toRosuPerformance()
    }

    private fun getBeatmap(
        beatmapID: Long,
        mode: org.spring.osu.OsuMode,
        set: (JniBeatmap) -> Unit
    ): Pair<JniBeatmap, Boolean> {
        val map = beatmapApiService.getBeatMapFileByte(beatmapID) ?: throw Exception("无法获取谱面文件, 请稍后再试")
        val beatmap = JniBeatmap(map)
        set(beatmap)
        val change = if (beatmap.mode != mode && beatmap.mode == org.spring.osu.OsuMode.Osu) {
            beatmap.convertInPlace(mode)
            true
        } else {
            false
        }
        return beatmap to change
    }

    override fun getAccFcPPList(
        beatmapID: Long,
        mode: OsuMode,
        mods: List<LazerMod>?,
        maxCombo: Int?,
        misses: Int?,
        isLazer: Boolean,
        accuracy: DoubleArray
    ): List<Double> {
        val gameMode = mode.toRosuMode()
        val cache = ArrayList<AutoCloseable>(1)
        val modsStr = if (mods.isNullOrEmpty().not()) {
            JacksonUtil.toJson(mods)
        } else {
            null
        }
        val (beatmap, change) = getBeatmap(beatmapID, gameMode) { cache.add(it) }
        return try {
            accuracy.map { acc ->
                val performance = beatmap.createPerformance().apply {
                    setLazer(isLazer)
                    if (change) setGameMode(gameMode)
                    maxCombo?.let { setCombo(it) }
                    modsStr?.let { setMods(it) }
                    misses?.let { setMisses(it) }
                    setAcc(acc)
                }
                performance.calculate().getPP()
            }
        } finally {
            cache.forEach { it.close() }
        }
    }


    override fun getAccPP(
        beatmapID: Long,
        mode: OsuMode,
        mods: List<LazerMod>?,
        maxCombo: Int?,
        misses: Int?,
        isLazer: Boolean,
        accuracy: Double
    ): RosuPerformance {
        val gameMode = mode.toRosuMode()
        val modsStr = if (mods.isNullOrEmpty().not()) {
            JacksonUtil.toJson(mods)
        } else {
            null
        }
        val cache = ArrayList<AutoCloseable>(1)
        val (beatmap, change) = getBeatmap(beatmapID, gameMode) { cache.add(it) }
        return try {
            val performance = beatmap.createPerformance().apply {
                setLazer(isLazer)
                if (change) setGameMode(gameMode)
                maxCombo?.let { setCombo(it) }
                modsStr?.let { setMods(it) }
                setAcc(accuracy)
            }
            performance.calculate().toRosuPerformance()
        } finally {
            cache.forEach { it.close() }
        }
    }

    /* 1 */
    override fun getScoreStatisticsWithFullAndPerfectPP(score: LazerScore): RosuPerformance.FullRosuPerformance {
        val mode = score.mode.toRosuMode()
        val mods = score.mods
        val isNotPass = score.passed.not()
        val fcState: JniScoreState
        val state: JniScoreState
        with(score.statistics) {
            val statistics = toScoreStatistics(score.mode)
            fcState = JniScoreState.create(
                maxCombo = 99999,
                largeTickHits = largeTickHit,
                smallTickHits = smallTickHit,
                sliderEndHits = sliderTailHit,
                geki = statistics.countGeki,
                katu = statistics.countKatu,
                n300 = statistics.count300 + statistics.countMiss,
                n100 = statistics.count100,
                n50 = statistics.count50,
            )
            state = JniScoreState.create(
                maxCombo = score.maxCombo,
                largeTickHits = largeTickHit,
                smallTickHits = smallTickHit,
                sliderEndHits = sliderTailHit,
                geki = statistics.countGeki,
                katu = statistics.countKatu,
                n300 = statistics.count300,
                n100 = statistics.count100,
                n50 = statistics.count50,
                misses = statistics.countMiss,
            )
        }

        if (isNotPass) {
            state.n300 = 0
        }

        val beatmapID = score.beatMapID
        val lazer = score.isLazer

        val closable = ArrayList<AutoCloseable>(1)

        return try {
            val (beatmap, change) = getBeatmap(beatmapID, mode) { closable.add(it) }
            val notFC = beatmap.createPerformance(state).apply {
                setLazer(lazer)
                setPassedObjects(beatmap.objects)
                if (isNotPass) setHitResultPriority(true)
                if (change) this.setGameMode(mode)
                if (mods.isNotEmpty()) setMods(JacksonUtil.toJson(mods))
            }.calculate()
            val result = RosuPerformance.FullRosuPerformance(notFC)
            val fc = beatmap.createPerformance(fcState).apply {
                setLazer(lazer)
                setPassedObjects(beatmap.objects)
                if (change) this.setGameMode(mode)
                if (mods.isNotEmpty()) setMods(JacksonUtil.toJson(mods))
            }.calculate().getPP()
            val pf = beatmap.createPerformance().apply {
                isLazer(lazer)
                setPassedObjects(beatmap.objects)
                if (change) this.setGameMode(mode)
                if (mods.isNotEmpty()) setMods(JacksonUtil.toJson(mods))
            }.calculate().getPP()
            result.fullPP = fc
            result.perfectPP = pf
            result
        } finally {
            closable.forEach { it.close() }
        }
    }

    override fun applyPPToScore(score: LazerScore) {
        if (score.PP != null && score.PP!! > 0) return
        score.PP = getScorePP(score).pp
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

    override fun applyStarToBeatMap(beatMap: BeatMap?, mode: OsuMode, mods: List<LazerMod>) {
        if (
            beatMap == null ||
            beatMap.mode == mode ||
            beatMap.mode != OsuMode.OSU ||
            LazerMod.hasStarRatingChange(mods).not()
        ) return
        getStar(beatMap.beatMapID, mode, mods).let { beatMap.starRating = it }
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

    override fun applyPPToScores(scores: List<LazerScore>) {
        val actions = scores.map {
            return@map AsyncMethodExecutor.Supplier<Unit> {
                applyPPToScore(it)
            }
        }

        AsyncMethodExecutor.AsyncSupplier(actions)
    }


    override fun getStar(beatMapID: Long, mode: OsuMode, mods: List<LazerMod>): Double {
        val closeables = ArrayList<AutoCloseable>(2)
        return try {
            val (beatmap, _) = getBeatmap(beatMapID, mode.toRosuMode()) { closeables.add(it) }
            beatmap.createDifficulty().apply {
                closeables.add(this)
                if (mods.isNotEmpty()) setMods(JacksonUtil.toJson(mods))
            }.calculate(beatmap).getStarRating()
        } finally {
            closeables.forEach { it.close() }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(Companion::class.java)

        // 应用四维的变化 4 dimensions
        @JvmStatic
        fun applyBeatMapChanges(beatMap: BeatMap?, mods: List<LazerMod>) {
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

        @JvmStatic
        fun applyBeatMapChanges(score: LazerScore) {
            applyBeatMapChanges(score.beatMap, score.mods)
        }

        @JvmStatic
        fun getMillisFromAR(ar: Float): Float = when {
            ar > 11f -> 300f
            ar > 5f -> 1200 - (150 * (ar - 5))
            ar > 0f -> 1800 - (120 * ar)
            else -> 1800f
        }

        @JvmStatic
        fun getARFromMillis(ms: Float): Float = when {
            ms < 300 -> 11f
            ms < 1200 -> 5 + (1200 - ms) / 150f
            ms < 2400 -> (1800 - ms) / 120f
            else -> -5f
        }

        @JvmStatic
        fun applyAR(ar: Float, mods: List<LazerMod>): Float {
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

        @JvmStatic
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

        @JvmStatic
        fun getODFromMillis(ms: Float): Float = when {
            ms < 14 -> 11f
            else -> (80 - ms) / 6f
        }

        // 只有在仅计算主模式的时候才能使用这个方法
        @JvmStatic
        fun applyOD(od: Float, mods: List<LazerMod>): Float {
            return applyOD(od, mods, OsuMode.OSU)
        }

        @JvmStatic
        fun applyOD(od: Float, mods: List<LazerMod>, mode: OsuMode): Float {
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

        @JvmStatic
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
            return c.clamp().roundToDigits2()
        }

        @JvmStatic
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
            return h.clamp().roundToDigits2()
        }

        @JvmStatic
        fun applyBPM(bpm: Float?, mods: List<LazerMod>): Float {
            return ((bpm ?: 0f) * LazerMod.getModSpeed(mods)).roundToDigits2()
        }

        @JvmStatic
        fun applyLength(length: Int?, mods: List<LazerMod>): Int {
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

        private inline fun <reified T : Mod> List<LazerMod>.contains(type: T) = LazerMod.hasMod(this, type)

        private fun JniPerformanceAttributes.toRosuPerformance(): RosuPerformance {
            return RosuPerformance(this)
        }
    }
}
