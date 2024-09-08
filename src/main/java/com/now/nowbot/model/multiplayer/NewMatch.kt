package com.now.nowbot.model.multiplayer

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.model.JsonData.BeatMap
import com.now.nowbot.model.JsonData.MicroUser
import com.now.nowbot.model.JsonData.Statistics
import com.now.nowbot.model.enums.OsuMode
import java.time.OffsetDateTime

data class NewMatch(
    @JsonProperty("match")
    var state: MatchStat,

    val events: MutableList<MatchEvent>,

    val users: MutableList<MicroUser>,

    @JsonProperty("first_event_id")
    var firstEventID: Long,

    @JsonProperty("latest_event_id")
    var latestEventID: Long,
) {
    val isMatchEnd: Boolean
        get() = state.endTime != null

    val currentGameID: Long?
        get() = events.lastOrNull { it.game != null }?.game?.id

    val name by state::name

    val ID by state::ID

    val startTime by state::startTime

    val endTime by state::endTime

    data class MatchStat(
        @JsonProperty("id")
        val ID: Long,

        @JsonProperty("start_time")
        val startTime: OffsetDateTime,

        @JsonProperty("end_time")
        val endTime: OffsetDateTime?,

        val name: String,
    )

    data class MatchEvent(
        @JsonProperty("id")
        val ID: Long,

        @JsonProperty("detail")
        private val detailObj: JsonNode,

        @JsonProperty("timestamp")
        val timestamp: OffsetDateTime,

        @JsonProperty("user_id")
        val userID: Long?,

        @JsonProperty("game")
        val game: MatchGame?,
    ) {
        val type: EventType
                get() = EventType.getType(detailObj["type"].asText())

        val detail: String
            get() = detailObj["text"].asText()


    }

    data class MatchGame(
        val id: Long,
        val beatmap: BeatMap,

        val beatmapID: Long,
        val startTime: OffsetDateTime,
        val endTime: OffsetDateTime?,
        val modeInt: Int,
        val mods: List<String>,
        val scores: List<MatchScore>,
        val teamType: String,
    ) {
        val mode: OsuMode
            get() = OsuMode.getMode(modeInt)
        val isTeamVS = teamType.contains("team-vs")
    }

    data class MatchScore(
        @JsonProperty("match")
        val playerStat: MatchScorePlayerStat,
        @JsonProperty("best_id")
        val bestID: Long,
        @JsonProperty("user_id")
        val userID: Long,
        @JsonProperty("id")
        val scoreID: Long,
        val maxCombo: Int,
        val mode: String,
        val modeInt: Int,
        val mods: List<String>,
        val passed: Boolean,
        val perfect: Boolean,
        val pp: Double,
        val rank: String,
        val score: Int,
        val statistics: Statistics,
    )

    data class MatchScorePlayerStat(
        val slot: Int,
        val team: String,
        val pass: Boolean,
    )

    enum class EventType(val value: String) {
        PlayerJoined("player-joined"),
        PlayerKicked("player-kicked"),
        PlayerLeft("player-left"),
        Other("other"),
        HostChanged("host-changed"),
        MatchDisbanded("match-disbanded"),
        MatchCreated("match-created"),
        ;

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

    operator fun plusAssign(match: NewMatch) {
        // 更新玩家
        if (match.users.isNotEmpty()) {
            val userSet = users.map { it.userID }.toSet()
            val newUsers = match.users.filter { it.userID in userSet }
            users.addAll(newUsers)
        }
        //更新状态
        state = match.state
        latestEventID = match.latestEventID
        firstEventID = match.firstEventID

        if (match.events.isEmpty()) return
        when {
            // 插入新事件
            events.last().ID < match.events.first().ID -> events.addAll(match.events)
            // 插入旧事件
            events.first().ID > match.events.last().ID -> events.addAll(0, match.events)
            // 中间插入
            events.last().ID < match.events.last().ID -> {
                events.removeIf { it.ID >= match.events.first().ID }
                events.addAll(match.events)
            }
            // 中间插入
            events.first().ID > match.events.first().ID -> {
                events.removeIf { it.ID <= match.events.last().ID }
                events.addAll(0, match.events)
            }
        }
    }
}