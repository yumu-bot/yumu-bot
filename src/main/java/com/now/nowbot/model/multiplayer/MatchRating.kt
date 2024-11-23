package com.now.nowbot.model.multiplayer

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.json.MicroUser
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToInt

// 原来的 MatchCalculate
class MatchRating(
    val match: Match,
    private val ratingParam: RatingParam,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService
) {
    @JsonIgnore
    val rounds: List<Match.MatchRound>
        = run {
            val r: MutableList<Match.MatchRound> = match.events
                .stream()
                .filter { it.round != null}
                .map { it.round!! }
                .filter { ! it?.scores.isNullOrEmpty() }
                .filter { it?.endTime != null }
                .toList()

        return@run r.applyParams(ratingParam)
        }

    @get:JsonProperty("round_count")
    val roundCount: Int
        get() = this.rounds.size

    @JsonIgnore
    val scores: List<Match.MatchScore> = run {
        return@run rounds.flatMap{ it.scores }
    }

    @get:JsonProperty("score_count")
    val scoreCount: Int
        get() = this.scores.size

    @JsonIgnore
    val players: Map<Long, MicroUser> = run {
        // 有可用成绩的才能进这个分组
        val playersHasScoreSet = scores.stream().map { it.userID }.collect(Collectors.toSet())

        return@run match.players.distinctBy { it.userID }
            .filter { it.userID in playersHasScoreSet }
            .associateBy { it.userID }
    }

    @get:JsonProperty("player_count")
    val playerCount: Int
        get() = this.players.size

    private fun MutableList<Match.MatchRound>.applyParams(param: RatingParam): List<Match.MatchRound> {
        var rs = this

        if (param.delete) {
            rs.forEach {
                if (it.scores.isNotEmpty()) {
                    it.scores = it.scores.filter { s -> s.score > 10000 }
                }
            }
        }

        if (! param.rematch) {
            // 保留最后出现的元素
            rs = rs.reversed().distinctBy { it.beatMapID }.reversed().toMutableList()
        }


        // skip and remove
        val size = rs.count()
        val skip = param.skip
        val limit = size - param.ignore

        if (skip < 0
            || skip > size
            || limit < 0
            || limit > size
            || limit - skip < 0) {
            return rs
        }

        rs = rs.subList(skip, limit)

        if (! param.remove.isNullOrEmpty()) {
            val remove = param.remove
                .map { it - skip }
                .filter { (it < limit - skip) && (it > 0) }
                .toSet()
                .sortedDescending()

            for (i in 0..< rs.count()) {
                if (i in remove) {
                    rs.removeAt(i)
                }
            }
        }

        // easy multiplier
        if (param.easy != 1.0) {
            rs.forEach {
                for (s in it.scores) {
                    if (LazerMod.hasModByString(it.mods, LazerMod.Easy)) {
                        s.score = (s.score * param.easy).roundToInt()
                    }
                }
            }
        }

        // add ranking
        rs.forEach {
            val i = AtomicInteger(1)

            val ss = it.scores.sortedByDescending { s -> s.score }

            for (s in ss) {
                s.ranking = i.getAndIncrement()
            }
        }

        // add user
        rs.forEach {
            it.scores.stream().peek {
                s -> s.user = players[s.userID]
            }
        }

        // apply sr change
        rs.forEach {
            if (it.beatMap != null) {
                val b = beatmapApiService.getBeatMapFromDataBase(it.beatMapID)

                calculateApiService.applyStarToBeatMap(b, it.mode, LazerMod.getModsList(it.mods))

                it.beatMap = b
            }
        }

        return rs.toList()
    }

    @JvmRecord data class RatingParam(
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

    @JsonIgnore
    private var playerDataMap: Map<Long, PlayerData> =
        players.map { (k, v) -> k to PlayerData(v)}.toMap()

    @get:JsonProperty("team_point_map")
    val teamPointMap: Map<String, Int>
        get() {
            val map = mutableMapOf<String, Int>()

            for (r in rounds) {
                val winner = r.winningTeam
                if (winner != null) {
                    val count = map[winner] ?: 0

                    map[winner] = count + 1
                }
            }

            return map.toMap()
        }

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
        calculateMuRating()

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

                if (data.team == r.winningTeam) {
                    rws = 1.0 * s.score / r.winningTeamScore
                    data.win += 1
                } else if (r.isTeamVS && null == r.winningTeam) {
                    rws = 1.0 * s.score / r.winningTeamScore
                } else {
                    rws = 0.0
                    data.lose += 1
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

    private fun calculateMuRating() {
        playerDataMap.values.forEach{
            it.calculateERA(minMQ, scalingFactor)
            it.calculateDRA(players.size, scores.size)
            it.calculateMRA()
        }
    }

    private fun calculateIndex() {
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

    class PlayerData(p: MicroUser) {
        val player: MicroUser = p

        var team: String? = null

        @JsonIgnore
        var scores: MutableList<Int> = mutableListOf()

        @JsonIgnore
        var roundWinShares: MutableList<Double> = mutableListOf()

        //totalScore
        var total: Long = 0L

        //标准化的单场个人得分 RRAs，即标准分 = score/TotalScore
        @JsonIgnore
        var rawRatings: MutableList<Double> = mutableListOf()

        //总得斗力点 TMG，也就是RRAs的和
        @JsonIgnore
        var totalMuPoint: Double = 0.0

        //场均标准分
        @JsonIgnore
        var averageMuPoint: Double = 0.0

        //AMG/Average(AMG) 场均标准分的相对值
        @JsonIgnore
        var normalizedMuPoint: Double = 0.0

        var era: Double = 0.0

        //(TMG*playerNumber)/参赛人次
        var dra: Double = 0.0

        //MRA = 0.7 * ERA + 0.3 * DRA
        var mra: Double = 0.0

        // SRA会算，MRA就不计算了
        /*
        var hrra: Double = 0.0
        var hdra: Double = 0.0
        var dtra: Double = 0.0
        var ezra: Double = 0.0

        @JsonIgnore
        var totalHR: Long = 0L
        @JsonIgnore
        var totalHD: Long = 0L
        @JsonIgnore
        var totalDT: Long = 0L
        @JsonIgnore
        var totalEZ: Long = 0L

         */

        //平均每局胜利分配 RWS v3.4添加
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

        //胜负场次
        var win: Int = 0

        var lose: Int = 0

        //有关联的所有场次，注意不是参加的场次 AssociatedRoundCount
        @JsonProperty("arc")
        var associatedRoundCount: Int = 0

        fun calculateTotalScore() {
            total = scores.sumOf { it.toLong() }
        }

        // 注意。Series 中，不需要再次统计 TMG。
        // 无所谓了现在
        fun calculateTMG() {
            totalMuPoint = rawRatings.sum()
        }

        fun calculateAverageScore() {
            if (rawRatings.isNotEmpty()) {
                averageMuPoint = totalMuPoint / rawRatings.size
            }
        }

        //aAMG 是所有玩家单独的 AMG 加起来的平均值
        fun calculateMQ(aAMG: Double) {
            normalizedMuPoint = averageMuPoint / aAMG
        }

        fun calculateERA(minMQ: Double, scalingFactor: Double) {
            era = (normalizedMuPoint - minMQ * scalingFactor) / (1 - minMQ * scalingFactor)
        }

        fun calculateDRA(playerCount: Int, scoreCount: Int) {
            dra = (totalMuPoint / scoreCount) * playerCount
        }

        fun calculateMRA() {
            mra = 0.7 * era + 0.3 * dra
        }

        fun calculateRWS() {
            rws = roundWinShares.average()
        }

        fun calculateClass() {
            playerClass = PlayerClass(eraIndex, draIndex, rwsIndex)
        }
    }

    companion object {
        fun MatchRating.insertMicroUserToScores() {
            this.match.events.forEach { e ->
                if (e.round != null) {
                    e.round.scores.forEach {
                        s -> s.user = this.players[s.userID]
                    }
                }
            }
        }
    }
}