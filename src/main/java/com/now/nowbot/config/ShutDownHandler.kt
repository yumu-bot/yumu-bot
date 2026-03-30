package com.now.nowbot.config

import com.now.nowbot.service.messageServiceImpl.GuessService
import com.now.nowbot.service.messageServiceImpl.MatchListenerService
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component

@Component
class ShutdownHandler {
    @PreDestroy
    fun onShutdown() {
        // Spring 的 @PreDestroy 通常比原生的 ShutdownHook 更早执行
        // 此时 WebSocket 连接通常还未断开
        runBlocking {
            val j1 = launch { MatchListenerService.stopAllListenerFromReboot() }
            val j2 = launch { GuessService.stopAllGamesFromReboot() }
            joinAll(j1, j2)
        }
    }
}