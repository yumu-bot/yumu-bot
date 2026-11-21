package com.now.nowbot.model.match

import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerMod.Companion.contains
import com.now.nowbot.model.osu.LazerMod.Companion.toLazerMods
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import org.slf4j.LoggerFactory
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sqrt

private const val COMBO_GUIDANCE_SCORE = 700_000.0
private const val ACCURACY_GUIDANCE_SCORE = 300_000.0

// 黄金分割：ERA : DRA = 0.6180339887498949... : 0.3819660112501051...
// 用 √5 的闭式表达式而不是四舍五入后的常量，避免精度损失。
private val GOLDEN_RATIO_ERA_WEIGHT = (sqrt(5.0) - 1.0) / 2.0
private val GOLDEN_RATIO_DRA_WEIGHT = 1.0 - GOLDEN_RATIO_ERA_WEIGHT

private val BONUS_MOD_ACRONYMS = setOf("HD", "DT", "NC", "HR", "EZ", "FL")

/**
 * 对局计分方式。osu! 的 match API 会给出 score / accuracy / combo / scorev2。
 *
 * 空值、未知值以及历史代码里的 "score-v2" 都按 scorev2 处理，保持原来的分数占比统计。
 */
enum class MatchScoringType {
    SCORE_V2,
    SCORE,
    ACCURACY,
    COMBO;

    val comboWeight: Double
        get() = if (this == SCORE || this == COMBO) 1.0 else 0.0

    val accuracyWeight: Double
        get() = if (this == SCORE || this == ACCURACY) 1.0 else 0.0

    companion object {
        fun from(value: String?): MatchScoringType {
            return when (value?.trim()?.lowercase()?.replace("-", "")?.replace("_", "")) {
                null, "", "scorev2", "v2" -> SCORE_V2
                "score" -> SCORE
                "accuracy", "acc" -> ACCURACY
                "combo" -> COMBO
                // 未知类型不做激进处理，继续按原来的 scorev2 统计
                else -> SCORE_V2
            }
        }
    }
}

/**
 * 谱面统计表的 key。
 *
 * 同一张谱面可能在不同对局里使用不同计分方式或地图池 mod；这三种信息都会影响指导分，
 * 因此不能只按 beatmapID 聚合。scorev2 不依赖 mod，所以 modsKey 固定为空。
 */
data class BeatmapScoreKey(
    val beatmapID: Long,
    val scoringType: MatchScoringType,
    val modsKey: String = "",
)

/**
 * 一张谱面在一批成绩里的得分统计。
 *
 * scorev2：RRA 仍使用 [averageScore]（原始分数的平均分）。
 * score / accuracy / combo：RRA 使用 [guidanceScore]（MRA v4 指导分），
 * [averageCombo] / [averageAccuracy] / [modBonus] 是指导分的计算依据，
 * [ratingTotalScore] 是按指导分折算出来的谱面总分。
 *
 * [maxCombo] 优先取谱面数据；谱面缺失或无法计算时，回退到该谱面所有成绩中的最大 combo。
 */
data class BeatmapScoreStat(
    val beatmapID: Long,
    val scoringType: MatchScoringType,
    val modsKey: String,
    val totalScore: Long,
    val scoreCount: Int,
    val maxCombo: Int,
    val averageCombo: Double,
    val averageAccuracy: Double,
    val modBonus: Double,
    val guidanceScoreCount: Int,
    val guidanceScore: Double,
) {
    val averageScore: Double =
        if (scoreCount == 0) 0.0 else totalScore.toDouble() / scoreCount

    /**
     * 实际参与 RRA 的基准分：scorev2 用原始平均分，其余用指导分。
     */
    val ratingAverageScore: Double =
        if (scoringType == MatchScoringType.SCORE_V2) averageScore else guidanceScore

    /**
     * 按指导分折算的谱面总分。scorev2 时就是原始总分；
     * 其他计分方式只统计真正参与指导分计算（combo / accuracy 达标）的成绩。
     */
    val ratingTotalScore: Double =
        if (scoringType == MatchScoringType.SCORE_V2) totalScore.toDouble()
        else guidanceScore * guidanceScoreCount

    /**
     * 单条成绩在指导分体系下的 expected。
     *
     * scorev2 不使用这个表达式；accuracy / combo 模式只会启用其中一个权重。
     */
    fun expectedScore(score: LazerScore): Double {
        if (scoringType == MatchScoringType.SCORE_V2 || maxCombo <= 0) return 0.0

        val combo = min(score.maxCombo, maxCombo).coerceAtLeast(0)
        val accuracy = score.accuracy.takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) ?: 0.0

        return scoringType.comboWeight * COMBO_GUIDANCE_SCORE * combo.toDouble() / maxCombo +
            scoringType.accuracyWeight * ACCURACY_GUIDANCE_SCORE * accuracy.pow(8)
    }
}

