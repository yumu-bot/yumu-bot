package com.now.nowbot.model.multiplayer

import com.now.nowbot.model.JsonData.BeatMap
import com.now.nowbot.model.JsonData.MicroUser
import com.now.nowbot.model.enums.OsuMod
import com.now.nowbot.model.enums.OsuMode
import java.time.OffsetDateTime

interface MatchAdapter {
    fun onStart()

    fun onGameStart(event: GameStartEvent)

    fun onGameEnd(event: GameEndEvent)

    fun onMatchEnd(type: NewMatchListener.StopType)

    fun onError(e: Exception)

    data class GameStartEvent(
        // 游戏 id, 用来确认是否是同一场游戏, 用于处理 abort 的情况
        val ID: Long,
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
        val game: NewMatch.MatchGame,
        val ID: Long,
        val users: Map<Long, MicroUser>,
        val name: String,
        val startTime: OffsetDateTime,
    )
}