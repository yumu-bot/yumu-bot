package com.now.nowbot.service.osuApiService.impl

import com.now.nowbot.dao.ScoreDao
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod.Companion.isAffectStarRating
import com.now.nowbot.model.osu.LazerMod.Companion.isValueMod
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.LazerScoreForCalculate
import com.now.nowbot.model.osu.RosuPerformance
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.JacksonUtil
import org.spring.osu.extended.rosu.JniBeatmap
import org.spring.osu.extended.rosu.JniPerformanceAttributes
import org.spring.osu.extended.rosu.JniScoreState
import org.springframework.stereotype.Service
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.map
import kotlin.collections.set
import kotlin.time.Duration.Companion.seconds

@Service class CalculateApiImpl(
    private val scoreDao: ScoreDao,
    private val beatmapApiService: OsuBeatmapApiService,
) : OsuCalculateApiService {
    companion object {

        // 如果为真，则会启用 rosu。
        private val R_OSU = false

        private fun JniPerformanceAttributes.toRosuPerformance(): RosuPerformance {
            return RosuPerformance(this)
        }

        private val calculatePriority = listOf(
            CalculateStrategy.LOCAL_DATABASE,
            CalculateStrategy.OFFICIAL_API,
            CalculateStrategy.LOCAL_API,
            CalculateStrategy.R_OSU,
        )
    }

    enum class CalculateStrategy {
        LOCAL_DATABASE,
        R_OSU,
        OFFICIAL_API,
        LOCAL_API
    }

    override fun getScorePerfectPP(score: LazerScore): RosuPerformance {
        if (!R_OSU) return RosuPerformance()

        val mode = score.mode.toRosuMode()
        val mods = score.mods
        val beatmapID = score.beatmapID
        val lazer = score.isLazer

        val closable = ArrayList<AutoCloseable>(1)
        return try {
            val (beatmap, change) = getJniBeatmapAndIsConvert(beatmapID, mode) { closable.add(it) } ?: return RosuPerformance()
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

    override fun getScoreFullComboPP(score: LazerScore): RosuPerformance {
        if (!R_OSU) return RosuPerformance()

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
            val (beatmap, change) = getJniBeatmapAndIsConvert(beatmapID, mode) { closable.add(it) } ?: return RosuPerformance()
            beatmap.createPerformance(state).apply {
                setLazer(lazer)
                if (change) this.setGameMode(mode)
                if (mods.isNotEmpty()) setMods(JacksonUtil.toJson(mods))
            }.calculate()
        } finally {
            closable.forEach { it.close() }
        }.toRosuPerformance()
    }

    override fun getScoreStatisticsWithFullAndPerfectPP(score: LazerScore): RosuPerformance.FullRosuPerformance? {
        if (!R_OSU) return null

        val mode = score.mode.toRosuMode()

        /**
         * 如果是 sb_score，则过滤掉 RX 的 mod，确保最大 pp 符合预期
         */
        val mods = if (score.type == "sb_score") {
            score.mods.filterNot { it is LazerMod.Relax || it is LazerMod.Autopilot }
        } else {
            score.mods
        }
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

        val beatmapID = if (score.beatmapID > 0L) score.beatmapID else score.beatmap.beatmapID
        val lazer = score.isLazer

        val closable = ArrayList<AutoCloseable>(1)

        return try {
            val (beatmap, isConvert) = getJniBeatmapAndIsConvert(beatmapID, mode) { closable.add(it) } ?: return null
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

    override fun applyStarToScore(score: LazerScore) {
        applyStarToScores(listOf(score))
    }

    override fun applyStarToScores(scores: Collection<LazerScore>) {
        val needApply = scores.filter { s ->
            s.beatmapID != 0L && (s.mods.isAffectStarRating() || s.beatmap.starRating < 1e-4)
        }

        if (needApply.isEmpty()) return

        val details = needApply.map { s ->
            BeatmapDetails(s.beatmapID, s.mode, s.mods, s.beatmap.hasLeaderBoard)
        }.distinct()

        val result = getBeatmapStars(details)

        needApply.forEach { s ->
            val searchKey = BeatmapDetails(s.beatmapID, s.mode, s.mods, s.beatmap.hasLeaderBoard)

            result[searchKey]?.let { star ->
                s.beatmap.starRating = star
            }
        }
    }

    override fun applyStarToBeatmap(
        beatmap: Beatmap?,
        mode: OsuMode,
        mods: List<LazerMod>
    ) {
        if (beatmap == null || beatmap.mode.isNotConvertAble(mode)) return

        val map = getBeatmapStars(listOf(
            BeatmapDetails(beatmap.beatmapID, mode, mods, beatmap.hasLeaderBoard
            )))

        map.values.firstOrNull()?.let {
            beatmap.starRating = it
        }
    }

    override fun applyPPToScore(score: LazerScore) {
        if (score.pp > 1e-4 || !R_OSU) {
            return
        } else {
            score.pp = getScoreRosuPerformance(score).pp
        }
    }

    override fun applyPPToScores(scores: Collection<LazerScore>) {
        val actions = scores.map {
            Callable {
                applyPPToScore(it)
            }
        }

        AsyncMethodExecutor.awaitCallableExecute(actions)
    }

    override fun applyPPToScoresWithSameBeatmap(scores: Collection<LazerScore>) {
        val noPPs = scores.filter { it.pp <= 1e-4 }

        val attrs = getScoresPPWithSameBeatmap(noPPs)

        noPPs.forEach { score ->
            attrs[score.scoreID]?.pp?.let { score.pp = it }
        }
    }

    private fun getScoresPPWithSameBeatmap(scores: Collection<LazerScore>): Map<Long, RosuPerformance> {
        if (scores.isEmpty() || !R_OSU) return emptyMap()

        val beatmapID = scores.first().beatmapID
        val mode = scores.first().mode.toRosuMode()

        val closable = ArrayList<AutoCloseable>(scores.size + 1)
        return try {
            val (beatmap, change) = getJniBeatmapAndIsConvert(beatmapID, mode) { closable.add(it) } ?: return mapOf()

            scores.map { score ->
                val mods = score.mods

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

                val isNotPass = !score.passed
                val lazer = score.isLazer

                score.scoreID to beatmap.createPerformance(state).apply {
                    setLazer(lazer)
                    if (isNotPass) setHitResultPriority(true)
                    if (change) this.setGameMode(mode)
                    if (mods.isNotEmpty()) setMods(JacksonUtil.toJson(mods))
                }.calculate()
            }
        } finally {
            closable.forEach { it.close() }
        }.associate { (id, attr) ->
            id to attr.toRosuPerformance()
        }
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
        if (!R_OSU) return emptyList()

        val gameMode = mode.toRosuMode()
        val cache = ArrayList<AutoCloseable>(1)
        val modsStr = if (mods.isNullOrEmpty().not()) {
            JacksonUtil.toJson(mods)
        } else {
            null
        }
        val (beatmap, change) = getJniBeatmapAndIsConvert(beatmapID, gameMode) { cache.add(it) } ?: return emptyList()
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
        if (!R_OSU) return RosuPerformance()

        val gameMode = mode.toRosuMode()
        val modsStr = if (mods.isNullOrEmpty().not()) {
            JacksonUtil.toJson(mods)
        } else {
            null
        }
        val cache = ArrayList<AutoCloseable>(1)
        val (beatmap, isConvert) = getJniBeatmapAndIsConvert(beatmapID, gameMode) { cache.add(it) } ?: return RosuPerformance()
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

    override fun getBeatmapStar(
        beatmapID: Long,
        mode: OsuMode,
        mods: List<LazerMod>,
        hasLeaderBoard: Boolean
    ): Double {
        return getBeatmapStars(listOf(BeatmapDetails(beatmapID, mode, mods, hasLeaderBoard))).values.firstOrNull() ?: 0.0
    }

    data class BeatmapDetails(
        val beatmapID: Long,
        val mode: OsuMode,
        val mods: List<LazerMod> = emptyList(),
        val hasLeaderBoard: Boolean = false,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BeatmapDetails) return false

            if (beatmapID != other.beatmapID) return false
            if (mode != other.mode) return false

            if (mods.size != other.mods.size) return false

            return mods.containsAll(other.mods)
        }

        override fun hashCode(): Int {
            var result = beatmapID.hashCode()
            result = 31 * result + mode.hashCode()

            result = 31 * result + mods.sumOf { it.hashCode() }

            return result
        }
    }

    private fun getBeatmapStars(
        details: List<BeatmapDetails>,
        priority: List<CalculateStrategy> = calculatePriority
    ): Map<BeatmapDetails, Double> {

        val resultStarMap = mutableMapOf<BeatmapDetails, Double>()

        // 1. 使用 LinkedHashMap 保持原始顺序，同时提供 O(1) 的移除性能
        // Key 为 beatmapID, Value 为对应的详情对象
        val remaining = details.associateByTo(LinkedHashMap()) { it }

        for (strategy in priority) {
            if (remaining.isEmpty()) break

            // 2. 根据策略动态准备输入数据
            val input = when (strategy) {
                CalculateStrategy.LOCAL_API, CalculateStrategy.R_OSU -> remaining.values.toList()

                else -> remaining.values.filter { it.mods.isValueMod() }
            }

            if (input.isEmpty()) continue

            // 3. 执行获取逻辑
            val found: Map<BeatmapDetails, Double> = when (strategy) {
                CalculateStrategy.LOCAL_DATABASE -> {
                    input.mapNotNull { d ->
                        scoreDao.getStarRatingCache(d.beatmapID, d.mode, d.mods)?.let {
                            d to it.toDouble()
                        }
                    }.toMap()
                }

                CalculateStrategy.R_OSU -> {
                    if (R_OSU) {
                        getBeatmapStarFromLocal(input)
                    } else emptyMap()
                }

                CalculateStrategy.OFFICIAL_API -> {
                    getBeatmapStarFromOfficial(input)
                }

                CalculateStrategy.LOCAL_API -> {
                    getBeatmapStarFromLocalAPI(input)
                }
            }

            // 4. 合并结果并从“待办清单”中移除
            if (found.isNotEmpty()) {
                resultStarMap.putAll(found)

                found.keys.forEach { id -> remaining.remove(id) }
            }
        }

        return resultStarMap
    }


    private fun getBeatmapStarFromLocal(details: List<BeatmapDetails>): Map<BeatmapDetails, Double> {
        val starMap = mutableMapOf<BeatmapDetails, Double>()

        val closeables = ArrayList<AutoCloseable>(details.size * 2)

        details.forEach { nd ->
            try {
                val (beatmap, _) = getJniBeatmapAndIsConvert(nd.beatmapID, nd.mode.toRosuMode()) {
                    closeables.add(it)
                } ?: (null to false)

                if (beatmap != null) {
                    val difficulty = beatmap.createDifficulty().apply {
                        closeables.add(this)
                        if (nd.mods.isNotEmpty()) setMods(JacksonUtil.toJson(nd.mods))
                    }

                    val star = difficulty.calculate(beatmap).getStarRating()

                    if (star > 1e-4) {
                        scoreDao.saveStarRatingCacheAsync(nd.beatmapID, nd.mode, nd.mods, star.toFloat(), nd.hasLeaderBoard)
                        starMap[nd] = star
                    }
                }
            } finally {
                closeables.forEach { it.close() }
            }
        }

        return starMap
    }

    private fun getBeatmapStarFromOfficial(details: List<BeatmapDetails>): Map<BeatmapDetails, Double> {
        val resultMap = ConcurrentHashMap<BeatmapDetails, Double>()

        details.chunked(15).forEach { batch ->
            val actions = batch.map { nd ->
                Callable {
                    runCatching {
                        val attr = beatmapApiService.getAttributes(nd.beatmapID, nd.mode, nd.mods)
                        val star = attr.starRating

                        if (star > 1e-4) {
                            scoreDao.saveStarRatingCacheAsync(
                                nd.beatmapID, nd.mode, nd.mods,
                                star.toFloat(), nd.hasLeaderBoard
                            )
                            nd to star
                        } else null
                    }.getOrNull()
                }
            }

            val results = AsyncMethodExecutor.awaitCallableExecute(actions, 20.seconds)
            results.filterNotNull().forEach { (details, star) -> resultMap[details] = star }
        }

        val sortedStarMap = LinkedHashMap<BeatmapDetails, Double>()
        details.forEach { detail ->
            resultMap[detail]?.let { star ->
                sortedStarMap[detail] = star
            }
        }

        return sortedStarMap
    }

    private fun getBeatmapStarFromLocalAPI(details: List<BeatmapDetails>): Map<BeatmapDetails, Double> {
        val resultMap = ConcurrentHashMap<BeatmapDetails, Double>()

        details.chunked(15).forEach { batch ->
            val actions = batch.map { nd ->
                Callable {
                    runCatching {
                        val attr = beatmapApiService.getAttributesFromLocal(nd.beatmapID, nd.mode,
                            LazerScoreForCalculate(mods = nd.mods))
                        val star = attr.difficulty.starRating

                        if (star > 1e-4) {
                            nd to star
                        } else {
                            null
                        }
                    }.getOrNull()
                }
            }

            val results = AsyncMethodExecutor.awaitCallableExecute(actions, 20.seconds).filterNotNull()

            results.forEach { (details, star) ->
                scoreDao.saveStarRatingCacheAsync(
                    details.beatmapID, details.mode, details.mods, star.toFloat(), details.hasLeaderBoard
                )

                resultMap[details] = star
            }
        }

        val sortedStarMap = LinkedHashMap<BeatmapDetails, Double>()

        details.forEach { detail ->
            resultMap[detail]?.let { star ->
                sortedStarMap[detail] = star
            }
        }

        return sortedStarMap
    }



    private fun getScoreRosuPerformance(score: LazerScore): RosuPerformance {
        if (!R_OSU) return RosuPerformance()

        val mode = score.mode.toRosuMode()
        val mods = score.mods
        val isNotPass = !score.passed
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
            val (beatmap, isConvert) = getJniBeatmapAndIsConvert(beatmapID, mode) { closable.add(it) } ?: return RosuPerformance()
            beatmap.createPerformance(state).apply {
                setLazer(lazer)
                if (isNotPass) setHitResultPriority(true)
                if (isConvert) this.setGameMode(mode)
                if (mods.isNotEmpty()) setMods(JacksonUtil.toJson(mods))
            }.calculate()
        } finally {
            closable.forEach { it.close() }
        }.toRosuPerformance()
    }

    private fun getJniBeatmapAndIsConvert(
        beatmapID: Long, mode: org.spring.osu.OsuMode, set: (JniBeatmap) -> Unit
    ): Pair<JniBeatmap, Boolean>? {
        val map = beatmapApiService.getBeatmapFileByte(beatmapID) ?: return null
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

}
