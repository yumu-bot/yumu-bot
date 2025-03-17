package com.now.nowbot.model.multiplayer

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.multiplayer.MatchRating.PlayerData
import com.now.nowbot.model.multiplayer.MatchRating.RatingParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.exp
import kotlin.math.min

class SeriesRating(
    val matches: List<Match>,
    private val ratingParams: List<RatingParam>,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService
) {
    @JsonIgnore
    private val ratings: List<MatchRating> = run {
        val rs = mutableListOf<MatchRating>()

        for (i in matches.indices) {
            val m = matches[i]
            val p = ratingParams[i]

            val mr = MatchRating(m, p, beatmapApiService, calculateApiService)

            //  mr.calculate() 无需计算
            rs.add(mr)
        }

        return@run rs
    }

    @get:JsonProperty("match_count")
    val matchCount: Int
        get() = this.matches.size

    @JsonIgnore
    val players = run {
        return@run ratings.flatMap { it.players.entries }
            .associate { (k, v) -> k to v }
    }

    @get:JsonProperty("player_count")
    val playerCount: Int
        get() = this.players.size

    @JsonIgnore
    val rounds = run {
        return@run ratings.flatMap { it.rounds }.toList()
    }

    @get:JsonProperty("round_count")
    val roundCount: Int
        get() = this.rounds.size

    @JsonIgnore
    val scores = run {
        return@run ratings.flatMap { it.scores }.toList()
    }

    @JsonIgnore
    var name: String? = null

    @get:JsonProperty("score_count")
    val scoreCount: Int
        get() = this.scores.size

    /*
    @JsonIgnore
    private var totalHR: Triple<Long, Int, Int> = Triple(0, 0, 0)
    @JsonIgnore
    private var totalHD: Triple<Long, Int, Int> = Triple(0, 0, 0)
    @JsonIgnore
    private var totalDT: Triple<Long, Int, Int> = Triple(0, 0, 0)
    @JsonIgnore
    private var totalEZ: Triple<Long, Int, Int> = Triple(0, 0, 0)

     */

    // 以前的 SeriesStat
    val statistics: Match.MatchStat
        get() {
            val firstMatch = matches.minBy { it.startTime.toEpochSecond() }

            val startTime = firstMatch.startTime
            val endTime = matches.map { it.endTime }.maxBy { it?.toEpochSecond() ?: 0L }
            val name = name ?: (firstMatch.name + "...")
            return Match.MatchStat(firstMatch.id, startTime, endTime, name)
        }

    @get:JsonProperty("is_team_vs")
    val isTeamVs: Boolean
        get() {
            return if (rounds.isNotEmpty()) {
                rounds.first().teamType == "team-vs"
            } else {
                false
            }
        }

    @get:JsonProperty("average_star")
    val averageStarRating: Double
        get() {
            return if (rounds.isNotEmpty()) {
                1.0 * rounds.sumOf { it.beatMap?.starRating ?: 0.0 } / rounds.size
            } else {
                0.0
            }
        }

    @get:JsonProperty("first_map_bid")
    val firstMapBID: Long
        get() {
            return if (rounds.isNotEmpty()) {
                rounds.first().beatMapID
            } else {
                0L
            }
        }

    @get:JsonProperty("first_map_sid")
    val firstMapSID: Long
        get() {
            return if (rounds.isNotEmpty()) {
                rounds.first().beatMap?.beatMapSetID ?: 0L
            } else {
                0L
            }
        }

    @JsonIgnore
    private var playerDataMap: Map<Long, PlayerData> = players
        .map { (k, v) -> k to PlayerData(v)}
        .toMap()

    @JsonProperty("player_data_list")
    var playerDataList: List<PlayerData> = listOf()

    // 用于计算 mra
    @JsonIgnore
    private var roundAMG: Double = 0.0

    // 最小的 MQ，用来计算平均和最差的差值
    @JsonIgnore
    private var minMQ: Double = 100.0

    // 缩放因子
    @get:JsonIgnore
    private val scalingFactor: Double
        get() = if (players.size <= 2) {
            0.0
        } else {
            2.0 / (1.0 + exp(0.5 - 0.25 * players.size)) - 1.0
        }
    // 主计算
    fun calculate() {
        //挨个成绩赋予 RRA
        calculateRawRating()

        //挨个成绩赋予 RWS
        calculateAverageRoundWinShare()

        //这里必须分两次获取。TotalScore 是需要遍历第一遍然后算得的一个最终值
        calculateTotalScore()

        //挨个用户计算 AMG，并记录总 AMG，顺便赋予有关联的对局数量
        calculateAverageMuPoint()

        //挨个计算 MQ，并记录最小
        calculateNormalizedMuPoint()

        //根据 minMQ 计算出 ERA，DRA，MRA
        calculateMRA()

        //计算 E、D、M 的序号并排序
        calculateIndex()

        //计算玩家分类
        calculateClass()

        calculateData()
    }

    // RRA
    private fun calculateRawRating() {
        for (r in rounds) {
            val roundScoreSum = r.scores.sumOf { it.score }
            val roundScoreCount = r.scores.size

            if (roundScoreSum == 0 || roundScoreCount == 0) continue

            for (s in r.scores) {
                val data = playerDataMap[s.userID]
                if (data == null || s.score == 0) continue

                data.rawRatings.add(1.0 * s.score * roundScoreCount / roundScoreSum)
                data.scores.add(s.score)

                if (data.team == null) {
                    data.team = s.playerStat.team
                }
            }
        }
    }

    // RWS
    private fun calculateAverageRoundWinShare() {
        for (r in rounds) {
            if (r.winningTeamScore == 0L) return

            for (s in r.scores) {
                val data = playerDataMap[s.userID]
                if (data == null || s.score == 0) continue

                val rws: Double

                if (r.isTeamVS) {
                    if (data.team == r.winningTeam) {
                        rws = 1.0 * s.score / r.winningTeamScore
                        data.win += 1
                    } else if (null == r.winningTeam) {
                        rws = 1.0 * s.score / r.winningTeamScore
                    } else {
                        rws = 0.0
                        data.lose += 1
                    }
                } else {
                    if (s.score >= r.maxScore) {
                        rws = 1.0 * s.score / r.winningTeamScore
                        data.win += 1
                    } else {
                        rws = 0.0
                        data.lose += 1
                    }
                }

                data.roundWinShares.add(rws)
            }
        }
    }

    // TTS
    private fun calculateTotalScore() {
        playerDataMap.values
            .forEach { it.calculateTotalScore() }
    }

    // AMG
    private fun calculateAverageMuPoint() {
        playerDataMap.values
            .forEach {
                it.calculateRWS()
                it.calculateTMG()
                it.calculateAverageScore()
                it.associatedRoundCount = rounds.size
                roundAMG += it.averageMuPoint
            }
    }

    // MQ
    private fun calculateNormalizedMuPoint() {
        playerDataMap.values.forEach{
            it.calculateMQ(roundAMG / players.size)
            //除以的是所有玩家数
            minMQ = min(minMQ, it.normalizedMuPoint)
        }
    }

    private fun calculateMRA() {
        playerDataMap.values.forEach{
            it.calculateERA(minMQ, scalingFactor)
            it.calculateDRA(players.size, scores.size)
            it.calculateMRA()
        }
    }

    private fun calculateIndex() {
        if (players.isEmpty()) return

        val ai1 = AtomicInteger(1)
        val ai2 = AtomicInteger(1)
        val ai3 = AtomicInteger(1)
        val ai4 = AtomicInteger(1)

        val v = playerDataMap.values

        v.sortedByDescending { it.era }.forEach { it.eraIndex = (1.0 * ai1.getAndIncrement() / players.size) }
        v.sortedByDescending { it.dra }.forEach { it.draIndex = (1.0 * ai2.getAndIncrement() / players.size) }
        v.sortedByDescending { it.rws }.forEach { it.rwsIndex = (1.0 * ai3.getAndIncrement() / players.size) }
        v.sortedByDescending { it.mra }.forEach { it.ranking = (ai4.getAndIncrement()) }
    }

    private fun calculateClass() {
        playerDataMap.values
            .forEach { it.calculateClass() }
    }

    private fun calculateData() {
        // 根据 MRA 高低排序，重新写图
        val l = playerDataMap.values.toMutableList()

        l.sortByDescending { it.mra }

        playerDataList = l
    }
}