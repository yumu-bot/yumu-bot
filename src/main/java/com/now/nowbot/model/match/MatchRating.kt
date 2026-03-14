package com.now.nowbot.model.match

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.util.BeatmapUtil
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToLong

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
class MatchRating(
    val match: Match,
    private val ratingParam: RatingParam,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,

    @field:JsonProperty("is_skipping")
    val skipping: Boolean = false
) {
    @JsonIgnore
    private val fullRounds: List<Match.MatchRound> = match.events
        .mapNotNull { it.round }
        .filter { it.scores.isNotEmpty() && it.endTime != null }
        .distinctBy { it.roundID }

    @get:JsonIgnore
    val rounds: List<Match.MatchRound>

    @get:JsonIgnore
    val scores: List<LazerScore>

    @get:JsonIgnore
    val players: Map<Long, MicroUser>

    @JsonIgnore
    private val fullPlayers: Map<Long, MicroUser> = match.players.associateBy { it.userID }

    init {
        rounds = applyParams(fullRounds, ratingParam)
        scores = rounds.flatMap { it.scores }

        // 有可用成绩的才能进这个分组
        val hasScoreSet = scores.map { it.userID }.toSet()
        players = fullPlayers.filterKeys { it in hasScoreSet }
    }

    @get:JsonProperty("round_count")
    val roundCount: Int = this.rounds.size

    @get:JsonProperty("score_count")
    val scoreCount: Int = this.scores.size

    @get:JsonProperty("player_count")
    val playerCount: Int = this.players.size

    private fun applyParams(rounds: List<Match.MatchRound>, param: RatingParam): List<Match.MatchRound> {
        var rs = rounds.toList()

        if (param.delete) {
            rs = rs.map { round ->
                if (round.scores.isNotEmpty()) {
                    round.apply { scores = scores.filter { it.score > 10000L } }
                } else round
            }
        }

        if (!param.rematch) {
            // 保留最后出现的元素
            rs = rs.reversed().distinctBy { it.beatmapID }.reversed()
        }

        // skip and limit limits
        val size = rs.size
        val skip = param.skip.coerceIn(0, size)
        val limit = (size - param.ignore).coerceIn(skip, size)

        if (skip != 0 || limit != size) {
            rs = rs.subList(skip, limit)
        }

        // remove specific indices
        if (!param.remove.isNullOrEmpty()) {
            val removeSet = param.remove
                .map { it - skip }
                .filter { it in 1 until (limit - skip) }
                .toSet()

            if (removeSet.isNotEmpty()) {
                rs = rs.filterIndexed { index, _ -> index !in removeSet }
            }
        }

        // easy multiplier and ranking
        rs.forEach { round ->
            round.scores.forEach { s ->
                if (param.easy != 1.0 && LazerMod.hasMod(s.mods, LazerMod.Easy)) {
                    s.score = (s.score * param.easy).roundToLong()
                }
            }

            // 重新排序并赋 rank
            round.scores = round.scores.sortedByDescending { it.score }.onEachIndexed { index, s ->
                s.ranking = index + 1
            }
        }

        // add user
        rs.flatMap { it.scores }.forEach { s -> fullPlayers[s.userID]?.let { s.user = it } }

        // apply sr change
        rs.forEach { round ->
            round.beatmap?.let {
                val b = beatmapApiService.getBeatmapFromDatabase(round.beatmapID)
                calculateApiService.applyStarToBeatmap(b, round.mode, LazerMod.getModsList(round.mods))
                round.beatmap = b
            }
        }

        return rs
    }

    data class RatingParam(
        val skip: Int = 0,
        val ignore: Int = 0,
        val remove: List<Int>? = null,
        val easy: Double = 1.0,
        val delete: Boolean = true,
        val rematch: Boolean = true,
    )

    constructor(
        match: Match, beatmapApiService: OsuBeatmapApiService, calculateApiService: OsuCalculateApiService
    ) : this(match, RatingParam(), beatmapApiService, calculateApiService)

    @get:JsonProperty("is_team_vs")
    val isTeamVs: Boolean
        get() = rounds.firstOrNull()?.isTeamVS
            ?: match.events.mapNotNull { it.round }.lastOrNull()?.isTeamVS
            ?: false

    @get:JsonProperty("average_star")
    val averageStarRating: Double
        get() = if (rounds.isNotEmpty()) rounds.sumOf { it.beatmap?.starRating ?: 0.0 } / rounds.size else 0.0

    @get:JsonProperty("first_map_bid")
    val firstMapBID: Long
        get() = rounds.firstOrNull()?.beatmapID ?: 0L

    @get:JsonProperty("first_map_sid")
    val firstMapSID: Long
        get() = rounds.firstOrNull()?.beatmap?.beatmapsetID ?: 0L

    @JsonIgnore
    private var playerDataMap: Map<Long, PlayerData> = players.mapValues { PlayerData(it.value) }

    @get:JsonProperty("skip_ignore_map")
    val skipIgnoreMap: Map<String, Number>
        get() = mapOf(
            "skip" to ratingParam.skip,
            "ignore" to ratingParam.ignore,
            "easy" to ratingParam.easy
        )

    @get:JsonProperty("team_point_map")
    val teamPointMap: Map<String, Int>
        get() = rounds.mapNotNull { it.winningTeam }
            .groupingBy { it }
            .eachCount()

    @JsonProperty("player_data_list")
    var playerDataList: List<PlayerData> = emptyList()

    @JsonIgnore
    private var roundAMG: Double = 0.0

    @JsonIgnore
    private var minMQ: Double = 100.0

    @get:JsonIgnore
    private val scalingFactor: Double
        get() = if (players.size <= 2) 0.0 else 2.0 / (1.0 + exp(0.5 - 0.25 * players.size)) - 1.0

    // 主计算
    fun calculate() {
        calculateRawRating()
        calculateAverageRoundWinShare()
        calculateTotalScore()
        calculateAverageMuPoint()
        calculateNormalizedMuPoint()
        calculateMuRating()
        calculateIndex()
        calculateClass()
        calculateData()
    }

    private fun calculateRawRating() {
        for (r in rounds) {
            val roundScoreSum = r.scores.sumOf { it.score }
            val roundScoreCount = r.scores.size

            if (roundScoreSum == 0L || roundScoreCount == 0) continue

            for (s in r.scores) {
                if (s.score == 0L) continue
                playerDataMap[s.userID]?.let { data ->
                    data.rawRatings.add(s.score.toDouble() * roundScoreCount / roundScoreSum)
                    data.scores.add(s.score)
                    if (data.team == null) {
                        data.team = s.playerStat?.team
                    }
                }
            }
        }
    }

    private fun calculateAverageRoundWinShare() {
        for (r in rounds) {
            if (r.winningTeamScore == 0L) continue

            for (s in r.scores) {
                if (s.score == 0L) continue
                val data = playerDataMap[s.userID] ?: continue

                var rws = 0.0
                if (r.isTeamVS) {
                    val team = s.playerStat?.team
                    if (team == r.winningTeam) {
                        rws = s.score.toDouble() / r.winningTeamScore
                        data.win++
                    } else if (r.winningTeam.isNullOrEmpty() || team.isNullOrEmpty()) {
                        rws = s.score.toDouble() / r.winningTeamScore
                    } else {
                        data.lose++
                    }
                } else {
                    if (s.score >= r.maxScore) {
                        rws = s.score.toDouble() / r.winningTeamScore
                        data.win++
                    } else {
                        data.lose++
                    }
                }
                data.roundWinShares.add(rws)
            }
        }
    }

    private fun calculateTotalScore() {
        playerDataMap.values.forEach { it.calculateTotalScore() }
    }

    private fun calculateAverageMuPoint() {
        playerDataMap.values.forEach {
            it.calculateRWS()
            it.calculateTMG()
            it.calculateAverageScore()
            it.associatedRoundCount = rounds.size
            roundAMG += it.averageMuPoint
        }
    }

    private fun calculateNormalizedMuPoint() {
        val aAMG = roundAMG / players.size
        playerDataMap.values.forEach {
            it.calculateMQ(aAMG)
            minMQ = min(minMQ, it.normalizedMuPoint)
        }
    }

    private fun calculateMuRating() {
        playerDataMap.values.forEach {
            it.calculateERA(minMQ, scalingFactor)
            it.calculateDRA(playerCount, scoreCount)
            it.calculateMRA()
        }
    }

    private fun calculateIndex() {
        val values = playerDataMap.values.toList()
        if (values.isEmpty()) return

        val maxIndex = (values.size - 1).coerceAtLeast(1).toDouble()

        values.sortedByDescending { it.era }.forEachIndexed { i, data ->
            data.eraIndex = if (values.size > 1) i / maxIndex else 0.5
        }

        values.sortedByDescending { it.dra }.forEachIndexed { i, data ->
            data.draIndex = if (values.size > 1) i / maxIndex else 0.5
        }

        values.sortedWith(compareByDescending<PlayerData> { it.rws }.thenByDescending { it.dra })
            .forEachIndexed { i, data ->
                data.rwsIndex = if (values.size > 1) i / maxIndex else 0.5
            }

        values.sortedByDescending { it.mra }.forEachIndexed { i, data ->
            data.ranking = i + 1
        }
    }

    private fun calculateClass() {
        playerDataMap.values.forEach { it.calculateClass() }
    }

    private fun calculateData() {
        playerDataList = playerDataMap.values.sortedByDescending { it.mra }
    }

    class PlayerData(val player: MicroUser) {
        var team: String? = null

        @JsonIgnore
        var scores: MutableList<Long> = mutableListOf()

        @JsonIgnore
        var roundWinShares: MutableList<Double> = mutableListOf()

        var total: Long = 0L

        @JsonIgnore
        var rawRatings: MutableList<Double> = mutableListOf()

        @JsonIgnore
        var totalMuPoint: Double = 0.0

        @JsonIgnore
        var averageMuPoint: Double = 0.0

        @JsonIgnore
        var normalizedMuPoint: Double = 0.0

        var era: Double = 0.0
        var dra: Double = 0.0
        var mra: Double = 0.0
        var rws: Double = 0.0

        @JsonProperty("player_class")
        var playerClass: PlayerClass? = null

        @JsonIgnore
        var eraIndex: Double = 0.0
        @JsonIgnore
        var draIndex: Double = 0.0
        @JsonIgnore
        var rwsIndex: Double = 0.0

        var ranking: Int = 0
        var win: Int = 0
        var lose: Int = 0

        @JsonProperty("arc")
        var associatedRoundCount: Int = 0

        fun calculateTotalScore() { total = scores.sum() }

        fun calculateTMG() { totalMuPoint = rawRatings.sum() }

        fun calculateAverageScore() {
            if (rawRatings.isNotEmpty()) {
                averageMuPoint = totalMuPoint / rawRatings.size
            }
        }

        fun calculateMQ(aAMG: Double) { normalizedMuPoint = averageMuPoint / aAMG }

        fun calculateERA(minMQ: Double, scalingFactor: Double) {
            era = (normalizedMuPoint - minMQ * scalingFactor) / (1 - minMQ * scalingFactor)
        }

        fun calculateDRA(playerCount: Int, scoreCount: Int) {
            dra = (totalMuPoint / scoreCount) * playerCount
        }

        fun calculateMRA() { mra = 0.7 * era + 0.3 * dra }

        fun calculateRWS() { rws = roundWinShares.average().takeIf { !it.isNaN() } ?: 0.0 }

        fun calculateClass() { playerClass = PlayerClass(eraIndex, draIndex, rwsIndex) }
    }

    companion object {
        fun MatchRating.insertMicroUserToScores() {
            this.match.events
                .mapNotNull { it.round }
                .flatMap { it.scores }
                .forEach { s -> s.user = this.players[s.userID] ?: MicroUser() }
        }

        fun MatchRating.applyDTMod() {
            this.match.events
                .mapNotNull { it.round }
                .filter { it.beatmap != null }
                .forEach { round ->
                    val mods = LazerMod.getModsList(round.mods)
                    BeatmapUtil.applyBeatmapChanges(round.beatmap, mods)
                    calculateApiService.applyStarToBeatmap(round.beatmap, round.mode, mods)
                }
        }
    }
}