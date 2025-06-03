package com.now.nowbot.service.osuApiService.impl

import com.now.nowbot.mapper.BeatmapStarRatingCacheRepository
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.Mod
import com.now.nowbot.model.osu.ValueMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.RosuPerformance
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
import kotlin.reflect.full.companionObjectInstance

@Service class CalculateApiImpl(
    private val beatmapApiService: OsuBeatmapApiService,
    private val beatmapStarRatingCacheRepository: BeatmapStarRatingCacheRepository,
) : OsuCalculateApiService {

    override fun getScorePerfectPP(score: LazerScore): RosuPerformance {
        val mode = score.mode.toRosuMode()
        val mods = score.mods
        val beatmapID = score.beatmapID
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
                maxCombo = 2147483647 / 2,
                largeTickHits = largeTickHit,
                smallTickHits = smallTickHit,
                sliderEndHits = sliderTailHit,
                geki = state.countGeki!!,
                katu = state.countKatu!!,
                n300 = state.count300!! + state.countMiss!!,
                n100 = state.count100!!,
                n50 = state.count50!!,
            )
        }

        val beatmapID = score.beatmapID
        val lazer = score.isLazer

        val closable = ArrayList<AutoCloseable>(1)
        return try {
            val (beatmap, change) = getBeatmap(beatmapID, mode) { closable.add(it) }
            beatmap.createPerformance(state).apply {
                setLazer(lazer)
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
                geki = state.countGeki!!,
                katu = state.countKatu!!,
                n300 = state.count300!!,
                n100 = state.count100!!,
                n50 = state.count50!!,
                misses = state.countMiss!!,
            )
        }
        if (isNotPass) {
            state.n300 = 0
        }

        val beatmapID = score.beatmapID
        val lazer = score.isLazer

        val closable = ArrayList<AutoCloseable>(1)
        return try {
            val (beatmap, change) = getBeatmap(beatmapID, mode) { closable.add(it) }
            beatmap.createPerformance(state).apply {
                setLazer(lazer)
                if (isNotPass) setHitResultPriority(true)
                if (change) this.setGameMode(mode)
                if (mods.isNotEmpty()) setMods(JacksonUtil.toJson(mods))
            }.calculate()
        } finally {
            closable.forEach { it.close() }
        }.toRosuPerformance()
    }

    private fun getBeatmap(
        beatmapID: Long, mode: org.spring.osu.OsuMode, set: (JniBeatmap) -> Unit
    ): Pair<JniBeatmap, Boolean> {
        val map = beatmapApiService.getBeatMapFileByte(beatmapID) ?: throw Exception("无法获取谱面文件, 请稍后再试")
        val beatmap = JniBeatmap(map)
        set(beatmap)
        val isConvert = if (beatmap.mode != mode && beatmap.mode == org.spring.osu.OsuMode.Osu) {
            beatmap.convertInPlace(mode)
            true
        } else {
            false
        }
        return beatmap to isConvert
    }

    override fun getAccPPList(
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
        val (beatmap, isConvert) = getBeatmap(beatmapID, gameMode) { cache.add(it) }
        return try {
            val performance = beatmap.createPerformance().apply {
                setLazer(isLazer)
                if (isConvert) setGameMode(gameMode)
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
                maxCombo = 2147483647 / 2,
                largeTickHits = largeTickHit,
                smallTickHits = smallTickHit,
                sliderEndHits = sliderTailHit,
                geki = statistics.countGeki!!,
                katu = statistics.countKatu!!,
                n300 = statistics.count300!! + statistics.countMiss!!,
                n100 = statistics.count100!!,
                n50 = statistics.count50!!,
                misses = 0
            )
            state = JniScoreState.create(
                maxCombo = score.maxCombo,
                largeTickHits = largeTickHit,
                smallTickHits = smallTickHit,
                sliderEndHits = sliderTailHit,
                geki = statistics.countGeki!!,
                katu = statistics.countKatu!!,
                n300 = statistics.count300!!,
                n100 = statistics.count100!!,
                n50 = statistics.count50!!,
                misses = statistics.countMiss!!,
            )
        }

        if (isNotPass) {
            state.n300 = 0
        }

        val beatmapID = score.beatmapID
        val lazer = score.isLazer

        val closable = ArrayList<AutoCloseable>(1)

        return try {
            val (beatmap, isConvert) = getBeatmap(beatmapID, mode) { closable.add(it) }
            val notFC = beatmap.createPerformance(state).apply {
                setLazer(lazer)
                setPassedObjects(beatmap.objects)
                if (isNotPass) setHitResultPriority(true)
                if (isConvert) this.setGameMode(mode)
                if (mods.isNotEmpty()) setMods(JacksonUtil.toJson(mods))
            }.calculate()
            val result = RosuPerformance.FullRosuPerformance(notFC)
            val fc = beatmap.createPerformance(fcState).apply {
                setLazer(lazer)
                // setPassedObjects(beatmap.objects)
                if (isConvert) setGameMode(mode)
                if (mods.isNotEmpty()) setMods(JacksonUtil.toJson(mods))

            }.calculate().getPP()
            val pf = beatmap.createPerformance().apply {
                setLazer(lazer)
                // setPassedObjects(beatmap.objects)
                if (isConvert) setGameMode(mode)
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
        if (score.PP != null && score.PP!! > 0.0 + 1e-6) return
        score.PP = getScorePP(score).pp
    }

    override fun applyStarToScore(score: LazerScore, local: Boolean) {
        if (score.beatmapID == 0L || (LazerMod.noStarRatingChange(score.mods)) && score.beatmap.mode.isEqualOrDefault(score.mode)) {
            return
        }

        if (local) {
            applyStarToScoreFromLocal(score)

            if (score.beatmap.starRating < 0.15) {
                applyStarToScoreFromOfficial(score)
            }
        } else {
            applyStarToScoreFromOfficial(score)

            if (score.beatmap.starRating < 0.15) {
                applyStarToScoreFromLocal(score)
            }
        }
    }

    override fun applyStarToBeatMap(beatmap: Beatmap?, mode: OsuMode, mods: List<LazerMod>, local: Boolean) {
        if (beatmap == null || beatmap.mode.isNotConvertAble(mode)) return

        if (beatmap.mode.isConvertAble(mode)) {
            // TODO Local 无法计算转谱星级
            applyStarToBeatMapFromOfficial(beatmap, mode, mods)
            return
        }

        if (LazerMod.noStarRatingChange(mods)) return

        if (local) {
            applyStarToBeatMapFromLocal(beatmap, mode, mods)

            if (beatmap.starRating < 0.15) {
                applyStarToBeatMapFromOfficial(beatmap, mode, mods)
            }
        } else {
            applyStarToBeatMapFromOfficial(beatmap, mode, mods)

            if (beatmap.starRating < 0.15) {
                applyStarToBeatMapFromLocal(beatmap, mode, mods)
            }
        }
        /*

        getBeatMapStarRating(beatmap.beatmapID, mode, mods).let { beatmap.starRating = it }

         */
    }

    override fun applyStarToScores(scores: List<LazerScore>, local: Boolean) {
        val actions = scores.map {
            return@map AsyncMethodExecutor.Runnable {
                applyStarToScore(it, local)
            }
        }

        AsyncMethodExecutor.awaitRunnableExecute(actions)
    }

    private fun applyStarToScoreFromOfficial(score: LazerScore) {
        try {
            val attr = beatmapApiService.getAttributes(score.beatmapID, score.mode, LazerMod.getModsValue(score.mods))

            if (attr.starRating > 0.0) {
                score.beatmap.starRating = attr.starRating
            } else {
                log.error("给成绩应用星级：无法获取谱面 {}，无法应用 API 提供的星数！", score.beatmapID)
            }
        } catch (e: Exception) {
            log.error("给成绩应用星级：无法获取谱面 {}，无法获取 API 提供的星数！", score.beatmapID, e)
        }
    }

    private fun applyStarToBeatMapFromOfficial(beatmap: Beatmap, mode: OsuMode, mods: List<LazerMod>) {
        try {
            val attr = beatmapApiService.getAttributes(beatmap.beatmapID, mode, LazerMod.getModsValue(mods))

            if (attr.starRating > 0.0) {
                beatmap.starRating = attr.starRating
            } else {
                log.error("给谱面应用星级：无法获取谱面 {}，无法应用 API 提供的星数！", beatmap.beatmapID)
            }
        } catch (e: Exception) {
            log.error("给谱面应用星级：无法获取谱面 {}，无法获取 API 提供的星数！", beatmap.beatmapID, e)
        }
    }

    override fun applyPPToScores(scores: List<LazerScore>) {
        val actions = scores.map {
            return@map AsyncMethodExecutor.Runnable {
                applyPPToScore(it)
            }
        }

        AsyncMethodExecutor.awaitRunnableExecute(actions)
    }

    override fun applyBeatMapChanges(score: LazerScore) {
        applyBeatMapChanges(score.beatmap, score.mods)
    }

    // 应用四维的变化 4 dimensions
    override fun applyBeatMapChanges(beatmap: Beatmap?, mods: List<LazerMod>) {
        if (beatmap == null || beatmap.beatmapID == 0L) return

        val mode = beatmap.mode

        if (LazerMod.hasStarRatingChange(mods)) {
            beatmap.BPM = applyBPM(beatmap.BPM ?: 0f, mods)
            beatmap.AR = applyAR(beatmap.AR ?: 0f, mods)
            beatmap.CS = applyCS(beatmap.CS ?: 0f, mods)
            beatmap.OD = applyOD(beatmap.OD ?: 0f, mods, mode)
            beatmap.HP = applyHP(beatmap.HP ?: 0f, mods)
            beatmap.totalLength = applyLength(beatmap.totalLength, mods)
            beatmap.hitLength = applyLength(beatmap.hitLength, mods)
        }
    }

    override fun applyBeatMapChanges(scores: List<LazerScore>) {
        val actions = scores.map {
            return@map AsyncMethodExecutor.Runnable {
                applyBeatMapChanges(it)
            }
        }

        AsyncMethodExecutor.awaitRunnableExecute(actions)
    }

    private fun applyStarToScoreFromLocal(score: LazerScore) {
        applyStarToBeatMapFromLocal(score.beatmap, score.mode, score.mods)
    }

    private fun applyStarToBeatMapFromLocal(beatmap: Beatmap, mode: OsuMode, mods: List<LazerMod>) {
        beatmap.starRating = getBeatMapStarRating(beatmap.beatmapID, mode, mods)
    }

    override fun getBeatMapStarRating(beatmapID: Long, mode: OsuMode, mods: List<LazerMod>): Double {
        val isAllLegacy = mods.any { it.settings == null && it::class.companionObjectInstance is ValueMod }

        val modsValue: Int = if (isAllLegacy) {
            LazerMod.getModsValue(mods)
        } else {
            0
        }

        if (isAllLegacy) { // 如果是全部为 legacy mod 且 没有自定义属性的话，就从缓存里面取
            // 目前来看没有任何自定义 mod 计入 pp
            val star = beatmapStarRatingCacheRepository.findByKey(beatmapID, modsValue)
            if (star.isPresent) {
                return star.get()
            }
        }

        val closeables = ArrayList<AutoCloseable>(2)

        return try {
            val (beatmap, _) = getBeatmap(beatmapID, mode.toRosuMode()) { closeables.add(it) }
            beatmap.createDifficulty().apply {
                closeables.add(this)
                if (mods.isNotEmpty()) setMods(JacksonUtil.toJson(mods))
            }.calculate(beatmap).getStarRating().apply {
                if (isAllLegacy) {
                    try {
                        beatmapStarRatingCacheRepository.saveAndUpdate(beatmapID, mode.modeValue, modsValue, this)
                    } catch (e: Exception) {
                        log.error("保存星级缓存失败", e)
                    }
                }
            }
        } finally {
            closeables.forEach { it.close() }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(Companion::class.java)

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

        @JvmStatic fun getMillisFromOD(od: Float, mode: OsuMode): Float = when (mode) {
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

        private inline fun <reified T : Mod> List<LazerMod>.contains(type: T) = LazerMod.hasMod(this, type)

        private fun JniPerformanceAttributes.toRosuPerformance(): RosuPerformance {
            return RosuPerformance(this)
        }
    }
}
