package com.now.nowbot.config

import com.now.nowbot.service.messageServiceImpl.GuessService
import com.now.nowbot.service.messageServiceImpl.MatchListenerService
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.stereotype.Component
import kotlin.time.Duration.Companion.milliseconds

@Component
@Suppress("UNUSED")
class ShutdownHandler(
    private val matchListenerService: MatchListenerService,
    private val guessService: GuessService,
) : ApplicationListener<ContextClosedEvent> {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: ContextClosedEvent) {
        log.info("接收到停机信号，开始执行游戏和监听器的保存/清理工作...")

        try {
            runBlocking(Dispatchers.IO) {
                withTimeoutOrNull(10_000L.milliseconds) {
                    val j1 = launch {
                        try {
                            MatchListenerService.stopAllListenerFromReboot()
                        } catch (e: Exception) {
                            log.error("关闭比赛监听失败", e)
                        }
                    }
                    val j2 = launch {
                        try {
                            GuessService.stopAllGamesFromReboot()
                        } catch (e: Exception) {
                            log.error("关闭猜词游戏失败", e)
                        }
                    }
                    joinAll(j1, j2)
                } ?: log.warn("停机清理任务超时 (10秒)，强制跳过！")
            }
        } catch (e: Exception) {
            log.error("执行停机任务时发生未知异常", e)
        }

        log.info("停机清理工作执行完毕，允许 Spring 继续销毁容器。")
    }
}