/**
 * 已经按 RatingParam 处理过、可以直接参与计算的数据。
 * rounds / scores / players 之间不再需要回读 Match。
 */
internal data class PreparedMatch(
    val rounds: List<Match.MatchRound>,
    val scores: List<LazerScore>,
    val players: Map<Long, MicroUser>,
)

/**
 * 把 Match 整理成可计算数据：
 * 过滤、easy、排序、赋 Rank、批量取谱面、批量算星、批量挂别名。
 *
 * 这些操作本来散落在构造函数和 companion 扩展里，现在集中在这里。
 */
internal class MatchRatingPreparer(
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
) {
    fun prepare(match: Match, param: MatchRating.RatingParam): PreparedMatch {
        val fullRounds = match.events
            .mapNotNull { it.round }
            .filter { it.scores.isNotEmpty() && it.endTime != null }
            .distinctBy { it.roundID }

        var rounds = fullRounds

        if (param.delete) {
            rounds = rounds.map { round ->
                if (round.scores.isNotEmpty()) {
                    round.apply { scores = scores.filter { it.score > 10000L } }
                } else {
                    round
                }
            }
        }

        if (!param.rematch) {
            // 保留最后出现的元素
            rounds = rounds.reversed().distinctBy { it.beatmapID }.reversed()
        }

        // skip and limit limits
        val size = rounds.size
        val skip = param.skip.coerceIn(0, size)
        val limit = (size - param.ignore).coerceIn(skip, size)

        if (skip != 0 || limit != size) {
            rounds = rounds.subList(skip, limit).toList()
        }

        // remove specific indices
        if (!param.remove.isNullOrEmpty()) {
            val removeSet = param.remove
                .map { it - skip }
                .filter { it in 1 until (limit - skip) }
                .toSet()

            if (removeSet.isNotEmpty()) {
                rounds = rounds.filterIndexed { index, _ -> index !in removeSet }
            }
        }

        // easy multiplier and ranking
        for (round in rounds) {
            val roundScores = round.scores

            if (param.easy != 1.0) {
                for (score in roundScores) {
                    if (score.mods.contains(LazerMod.Easy)) {
                        score.score = (score.score * param.easy).roundToLong()
                    }
                }
            }

            // 重新排序并赋 rank
            round.scores = roundScores
                .sortedByDescending { it.score }
                .onEachIndexed { index, score -> score.ranking = index + 1 }
        }

        val fullPlayers = match.players.associateBy { it.userID }
        val scores = rounds.flatMap { it.scores }

        // add user
        for (score in scores) {
            fullPlayers[score.userID]?.let { score.user = it }
        }

        // 有可用成绩的才能进这个分组
        val hasScoreSet = scores.mapTo(HashSet(scores.size)) { it.userID }
        val players = fullPlayers.filterKeys { it in hasScoreSet }

        // 一次查库拿全部谱面；同一张谱面可能被不同 mods 的 round 复用，
        // 所以每个 round 用独立实例，避免后一个 round 的星数覆盖前一个。
        val beatmapIDs = rounds
            .asSequence()
            .map { it.beatmapID }
            .filter { it > 0L }
            .distinct()
            .toList()

        val loadedBeatmaps = if (beatmapIDs.isEmpty()) {
            emptyMap()
        } else {
            runCatching { beatmapApiService.getBeatmapsFromDatabase(beatmapIDs) }
                .getOrNull()
                .orEmpty()
                .associateBy { it.beatmapID }
        }

        for (round in rounds) {
            val loaded = loadedBeatmaps[round.beatmapID] ?: continue
            val originalMaxCombo = round.beatmap?.maxCombo

            // maxCombo 优先用数据库；数据库没存时保留 match 数据；都没有则由成绩最大 combo 兜底
            round.beatmap = loaded.copy(
                maxCombo = loaded.maxCombo?.takeIf { it > 0 } ?: originalMaxCombo
            )
        }

        // 所有谱面一次性批量算星数，不再每个 round 单独发一次请求
        val starRequests = ArrayList<OsuCalculateApiService.BeatmapStarRequest>(rounds.size)
        for (round in rounds) {
            val beatmap = round.beatmap ?: continue
            val mode = runCatching { round.mode }.getOrNull() ?: continue
            val mods = round.mods.toLazerMods()

            starRequests.add(OsuCalculateApiService.BeatmapStarRequest(beatmap, mode, mods))
        }

        if (starRequests.isNotEmpty()) {
            // 星数计算失败 (谱面文件损坏 / 接口异常) 只应影响 starRating 是否更新,
            // 不能让整个 MatchRating 构造失败, 否则这一局的图片和后续监听都会丢
            runCatching { calculateApiService.applyStarToBeatmaps(starRequests) }
                .onFailure { e ->
                    log.warn("批量计算 {} 张谱面的星数失败, 沿用数据库中的星数", starRequests.size, e)
                }
        }

        // 比赛类面板会直接渲染 round.beatmap / score，别名要提前挂到谱面集上
        beatmapApiService.applyAlias(scores)
        beatmapApiService.applyAliasForBeatmaps(rounds.mapNotNull { it.beatmap })

        return PreparedMatch(rounds, scores, players)
    }

    companion object {
        private val log = LoggerFactory.getLogger(MatchRatingPreparer::class.java)
    }
}

