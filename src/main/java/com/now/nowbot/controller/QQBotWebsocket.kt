package com.now.nowbot.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.qq.tencent.TencentAdapter
import com.now.nowbot.qq.tencent.YumuServer
import com.yumu.Listener
import com.yumu.WebsocketAdapter
import com.yumu.Yumu
import com.yumu.core.extensions.toJson
import com.yumu.model.WebsocketPackage
import jakarta.annotation.PostConstruct
import jakarta.websocket.OnClose
import jakarta.websocket.OnOpen
import jakarta.websocket.Session
import jakarta.websocket.server.ServerEndpoint
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CopyOnWriteArraySet

@RestController
@Suppress("unused")
@ServerEndpoint("/qq-ws")
class QQBotWebsocket {
    private val scope = CoroutineScope(Dispatchers.Default)

    @PostConstruct
    fun init() {
        Yumu.registerServer(TencentAdapter, YumuServer)
    }

    @OnOpen
    fun onOpen(session: Session) {
        val wsObject = WebsocketObject(session)
        session.addMessageHandler(String::class.java) { message: String? ->
            val request = WebsocketPackage.toNodePackage(message!!)
            session.launch { listener ->
                listener(request, wsObject)
            }
        }
    }

    @OnClose
    fun onClose(session: Session) {
        websockets.remove(session)
    }

    var on: Boolean = true

    companion object {
        private val log: Logger = LoggerFactory.getLogger(QQBotWebsocket::class.java)

        /**
         * 所有的链接
         */
        private val websockets = CopyOnWriteArraySet<Session>()
    }

    fun Session.launch(action: suspend (Listener) -> Unit) {
        scope.launch {
            supervisorScope {
                TencentAdapter.listener.forEach { l ->
                    try {
                        action(l)
                    } catch (ignore: TimeoutCancellationException) {

                    } catch (e: Exception) {
                        log.error("Error", e)
                    }
                }
            }
        }
    }

    val type = object : TypeReference<WebsocketPackage<JsonNode>>() {}

    class WebsocketObject(val session: Session) :WebsocketAdapter() {
        override suspend fun send(message: WebsocketPackage<*>) {
            withContext(Dispatchers.IO) {
                session.basicRemote.sendText(message.toJson())
            }
        }
    }
}
