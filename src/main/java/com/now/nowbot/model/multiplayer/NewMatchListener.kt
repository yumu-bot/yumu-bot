package com.now.nowbot.model.multiplayer

import com.now.nowbot.model.JsonData.MicroUser
import com.now.nowbot.model.enums.OsuMod
import com.now.nowbot.model.multiplayer.NewMatch.EventType
import com.now.nowbot.service.OsuApiService.OsuMatchApiService
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class NewMatchListener(
    val match: NewMatch,
    val matchApiService: OsuMatchApiService,
) {
    private val matchId = match.ID
    private var nowGameID: Long? = null
    private var nowEventID: Long = match.latestEventID
    private val eventListener = mutableListOf<MatchAdapter>()
    private val usersIDSet = mutableSetOf<Long>()
    private val userMap = mutableMapOf<Long, MicroUser>()
    private var future: ScheduledFuture<*>? = null
    private var kill: ScheduledFuture<*>? = null

    init {
        val firstHost = match.events.find { it.type == EventType.HostChanged }
        if (firstHost != null) {
            usersIDSet.add(firstHost.userID!!)
        }
        parseUsers(match.events, match.users)
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
                } else {
                    nowEventID = gameEvent.ID - 1
                }
            } else {
                nowEventID = newMatch.latestEventID
            }
            match += newMatch
            parseUsers(newMatch.events, newMatch.users)
            onEvent(newMatch)
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

        future = executorService.scheduleAtFixedRate(this::listen, 0, 10, TimeUnit.SECONDS)

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
        eventListener.add(listener)
    }

    private fun onEvent(e: NewMatch) {
        e.events.forEach {
            if (it.game == null) return@forEach
            val game = it.game
            val isEnd = game.endTime != null
            if (isEnd) {
                // 对局结束
                val event = MatchAdapter.GameEndEvent(
                    it.ID,
                    game.beatmap,
                    game.startTime,
                    game.endTime!!,
                    game.mode,
                    game.mods.map { OsuMod.getModFromAbbreviation(it) },
                    game.isTeamVS,
                    userMap,
                    game.scores
                )
                eventListener.forEach { l -> l.onGameEnd(event) }
            } else {
                // 对局开始
                val user = usersIDSet.map { id -> userMap[id] }.filterNotNull()
                val event = MatchAdapter.GameStartEvent(
                    it.ID,
                    match.name,
                    game.beatmap,
                    game.startTime,
                    game.mode,
                    game.mods.map { OsuMod.getModFromAbbreviation(it) },
                    game.isTeamVS,
                    user
                )
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
                EventType.PlayerJoined -> usersIDSet.add(it.userID!!)
                EventType.PlayerKicked -> usersIDSet.remove(it.userID!!)
                EventType.PlayerLeft -> usersIDSet.remove(it.userID!!)
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
