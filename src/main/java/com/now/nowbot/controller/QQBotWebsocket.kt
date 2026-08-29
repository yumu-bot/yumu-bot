package com.now.nowbot.controller

import com.now.nowbot.qq.tencent.TencentAdapter
import com.now.nowbot.qq.tencent.YumuServer
import com.now.nowbot.util.DataUtil.findCauseOfType
import com.yumu.Listener
import com.yumu.WebsocketAdapter
import com.yumu.Yumu
import com.yumu.core.extensions.toJson
import com.yumu.model.WebsocketPackage
import jakarta.annotation.PostConstruct
import jakarta.websocket.OnClose
import jakarta.websocket.OnError
import jakarta.websocket.OnOpen
import jakarta.websocket.Session
import jakarta.websocket.server.ServerEndpoint
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RestController
import java.io.EOFException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap


@RestController
@Suppress("unused")
@ServerEndpoint("/qq-ws")
class QQBotWebsocket {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @PostConstruct
    fun init() {
        Yumu.registerServer(TencentAdapter, YumuServer)
    }

    @OnOpen
    fun onOpen(session: Session) {
        val wsObject = WebsocketObject(session)
        websockets.add(session)

        session.addMessageHandler(String::class.java) { message: String? ->
            if (message.isNullOrBlank()) return@addMessageHandler
            val request = WebsocketPackage.toNodePackage(message)

            // 为每个消息处理启动协程
            session.launch { listener ->
                listener(request, wsObject)
            }
        }
    }

    @OnClose
    fun onClose(session: Session) {
        cleanupSession(session)
    }

    @OnError
    fun onError(session: Session, error: Throwable?) {
        val eof = error?.findCauseOfType<EOFException>()
        val io = error?.findCauseOfType<IOException>()

        if (eof != null || io != null && io.message?.contains("EOF") == true) {
            log.warn("QQBot 客户端连接意外断开 (EOF): {}", session.id)
        } else {
            log.error("QQBot WebSocket 发生错误, Session ID: ${session.id}", error)
        }

        cleanupSession(session)
    }

    private fun cleanupSession(session: Session) {
        websockets.remove(session)
        try {
            if (session.isOpen) {
                session.close()
            }
        } catch (_: Exception) {}
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(QQBotWebsocket::class.java)
        private val websockets = ConcurrentHashMap.newKeySet<Session>()
    }

    fun Session.launch(action: suspend (Listener) -> Unit) {
        scope.launch {
            TencentAdapter.listener.forEach { l ->
                try {
                    action(l)
                } catch (_: TimeoutCancellationException) {
                } catch (e: Exception) {
                    log.error("Listener processing error", e)
                }
            }
        }
    }

    class WebsocketObject(val session: Session) : WebsocketAdapter() {
        override suspend fun send(message: WebsocketPackage<*>) {
            if (!session.isOpen) return

            // 2. 使用 asyncRemote 避免线程阻塞，并对同一 session 发送进行串行化加锁
            withContext(Dispatchers.IO) {
                synchronized(session) {
                    if (session.isOpen) {
                        session.basicRemote.sendText(message.toJson())
                    }
                }
            }
        }
    }
}
