package com.now.nowbot.controller

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.now.nowbot.service.messageServiceImpl.BindService.Companion.contains
import okhttp3.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class WsController : WebSocketListener() {
    var webSocket: WebSocket? = null

    var bindController: BindController? = null

    override fun onOpen(webSocket: WebSocket, response: Response) {
        log.info("ws link:{}", response.code)
        this.webSocket = webSocket
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        log.info("receive ws message:{}", text)
        val om = ObjectMapper()
        try {
            val data = om.readTree(text)
            if (!data.has("state") || !data.has("code") || !data.has("echo")) {
                log.info("error:argument error")
            }
            val state: Array<String?> =
                data.get("state").asText().split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val code = data.get("code").asText()
            val echo = data.get("echo").asText()
            try {
                val l = state[1]!!.toLong()
                if (state.size != 2 || !contains(l)) {
                    // 不响应任何
                    log.error("no find key")
                    return
                }
            } catch (e: NumberFormatException) {
                log.error("parse error", e)
                return
            }
            if (bindController != null) {
                val resp: String = bindController!!.saveBind(code, state[1]!!)
                val p = HashMap<String?, String?>()
                p.put("response", resp)
                p.put("echo", echo)
                webSocket.send(om.writeValueAsString(p))
                log.info("bind over -> {}", resp)
            } else {
                log.error("ws error:init")
            }
        } catch (e: JsonProcessingException) {
            log.error("ws error:not json")
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
        try {
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
        log.info("ws 重连中")
        log.error("{}\n{}", code, reason)
        client.newWebSocket(req, this)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        try {
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
        log.info("ws 重连中", t)
        log.error("{}", response?.body)
        client.newWebSocket(req, this)
    }

    fun setMsgController(BindController: BindController?) {
        this.bindController = BindController
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(WsController::class.java)

        var ws: WsController? = null

        var client: OkHttpClient = OkHttpClient.Builder()
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .build()
        var req: Request = Request.Builder().url("ws://1.116.209.39:20007").build()

        val instance: WsController?
            get() {
                if (ws != null) {
                    return ws
                }

                ws = WsController()
                client.newWebSocket(req, ws!!)
                return ws
            }
    }
}
