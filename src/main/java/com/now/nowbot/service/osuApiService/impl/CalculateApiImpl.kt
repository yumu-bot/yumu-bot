package com.now.nowbot.service.osuApiService.impl

import com.now.nowbot.config.OsuLocalCalculateConfig
import com.now.nowbot.dao.ScoreDao
import com.now.nowbot.model.calculate.CosuScore
import com.now.nowbot.model.calculate.CalculatePerformance
import com.now.nowbot.model.calculate.CosuPerformance
import com.now.nowbot.model.calculate.CosuScore.Companion.toCosuScore
import com.now.nowbot.model.calculate.EmptyFullPerformance
import com.now.nowbot.model.calculate.EmptyPerformance
import com.now.nowbot.model.calculate.FullCalculatePerformance
import com.now.nowbot.model.calculate.FullCosuPerformance
import com.now.nowbot.model.calculate.RosuPerformance
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.Companion.toRosuMode
import com.now.nowbot.model.osu.*
import com.now.nowbot.model.osu.LazerMod.Companion.getClockRate
import com.now.nowbot.model.osu.LazerMod.Companion.isAffectStarRating
import com.now.nowbot.model.osu.LazerMod.Companion.isOfficialCalculateAbleMod
import com.now.nowbot.model.osu.LazerMod.Companion.toJson
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.util.AsyncMethodExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import me.aloic.rosupp.DifficultyRequest
import me.aloic.rosupp.PerformanceRequest
import me.aloic.rosupp.PerformanceResult
import me.aloic.rosupp.RosuPp
import me.aloic.rosupp.ScoreMode
import org.springframework.stereotype.Service
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import kotlin.use

