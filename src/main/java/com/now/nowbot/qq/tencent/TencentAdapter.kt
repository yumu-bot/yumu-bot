package com.now.nowbot.qq.tencent

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.controller.QQBotWebsocket
import com.now.nowbot.util.JacksonUtil
import com.yumu.WebsocketAdapter
import com.yumu.model.WebsocketPackage

typealias Listener = (suspend (WebsocketPackage<JsonNode>) -> Unit)

object TencentAdapter : WebsocketAdapter() {
    val listener = mutableListOf<Listener>()
    override suspend fun send(message: WebsocketPackage<*>) {
        QQBotWebsocket.sendMessage(JacksonUtil.toJson(message))
    }

    fun sendWithoutSuspend(message: WebsocketPackage<*>) {
        QQBotWebsocket.sendMessage(JacksonUtil.toJson(message))
    }

    override fun request(message: Listener) {
        listener.add(message)
    }
}