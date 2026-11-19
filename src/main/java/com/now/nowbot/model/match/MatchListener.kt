package com.now.nowbot.model.match

import com.now.nowbot.model.match.Match.EventType
import com.now.nowbot.model.osu.LazerMod.Companion.toLazerMods
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class MatchListener(
    val match: Match,
    private val matchApiService: OsuMatchApiService,
    vararg adapter: MatchAdapter
) {
    private val usersIDSet = mutableSetOf<Long>()
    val userMap = mutableMapOf<Long, MicroUser>()
    var beforeGame: (Match.MatchEvent, MatchListener) -> Unit = { _, _ -> }
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
        parseUsers(match.events, match.players)
        if (match.currentGameID != null) {
            val gameEvent = match.events.last { it.round != null }
            nowGameID = match.currentGameID
            nowEventID = gameEvent.eventID - 1
            // onEvent 内部已经做了逐对局 / 逐适配器的异常隔离
            onEvent(gameEvent)
        }
    }

    private fun listen() {
        val newMatch = try {
            matchApiService.getMatchAfter(matchId, nowEventID)
        } catch (e: Throwable) {
            log.error("ml: $matchId 查询失败: ", e)
            return
        }
        try {
            // 对局没有任何新事件
            if (nowEventID == newMatch.latestEventID) return

            // 现在有对局正在进行中
            if (newMatch.currentGameID != null) {
                val gameEvent = newMatch.events.lastOrNull { it.round != null }
                var isAbort = false
                if (nowGameID != null && newMatch.currentGameID != nowGameID) {
                    nowGameID = newMatch.currentGameID
                    isAbort = true
                }
                if (gameEvent != null && nowEventID == gameEvent.eventID - 1 && isAbort.not()) {
                    return
                } else if (gameEvent != null) {
                    nowEventID = gameEvent.eventID - 1
                } else {
                    nowEventID = newMatch.latestEventID
                }
            } else {
                nowEventID = newMatch.latestEventID
                nowGameID = null
            }

            // 先添加新玩家，再添加新对局
            parseUsers(newMatch.events, newMatch.players)
            addUsers(newMatch.events)

            match.append(newMatch)

            onAllEvent(newMatch.events)
            if (newMatch.isMatchEnd) stop(StopType.MATCH_END)
        } catch (e: Throwable) {
            // 这里是 ScheduledExecutorService.scheduleAtFixedRate 的任务:
            // 任何逃逸的异常(包括 Error)都会让 future 被取消, 监听从此静默死掉。
            // 所以兜底必须捕获 Throwable, 并保证 onError 自身也不会再抛。
            log.error("ml: $matchId 监听轮询失败: ", e)
            safeOnError(e)
        }
    }

    fun start() {
        if (isStart()) return

        if (match.isMatchEnd) {
            onStart()
            onStop(StopType.MATCH_END)
            return
        }

        future = executorService.scheduleAtFixedRate(this::listen, 0, 8, TimeUnit.SECONDS)
        kill = executorService.schedule({
            if (isStart()) stop(StopType.TIME_OUT)
        }, 6, TimeUnit.HOURS)
        onStart()
    }

    fun stop(type: StopType) {
        if (!isStart()) return
        onStop(type)
        kill?.cancel(true)
        future?.cancel(true)
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
    private fun onAllEvent(events: List<Match.MatchEvent>) {
        val gameEvent = events.filter { it.round != null }
        if (gameEvent.isEmpty()) return
        // game 事件多于一个认为可能有 abort
        if (gameEvent.size > 1) {
            val abortGames = gameEvent.dropLast(1)
            abortGames.forEach {
                val game = it.round!!
                if (game.endTime != null) { // 正常结束但是中间没有获取到 (因为请求延迟导致)
                    onEvent(it)
                } else { // abort
                    notifyAdapters { l -> l.onGameAbort(game.beatmapID) }
                }
            }
        }
        // 正常处理当前最新的对局事件
        onEvent(gameEvent.last())
    }

    /**
     * 针对单个对局处理。
     * 单个对局(或单个适配器)出错只影响它自己, 不能影响同一批里的其他对局, 更不能让监听线程退出。
     */
    private fun onEvent(event: Match.MatchEvent) {
        try {
            dispatchEvent(event)
        } catch (e: Throwable) {
            log.error("ml: $matchId 处理对局事件失败: ", e)
            safeOnError(e)
        }
    }

    private fun dispatchEvent(event: Match.MatchEvent) {
        val game = event.round ?: return

        try {
            beforeGame(event, this)
        } catch (e: Throwable) {
            // 补全谱面/用户信息失败不应该阻断这一局的通知: 结束局仍可用数据库里的谱面渲染
            log.error("ml: $matchId 补全对局信息失败: ", e)
            safeOnError(e)
        }

        val isEnd = game.endTime != null
        if (isEnd) {
            // 对局结束
            val listenerEvent = MatchAdapter.GameEndEvent(
                game,
                event.eventID,
                userMap,
            )
            notifyAdapters { l -> l.onGameEnd(listenerEvent) }
        } else {
            // 对局开始: 没有谱面信息时无法构造事件, 跳过这一局的开始通知
            val startBeatmap = game.beatmap
            if (startBeatmap == null) {
                log.warn("ml: $matchId 对局 ${game.roundID} 缺少谱面信息, 跳过开始事件")
                return
            }

            val user = usersIDSet.mapNotNull { id -> userMap[id] }
            val listenerEvent = MatchAdapter.GameStartEvent(
                id = event.eventID,
                matchName = match.name,
                beatmapID = game.beatmapID,
                beatmap = startBeatmap,
                start = game.startTime,
                mode = game.mode,
                mods = game.mods.toLazerMods(),
                isTeamVS = game.isTeamVS,
                teamType = game.teamType,
                users = user,
            )
            notifyAdapters { l -> l.onGameStart(listenerEvent) }
        }
    }

    /**
     * 逐个通知适配器: 某个适配器抛异常时只回调它自己的 onError, 其他适配器照常收到事件。
     */
    private fun notifyAdapters(action: (MatchAdapter) -> Unit) {
        eventListener.forEach { adapter ->
            try {
                action(adapter)
            } catch (e: Throwable) {
                log.error("ml: $matchId 适配器处理事件失败: ", e)
                notifyError(adapter, e)
            }
        }
    }

    private fun safeOnError(e: Throwable) {
        eventListener.forEach { notifyError(it, e) }
    }

    private fun notifyError(adapter: MatchAdapter, e: Throwable) {
        try {
            adapter.onError(e)
        } catch (t: Throwable) {
            // 错误通知本身也可能失败(例如消息发送异常), 不能让它冒泡到定时任务
            log.error("ml: $matchId 适配器错误回调失败: ", t)
        }
    }

    private fun onStart() {
        notifyAdapters { it.onStart() }
    }

    private fun onStop(type: StopType) {
        notifyAdapters { it.onMatchEnd(type) }
    }

    private fun isStart(): Boolean {
        val f = future
        return f != null && !f.isDone && !f.isCancelled
    }

    private fun addUsers(events: List<Match.MatchEvent>) {
        events
            .filter { it.round != null }
            .flatMap { it.round!!.scores }
            .forEach { userMap[it.userID]?.let { user -> it.user = user } }
    }

    private fun parseUsers(events: List<Match.MatchEvent>, users: List<MicroUser>) {
        users.forEach {
            userMap[it.userID] = it
        }
        events.forEach {
            when (it.type) {
                EventType.HostChanged -> usersIDSet.add(it.userID!!)
                EventType.PlayerJoined -> usersIDSet.add(it.userID!!)
                EventType.PlayerKicked -> usersIDSet.remove(it.userID!!)
                EventType.PlayerLeft -> usersIDSet.remove(it.userID!!)
                EventType.Other -> {
                    if (it.round?.endTime == null) return@forEach
                    val game = it.round
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
        ;

        override fun toString() = this.tips
    }

    companion object {
        // private val log = LoggerFactory.getLogger(MatchListener::class.java)
        val executorService: ScheduledExecutorService
        val log = LoggerFactory.getLogger(MatchListener::class.java)!!

        init {
            val threadFactory = Thread.ofVirtual().name("v-MatchListener", 50).factory()
            executorService = Executors.newScheduledThreadPool(Int.MAX_VALUE, threadFactory)
        }
    }
}
