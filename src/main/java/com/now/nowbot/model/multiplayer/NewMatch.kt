package com.now.nowbot.model.multiplayer

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.model.JsonData.BeatMap
import com.now.nowbot.model.JsonData.MicroUser
import com.now.nowbot.model.JsonData.Score
import java.time.OffsetDateTime

data class NewMatch(
    @JsonProperty("match")
    val state: MatchStat,

    val events: MutableList<MatchEvent>,

    val users: MutableList<MicroUser>,

    @JsonProperty("first_event_id")
    val firstEventID: Long,

    @JsonProperty("latest_event_id")
    val latestEventID: Long,
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
        private val modeInt: Int,
        val mods: List<String>,
        val scores: List<Score>,

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
}