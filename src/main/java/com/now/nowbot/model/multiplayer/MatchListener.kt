package com.now.nowbot.model.multiplayer

import com.now.nowbot.model.enums.OsuMod
import com.now.nowbot.model.json.MicroUser
import com.now.nowbot.model.multiplayer.MonitoredMatch.EventType
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class MatchListener(
    val match: MonitoredMatch,
    val matchApiService: OsuMatchApiService,
    vararg adapter: MatchAdapter
) {
    val usersIDSet = mutableSetOf<Long>()
    val userMap = mutableMapOf<Long, MicroUser>()
    var beforeGame: (MonitoredMatch.MatchEvent, MatchListener) -> Unit = { _, _ -> }
    private val matchId = match.id
    private var nowGameID: Long? = null
    private var nowEventID: Long = match.latestEventID
    private val eventListener = mutableSetOf<MatchAdapter>()
    private var future: ScheduledFuture<*>? = null
    private var kill: ScheduledFuture<*>? = null

    init {

        adapter.forEach {
            it.match = match
        }

        eventListener.addAll(adapter)
        parseUsers(match.events, match.users)
        if (match.currentGameID != null) {
            val gameEvent = match.events.last { it.game != null }
            nowGameID = match.currentGameID
            nowEventID = gameEvent.eventID - 1
            try {
                onEvent(gameEvent)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private fun listen() {
        try {
            val newMatch = matchApiService.getMonitoredMatchInfo(matchId, after = nowEventID)
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
                if (nowEventID == gameEvent.eventID - 1 && isAbort.not()) {
                    return
                } else {
                    nowEventID = gameEvent.eventID - 1
                }
            } else {
                nowEventID = newMatch.latestEventID
            }
            match += newMatch
            parseUsers(newMatch.events, newMatch.users)
            onEvent(newMatch.events)
            if (newMatch.isMatchEnd) stop(StopType.MATCH_END)
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
        }, 6, TimeUnit.HOURS)
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
    fun removeListener(listener: MatchAdapter): Boolean {
        eventListener.remove(listener)
        if (eventListener.isEmpty()) {
            stop(StopType.SERVER_REBOOT)
            return true
        }
        return false
    }

    /**
     * 处理批量对局
     */
    private fun onEvent(events: List<MonitoredMatch.MatchEvent>) {
        val gameEvent = events.filter { it.game != null }
        if (gameEvent.isEmpty()) return
        // game 事件多于一个认为可能有 abort
        if (gameEvent.size > 1) {
            val abortGames = gameEvent.dropLast(1)
            abortGames.forEach {
                val game = it.game!!
                if (game.endTime != null) { // 正常结束但是中间没有获取到 (因为请求延迟导致)
                    onEvent(it)
                } else { // abort
                    eventListener.forEach { l -> l.onGameAbort(game.beatmapID) }
                }
            }
        }
        // 正常处理当前最新的对局事件
        onEvent(gameEvent.last())
    }

    /**
     * 针对单个对局处理
     */
    private fun onEvent(event: MonitoredMatch.MatchEvent) {
        val game = event.game ?: return
        beforeGame(event, this)
        val isEnd = game.endTime != null
        if (isEnd) {
            // 对局结束
            val listenerEvent = MatchAdapter.GameEndEvent(
                game,
                event.eventID,
                userMap,
            )
            eventListener.forEach { l -> l.onGameEnd(listenerEvent) }
        } else {
            // 对局开始
            val user = usersIDSet.mapNotNull { id -> userMap[id] }
            val listenerEvent = with(game) {
                MatchAdapter.GameStartEvent(
                    event.eventID,
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
            eventListener.forEach { l -> l.onGameStart(listenerEvent) }
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

    fun isStart() = future?.isDone?.not() == true

    private fun parseUsers(events: List<MonitoredMatch.MatchEvent>, users: List<MicroUser>) {
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
        SERVER_REBOOT("服务器重启"),
        TIME_OUT("超时了"),
    }

    companion object {
        // private val log = LoggerFactory.getLogger(MatchListener::class.java)
        val executorService: ScheduledExecutorService

        init {
            val threadFactory = Thread.ofVirtual().name("v-MatchListener", 50).factory()
            executorService = Executors.newScheduledThreadPool(Int.MAX_VALUE, threadFactory)
        }
    }
}
