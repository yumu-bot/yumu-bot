package com.now.nowbot.qq.tencent

import com.yumu.Listener
import com.yumu.WebsocketAdapter
import com.yumu.model.WebsocketPackage


object TencentAdapter : WebsocketAdapter() {
    val listener = mutableListOf<Listener>()
    override suspend fun send(message: WebsocketPackage<*>) {
        throw Exception("啊, 我来发消息?")
    }

    override fun request(message: Listener) {
        listener.add(message)
    }
}