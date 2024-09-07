package com.now.nowbot.model.multiplayer

import com.now.nowbot.model.JsonData.Match
import com.now.nowbot.service.OsuApiService.OsuMatchApiService
import com.yumu.core.extensions.toJson
import org.slf4j.LoggerFactory
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path

class NewMatchListener(
    val match: NewMatch,
    val matchApiService: OsuMatchApiService,
) {
    val matchId = match.ID
    var nowEventID:Long = match.latestEventID
    val eventListener = mutableListOf<(Iterable<Match.MatchEvent>, NewMatch.EventType, Match) -> Boolean>()
    var future:ScheduledFuture<*>? = null
    var kill:ScheduledFuture<*>? = null

    private fun listen() {
        try{
            if (match.isMatchEnd) stopListener(StopType.MATCH_END)

            val newMatch = matchApiService.getNewMatchInfo(matchId, after =  nowEventID)

            // 对局没有任何新事件
            if (newMatch.latestEventID == nowEventID) return

            if (newMatch.currentGameID != null) {
                // 正在进行中
                TODO("找时间测试")
            }
            nowEventID = newMatch.latestEventID

            println(newMatch.toJson())

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

        kill = executorService.schedule( {
            if (isStart()) stopListener(StopType.TIME_OUT)
        }, 3, TimeUnit.HOURS)

        onStart()
    }

    fun onError(e: Exception) {

    }

    fun onStart() {

    }

    fun onStop(type: StopType) {

    }

    fun stopListener(type: StopType) {
        if (!isStart()) return
        kill?.cancel(true)
        future?.cancel(true)
        onStop(type)
    }

    fun isStart() = future?.isDone?.not() ?: false

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

fun main() {
    val path = Path("/home/spring/Documents/nowbot/osufile/")
    val observer = FileSystems.getDefault().newWatchService()
    path.register(
        observer,
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_DELETE,
        StandardWatchEventKinds.ENTRY_MODIFY,
        StandardWatchEventKinds.OVERFLOW,
        )
    var keep:Boolean
    do {
        val key = observer.take()
        key.pollEvents().forEach {
            val file = it.context() as Path
            when(it.kind()) {
                StandardWatchEventKinds.ENTRY_CREATE -> {
                    println("创建文件: $file")
                }
                StandardWatchEventKinds.ENTRY_DELETE -> {
                    println("删除文件: $file")
                }
                StandardWatchEventKinds.ENTRY_MODIFY -> {
                    println("修改文件: $file")
                }
                StandardWatchEventKinds.OVERFLOW -> {
                    println("事件丢失: $file")
                }
            }
        }
        keep = key.reset()
    }while (keep)
}