package com.now.nowbot.model.multiplayer

import com.now.nowbot.model.enums.OsuMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.MicroUser
import java.time.OffsetDateTime

interface MatchAdapter {
    var match : MonitoredMatch

    fun onStart()

    fun onGameAbort(beatmapID: Long)

    fun onGameStart(event: GameStartEvent)

    fun onGameEnd(event: GameEndEvent)

    fun onMatchEnd(type: MatchListener.StopType)

    fun onError(e: Exception)

    data class GameStartEvent(
        // 游戏 id, 用来确认是否是同一场游戏, 用于处理 abort 的情况
        val id: Long,
        val matchName: String,
        val beatmapID: Long,
        var beatmap: BeatMap,
        val start: OffsetDateTime,
        val mode: OsuMode,
        val mods: List<OsuMod>,
        val isTeamVS: Boolean,
        val teamType:String,
        val users: List<MicroUser>,
    )

    data class GameEndEvent(
        val game: MonitoredMatch.MatchGame,
        val id: Long,
        val users: Map<Long, MicroUser>,
    )
}