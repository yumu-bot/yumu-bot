package com.now.nowbot.model.match

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.MicroUser
import java.time.OffsetDateTime

interface MatchAdapter {
    var match : Match

    fun onStart()

    fun onGameAbort(beatmapID: Long)

    fun onGameStart(event: GameStartEvent)

    fun onGameEnd(event: GameEndEvent)

    fun onMatchEnd(type: MatchListener.StopType)

    fun onError(e: Throwable)

    data class GameStartEvent(
        // 游戏 id, 用来确认是否是同一场游戏, 用于处理 abort 的情况
        val id: Long,
        val matchName: String,
        val beatmapID: Long,
        var beatmap: Beatmap,
        val start: OffsetDateTime,
        val mode: OsuMode,
        val mods: List<LazerMod>,
        val isTeamVS: Boolean,
        val teamType:String,
        val users: List<MicroUser>,
    )

    data class GameEndEvent(
        val game: Match.MatchRound,
        val id: Long,
        val users: Map<Long, MicroUser>,
    )
}