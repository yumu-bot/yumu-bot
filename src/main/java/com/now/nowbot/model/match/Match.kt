package com.now.nowbot.model.match

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.MicroUser
import java.time.OffsetDateTime
import kotlin.math.max

data class Match(
    @field:JsonProperty("match") var statistics: MatchStat,
    @field:JsonProperty("events") var events: List<MatchEvent>,
    @field:JsonProperty("users") var players: List<MicroUser>,
    @field:JsonProperty("first_event_id") var firstEventID: Long,
    @field:JsonProperty("latest_event_id") var latestEventID: Long,
) {
    @get:JsonProperty("is_match_end")
    val isMatchEnd: Boolean
        get() = statistics.endTime != null

    @get:JsonProperty("current_game_id")
    val currentGameID: Long? by lazy {
        val e = events.lastOrNull { it.round != null } ?: return@lazy null
        if (e.round?.endTime == null) return@lazy e.round?.roundID
        return@lazy null
    }

    val name by statistics::name

    val id by statistics::matchID

    val startTime by statistics::startTime

    val endTime by statistics::endTime

    data class MatchStat(
        @field:JsonProperty("id") val matchID: Long,
        @field:JsonProperty("start_time") val startTime: OffsetDateTime,
        @field:JsonProperty("end_time") val endTime: OffsetDateTime?,
        var name: String,
    )

    data class MatchEvent(
        @field:JsonProperty("id") val eventID: Long,
        @field:JsonProperty("detail") val detail: MatchEventDetail,
        @field:JsonProperty("timestamp") val timestamp: OffsetDateTime,
        @field:JsonProperty("user_id") val userID: Long?,
        @field:JsonProperty("game") val round: MatchRound?,
    ) {
        val type: EventType
            get() = EventType.getType(detail.type)

        val text: String
            get() = detail.text ?: ""
    }

    data class MatchEventDetail(
        @field:JsonProperty("type") val type: String,
        @field:JsonProperty("text") val text: String?,
    )

    data class MatchRound(
        @field:JsonProperty("id") val roundID: Long,
        @field:JsonProperty("beatmap") var beatmap: Beatmap?,
        @field:JsonProperty("beatmap_id") val beatmapID: Long,
        @field:JsonProperty("start_time") val startTime: OffsetDateTime,
        @field:JsonProperty("end_time") val endTime: OffsetDateTime?,
        val modeInt: Int,
        val mods: List<String>,
        var scores: List<LazerScore>,
        val teamType: String,
        val scoringType: String,
    ) {
        val mode: OsuMode
            get() = OsuMode.getMode(modeInt)

        @get:JsonProperty("is_team_vs")
        val isTeamVS: Boolean
            get() = teamType == "team-vs" || teamType == "tag-team-vs"

        // 预计算得分，避免多次访问时重复 filter + sum
        private val teamScores by lazy {
            scores.groupBy { it.playerStat?.team }
                .mapValues { (_, scoreList) -> scoreList.sumOf { it.score } }
        }

        @get:JsonProperty("red_team_score")
        val redTeamScore: Long
            get() = teamScores["red"] ?: 0L

        @get:JsonProperty("blue_team_score")
        val blueTeamScore: Long
            get() = teamScores["blue"] ?: 0L

        @get:JsonProperty("total_team_score")
        val totalTeamScore: Long
            get() = scores.sumOf { it.score }

        @get:JsonIgnore
        val maxScore: Long
            get() = scores.maxOf { it.score }

        @get:JsonProperty("winning_team")
        val winningTeam: String?
            get() =
                if (isTeamVS) {
                    val red = getTeamScore("red")
                    val blue = getTeamScore("blue")

                    if (red > blue) "red"
                    else if (red < blue) "blue"
                    else null
                } else {
                    "none"
                }

        @get:JsonProperty("winning_team_score")
        val winningTeamScore: Long
            get() =
                if (isTeamVS) {
                    max(redTeamScore, blueTeamScore)
                } else {
                    totalTeamScore
                }

        private fun getTeamScore(teamType: String? = null): Long {
            return when(teamType) {
                "red" -> scores
                    .filter { it.playerStat?.team == "red" }
                    .sumOf { it.score }
                "blue" -> scores
                    .filter { it.playerStat?.team == "blue" }
                    .sumOf { it.score }
                else -> scores.sumOf { it.score }
            }
        }
    }

    enum class EventType(val value: String) {
        PlayerJoined("player-joined"),
        PlayerKicked("player-kicked"),
        PlayerLeft("player-left"),
        Other("other"),
        HostChanged("host-changed"),
        MatchDisbanded("match-disbanded"),
        MatchCreated("match-created");

        companion object {
            fun getType(value: String) = when (value) {
                PlayerJoined.value -> PlayerJoined
                PlayerKicked.value -> PlayerKicked
                PlayerLeft.value -> PlayerLeft
                HostChanged.value -> HostChanged
                MatchDisbanded.value -> MatchDisbanded
                MatchCreated.value -> MatchCreated
                else -> Other
            }
        }
    }

    // 扩展函数风格：可以直接通过 matchA.append(matchB) 调用
    fun append(m: Match) {
        if (m.events.isEmpty()) return
        this.events = (this.events + m.events).distinctBy { it.eventID }.sortedBy { it.eventID }

        this.players = (this.players + m.players).distinctBy { it.userID }

        this.statistics = m.statistics
        this.latestEventID = m.latestEventID
        this.firstEventID = this.events.firstOrNull()?.eventID ?: m.firstEventID
    }
}
