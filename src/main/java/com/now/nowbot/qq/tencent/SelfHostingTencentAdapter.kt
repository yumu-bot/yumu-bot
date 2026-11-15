package com.now.nowbot.qq.tencent

import com.yumu.Listener
import com.yumu.WebsocketAdapter
import com.yumu.core.extensions.toJson
import com.yumu.model.WebsocketPackage

class SelfHostingTencentAdapter : WebsocketAdapter() {
    private val listeners = mutableListOf<Listener>()

    override fun request(message: Listener) {
        listeners.add(message)
    }

    override suspend fun send(message: WebsocketPackage<*>) {
        val packet = WebsocketPackage.toNodePackage(message.toJson())
        if (packet.isResponse) {
            channels[packet.echo]?.second?.send(packet)
        } else {
            try {
                listeners.forEach { it(packet, this) }
            } catch (e: Throwable) {
                channels.remove(packet.echo)?.second?.close()
                throw e
            }
        }
    }
}
