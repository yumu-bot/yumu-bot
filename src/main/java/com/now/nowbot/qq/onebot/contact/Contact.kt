package com.now.nowbot.qq.onebot.contact

import com.mikuac.shiro.common.utils.MsgUtils
import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.dto.action.common.ActionData
import com.mikuac.shiro.dto.action.common.MsgId
import com.now.nowbot.config.OneBotConfig
import com.now.nowbot.qq.contact.Contact
import com.now.nowbot.qq.message.*
import com.now.nowbot.qq.message.Message.JsonMessage
import com.now.nowbot.qq.onebot.OneBotMessageReceipt
import com.now.nowbot.util.QQMsgUtil

open class Contact(var bot: Bot, override val contactID: Long = 0L) : Contact {

    override var name: String? = "Unknown"

    override fun sendMessage(msg: MessageChain): OneBotMessageReceipt {
        return try {
            executeSend(msg)
        } catch (e: Exception) {
            Contact.log.warn("发送消息异常，尝试恢复 Bot 实例...", e)

            if (tryRecoverBot()) {
                try {
                    executeSend(msg)
                } catch (retryEx: Exception) {
                    Contact.log.error("Bot 实例恢复后再次发送失败", retryEx)
                    OneBotMessageReceipt.create()
                }
            } else {
                Contact.log.error("当前 bot 离线, 且未找到代替 bot，放弃发送：{}", msg.rawMessage)
                OneBotMessageReceipt.create()
            }
        }
    }

    /**
     * 核心的发送逻辑（从原 sendMessage 中剥离）
     */
    private fun executeSend(msg: MessageChain): OneBotMessageReceipt {
        val id: Long
        val d: ActionData<MsgId?>?

        when (this) {
            is Group -> {
                d = bot.customRawRequest(
                    { "send_group_msg" }, mapOf<String, Any>(
                        "group_id" to contactID,
                        "message" to getMsgJson(msg),
                        "auto_escape" to false
                    ), MsgId::class.java
                )
                id = contactID
            }

            is GroupContact -> {
                d = bot.customRawRequest(
                    { "send_private_msg" }, mapOf<String, Any>(
                        "group_id" to this.groupID,
                        "user_id" to this.contactID,
                        "message" to getMsgJson(msg),
                        "auto_escape" to false
                    ), MsgId::class.java
                )
                id = this.groupID
            }

            else -> {
                d = bot.customRawRequest(
                    { "send_private_msg" }, mapOf<String, Any>(
                        "user_id" to contactID,
                        "message" to getMsgJson(msg),
                        "auto_escape" to false
                    ), MsgId::class.java
                )
                id = contactID
            }
        }

        val messageID = d?.data?.messageId

        if (messageID != null) {
            return OneBotMessageReceipt.create(bot, messageID, this)
        } else {
            val status = try {
                if (bot.canSendImage()?.status != null) "正常" else "无法获取"
            } catch (_: Exception) {
                "状态异常/掉线"
            }

            if (msg.messageList.any { it is ImageMessage }) {
                Contact.log.info("发送消息：账号 ${bot.selfId} 在 $id 发送图片时获取回执失败。发送图片状态：$status")
            } else {
                Contact.log.info("发送消息：账号 ${bot.selfId} 在 $id 发送此消息时获取回执失败：\n${getMsg4Chain(msg).take(100)}")
            }

            return OneBotMessageReceipt.create()
        }
    }

    /**
     * 替代原有的 ifNewBot 和 testBot。
     * 不再发送网络请求，只做内存级别的实例查找替换。
     */
    private fun tryRecoverBot(): Boolean {
        val container = OneBotConfig.getBotContainer().robots

        if (container.containsKey(bot.selfId)) {
            val newBot = container[bot.selfId]
            if (newBot != null) {
                this.bot = newBot
                return true
            }
        }
        return false
    }

    companion object {
        protected fun getMsg4Chain(messageChain: MessageChain): String {
            val s = messageChain.messageList

            if (s.isEmpty()) return ""

            val builder = MsgUtils.builder()
            for (message in s) {
                when (message) {
                    is ImageMessage -> {
                        if (message.isByteArray) {
                            builder.img("base64://${QQMsgUtil.byte2str(message.data)}")
                        } else {
                            builder.img(message.path)
                        }
                    }

                    is VoiceMessage -> builder.voice("base64://${QQMsgUtil.byte2str(message.data)}")

                    is AtMessage -> {
                        if (!message.isAll) {
                            builder.at(message.target)
                        } else {
                            builder.atAll()
                        }
                    }

                    is TextMessage -> builder.text(message.toString())
                    is ReplyMessage -> builder.reply(message.id.toString())
                }
            }

            return builder.build()
        }

        protected fun getMsgJson(chain: MessageChain): List<JsonMessage> {
            return chain.messageList.mapNotNull { it.toJson() }
        }
    }
}
