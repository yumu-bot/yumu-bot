package com.now.nowbot.model.multiplayer

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.MicroUser
import com.now.nowbot.model.json.Statistics
import org.springframework.util.CollectionUtils
import java.time.OffsetDateTime
import kotlin.math.max

data class Match(
    @JsonProperty("match") var statistics: MatchStat,
    val events: MutableList<MatchEvent>,
    @JsonProperty("users")
    var players: MutableList<MicroUser>,
    @JsonProperty("first_event_id") var firstEventID: Long,
    @JsonProperty("latest_event_id") var latestEventID: Long,
) {
    @get:JsonProperty("is_match_end")
    val isMatchEnd: Boolean
        get() = statistics.endTime != null

    @get:JsonProperty("current_game_id")
    val currentGameID: Long?
        get() = events.lastOrNull { it.round != null }?.round?.roundID

    val name by statistics::name

    val id by statistics::matchID

    val startTime by statistics::startTime

    val endTime by statistics::endTime

    data class MatchStat(
        @JsonProperty("id") val matchID: Long,
        @JsonProperty("start_time") val startTime: OffsetDateTime,
        @JsonProperty("end_time") val endTime: OffsetDateTime?,
        var name: String,
    )

    data class MatchEvent(
        @JsonProperty("id") val eventID: Long,
        @JsonProperty("detail") val detail: MatchEventDetail,
        @JsonProperty("timestamp") val timestamp: OffsetDateTime,
        @JsonProperty("user_id") val userID: Long?,
        @JsonProperty("game") val round: MatchRound?,
    ) {
        val type: EventType
            get() = EventType.getType(detail.type)

        val text: String
            get() = detail.text ?: ""
    }

    data class MatchEventDetail(
        @JsonProperty("type") val type: String,
        @JsonProperty("text") val text: String?,
    )

    data class MatchRound(
        @JsonProperty("id") val roundID: Long,
        @JsonProperty("beatmap") var beatMap: BeatMap?,
        @JsonProperty("beatmap_id") val beatMapID: Long,
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

        @get:JsonProperty("is_team_vs")
        val isTeamVS: Boolean
            get() = teamType == "team-vs" || teamType == "tag-team-vs"

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

        val winningTeamScore: Long
            get() =
                if (isTeamVS) {
                    max(getTeamScore("red"), getTeamScore("blue"))
                } else {
                    getTeamScore()
                }


        private fun getTeamScore(teamType: String? = null): Long {
            return when(teamType) {
                "red" -> scores
                    .filter { it.playerStat.team == "red" }
                    .sumOf { it.score.toLong() }
                "blue" -> scores
                    .filter { it.playerStat.team == "blue" }
                    .sumOf { it.score.toLong() }
                else -> scores.sumOf { it.score.toLong() }
            }
        }
    }

    data class MatchScore(
        @JsonProperty("match") val playerStat: MatchScorePlayerStat,
        @JsonProperty("best_id") val bestID: Long,
        @JsonProperty("user_id") val userID: Long,
        @JsonProperty("id") val scoreID: Long,
        @JsonProperty("accuracy") val accuracy: Double?,
        @JsonProperty("max_combo") val maxCombo: Int,
        @JsonProperty("mode") val mode: String,
        @JsonProperty("mode_int") val modeInt: Int,
        val mods: List<String>,
        val passed: Boolean,
        val perfect: Boolean,
        val replay: Boolean,
        val pp: Double,
        val rank: String,
        var score: Int,
        val statistics: Statistics,
        val type: String,
    ) {
        // 自己设
        var user: MicroUser? = null
        var ranking: Int? = null
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

    /*
    fun toMatch(): LegacyMatch {
        val match = LegacyMatch()
        match.players = this.players
        match.currentGameID = this.currentGameID
        match.firstEventID = this.firstEventID
        match.latestEventID = this.latestEventID

        val t = LegacyMatch.MatchStat()
        t.matchID = this.id
        t.name = this.name
        t.startTime = this.startTime
        t.endTime = this.endTime
        match.matchStat = t

        val es = mutableListOf<LegacyMatch.MatchEvent>()

        for (me in this.events) {
            val e = LegacyMatch.MatchEvent()

            val r = LegacyMatch.MatchRound()

            if (me.round != null) {
                val g = me.round

                val ss = mutableListOf<LegacyMatch.MatchScore>()

                for (ms in g.scores) {
                    val s = LegacyMatch.MatchScore()

                    val mt = ms.playerStat
                    val pt = LegacyMatch.MatchScore.PlayerStat(mt.slot, mt.team, mt.pass)
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
                    s.perfect = ms.perfect // s.ranking = null
                    s.rank = ms.rank
                    s.replay = ms.replay
                    s.statistics = ms.statistics // s.timestamp = ms.time
                    s.type = ms.type

                    ss.add(s)
                }
                r.scores = ss
                r.mods = g.mods
                r.mode = g.mode.name
                r.startTime = g.startTime
                r.endTime = g.endTime
                r.beatMap = g.beatMap
                r.beatMapID = g.beatMapID
                r.modInt = OsuMod.getModsValueFromAcronyms(g.mods)
                r.scoringType = g.scoringType
                r.teamType = g.teamType
                r.roundID = g.roundID.toInt()

                run {
                    val isVs = (g.teamType == "team-vs")

                    val total =
                        g.scores.map(MatchScore::score).filter { it > 10000 }.stream().reduce(0) { a, b -> a + b }
                            .toLong()

                    val red = if (isVs) {
                        g.scores.stream().filter { it.playerStat.team == "red" }.map(MatchScore::score)
                            .filter { it > 10000 }.reduce(0) { a, b -> a + b }.toLong()
                    } else {
                        0L
                    }

                    val blue = if (isVs) {
                        g.scores.filter { it.playerStat.team == "blue" }.map(MatchScore::score).filter { it > 10000 }
                            .stream().reduce(0) { a, b -> a + b }.toLong()
                    } else {
                        0L
                    }

                    val isRedWin = isVs && red > blue
                    val isBlueWin = isVs && red < blue

                    r.teamScore = LegacyMatch.MatchRound.TeamScore(total, red, blue)
                    r.winningTeam = if (isBlueWin) "blue"
                    else if (isRedWin) "red" else if (isVs) null else "none"
                    r.winningTeamScore = if (isBlueWin) blue
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

            e.detail = LegacyMatch.MatchEvent.Detail(me.detail.type, me.detail.text)

            es.add(e)
        }

        match.events = es

        return match
    }

     */

    operator fun plusAssign(match: Match) {
        // 更新玩家
        if (match.players.isNotEmpty()) {
            val userSet = players.map { it.userID }.toSet()
            val newUsers = match.players.filter { it.userID in userSet }
            players.addAll(newUsers)
        }

        // 更新状态
        statistics = match.statistics
        latestEventID = match.latestEventID
        firstEventID = match.firstEventID

        if (match.events.isEmpty()) return
        when { // 插入新事件
            events.last().eventID < match.events.first().eventID -> events.addAll(match.events) // 插入旧事件
            events.first().eventID > match.events.last().eventID -> events.addAll(0, match.events) // 中间插入
            events.last().eventID < match.events.last().eventID -> {
                events.removeIf { it.eventID >= match.events.first().eventID }
                events.addAll(match.events)
            } // 中间插入
            events.first().eventID > match.events.first().eventID -> {
                events.removeIf { it.eventID <= match.events.last().eventID }
                events.addAll(0, match.events)
            }
        }
    }

    companion object {
        fun Match.append(m: Match) {
            if (m.events.isEmpty()) return
            // 合并事件

            if (this.events.first().eventID > m.events.last().eventID) {
                // 新事件在前
                this.events.addAll(0, m.events)
            } else if (events.last().eventID < m.events.first().eventID) {
                // 新事件在后
                this.events.addAll(m.events)
            } else if (events.first().eventID > m.events.first().eventID) {
                // 在中间
                events.removeIf { e: MatchEvent -> e.eventID <= m.events.last().eventID }
                this.events.addAll(0, m.events)
            } else if (events.last().eventID < m.events.last().eventID) {
                // 在中间
                events.removeIf { e: MatchEvent -> e.eventID >= m.events.first().eventID }
                this.events.addAll(m.events)
            }

            if (CollectionUtils.isEmpty(this.players)) {
                this.players = m.players
            } else {
                val p = ArrayList<MicroUser>(this.players.size)

                p.addAll(this.players)
                p.addAll(m.players)

                this.players = p.stream().distinct().toList()
            }

            //更新状态
            this.statistics = m.statistics
            this.latestEventID = m.latestEventID
            this.firstEventID = m.firstEventID
        }
    }
}