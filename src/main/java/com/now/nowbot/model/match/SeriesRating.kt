package com.now.nowbot.model.match


import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.match.MatchRating.PlayerData
import com.now.nowbot.model.match.MatchRating.RatingParam
import com.now.nowbot.model.osu.LazerMod.Companion.toLazerMods
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.util.BeatmapUtil
import org.slf4j.LoggerFactory
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import java.time.OffsetDateTime

/**
 * 系列赛评分面板数据。
 *
 * 结构和 [MatchRating] 一一对应，同样只做两件事：
 * 1. 持有对局过滤后的 round / score / player 视图（由 [SeriesRatingPreparer] 负责准备）；
 * 2. 把计算委托给 [RatingCalculator]，暴露面板需要的 JSON 字段。
 *
 * 和 [MatchRating] 的唯一区别是入参：这里接受多场比赛，以及每场各自的一份 [RatingParam]
 * （skip / ignore / remove 等按场生效）。所有比赛会合并成一个计算范围，
 * 因此系列赛里的 RRA 也是按谱面汇总的平均分计算的，而不是每局各算各的。
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
class SeriesRating(
    val matches: List<Match>,
    ratingParams: List<RatingParam>,
    beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService
) {
    @JsonIgnore
    private val prepared: PreparedSeries =
        SeriesRatingPreparer(beatmapApiService, calculateApiService).prepare(matches, ratingParams)

    @get:JsonIgnore
    val rounds: List<Match.MatchRound> = prepared.rounds

    @get:JsonIgnore
    val scores: List<LazerScore> = prepared.scores

    @get:JsonIgnore
    val players: Map<Long, MicroUser> = prepared.players

    @get:JsonProperty("match_count")
    val matchCount: Int = matches.size

    @get:JsonProperty("round_count")
    val roundCount: Int = rounds.size

    @get:JsonProperty("score_count")
    val scoreCount: Int = scores.size

    @get:JsonProperty("player_count")
    val playerCount: Int = players.size

    @get:JsonProperty("is_team_vs")
    val isTeamVs: Boolean
        get() = rounds.firstOrNull()?.isTeamVS
            ?: matches.lastOrNull()?.events?.mapNotNull { it.round }?.lastOrNull()?.isTeamVS
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

    @JsonIgnore
    var name: String? = null

    /** 以前的 SeriesStat：系列赛整体起止时间与展示名，由最早和最晚的一场比赛推出来。 */
    val statistics: Match.MatchStat
        get() {
            val firstMatch = matches.minByOrNull { it.startTime.toEpochSecond() }
                ?: return Match.MatchStat(0L, OffsetDateTime.MIN, null, name ?: "")

            val startTime = firstMatch.startTime
            val endTime = matches.mapNotNull { it.endTime }.maxByOrNull { it.toEpochSecond() }
            val matchName = name ?: (firstMatch.name + "...")

            return Match.MatchStat(firstMatch.id, startTime, endTime, matchName)
        }

    /**
     * 主计算。可以重复调用，每次都会基于当前 rounds 重新生成玩家数据。
     */
    fun calculate() {
        val result = RatingCalculator(
            rounds = rounds,
            players = players,
            associatedRoundCounts = prepared.associatedRoundCounts,
        ).calculate()

        playerDataList = result.playerDataList
        beatmapScoreStats = result.beatmapScoreStats
        teamPointMap = result.teamPointMap
    }

    /**
     * 给所有成绩挂上玩家。系列赛面板会直接拿 score.user 渲染。
     */
    fun insertMicroUserToScores() {
        matches
            .asSequence()
            .flatMap { it.events.asSequence() }
            .mapNotNull { it.round }
            .flatMap { it.scores.asSequence() }
            .forEach { score -> score.user = players[score.userID] ?: MicroUser() }
    }

    /**
     * 应用 DT 等 mod 对谱面四维的改动，并补齐尚未拿到星数的谱面。
     *
     * 星数在准备阶段已经批量算过，这里只处理遗漏的谱面，避免重复一次全量计算。
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

    companion object {
        private val log = LoggerFactory.getLogger(SeriesRating::class.java)
    }
}
