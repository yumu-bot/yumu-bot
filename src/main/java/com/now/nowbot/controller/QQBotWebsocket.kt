package com.now.nowbot.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.qq.tencent.Listener
import com.now.nowbot.qq.tencent.TencentAdapter
import com.now.nowbot.qq.tencent.YumuServer
import com.now.nowbot.util.ContextUtil
import com.now.nowbot.util.JacksonUtil
import com.yumu.Yumu
import com.yumu.model.WebsocketPackage
import jakarta.annotation.PostConstruct
import jakarta.websocket.CloseReason
import jakarta.websocket.OnClose
import jakarta.websocket.OnOpen
import jakarta.websocket.Session
import jakarta.websocket.server.ServerEndpoint
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RestController
import java.io.IOException
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicInteger

@ServerEndpoint("/qq-ws")
@RestController
class QQBotWebsocket {
    private var session: Session? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @PostConstruct
    fun init() {
        Yumu.registerServer(TencentAdapter, YumuServer)
    }

    @OnOpen
    fun onOpen(session: Session) {
        this.session = session
        session.addMessageHandler(String::class.java) { message: String? ->
            session.launch {
                it(WebsocketPackage.toNodePackage(message!!))
            }
        }
        websockets.add(this)
        addCount()
    }

    @OnClose
    fun onClose() {
        if (websockets.contains(this)) {
            on = false
            websockets.remove(this)
            subCount()
        }
        println(session!!.id + " is closed")
    }

    var on: Boolean = true

    @Throws(IOException::class)
    fun sendMessage(message: ByteArray) {
        session!!.basicRemote.sendStream.write(message)
        session!!.basicRemote.sendStream.flush()
    }

    @Throws(IOException::class)
    fun close() {
        websockets.remove(this)
        session!!.close(CloseReason(CloseReason.CloseCodes.getCloseCode(1015), "closed"))
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(QQBotWebsocket::class.java)
        val sessionContextName = "ws-session"

        fun sendMessage(message: String) {
            val ws = ContextUtil.getContext(sessionContextName, Session::class.java)
            ws?.send(message) ?: throw Exception("session not found")
        }

        /**
         * 连接数
         */
        private val SocketConut = AtomicInteger(0)

        /**
         * 所有的链接
         */
        private val websockets = CopyOnWriteArraySet<QQBotWebsocket>()
        private fun addCount() {
            SocketConut.getAndIncrement()
        }

        private fun subCount() {
            SocketConut.getAndDecrement()
        }


        fun Session.send(message: String) {
            basicRemote.sendText(message)
        }
    }

    fun Session.launch(action: suspend (Listener) -> Unit) {
        val session = this
        scope.launch {
            supervisorScope {
                TencentAdapter.listener.forEach { l ->
                    ContextUtil.setContext(sessionContextName, session)
                    try {
                        action(l)
                    } catch (e: Exception) {
                        log.error("err:", e)
                    } finally {
                        ContextUtil.remove()
                    }
                }
            }
        }
    }

    val type = object : TypeReference<WebsocketPackage<JsonNode>>() {}
    fun String.toPackage(): WebsocketPackage<JsonNode> {
        return JacksonUtil.mapper.readValue(this, type)
    }
}
