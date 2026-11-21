package com.now.nowbot.util

import com.now.nowbot.model.match.Match
import com.now.nowbot.model.multiplayer.Room
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.MicroUser

/**
 * 旧版 match 数据转换工具。
 *
 * 新版 multiplayer 接口（Room / RoomInfo / Playlist）的数据结构和旧版 Match 不同，
 * 这里负责把新版房间数据转换成旧版逻辑（MatchRating、面板渲染等）可以直接使用的 Match。
 */
object MatchUtil {

    /**
     * 将新版 multiplayer 的 [Room] 转换成旧版 [Match]。
     *
     * - [Room.roomInfo] 转换成 [Match.MatchStat]
     * - [Room.items]（Playlist）转换成 [Match.MatchEvent] + [Match.MatchRound]
     * - [Room.events] 用于确定实际进行过的 playlist 轮次
     */
    fun Room.toMatch(): Match {
        val room = this

        val roundIDs = room.events.mapNotNull { it.itemID }.toSet()
        val itemMap = room.items.associateBy { i -> i.listID }
        val beatmaps = room.beatmaps.associateBy { it.beatmapID }
        val info = room.roomInfo
        val players = room.users.associateBy { it.userID }

        val its = roundIDs.mapNotNull { r -> itemMap[r] }

        val playedUserSet = its.mapNotNull { it.scores?.map { s -> s.userID } }.flatten().sorted().toSet()
        val teamVS = playedUserSet.size == 2

        return Match(
            statistics = Match.MatchStat(info.roomID, info.startedTime, info.endedTime, info.name),
            events = its.map { i ->
                val beatmap = beatmaps.getOrDefault(i.beatmapID, Beatmap(beatmapID = i.beatmapID))

                val scores = i.scores.orEmpty().onEach { s ->

                    // 给 room 手动赋红蓝
                    val slot = playedUserSet.indexOf(s.userID)
                    val team = if (slot != -1 && teamVS) {
                        if (slot % 2 == 0) {
                            "blue"
                        } else {
                            "red"
                        }
                    } else {
                        "none"
                    }

                    s.playerStat = LazerScore.MatchScorePlayerStat(slot.toByte(), team, s.passed)
                    s.user = players.getOrDefault(s.userID, MicroUser().apply {
                        this.userID = s.userID
                    })
                }

                val round = Match.MatchRound(
                    i.listID, beatmap, i.beatmapID, i.createdTime, i.playedTime, beatmap.modeInt,
                    mods = i.allowedMods.map { it.acronym },
                    scores = scores,
                    teamType = if (teamVS) "team-vs" else i.details?.teams ?: "head-to-head",
                    scoringType = "score-v2",
                )

                Match.MatchEvent(
                    i.listID,
                    Match.MatchEventDetail(
                        i.details?.roomType ?: "ranked_play", i.details?.roomType
                    ),
                    i.createdTime, i.ownerID,
                    round = round,
                )
            },
            players = room.users,
            firstEventID = room.firstEventID,
            latestEventID = room.latestEventID
        )
    }
}