@Service
class CalculateApiImpl(
    private val scoreDao: ScoreDao,
    private val beatmapApiService: OsuBeatmapApiService,
    private val rosu: RosuPp?,

    config: OsuLocalCalculateConfig,
) : OsuCalculateApiService {

    enum class CalculateStrategy {
        LOCAL_DATABASE,
        R_OSU,
        OFFICIAL_API,
        C_OSU
    }

    val calculatePriority = config.priority.ifEmpty {
        listOf(CalculateStrategy.LOCAL_DATABASE, CalculateStrategy.OFFICIAL_API)
    }

    private val enableRosu = config.rosu

    override fun getScorePerfectPP(
        score: LazerScore
    ): CalculatePerformance {
        val priority = calculatePriority

        for (strategy in priority) {
            val found: CalculatePerformance? = when (strategy) {
                CalculateStrategy.R_OSU -> getScorePerfectPPFromRosu(score)

                CalculateStrategy.C_OSU -> getScorePerfectPPFromCosu(score)

                else -> null
            }


            if (found != null) {
                return found
            }
        }

        return EmptyPerformance
    }

    private fun getScorePerfectPPFromCosu(score: LazerScore): CosuPerformance? {
        val cs = score.toCosuScore(CosuScore.ScoreType.PERFECT)

        return beatmapApiService.getAttributesFromLocal(score.beatmapID, score.mode, cs).performance
    }

    private fun getScorePerfectPPFromRosu(score: LazerScore): RosuPerformance? {
        if (rosu == null) return null

        val bytes = beatmapApiService.getBeatmapFileByte(score.beatmapID) ?: return null

        return rosu.let { r ->
            r.loadBeatmap(bytes).use { beatmap ->
                val difficulty = score.buildDifficultyRequest()
                val performance = score.buildPerfectRequest(difficulty)
                val result = r.calculatePerformance(beatmap, performance)

                RosuPerformance(result)
            }
        }
    }

    override fun getScoreFullComboPP(score: LazerScore): CalculatePerformance {

        val priority = calculatePriority

        for (strategy in priority) {
            val found: CalculatePerformance? = when (strategy) {
                CalculateStrategy.R_OSU -> getScoreFullComboPPFromRosu(score)

                CalculateStrategy.C_OSU -> getScoreFullComboPPFromCosu(score)

                else -> null
            }

            if (found != null) {
                return found
            }
        }

        return EmptyPerformance
    }

    private fun getScoreFullComboPPFromRosu(score: LazerScore): RosuPerformance? {
        if (rosu == null) return null

        val bytes = beatmapApiService.getBeatmapFileByte(score.beatmapID) ?: return null

        return rosu.let { r ->
            r.loadBeatmap(bytes).use { beatmap ->
                val difficulty = score.buildDifficultyRequest()
                val performance = score.buildFullComboRequest(difficulty)
                val result = r.calculatePerformance(beatmap, performance)

                RosuPerformance(result)
            }
        }
    }

    private fun getScoreFullComboPPFromCosu(score: LazerScore): CosuPerformance? {
        val cs = score.toCosuScore(CosuScore.ScoreType.FULL_COMBO)

        return beatmapApiService.getAttributesFromLocal(score.beatmapID, score.mode, cs).performance
    }

    override fun getScoreStatisticsWithFullAndPerfectPP(score: LazerScore): FullCalculatePerformance? {

        val priority = calculatePriority

        for (strategy in priority) {
            val found: FullCalculatePerformance? = when (strategy) {
                CalculateStrategy.R_OSU -> getScoreStatisticsWithFullAndPerfectPPFromRosu(score)

                CalculateStrategy.C_OSU -> getScoreStatisticsWithFullAndPerfectPPFromCosu(score)

                else -> null
            }

            if (found != null) {
                return found
            }
        }

        return EmptyFullPerformance

    }

    private fun getScoreStatisticsWithFullAndPerfectPPFromCosu(score: LazerScore): FullCosuPerformance? {
        val cs = score.toCosuScore()
        val fc = score.toCosuScore(CosuScore.ScoreType.FULL_COMBO)
        val pf = score.toCosuScore(CosuScore.ScoreType.PERFECT)

        // 将 runBlocking 的结果直接返回给外部
        return runBlocking(Dispatchers.IO) {
            val performanceDeferred = async {
                beatmapApiService.getAttributesFromLocal(score.beatmapID, score.mode, cs).performance
            }
            val fcPPDeferred = async {
                beatmapApiService.getAttributesFromLocal(score.beatmapID, score.mode, fc).performance?.pp
            }
            val pfPPDeferred = async {
                beatmapApiService.getAttributesFromLocal(score.beatmapID, score.mode, pf).performance?.pp
            }

            val perf = performanceDeferred.await() ?: return@runBlocking null
            val fcPP = fcPPDeferred.await() ?: return@runBlocking null
            val pfPP = pfPPDeferred.await() ?: return@runBlocking null

            // 在协程作用域内完成构建
            FullCosuPerformance(perf, fcPP, pfPP)
        }
    }


    private fun getScoreStatisticsWithFullAndPerfectPPFromRosu(score: LazerScore): RosuPerformance.FullRosuPerformance? {
        if (rosu == null) return null

        /**
         * 如果是 sb_score，则过滤掉 RX 的 mod，确保最大 pp 符合预期
         */
        val mods = if (score.type == "sb_score") {
            score.mods.filterNot { it is LazerMod.Relax || it is LazerMod.Autopilot }
        } else {
            score.mods
        }

        val bytes = beatmapApiService.getBeatmapFileByte(score.beatmapID) ?: return null

        val result: PerformanceResult
        val fullPP: Double
        val perfectPP: Double

        rosu.let { r ->
            r.loadBeatmap(bytes).use { beatmap ->
                val difficulty = customDifficultyRequest(mods, score.isLazer, score.mode)
                val performance = score.buildPerformanceRequest(difficulty)
                result = r.calculatePerformance(beatmap, performance)
                fullPP = r.calculatePerformance(beatmap, score.buildFullComboRequest(difficulty)).pp
                perfectPP = r.calculatePerformance(beatmap, score.buildPerfectRequest(difficulty)).pp
            }
        }

        return RosuPerformance.FullRosuPerformance(result).apply {
            this.fullPP = fullPP
            this.perfectPP = perfectPP
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

        val map = getBeatmapStars(
            listOf(
                BeatmapDetails(
                    beatmap.beatmapID, mode, mods, beatmap.hasLeaderBoard
                )
            )
        )

        map.values.firstOrNull()?.let {
            beatmap.starRating = it
        }
    }

    override fun applyPPToScore(score: LazerScore) {
        val priority = calculatePriority

        for (strategy in priority) {
            when (strategy) {
                CalculateStrategy.R_OSU -> applyPPToScoreFromRosu(score)

                CalculateStrategy.C_OSU -> applyPPToScoreFromCosu(score)

                else -> continue
            }
        }

        return
    }

    override fun applyPPToScores(scores: Collection<LazerScore>) {
        runBlocking(Dispatchers.IO) {
            val deferred = scores.map {
                async {
                    applyPPToScore(it)
                }
            }

            deferred.awaitAll()
        }
    }

    private fun applyPPToScoreFromRosu(score: LazerScore): Double {
        if (rosu == null || score.pp > 1e-4) return -1.0

        val performance = getScoreRosuPerformance(score) ?: return -1.0

        val pp = performance.pp
        score.pp = pp
        return pp
    }

    private fun applyPPToScoreFromCosu(score: LazerScore): Double {
        if (score.pp > 1e-4) {
            return -1.0
        } else {
            val pp = beatmapApiService.getAttributesFromLocal(score.beatmapID, score.mode, score.toCosuScore()).performance?.pp

            pp?.let { score.pp = it }

            return pp ?: -1.0
        }
    }

    override fun applyPPToScoresWithSameBeatmap(scores: Collection<LazerScore>) {
        val noPPs = scores.filter { it.pp <= 1e-4 }

        val attrs = getScoresPPWithSameBeatmap(noPPs)

        noPPs.forEach { score ->
            when (val cp = attrs[score.scoreID]) {
                is RosuPerformance -> score.pp = cp.pp
                is CosuPerformance -> score.pp = cp.pp
                else -> {}
            }
        }
    }

    private fun getScoresPPWithSameBeatmap(scores: Collection<LazerScore>): Map<Long, CalculatePerformance> {
        val priority = calculatePriority


        for (strategy in priority) {
            val found = when (strategy) {
                CalculateStrategy.R_OSU -> getScoresPPWithSameBeatmapFromRosu(scores)

                CalculateStrategy.C_OSU -> getScoresPPWithSameBeatmapFromCosu(scores)

                else -> emptyMap()
            }

            if (found.isNotEmpty()) {
                return found
            }
        }

        return emptyMap()
    }

    private fun getScoresPPWithSameBeatmapFromCosu(scores: Collection<LazerScore>): Map<Long, CosuPerformance> {
        return scores.mapNotNull { score ->
            val cs = score.toCosuScore()
            val pp = beatmapApiService.getAttributesFromLocal(score.beatmapID, score.mode, cs).performance

            pp?.let { score.scoreID to pp }
        }.toMap()
    }

    private fun getScoresPPWithSameBeatmapFromRosu(scores: Collection<LazerScore>): Map<Long, RosuPerformance> {
        if (scores.isEmpty() || rosu == null) return emptyMap()

        val beatmapID = scores.first().beatmapID

        val bytes = beatmapApiService.getBeatmapFileByte(beatmapID) ?: return emptyMap()

        return scores.associate { score ->
            val result = rosu.let { r ->
                r.loadBeatmap(bytes).use { beatmap ->
                    val difficulty = score.buildDifficultyRequest()
                    val performance = score.buildPerformanceRequest(difficulty)
                    val result = r.calculatePerformance(beatmap, performance)

                    RosuPerformance(result)
                }
            }

            score.scoreID to result
        }
    }

    override fun getAccPPList(
        beatmapID: Long,
        mode: OsuMode,
        mods: List<LazerMod>,
        combo: Int?,
        misses: Int?,
        isLazer: Boolean,
        accuracy: DoubleArray,
        clockRate: Double?,
    ): List<Double> {
        if (rosu == null) return emptyList()

        val bytes = beatmapApiService.getBeatmapFileByte(beatmapID) ?: return emptyList()

        return accuracy.map { acc ->
            rosu.let { r ->
                r.loadBeatmap(bytes).use { beatmap ->
                    val difficulty = customDifficultyRequest(mods, isLazer, mode, clockRate)
                    val performance = difficulty.customPerformanceRequest(acc, combo, misses)
                    val result = r.calculatePerformance(beatmap, performance)

                    RosuPerformance(result).pp
                }
            }
        }
    }

    override fun getAccPP(
        beatmapID: Long,
        mode: OsuMode,
        mods: List<LazerMod>,
        combo: Int?,
        misses: Int?,
        isLazer: Boolean,
        accuracy: Double,
        clockRate: Double?
    ): Double {
        if (rosu == null) return 0.0

        val bytes = beatmapApiService.getBeatmapFileByte(beatmapID) ?: return 0.0

        return rosu.let { r ->
            r.loadBeatmap(bytes).use { beatmap ->
                val difficulty = customDifficultyRequest(mods, isLazer, mode, clockRate)
                val performance = difficulty.customPerformanceRequest(accuracy, combo, misses)
                val result = r.calculatePerformance(beatmap, performance)

                RosuPerformance(result).pp
            }
        }
    }

    override fun getBeatmapStar(
        beatmapID: Long,
        mode: OsuMode,
        mods: List<LazerMod>,
        hasLeaderBoard: Boolean
    ): Double {
        return getBeatmapStars(listOf(BeatmapDetails(beatmapID, mode, mods, hasLeaderBoard))).values.firstOrNull()
            ?: 0.0
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
    ): Map<BeatmapDetails, Double> {
        val priority = calculatePriority

        val resultStarMap = mutableMapOf<BeatmapDetails, Double>()

        // 1. 使用 LinkedHashMap 保持原始顺序，同时提供 O(1) 的移除性能
        // Key 为 beatmapID, Value 为对应的详情对象
        val remaining = details.associateByTo(LinkedHashMap()) { it }

        for (strategy in priority) {
            if (remaining.isEmpty()) break

            // 2. 根据策略动态准备输入数据
            val input = when (strategy) {
                CalculateStrategy.C_OSU, CalculateStrategy.R_OSU -> remaining.values.toList()

                else -> remaining.values.filter { it.mods.isOfficialCalculateAbleMod() }
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
                    if (enableRosu) {
                        getBeatmapStarFromLocal(input)
                    } else emptyMap()
                }

                CalculateStrategy.OFFICIAL_API -> {
                    getBeatmapStarFromOfficial(input)
                }

                CalculateStrategy.C_OSU -> {
                    getBeatmapStarFromCosu(input)
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
        if (rosu == null) return emptyMap()

        val starMap = mutableMapOf<BeatmapDetails, Double>()

        details.forEach { nd ->
            val bytes = beatmapApiService.getBeatmapFileByte(nd.beatmapID)

            if (bytes != null) {
                rosu.let { r ->
                    r.loadBeatmap(bytes).use { beatmap ->
                        val difficulty = customDifficultyRequest(nd.mods, null, nd.mode)
                        val result = r.calculateDifficulty(beatmap, difficulty)

                        val star = result.stars

                        if (star > 1e-4) {
                            scoreDao.saveStarRatingCacheAsync(
                                nd.beatmapID,
                                nd.mode,
                                nd.mods,
                                star.toFloat(),
                                nd.hasLeaderBoard
                            )
                            starMap[nd] = star
                        }
                    }
                }
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

    private fun getBeatmapStarFromCosu(details: List<BeatmapDetails>): Map<BeatmapDetails, Double> {
        val resultMap = ConcurrentHashMap<BeatmapDetails, Double>()

        details.chunked(15).forEach { batch ->
            val actions = batch.map { nd ->
                Callable {
                    runCatching {
                        val attr = beatmapApiService.getAttributesFromLocal(
                            nd.beatmapID, nd.mode,
                            CosuScore(mods = nd.mods)
                        )
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

    private fun getScoreRosuPerformance(score: LazerScore): RosuPerformance? {
        if (rosu == null) return RosuPerformance()

        val bytes = beatmapApiService.getBeatmapFileByte(score.beatmapID) ?: return null

        return rosu.let { r ->
            r.loadBeatmap(bytes).use { beatmap ->
                val difficulty = score.buildDifficultyRequest()
                val performance = score.buildPerformanceRequest(difficulty)
                val result = r.calculatePerformance(beatmap, performance)

                RosuPerformance(result)
            }
        }
    }

    companion object {
        // private val log = LoggerFactory.getLogger(OsuCalculateApiService::class.java)

        fun customDifficultyRequest(mods: List<LazerMod>, isLazer: Boolean? = null, mode: OsuMode = OsuMode.DEFAULT, clockRate: Double? = null): DifficultyRequest {
            val client = if (mode.isDefault()) {
                ScoreMode.DEFAULT
            } else {
                when(mode.safeModeValue) {
                    0.toByte(), 3.toByte() -> when(isLazer) {
                        true -> ScoreMode.LAZER
                        false -> ScoreMode.STABLE
                        null -> ScoreMode.DEFAULT
                    }

                    else -> ScoreMode.DEFAULT
                }
            }

            val builder = DifficultyRequest.builder()
                .mode(mode.toRosuMode())
                .modsJson(mods.toJson())
                .clockRate(clockRate ?: mods.getClockRate().toDouble())

            client.takeIf { it != ScoreMode.DEFAULT }?.let { builder.scoreMode(it) }

            return builder.build()
        }

        fun LazerScore.buildDifficultyRequest(): DifficultyRequest {
            val client = if (this.mode.isDefault()) {
                ScoreMode.DEFAULT
            } else {
                when(this.mode.safeModeValue) {
                    0.toByte(), 3.toByte() -> when(this.isLazer) {
                        true -> ScoreMode.LAZER
                        false -> ScoreMode.STABLE
                    }

                    else -> ScoreMode.DEFAULT
                }
            }

            val builder = DifficultyRequest.builder()
                .mode(this.mode.toRosuMode())
                .modsJson(this.mods.toJson())
                .clockRate(this.mods.getClockRate().toDouble())

            client.takeIf { it != ScoreMode.DEFAULT }?.let { builder.scoreMode(it) }

            return builder.build()
        }

        fun DifficultyRequest.customPerformanceRequest(
            accuracy: Double? = 1.0,
            combo: Int?,
            misses: Int? = 0,
        ): PerformanceRequest {
            val builder = PerformanceRequest.builder(this)

            accuracy?.let { builder.accuracy(it * 100.0) }
            combo?.let { builder.combo(it) }
            misses?.let { builder.misses(it) }

            return builder.build()
        }

        fun LazerScore.buildFullComboRequest(
            difficulty: DifficultyRequest,
        ): PerformanceRequest {
            val builder = PerformanceRequest.builder(difficulty)
                .accuracy(this.accuracy * 100.0)
                .combo(this.beatmap.maxCombo ?: (Int.MAX_VALUE / 2))

            val t = this.statistics

            when(this.mode.safeModeValue) {
                0.toByte() -> if (this.isLazer) { builder
                    .n300(t.great + t.miss)
                    .n100(t.ok)
                    .n50(t.meh)
                    .misses(0)
                    .largeTickHits(t.largeTickHit)
                    .smallTickHits(t.smallTickHit)
                    .sliderEndHits(t.sliderTailHit)
                } else { builder
                    .n300(t.great + t.miss)
                    .n100(t.ok)
                    .n50(t.meh)

                    this.legacyScore?.takeIf { it > 0 }?.let { builder.legacyTotalScore(it.toInt()) }
                }

                1.toByte() -> { builder
                    .n300(t.great + t.miss)
                    .n100(t.ok)
                    .misses(0)

                    // t.largeBonus.takeIf { it > 0 }?.let { builder.nGeki(it) }
                }

                2.toByte() -> builder
                    .n300(t.great + t.miss)
                    .n100(t.largeTickHit + t.largeTickMiss)
                    .n50(t.smallTickHit)
                    .misses(0)

                3.toByte() -> {
                    val rate = t.perfect / (t.perfect + t.great).coerceAtLeast(1)
                    val perfect = (t.miss * rate).coerceAtMost(t.miss)
                    val great = (t.miss - perfect).coerceAtLeast(0)

                    builder
                        .nGeki(t.perfect + perfect)
                        .n300(t.great + great)
                        .nKatu(t.good)
                        .n100(t.ok)
                        .n50(t.meh)
                        .misses(0)
                }
            }

            if (!this.passed) {
                builder.n300(0)
            }

            return builder.build()
        }

        fun LazerScore.buildPerfectRequest(
            difficulty: DifficultyRequest,
        ): PerformanceRequest {
            val builder = PerformanceRequest.builder(difficulty)
                .accuracy(100.0)
                .combo(this.beatmap.maxCombo ?: (Int.MAX_VALUE / 2))

            val t = this.statistics
            val m = this.maximumStatistics

            when(this.mode.safeModeValue) {
                0.toByte() -> if (this.isLazer || m.great > 0) { builder
                    .n300(m.great)
                    .n100(0)
                    .n50(0)
                    .misses(0)
                    .largeTickHits(m.largeTickHit)
                    .smallTickHits(m.smallTickHit)
                    .sliderEndHits(m.sliderTailHit)

                    this.legacyScore?.takeIf { it > 0 }?.let { builder.legacyTotalScore(it.toInt()) }
                } else { builder
                    .n300(t.great + t.ok + t.meh + t.miss)
                    .n100(0)
                    .n50(0)
                    .misses(0)

                    this.legacyScore?.takeIf { it > 0 }?.let { builder.legacyTotalScore(it.toInt()) }
                }

                1.toByte() -> {
                    if (m.great > 0) {builder
                        .n300(m.great)
                        .n100(0)
                        .misses(0)
                    } else { builder
                        .n300(t.great + t.ok + t.miss)
                        .n100(0)
                        .misses(0)
                    }

                    // m.largeBonus.takeIf { it > 0 }?.let { builder.nGeki(it) }
                }

                2.toByte() -> if (m.great > 0) { builder
                    .n300(m.great)
                    .n100(m.largeTickHit)
                    .n50(m.smallTickHit)
                    .misses(0)
                } else { builder
                    .n300(t.great + t.miss)
                    .n100(t.largeTickHit + t.largeTickMiss)
                    .n50(t.smallTickHit + t.smallTickMiss)
                    .misses(0)
                }

                3.toByte() -> if (m.perfect > 0) { builder
                    .nGeki(m.perfect)
                    .n300(0)
                    .nKatu(0)
                    .n100(0)
                    .n50(0)
                    .misses(0)
                } else { builder
                    .nGeki(t.perfect + t.great + t.good + t.ok + t.meh + t.miss)
                    .n300(0)
                    .nKatu(0)
                    .n100(0)
                    .n50(0)
                    .misses(0)
                }
            }

            return builder.build()
        }

        fun LazerScore.buildPerformanceRequest(
            difficulty: DifficultyRequest,
        ): PerformanceRequest {
            val builder = PerformanceRequest.builder(difficulty)
                .accuracy(this.accuracy * 100.0)
                .combo(this.maxCombo)

            val t = this.statistics

            when(this.mode.safeModeValue) {
                0.toByte() -> if (this.isLazer) { builder
                    .n300(t.great)
                    .n100(t.ok)
                    .n50(t.meh)
                    .misses(t.miss)
                    .largeTickHits(t.largeTickHit)
                    .smallTickHits(t.smallTickHit)
                    .sliderEndHits(t.sliderTailHit)
                } else { builder
                    .n300(t.great)
                    .n100(t.ok)
                    .n50(t.meh)
                    .misses(t.miss)

                    this.legacyScore?.takeIf { it > 0 }?.let { builder.legacyTotalScore(it.toInt()) }
                }

                1.toByte() -> { builder
                    .n300(t.great)
                    .n100(t.ok)
                    .misses(t.miss)

                    // t.largeBonus.takeIf { it > 0 }?.let { builder.nGeki(it) }
                }

                2.toByte() -> builder
                    .n300(t.great)
                    .n100(t.largeTickHit)
                    .n50(t.smallTickHit)
                    .misses(t.miss + t.largeTickMiss)

                3.toByte() -> builder
                    .nGeki(t.perfect)
                    .n300(t.great)
                    .nKatu(t.good)
                    .n100(t.ok)
                    .n50(t.meh)
                    .misses(t.miss)

            }

            return builder.build()
        }
    }
}
