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
        get() = getMessageChain(
            event.arrayMsg,
            bot.trueBot,
            (event as? GroupMessageEvent)?.groupId,
        )

    override val rawMessage: String
        get() = event.rawMessage

    override val textMessage: String
        get() = message.messageList
            .filterIsInstance<TextMessage>()
            .joinToString("") { it.toString() }

    companion object {

        fun getMessageChain(
            messages: List<ArrayMsg>,
            bot: Bot? = null,
            groupID: Long? = null,
        ): MessageChain {
            val msg = messages.map {
                // shiro 的 MsgTypeEnum 没有 file，只能按原始类型名判断
                if (it.rawType?.equals("file", ignoreCase = true) == true) {
                    return@map parseFile(it, bot, groupID)
                }

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

                    else -> {
                        val message = "[${it.type ?: MsgTypeEnum.unknown}]: 不支持的操作类型"
                        //val detail = "$message\n${it.data.toPrettyString()}"

                        //com.now.nowbot.qq.contact.Contact.log.info(detail)

                        TextMessage(message)
                    }
                }
            }

            return MessageChain(msg)
        }

        /**
         * 解析 file 段。
         *
         * url 有时不会直接给出，此时对群文件调用 get_group_file_url 补全。
         * busid 缺省为 102（群文件）。
         */
        private fun parseFile(msg: ArrayMsg, bot: Bot?, groupID: Long?): FileMessage {
            val fileID = msg.getStringData("file_id")
                .ifBlank { msg.getStringData("file") }
            val busID = msg.getLongData("busid").toInt().let { if (it > 0) it else 102 }
            val name = msg.getStringData("file_name")
                .ifBlank { msg.getStringData("file").substringAfterLast('/').substringAfterLast('\\') }

            val url = msg.getStringData("url").takeIf { it.isNotBlank() }
                ?: run {
                    if (bot == null || groupID == null || fileID.isBlank()) return@run null

                    try {
                        bot.getGroupFileUrl(groupID, fileID, busID)?.data?.url
                    } catch (_: Exception) {
                        null
                    }
                }

            return FileMessage(
                name = name.ifBlank { null },
                url = url,
                fileID = fileID.ifBlank { null },
                busID = busID,
                size = msg.getLongData("file_size"),
            )
        }

//        private fun decode(m: String): String {
//            return ShiroUtils.unescape(m)
//        }

        private fun decodeArr(m: String): String {
            return m
        }
    }
}
