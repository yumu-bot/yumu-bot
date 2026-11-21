package com.now.nowbot.model.match

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.osu.LazerMod.Companion.toLazerMods
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.util.BeatmapUtil
import org.slf4j.LoggerFactory
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

/**
 * 一场对局的评分面板数据。
 *
 * 这个类现在只做两件事：
 * 1. 持有对局过滤后的 round / score / player 视图（由 [MatchRatingPreparer] 负责准备）；
 * 2. 把计算委托给 [RatingCalculator]，暴露面板需要的 JSON 字段。
 *
 * beatmapApiService / calculateApiService 仍然是构造参数，因为准备阶段需要直接批量取谱面、
 * 批量算星数和挂别名；两者由 [MatchRatingPreparer] 直接使用，不再散落在 companion 扩展里。
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
class MatchRating(
    val match: Match,
    private val ratingParam: RatingParam,
    beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,

    @field:JsonProperty("is_skipping")
    val skipping: Boolean = false
) {
    @JsonIgnore
    private val prepared: PreparedMatch =
        MatchRatingPreparer(beatmapApiService, calculateApiService).prepare(match, ratingParam)

    @get:JsonIgnore
    val rounds: List<Match.MatchRound> = prepared.rounds

    @get:JsonIgnore
    val scores: List<LazerScore> = prepared.scores

    @get:JsonIgnore
    val players: Map<Long, MicroUser> = prepared.players

    @get:JsonProperty("round_count")
    val roundCount: Int = rounds.size

    @get:JsonProperty("score_count")
    val scoreCount: Int = scores.size

    @get:JsonProperty("player_count")
    val playerCount: Int = players.size

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

    @get:JsonProperty("skip_ignore_map")
    val skipIgnoreMap: Map<String, Number>
        get() = mapOf(
            "skip" to ratingParam.skip,
            "ignore" to ratingParam.ignore,
            "easy" to ratingParam.easy
        )

    /**
     * 每个胜方（非 team-vs 为 "none"）拿到的 round 数。由 calculate() 一次性写入。
     */
    @get:JsonProperty("team_point_map")
    var teamPointMap: Map<String, Int> = emptyMap()
        private set

    @JsonProperty("player_data_list")
    var playerDataList: List<PlayerData> = emptyList()

    /**
     * 以「谱面 + 计分方式 + 地图池 mod」为 key 的统计表。
     *
     * scorev2 使用 averageScore 计算 RRA；score / accuracy / combo 使用 guidanceScore。
     */
    @get:JsonIgnore
    var beatmapScoreStats: Map<BeatmapScoreKey, BeatmapScoreStat> = emptyMap()
        private set

    /**
     * 主计算。可以重复调用，每次都会基于当前 rounds 重新生成玩家数据。
     */
    fun calculate() {
        val result = RatingCalculator(
            rounds = rounds,
            players = players,
            associatedRoundCounts = players.keys.associateWith { rounds.size },
        ).calculate()

        playerDataList = result.playerDataList
        beatmapScoreStats = result.beatmapScoreStats
        teamPointMap = result.teamPointMap
    }

    /**
     * 给所有成绩挂上玩家。对局面板会直接拿 score.user 渲染。
     */
    fun insertMicroUserToScores() {
        match.events
            .asSequence()
            .mapNotNull { it.round }
            .flatMap { it.scores.asSequence() }
            .forEach { score -> score.user = players[score.userID] ?: MicroUser() }
    }

    /**
     * 应用 DT 等 mod 对谱面四维的改动，并补齐尚未拿到星数的谱面。
     *
     * 星数在构造时已经批量算过，这里只处理遗漏的谱面，避免重复一次全量计算。
     */
    fun applyDTMod() {
        val requests = ArrayList<OsuCalculateApiService.BeatmapStarRequest>(rounds.size)

        for (round in rounds) {
            val beatmap = round.beatmap ?: continue
            val mods = round.mods.toLazerMods()

            BeatmapUtil.applyBeatmapChanges(beatmap, mods)

            if (beatmap.starRating <= 1e-4) {
                runCatching { round.mode }.getOrNull()?.let { mode ->
                    requests.add(OsuCalculateApiService.BeatmapStarRequest(beatmap, mode, mods))
                }
            }
        }

        if (requests.isNotEmpty()) {
            runCatching { calculateApiService.applyStarToBeatmaps(requests) }
                .onFailure { e -> log.warn("批量补齐 {} 张谱面的星数失败", requests.size, e) }
        }
    }

    data class RatingParam(
        val skip: Int = 0,
        val ignore: Int = 0,
        val remove: List<Int>? = null,
        val easy: Double = 1.0,
        val delete: Boolean = true,
        val rematch: Boolean = true,
    )

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

        @get:JsonProperty("player_class")
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

        @get:JsonProperty("arc")
        var associatedRoundCount: Int = 0
    }

    companion object {
        private val log = LoggerFactory.getLogger(MatchRating::class.java)
    }
}
