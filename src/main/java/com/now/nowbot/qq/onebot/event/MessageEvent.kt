package com.now.nowbot.qq.onebot.event

import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.dto.event.message.GroupMessageEvent
import com.mikuac.shiro.dto.event.message.MessageEvent
import com.mikuac.shiro.enums.MsgTypeEnum
import com.mikuac.shiro.model.ArrayMsg
import com.now.nowbot.qq.message.*
import com.now.nowbot.qq.onebot.contact.Contact
import com.now.nowbot.qq.onebot.contact.Group
import java.net.MalformedURLException
import java.net.URI

open class MessageEvent(val event: MessageEvent, bot: Bot?) : Event(bot?.selfId ?: 0),
    com.now.nowbot.qq.event.MessageEvent {
    override fun getSubject(): Contact {
        if (event is GroupMessageEvent) {
            return Group(bot.botID, event.groupId)
        }
        return Contact(bot.botID, event.userId)
    }

    override fun getSender(): Contact {
        return if (event is GroupMessageEvent) {
            Group(bot.botID, event.sender.userId)
        } else {
            Contact(bot.botID, event.userId)
        }
    }

    override fun getMessage(): MessageChain {
        return getMessageChain(event.arrayMsg)
    }

    override fun getRawMessage(): String {
        return event.rawMessage
    }

    override fun getTextMessage(): String {
        return message.messageList
            .filterIsInstance<TextMessage>()
            .joinToString("") { it.toString() }
    }

    companion object {

        @JvmStatic
        fun getMessageChain(msgs: List<ArrayMsg>): MessageChain {
            val msg = msgs.map {
                return@map when (it.type) {
                    MsgTypeEnum.at -> {
                        val qqStr = it.getStringData("qq")

                        //艾特全体是 -1。扔过来的可能是 "all"
                        AtMessage(qqStr.toLongOrNull() ?: -1L)
                    }

                    MsgTypeEnum.text -> TextMessage(
                        it.getStringData("text")
                    )

                    MsgTypeEnum.reply -> ReplyMessage(
                        it.getLongData("id"),
                        it.getStringData("text")
                    )

                    MsgTypeEnum.image -> {
                        try {
                            ImageMessage(URI(it.data.get("url").asText("")).toURL())
                        } catch (e: MalformedURLException) {
                            TextMessage("[图片;加载异常]")
                        } catch (e: IllegalArgumentException) {
                            TextMessage("[图片;加载异常]")
                        }
                    }

                    else -> TextMessage(
                        String.format("[%s;不支持的操作类型]", it.type ?: MsgTypeEnum.unknown)
                    )
                }
            }

            return MessageChain(msg)
        }
    }
}
