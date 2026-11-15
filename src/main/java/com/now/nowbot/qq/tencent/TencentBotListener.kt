package com.now.nowbot.qq.tencent

import com.github.benmanes.caffeine.cache.Caffeine
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.restrict.RestrictImplement
import com.yumu.qq.bot.EventListener
import com.yumu.qq.bot.OpenAPI
import com.yumu.qq.enums.WebsocketIntents
import com.yumu.qq.event.GroupMessageEvent
import com.yumu.qq.event.InteractionEvent
import com.yumu.qq.event.UserMessageEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Duration
import com.yumu.qq.model.Event as TencentEvent

object TencentBotListener : EventListener {
    private val log = LoggerFactory.getLogger(TencentBotListener::class.java)
    private var selfId: String? = null
    private val handledMessages = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(5))
        .build<String, Boolean>()

    @Volatile
    var safeOpenApi: OpenAPI? = null

    override var api: OpenAPI
        get() = checkNotNull(safeOpenApi) { "API not initialized" }
        set(value) {
            onSelfHosting(value)
        }


    override val intents: Int = WebsocketIntents.GROUP_AND_C2C_EVENT.value or WebsocketIntents.INTERACTION.value

    override fun setSelfId(id: String) {
        selfId = id.takeIf { it.isNotBlank() }
    }

    override suspend fun onGroupMessage(wsPackage: TencentEvent<GroupMessageEvent>) {
        val message = wsPackage.data ?: return
        if (message.author.isBot) return
        var content = message.content.trimStart()
        val id = selfId
        if (id != null) {
            content = content.removePrefix("<@$id>")
                .removePrefix("<@!$id>")
                .removePrefix("\\u003c@$id\\u003e")
                .removePrefix("\\u003c@!$id\\u003e")
        } else if (wsPackage.type == GroupMessageEvent.KEY_AT) {
            content = content.replaceFirst(Regex("^(?:<@!?[^>]+>|\\\\u003c@!?.+?\\\\u003e)"), "")
        }
        handleMessage(content, message.groupOpenID, message.id, true)
    }

    override suspend fun onInteraction(wsPackage: TencentEvent<InteractionEvent>) {

    }

    override suspend fun onUserMessage(wsPackage: TencentEvent<UserMessageEvent>) {
        val message = wsPackage.data ?: return
        handleMessage(message.content, message.userOpenID, message.id, false)
    }

    private suspend fun handleMessage(content: String, openID: String, messageID: String, isGroup: Boolean) {
        val command = content.trim()
        if (command.isBlank() || openID.isBlank() || messageID.isBlank()) return
        val key = "$isGroup:$openID:$messageID"
        if (handledMessages.asMap().putIfAbsent(key, true) != null) return
        val sender = TencentMessageSender(api, openID, messageID, isGroup)
        withContext(Dispatchers.IO) {
            val send: (MessageChain) -> Unit = { chain ->
                runBlocking { sender.send(chain) }
            }
            val contact = Contact(0, sendChain = send) {}
            val event = Event(contact, command)
            try {
                RestrictImplement.onTencentMessage(event) { reply ->
                    reply?.let(send)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.error("腾讯消息处理失败, messageID={}", messageID, e)
            }
        }
    }

    fun onSelfHosting(api: OpenAPI) {
        safeOpenApi = api
        TencentAdapter.registerSelfHosting()
    }
}