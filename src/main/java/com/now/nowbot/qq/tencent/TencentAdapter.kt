package com.now.nowbot.qq.tencent

import com.yumu.Listener
import com.yumu.WebsocketAdapter
import com.yumu.Yumu
import com.yumu.model.WebsocketPackage


object TencentAdapter : WebsocketAdapter() {
    val listener = mutableListOf<Listener>()

    @Synchronized
    fun registerServer() {
        if (Yumu.adapter is SelfHostingTencentAdapter || listener.isNotEmpty()) return
        Yumu.registerServer(this, YumuServer)
    }

    @Synchronized
    fun registerSelfHosting() {
        if (Yumu.adapter is SelfHostingTencentAdapter) return
        Yumu.registerServer(SelfHostingTencentAdapter(), YumuServer)
    }

    override suspend fun send(message: WebsocketPackage<*>) {
        throw Exception("啊, 我来发消息?")
    }

    override fun request(message: Listener) {
        listener.add(message)
    }
}