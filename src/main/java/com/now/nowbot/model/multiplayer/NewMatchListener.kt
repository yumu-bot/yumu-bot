package com.now.nowbot.model.multiplayer

import com.now.nowbot.model.JsonData.BeatMap
import com.now.nowbot.model.JsonData.MicroUser
import com.now.nowbot.model.enums.OsuMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.multiplayer.NewMatch.EventType
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import com.now.nowbot.service.OsuApiService.OsuMatchApiService
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class NewMatchListener(
    val match: NewMatch,
    val beatmapApiService: OsuBeatmapApiService,
    val matchApiService: OsuMatchApiService,
    vararg listerner: MatchAdapter
) {
    private val matchId = match.ID
    private var nowGameID: Long? = null
    private var nowEventID: Long = match.latestEventID
    private val eventListener = mutableSetOf<MatchAdapter>()
    private val usersIDSet = mutableSetOf<Long>()
    private val userMap = mutableMapOf<Long, MicroUser>()
    private var future: ScheduledFuture<*>? = null
    private var kill: ScheduledFuture<*>? = null

    init {
        eventListener.addAll(listerner)
        parseUsers(match.events, match.users)
        if (match.currentGameID != null) {
            val gameEvent = match.events.last { it.game != null }
            nowGameID = match.currentGameID
            nowEventID = gameEvent.ID - 1
            onEvent(listOf(gameEvent))
        }
    }

    private fun listen() {
        try {
            if (match.isMatchEnd) stop(StopType.MATCH_END)
            val newMatch = matchApiService.getNewMatchInfo(matchId, after = nowEventID)
            // 对局没有任何新事件
            if (nowEventID == newMatch.latestEventID) return
            if (newMatch.currentGameID != null) {
                // 现在有对局正在进行中
                val gameEvent = newMatch.events.last { it.game != null }
                var isAbort = false
                if (newMatch.currentGameID != nowGameID) {
                    nowGameID = newMatch.currentGameID
                    isAbort = true
                }
                if (nowEventID == gameEvent.ID - 1 && isAbort.not()) {
                    return
                } else if (gameEvent.game?.endTime!= null) {
                    // 对局结束之后的 abort
                    nowGameID = gameEvent.ID
                } else {
                    nowEventID = gameEvent.ID - 1
                }
            } else {
                nowEventID = newMatch.latestEventID
            }
            match += newMatch
            parseUsers(newMatch.events, newMatch.users)
            onEvent(newMatch.events)
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun start() {
        if (isStart()) return

        if (match.isMatchEnd) {
            onStart()
            onStop(StopType.MATCH_END)
            return
        }

        nowEventID = match.latestEventID

        future = executorService.scheduleAtFixedRate(this::listen, 0, 8, TimeUnit.SECONDS)

        kill = executorService.schedule({
            if (isStart()) stop(StopType.TIME_OUT)
        }, 3, TimeUnit.HOURS)

        onStart()
    }

    fun stop(type: StopType) {
        if (!isStart()) return
        kill?.cancel(true)
        future?.cancel(true)
        onStop(type)
    }

    fun addListener(listener: MatchAdapter) {
        listener.match = match
        eventListener.add(listener)
    }

    /**
     * 当没有任何监听者的时候, 会自动关闭监听
     *
     * @return 返回 true 代表不存在 ListenerAdapter 了
     */
    fun removeListener(listener: MatchAdapter) : Boolean{
        eventListener.remove(listener)
        if (eventListener.isEmpty()) {
            stop(StopType.SERVICE_STOP)
            return true
        }
        return false
    }

    private fun onEvent(events: List<NewMatch.MatchEvent>) {
        events.forEach {
            if (it.game == null) return@forEach
            val game = it.game

            with(game) {
                if (beatmap != null) {
                    beatmap = beatmapApiService.getBeatMapInfo(beatmapID)
                    beatmapApiService.applySRAndPP(beatmap, mode, OsuMod.getModsValueFromAbbrList(mods))
                } else {
                    beatmap = BeatMap(beatmapID)
                }
            }

            val isEnd = game.endTime != null
            if (isEnd) {
                // 对局结束
                val event = MatchAdapter.GameEndEvent(
                    game,
                    it.ID,
                    userMap,
                )
                eventListener.forEach { l -> l.onGameEnd(event) }
            } else {
                // 对局开始
                val user = usersIDSet.map { id -> userMap[id] }.filterNotNull()

                val event = with(game) {
                    MatchAdapter.GameStartEvent(
                        it.ID,
                        match.name,
                        beatmapID,
                        beatmap!!,
                        startTime,
                        mode,
                        mods.map { OsuMod.getModFromAbbreviation(it) },
                        isTeamVS,
                        teamType,
                        user
                    )
                }
                eventListener.forEach { l -> l.onGameStart(event) }
            }
        }
    }

    private fun onError(e: Exception) {
        eventListener.forEach { it.onError(e) }
    }

    private fun onStart() {
        eventListener.forEach { it.onStart() }
    }

    private fun onStop(type: StopType) {
        eventListener.forEach { it.onMatchEnd(type) }
    }

    fun isStart() = future?.isDone?.not() ?: false

    private fun parseUsers(events: List<NewMatch.MatchEvent>, users: List<MicroUser>) {
        users.forEach {
            userMap[it.id] = it
        }
        events.forEach {
            when (it.type) {
                EventType.HostChanged -> usersIDSet.add(it.userID!!)
                EventType.PlayerJoined -> usersIDSet.add(it.userID!!)
                EventType.PlayerKicked -> usersIDSet.remove(it.userID!!)
                EventType.PlayerLeft -> usersIDSet.remove(it.userID!!)
                EventType.Other -> {
                    if (it.game?.endTime == null) return@forEach
                    val game = it.game
                    game.scores.forEach { s -> usersIDSet.add(s.userID) }
                }

                else -> {}
            }
        }
    }

    enum class StopType(val tips: String) {
        MATCH_END("比赛正常结束"),
        USER_STOP("调用者关闭"),
        SUPER_STOP("超级管理员关闭"),
        SERVICE_STOP("服务器重启"),
        TIME_OUT("超时了"),
    }

    companion object {
        private val log = LoggerFactory.getLogger(NewMatchListener::class.java)
        val executorService: ScheduledExecutorService

        init {
            val threadFactory = Thread.ofVirtual().name("v-MatchListener", 50).factory()
            executorService = Executors.newScheduledThreadPool(Int.MAX_VALUE, threadFactory)
        }
    }
}
