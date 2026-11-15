package com.now.nowbot.qq.tencent

import com.yumu.qq.bot.EventListener
import com.yumu.qq.bot.OpenAPI
import com.yumu.qq.enums.WebsocketIntents
import com.yumu.qq.event.GroupMessageEvent
import com.yumu.qq.event.InteractionEvent
import com.yumu.qq.event.UserMessageEvent
import com.yumu.qq.model.Event

object TencentBotListener : EventListener {
    var safeOpenApi: OpenAPI? = null

    override var api: OpenAPI
        get() = checkNotNull(safeOpenApi) { "API not initialized" }
        set(value) {
            safeOpenApi = value
            onSelfHosting(value)
        }


    override val intents: Int = WebsocketIntents.GROUP_AND_C2C_EVENT.value or WebsocketIntents.INTERACTION.value

    override suspend fun onGroupMessage(wsPackage: Event<GroupMessageEvent>) {

    }

    override suspend fun onInteraction(wsPackage: Event<InteractionEvent>) {

    }

    override suspend fun onUserMessage(wsPackage: Event<UserMessageEvent>) {

    }

    fun onSelfHosting(api: OpenAPI) {

    }
}