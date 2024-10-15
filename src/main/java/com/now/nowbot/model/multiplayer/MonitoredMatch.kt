package com.now.nowbot.model.multiplayer

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.model.enums.OsuMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.Match
import com.now.nowbot.model.json.MicroUser
import com.now.nowbot.model.json.Statistics
import java.time.OffsetDateTime

data class MonitoredMatch(
        @JsonProperty("match") var statistics: MatchStat,
        val events: MutableList<MatchEvent>,
        val users: MutableList<MicroUser>,
        @JsonProperty("first_event_id") var firstEventID: Long,
        @JsonProperty("latest_event_id") var latestEventID: Long,
) {
    val isMatchEnd: Boolean
        get() = statistics.endTime != null

    val currentGameID: Long?
        get() = events.lastOrNull { it.game != null }?.game?.gameID

    val name by statistics::name

    val id by statistics::matchID

    val startTime by statistics::startTime

    val endTime by statistics::endTime

    data class MatchStat(
            @JsonProperty("id") val matchID: Long,
            @JsonProperty("start_time") val startTime: OffsetDateTime,
            @JsonProperty("end_time") val endTime: OffsetDateTime?,
            val name: String,
    )

    data class MatchEvent(
            @JsonProperty("id") val eventID: Long,
            @JsonProperty("detail") private val detailObj: JsonNode?,
            @JsonProperty("timestamp") val timestamp: OffsetDateTime,
            @JsonProperty("user_id") val userID: Long?,
            @JsonProperty("game") val game: MatchGame?,
    ) {
        val type: EventType
            get() = EventType.getType(detailObj?.get("type")?.asText() ?: "")

        val detail: String
            get() = detailObj?.get("text")?.asText() ?: ""
    }

    data class MatchGame(
            @JsonProperty("id") val gameID: Long,
            var beatmap: BeatMap?,
            @JsonProperty("beatmap_id") val beatmapID: Long,
            @JsonProperty("start_time") val startTime: OffsetDateTime,
            @JsonProperty("end_time") val endTime: OffsetDateTime?,
            val modeInt: Int,
            val mods: List<String>,
            var scores: List<MatchScore>,
            val teamType: String,
            val scoringType: String,
    ) {
        val mode: OsuMode
            get() = OsuMode.getMode(modeInt)

        val isTeamVS: Boolean
            get() = teamType == "team-vs" || teamType == "tag-team-vs"
    }

    data class MatchScore(
            @JsonProperty("match") val playerStat: MatchScorePlayerStat,
            @JsonProperty("best_id") val bestID: Long,
            @JsonProperty("user_id") val userID: Long,
            @JsonProperty("id") val scoreID: Long,
            val accuracy: Double?,
            val maxCombo: Int,
            val mode: String,
            val modeInt: Int,
            val mods: List<String>,
            val passed: Boolean,
            val perfect: Boolean,
            val replay: Boolean,
            val pp: Double,
            val rank: String,
            val score: Int,
            val statistics: Statistics,
            val type: String,
    ) {
        // 自己设
        var user: MicroUser = MicroUser()
    }

    data class MatchScorePlayerStat(val slot: Int, val team: String, val pass: Boolean)

    enum class EventType(val value: String) {
        PlayerJoined("player-joined"),
        PlayerKicked("player-kicked"),
        PlayerLeft("player-left"),
        Other("other"),
        HostChanged("host-changed"),
        MatchDisbanded("match-disbanded"),
        MatchCreated("match-created");

        companion object {
            fun getType(value: String) =
                    when (value) {
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

    fun toMatch(): Match {
        val match = Match()
        match.players = this.users
        match.currentGameID = this.currentGameID
        match.firstEventID = this.firstEventID
        match.latestEventID = this.latestEventID

        val t = Match.MatchStat()
        t.matchID = this.id
        t.name = this.name
        t.startTime = this.startTime
        t.endTime = this.endTime
        match.matchStat = t

        val es = mutableListOf<Match.MatchEvent>()

        for (me in this.events) {
            val e = Match.MatchEvent()

            val r = Match.MatchRound()

            if (me.game != null) {
                val g = me.game

                val ss = mutableListOf<Match.MatchScore>()

                for (ms in g.scores) {
                    val s = Match.MatchScore()

                    val mt = ms.playerStat
                    val pt = Match.MatchScore.PlayerStat(mt.slot, mt.team, mt.pass)
                    s.playerStat = pt

                    s.pp = ms.pp
                    s.score = ms.score
                    s.rank = ms.rank
                    s.bestID = ms.bestID
                    s.userID = ms.userID
                    s.scoreID = ms.scoreID
                    s.passed = ms.passed
                    s.accuracy = ms.accuracy
                    s.maxCombo = ms.maxCombo
                    s.mode = ms.mode
                    s.modeInt = ms.modeInt
                    s.mods = ms.mods.toTypedArray()
                    s.perfect = ms.perfect
                    // s.ranking = null
                    s.rank = ms.rank
                    s.replay = ms.replay
                    s.statistics = ms.statistics
                    // s.timestamp = ms.time
                    s.type = ms.type

                    ss.add(s)
                }
                r.scores = ss
                r.mods = g.mods
                r.mode = g.mode.name
                r.startTime = g.startTime
                r.endTime = g.endTime
                r.beatMap = g.beatmap
                r.beatMapID = g.beatmapID
                r.modInt = OsuMod.getModsValueFromAbbrList(g.mods)
                r.scoringType = g.scoringType
                r.teamType = g.teamType
                r.roundID = g.gameID.toInt()

                run {
                    val isVs = (g.teamType == "team-vs")

                    val total = g.scores.map(MatchScore::score).filter { it > 10000 }.stream().reduce(0) { a, b -> a + b }.toLong()

                    val red =
                            if (isVs) {
                                g.scores.stream()
                                    .filter { it.playerStat.team == "red" }
                                    .map(MatchScore::score)
                                    .filter { it > 10000 }
                                    .reduce(0) { a, b -> a + b }.toLong()
                            } else {
                                0L
                            }

                    val blue =
                            if (isVs) {
                                g.scores
                                    .filter { it.playerStat.team == "blue" }
                                    .map(MatchScore::score)
                                    .filter { it > 10000 }
                                    .stream().reduce(0) { a, b -> a + b }.toLong()
                            } else {
                                0L
                            }

                    val isRedWin = isVs && red > blue
                    val isBlueWin = isVs && red < blue

                    r.teamScore = Match.MatchRound.TeamScore(total, red, blue)
                    r.winningTeam =
                            if (isBlueWin) "blue"
                            else if (isRedWin) "red" else if (isVs) null else "none"
                    r.winningTeamScore =
                            if (isBlueWin) blue
                            else if (isRedWin) red
                            else if (isVs) blue
                            else {
                                (g.scores.maxOfOrNull(MatchScore::score) ?: 0).toLong()
                            }
                }
            }
            e.userID = me.userID
            e.eventID = me.eventID
            e.round = r
            e.timestamp = me.timestamp

            e.detail = Match.MatchEvent.Detail(me.type.name, me.detail)

            es.add(e)
        }

        match.events = es

        return match
    }

    operator fun plusAssign(match: MonitoredMatch) {
        // 更新玩家
        if (match.users.isNotEmpty()) {
            val userSet = users.map { it.userID }.toSet()
            val newUsers = match.users.filter { it.userID in userSet }
            users.addAll(newUsers)
        }
        // 更新状态
        statistics = match.statistics
        latestEventID = match.latestEventID
        firstEventID = match.firstEventID

        if (match.events.isEmpty()) return
        when {
            // 插入新事件
            events.last().eventID < match.events.first().eventID -> events.addAll(match.events)
            // 插入旧事件
            events.first().eventID > match.events.last().eventID -> events.addAll(0, match.events)
            // 中间插入
            events.last().eventID < match.events.last().eventID -> {
                events.removeIf { it.eventID >= match.events.first().eventID }
                events.addAll(match.events)
            }
            // 中间插入
            events.first().eventID > match.events.first().eventID -> {
                events.removeIf { it.eventID <= match.events.last().eventID }
                events.addAll(0, match.events)
            }
        }
    }
}