/**
 * 已经按各自的 RatingParam 处理过、可以直接参与计算的系列赛数据。
 * rounds / scores / players 之间不再需要回读 Match。
 */
internal data class PreparedSeries(
    val rounds: List<Match.MatchRound>,
    val scores: List<LazerScore>,
    val players: Map<Long, MicroUser>,

    /** 每个玩家在所有比赛里的 round 总数，系列赛的 Play% 用它做分母。 */
    val associatedRoundCounts: Map<Long, Int>,
)

/**
 * 把多个 Match 整理成可计算数据。
 *
 * 每场对局都走一遍 [MatchRatingPreparer] 的同一套流程（过滤、easy、排序、赋 Rank、
 * 批量取谱面、批量算星、批量挂别名），然后合并成一个计算范围，
 * 因此系列赛里的 RRA 也是按谱面汇总的平均分计算的，而不是每局各算各的。
 */
internal class SeriesRatingPreparer(
    beatmapApiService: OsuBeatmapApiService,
    calculateApiService: OsuCalculateApiService,
) {
    private val matchPreparer = MatchRatingPreparer(beatmapApiService, calculateApiService)

    fun prepare(matches: List<Match>, params: List<MatchRating.RatingParam>): PreparedSeries {
        val preparedMatches = matches.mapIndexed { index, match ->
            matchPreparer.prepare(match, params.getOrElse(index) { MatchRating.RatingParam() })
        }

        return PreparedSeries(
            rounds = preparedMatches.flatMap { it.rounds },
            scores = preparedMatches.flatMap { it.scores },
            players = preparedMatches
                .flatMap { it.players.values }
                .associateBy { it.userID },
            associatedRoundCounts = countAssociatedRounds(matches),
        )
    }

    /**
     * 系列赛的 ARC：玩家所在的每场比赛里，所有 round 都算一次。
     *
     * 和单场比赛的 ARC = rounds.size 不同，这里不能直接用合并后的 rounds.size，
     * 否则打过很多场比赛的玩家会被低估 Play%。
     */
    private fun countAssociatedRounds(matches: List<Match>): Map<Long, Int> {
        val capacity = matches.sumOf { it.players.size } * 4 / 3 + 1
        val result = HashMap<Long, Int>(capacity)

        for (match in matches) {
            val roundCountInMatch = match.events.count { it.round != null }
            if (roundCountInMatch == 0) continue

            for (player in match.players) {
                result[player.userID] = (result[player.userID] ?: 0) + roundCountInMatch
            }
        }

        return result
    }
}

/**
 * 一场对局或一个系列赛共用的评分计算器。
 *
 * 设计要点：
 * 1. 先把 round 的比分、胜方、最大值等一次性快照下来，后面不再反复读 [Match.MatchRound] 的 getter；
 * 2. 先按 beatmapID + 计分方式 + 地图池 mod 汇总统计表，再统一计算 RRA；
 * 3. 所有玩家统计只遍历一遍成绩。
 */
