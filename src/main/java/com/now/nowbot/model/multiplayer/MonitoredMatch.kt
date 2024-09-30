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
        private val detailObj: JsonNode?,

        @JsonProperty("timestamp")
        val timestamp: OffsetDateTime,

        @JsonProperty("user_id")
        val userID: Long?,

        @JsonProperty("game")
        val game: MatchGame?,
    ) {
        val type: EventType
            get() = EventType.getType(detailObj?.get("type")?.asText() ?: "")

        val detail: String
            get() = detailObj?.get("text")?.asText() ?: ""


    }

    data class MatchGame(
        val id: Long,
        var beatmap: BeatMap?,

        val beatmapID: Long,
        val startTime: OffsetDateTime,
        val endTime: OffsetDateTime?,
        val modeInt: Int,
        val mods: List<String>,
        var scores: List<MatchScore>,
        val teamType: String,
        val scoringType: String,
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
        val type: String
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

    fun toMatch() : Match {
        val match = Match()
        match.players = this.users
        match.currentGameID = this.currentGameID
        match.firstEventID = this.firstEventID
        match.latestEventID = this.latestEventID

        val t = Match.MatchStat()
        t.matchID = this.ID
        t.name = this.name
        t.startTime = this.startTime
        t.endTime = this.endTime
        match.matchStat = t

        val es = mutableListOf<Match.MatchEvent>()

        for(me in this.events) {
            val e = Match.MatchEvent()

            val r = Match.MatchRound()

            if (me.game != null) {
                val g = me.game

                val ss = mutableListOf<Match.MatchScore>()

                for(ms in g.scores) {
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
                    //s.timestamp = ms.time
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
                r.roundID = g.id.toInt()

                run {
                    val isVs = (g.teamType == "team-vs")
                    val total = if (g.scores.isNotEmpty()) {
                        g.scores.map(MatchScore::score).filter { it > 10000 }
                            .reduce { a, b -> a + b }.toLong()
                    } else {
                        0L
                    }

                    val red = if (isVs && g.scores.isNotEmpty()) {
                        g.scores.filter { it.playerStat.team == "red" }
                            .map(MatchScore::score).filter { it > 10000 }
                            .reduce { a, b -> a + b }.toLong()
                    } else {
                        0L
                    }

                    val blue = if (isVs && g.scores.isNotEmpty()) {
                        g.scores.filter { it.playerStat.team == "blue" }
                            .map(MatchScore::score).filter { it > 10000 }
                            .reduce { a, b -> a + b }.toLong()
                    } else {
                        0L
                    }

                    val isRedWin = isVs && red > blue
                    val isBlueWin = isVs && red < blue

                    r.teamScore = Match.MatchRound.TeamScore(total, red, blue)
                    r.winningTeam = if (isBlueWin) "blue" else if (isRedWin) "red" else if (isVs) null else "none"
                    r.winningTeamScore = if (isBlueWin) blue else if (isRedWin) red else if (isVs) blue else {
                        (g.scores.maxOfOrNull(MatchScore::score)?: 0).toLong()
                    }
                }

            }
            e.userID = me.userID
            e.eventID = me.ID
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