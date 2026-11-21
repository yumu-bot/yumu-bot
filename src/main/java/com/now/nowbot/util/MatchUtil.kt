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
     *
     * 队伍信息来自 `playlist_items[].details.teams`，而不是按玩家人数的奇偶猜测：
     * 一个 team_versus 房间可以有任意多名玩家，按人数判断会把所有成绩都标成 `none`。
     */
    fun Room.toMatch(): Match {
        val room = this

        // 只统计真正打完的局；game_started / game_aborted 不算一局
        val roundIDs = room.events
            .asSequence()
            .filter { it.itemID != null && it.type == Room.EventType.GameCompleted }
            .mapNotNull { it.itemID }
            .distinct()
            .toList()

        val itemMap = room.items.associateBy { i -> i.listID }
        val beatmaps = room.beatmaps.associateBy { it.beatmapID }
        val beatmapsets = room.beatmapsets.associateBy { it.beatmapsetID }
        val info = room.roomInfo
        val players = room.users.associateBy { it.userID }

        val its = roundIDs.mapNotNull { r -> itemMap[r] }

        return Match(
            statistics = Match.MatchStat(info.roomID, info.startedTime, info.endedTime, info.name),
            events = its.map { i ->
                val beatmap = beatmaps[i.beatmapID].let { bm ->
                    when {
                        bm == null -> Beatmap(beatmapID = i.beatmapID)
                        bm.beatmapset == null -> beatmapsets[bm.beatmapsetID]?.let { bm.copy(beatmapset = it) } ?: bm
                        else -> bm
                    }
                }

                val teamType = toMatchTeamType(i.details?.roomType ?: info.type)
                val isTeamVS = teamType == "team-vs" || teamType == "tag-team-vs"
                val teams = i.details?.teams.orEmpty()
                val slots = i.details?.slots.orEmpty()

                val scores = i.scores.orEmpty().onEach { s ->
                    val team = if (isTeamVS) {
                        teams[s.userID]?.lowercase()?.takeIf { it == "red" || it == "blue" } ?: "none"
                    } else {
                        "none"
                    }

                    // slot 只用于展示时排序；没有槽位信息时给 -1，不要和真实槽位混在一起
                    val slot = slots[s.userID] ?: -1

                    s.playerStat = LazerScore.MatchScorePlayerStat(slot.toByte(), team, s.passed)
                    s.user = players.getOrDefault(s.userID, MicroUser().apply {
                        this.userID = s.userID
                    })
                }

                // allowed_mods 是房间允许的 mod 池，不是这局实际使用的 mod；
                // 用成绩里的 mod 才能真正反映本局，缺失时再退回 required_mods。
                val mods = scores.firstOrNull { it.mods.isNotEmpty() }
                    ?.mods
                    ?.map { it.acronym }
                    ?: i.requiredMods.map { it.acronym }

                val round = Match.MatchRound(
                    i.listID,
                    beatmap,
                    i.beatmapID,
                    startTime = i.details?.startedTime ?: i.createdTime,
                    endTime = i.playedTime,
                    modeInt = i.rulesetID.toInt(),
                    mods = mods,
                    scores = scores,
                    teamType = teamType,
                    scoringType = "score-v2",
                )

                Match.MatchEvent(
                    i.listID,
                    Match.MatchEventDetail(
                        i.details?.roomType ?: "game", i.details?.roomType
                    ),
                    i.details?.startedTime ?: i.createdTime, i.ownerID,
                    round = round,
                )
            },
            players = room.users,
            firstEventID = room.firstEventID,
            latestEventID = room.lastEventID
        )
    }

    /**
     * 把 multiplayer 的房间类型转换成旧版 Match 使用的队伍类型。
     *
     * 旧版逻辑只把 `team-vs` / `tag-team-vs` 当作队伍赛，其余都按个人赛处理。
     */
    private fun toMatchTeamType(roomType: String?): String =
        when (roomType?.trim()?.lowercase()?.replace('-', '_')) {
            "team_versus", "team_vs", "teamvs" -> "team-vs"
            "tag_team_versus", "tag_team_vs", "tagteamvs" -> "tag-team-vs"
            else -> "head-to-head"
        }
}