internal class RatingCalculator(
    private val rounds: List<Match.MatchRound>,
    private val players: Map<Long, MicroUser>,
    private val associatedRoundCounts: Map<Long, Int> = emptyMap(),
) {
    fun calculate(): RatingResult {
        if (players.isEmpty()) {
            return RatingResult(emptyList(), emptyMap(), emptyMap())
        }

        val playerDataMap = LinkedHashMap<Long, MatchRating.PlayerData>(players.size * 4 / 3 + 1)
        players.forEach { (userID, player) -> playerDataMap[userID] = MatchRating.PlayerData(player) }

        // 1. 先给每个 round 做一次快照，同时累计每张谱面的统计
        val beatmapAccumulators = HashMap<BeatmapScoreKey, BeatmapAccumulator>(rounds.size * 4 / 3 + 1)
        val roundStats = ArrayList<RoundStat>(rounds.size)

        for (round in rounds) {
            roundStats.add(createRoundStat(round, beatmapAccumulators))
        }

        // 这就是按谱面汇总的「总分 / 次数 / 平均分 / 指导分」表
        val beatmapScoreStats = LinkedHashMap<BeatmapScoreKey, BeatmapScoreStat>(beatmapAccumulators.size * 4 / 3 + 1)
        for ((key, accumulator) in beatmapAccumulators) {
            beatmapScoreStats[key] = accumulator.toStat(key)
        }

        // 2. 一次遍历所有成绩，完成 RRA / RWS / 总分 / 胜场统计
        for (stat in roundStats) {
            val round = stat.round
            val scoreStat = beatmapScoreStats[stat.key]
            val ratingBase = scoreStat?.ratingAverageScore ?: 0.0

            for (score in round.scores) {
                val data = playerDataMap[score.userID] ?: continue
                val value = score.score

                if (value == 0L) continue

                data.scores.add(value)
                data.total += value

                // scorev2：原始分 / 原始平均分
                // score / accuracy / combo：expected / 指导分
                if (scoreStat != null && ratingBase > 0.0) {
                    val rating = if (scoreStat.scoringType == MatchScoringType.SCORE_V2) {
                        value / ratingBase
                    } else {
                        scoreStat.expectedScore(score) / ratingBase
                    }

                    data.rawRatings.add(rating)
                }

                if (data.team == null) {
                    data.team = score.playerStat?.team
                }

                // 胜方总分为 0 的 round 不参与 RWS，和原逻辑保持一致
                if (stat.winningTeamScore == 0L) continue

                val team = score.playerStat?.team
                var roundWinShare = 0.0

                if (stat.isTeamVS) {
                    if (team == stat.winningTeam) {
                        roundWinShare = value.toDouble() / stat.winningTeamScore
                        data.win++
                    } else if (stat.winningTeam.isNullOrEmpty() || team.isNullOrEmpty()) {
                        roundWinShare = value.toDouble() / stat.winningTeamScore
                    } else {
                        data.lose++
                    }
                } else {
                    if (value >= stat.maxScore) {
                        roundWinShare = value.toDouble() / stat.winningTeamScore
                        data.win++
                    } else {
                        data.lose++
                    }
                }

                // 失败局也要把 0.0 计入平均；否则 RWS 会只对胜局求平均而虚高
                data.roundWinShares.add(roundWinShare)
            }
        }

        // 3. 汇总每个玩家的派生数据
        val playerCount = playerDataMap.size

        for ((userID, data) in playerDataMap) {
            data.associatedRoundCount = associatedRoundCounts[userID] ?: 0
            data.totalMuPoint = data.rawRatings.sum()
            data.averageMuPoint =
                if (data.rawRatings.isEmpty()) 0.0 else data.totalMuPoint / data.rawRatings.size
            data.rws = data.roundWinShares.average().takeIf { !it.isNaN() } ?: 0.0
        }

        // 4. MQ / ERA / DRA / MRA
        val roundAMG = playerDataMap.values.sumOf { it.averageMuPoint }
        val averageAMG = if (playerCount > 0) roundAMG / playerCount else 0.0

        for (data in playerDataMap.values) {
            data.normalizedMuPoint =
                if (averageAMG > 0.0) data.averageMuPoint / averageAMG else 0.0
        }

        val minMQ = playerDataMap.values.minOfOrNull { it.normalizedMuPoint } ?: 0.0
        val scalingFactor =
            if (playerCount <= 2) 0.0 else 2.0 / (1.0 + exp(0.5 - 0.25 * playerCount)) - 1.0

        // minSRA 已经接近 1 时，ERA 的平移/缩放只会把微小差异放大，甚至产生抖动；
        // 这种情况直接让所有玩家 ERA = 1。
        val minSRANearOne = minMQ in 0.99..1.01

        // DRA = √TRA × N / Σ√TRA，和 SRA 一样是跨玩家的归一化值
        val sumSqrtTRA = playerDataMap.values.sumOf { sqrt(it.totalMuPoint) }

        for (data in playerDataMap.values) {
            val denominator = 1.0 - minMQ * scalingFactor

            data.era = when {
                minSRANearOne -> 1.0
                denominator == 0.0 -> 0.0
                else -> (data.normalizedMuPoint - minMQ * scalingFactor) / denominator
            }

            data.dra = if (sumSqrtTRA > 0.0) {
                sqrt(data.totalMuPoint) * playerCount / sumSqrtTRA
            } else {
                0.0
            }

            data.mra =
                GOLDEN_RATIO_ERA_WEIGHT * data.era + GOLDEN_RATIO_DRA_WEIGHT * data.dra
        }

        // 5. 排名、分类
        assignIndices(playerDataMap.values)

        for (data in playerDataMap.values) {
            data.playerClass = PlayerClass.getV2(data.eraIndex, data.draIndex, data.rwsIndex)
        }

        val playerDataList = playerDataMap.values.sortedByDescending { it.mra }

        return RatingResult(
            playerDataList = playerDataList,
            beatmapScoreStats = beatmapScoreStats,
            teamPointMap = countTeamPoints(roundStats),
        )
    }

    private fun createRoundStat(
        round: Match.MatchRound,
        beatmapAccumulators: MutableMap<BeatmapScoreKey, BeatmapAccumulator>,
    ): RoundStat {
        var totalScore = 0L
        var maxScore = 0L
        var redScore = 0L
        var blueScore = 0L

        // 只用 teamType 判断，避免再调用 MatchRound.isTeamVS / teamScores 等会重复求和的 getter
        val isTeamVS = round.teamType == "team-vs" || round.teamType == "tag-team-vs"

        val scoringType = MatchScoringType.from(round.scoringType)
        val modsKey = if (scoringType == MatchScoringType.SCORE_V2) "" else poolModsKey(round.mods)
        val key = BeatmapScoreKey(round.beatmapID, scoringType, modsKey)
        val beatmapMaxCombo = round.beatmap?.maxCombo?.takeIf { it > 0 }

        // 谱面 maxCombo 缺失（被删除 / 无法计算）时，用这一轮所有成绩的最大 combo 兜底；
        // 同一个 key 的多个 round 取最大值，就等价于「该谱面所有成绩中的最大 combo」。
        val maxCombo = beatmapMaxCombo
            ?: round.scores.maxOfOrNull { it.maxCombo.coerceAtLeast(0) }
            ?: 0
        val accumulator = beatmapAccumulators.getOrPut(key) { BeatmapAccumulator() }

        if (maxCombo > accumulator.maxCombo) {
            accumulator.maxCombo = maxCombo
        }

        accumulator.modBonus =
            if (scoringType == MatchScoringType.SCORE_V2) 1.0 else modBonus(round.mods)

        for (score in round.scores) {
            val value = score.score

            totalScore += value
            if (value > maxScore) maxScore = value

            when (score.playerStat?.team) {
                "red" -> redScore += value
                "blue" -> blueScore += value
            }

            accumulator.totalScore += value
            accumulator.scoreCount++

            // 指导分只统计 combo >= 10 且 accuracy >= 0.001 的成绩；
            // combo 先判断原始值，再限制到有效 maxCombo 以内。
            if (scoringType != MatchScoringType.SCORE_V2 && maxCombo > 0) {
                val combo = score.maxCombo
                val accuracy = score.accuracy.takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) ?: 0.0

                if (combo >= 10 && accuracy >= 0.001) {
                    accumulator.comboSum += min(combo, maxCombo).toDouble()
                    accumulator.accuracySum += accuracy
                    accumulator.guidanceScoreCount++
                }
            }
        }

        val winningTeam = if (isTeamVS) {
            when {
                redScore > blueScore -> "red"
                redScore < blueScore -> "blue"
                else -> null
            }
        } else {
            "none"
        }

        val winningTeamScore = if (isTeamVS) maxOf(redScore, blueScore) else totalScore

        return RoundStat(
            round = round,
            key = key,
            isTeamVS = isTeamVS,
            winningTeam = winningTeam,
            winningTeamScore = winningTeamScore,
            maxScore = maxScore,
        )
    }

    private fun assignIndices(values: Collection<MatchRating.PlayerData>) {
        val size = values.size
        val divisor = (size - 1).toDouble()

        fun toIndex(position: Int): Double = if (size > 1) position / divisor else 0.5

        values.sortedByDescending { it.era }.forEachIndexed { position, data ->
            data.eraIndex = toIndex(position)
        }

        values.sortedByDescending { it.dra }.forEachIndexed { position, data ->
            data.draIndex = toIndex(position)
        }

        values.sortedWith(compareByDescending<MatchRating.PlayerData> { it.rws }.thenByDescending { it.dra })
            .forEachIndexed { position, data ->
                data.rwsIndex = toIndex(position)
            }

        values.sortedByDescending { it.mra }.forEachIndexed { position, data ->
            data.ranking = position + 1
        }
    }

    private fun countTeamPoints(roundStats: List<RoundStat>): Map<String, Int> {
        val result = LinkedHashMap<String, Int>()

        for (stat in roundStats) {
            val team = stat.winningTeam ?: continue
            result[team] = (result[team] ?: 0) + 1
        }

        return result
    }

    private fun poolModsKey(mods: List<String>): String {
        return mods
            .asSequence()
            .map { it.uppercase() }
            .filter { it in BONUS_MOD_ACRONYMS }
            .distinct()
            .sorted()
            .joinToString("|")
    }

    private fun modBonus(mods: List<String>): Double {
        val acronyms = mods.mapTo(HashSet(mods.size)) { it.uppercase() }
        var bonus = 1.0

        if ("HD" in acronyms) bonus *= 1.04
        if ("DT" in acronyms || "NC" in acronyms) bonus *= 1.23
        if ("HR" in acronyms) bonus *= 1.09
        if ("EZ" in acronyms) bonus *= 0.75
        if ("FL" in acronyms) bonus *= 1.2

        return bonus
    }

    private class BeatmapAccumulator {
        var totalScore: Long = 0L
        var scoreCount: Int = 0
        var maxCombo: Int = 0
        var comboSum: Double = 0.0
        var accuracySum: Double = 0.0
        var guidanceScoreCount: Int = 0
        var modBonus: Double = 1.0

        fun toStat(key: BeatmapScoreKey): BeatmapScoreStat {
            val averageCombo =
                if (guidanceScoreCount > 0) comboSum / guidanceScoreCount else 0.0
            val averageAccuracy =
                if (guidanceScoreCount > 0) accuracySum / guidanceScoreCount else 0.0

            val guidanceScore =
                if (key.scoringType == MatchScoringType.SCORE_V2 || maxCombo <= 0 || guidanceScoreCount == 0) {
                    0.0
                } else {
                    (
                        key.scoringType.comboWeight * COMBO_GUIDANCE_SCORE * (averageCombo / maxCombo) +
                            key.scoringType.accuracyWeight * ACCURACY_GUIDANCE_SCORE * averageAccuracy.pow(8)
                        ) * modBonus
                }

            return BeatmapScoreStat(
                beatmapID = key.beatmapID,
                scoringType = key.scoringType,
                modsKey = key.modsKey,
                totalScore = totalScore,
                scoreCount = scoreCount,
                maxCombo = maxCombo,
                averageCombo = averageCombo,
                averageAccuracy = averageAccuracy,
                modBonus = modBonus,
                guidanceScoreCount = guidanceScoreCount,
                guidanceScore = guidanceScore,
            )
        }
    }

    private data class RoundStat(
        val round: Match.MatchRound,
        val key: BeatmapScoreKey,
        val isTeamVS: Boolean,
        val winningTeam: String?,
        val winningTeamScore: Long,
        val maxScore: Long,
    )
}

internal data class RatingResult(
    val playerDataList: List<MatchRating.PlayerData>,
    val beatmapScoreStats: Map<BeatmapScoreKey, BeatmapScoreStat>,
    val teamPointMap: Map<String, Int>,
)
