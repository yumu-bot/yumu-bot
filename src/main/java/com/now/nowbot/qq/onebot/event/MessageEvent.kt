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

open class MessageEvent(val event: MessageEvent, bot: Bot): Event(bot), com.now.nowbot.qq.event.MessageEvent {
    override val subject: Contact
        get() = if (event is GroupMessageEvent) {
            Group(bot!!.trueBot, event.groupId)
        } else {
            Contact(bot!!.trueBot, event.userId)
        }

    override val sender : Contact
        get() = if (event is GroupMessageEvent) {
            Group(bot!!.trueBot, event.sender.userId)
        } else {
            Contact(bot!!.trueBot, event.userId)
        }

    override val message: MessageChain
        get() = getMessageChain(event.arrayMsg)

    override val rawMessage: String
        get() = event.rawMessage

    override val textMessage: String
        get() = message.messageList
            .filterIsInstance<TextMessage>()
            .joinToString("") { it.toString() }

    companion object {

        fun getMessageChain(messages: List<ArrayMsg>): MessageChain {
            val msg = messages.map {
                return@map when (it.type) {
                    MsgTypeEnum.at -> {
                        val qqStr = it.getStringData("qq")

                        //艾特全体是 -1。扔过来的可能是 "all"
                        AtMessage(qqStr.toLongOrNull() ?: -1L)
                    }

                    MsgTypeEnum.text -> TextMessage(
                        decodeArr(it.getStringData("text"))
                    )

                    MsgTypeEnum.reply -> ReplyMessage(
                        it.getLongData("id"),
                        decodeArr(it.getStringData("text"))
                    )

                    MsgTypeEnum.image -> {
                        try {
                            ImageMessage(URI.create(it.getStringData("url")).toURL())
                        } catch (_: MalformedURLException) {
                            TextMessage("[图片;加载异常]")
                        } catch (_: IllegalArgumentException) {
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

//        private fun decode(m: String): String {
//            return ShiroUtils.unescape(m)
//        }

        private fun decodeArr(m: String): String {
            return m
        }
    }
}
