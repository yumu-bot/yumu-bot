package com.now.nowbot.qq.onebot.contact

import com.mikuac.shiro.common.utils.MsgUtils
import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.dto.action.common.ActionData
import com.mikuac.shiro.dto.action.common.MsgId
import com.now.nowbot.qq.contact.Contact
import com.now.nowbot.qq.message.*
import com.now.nowbot.qq.message.Message.JsonMessage
import com.now.nowbot.qq.onebot.BotManager
import com.now.nowbot.qq.onebot.OneBotMessageReceipt
import com.now.nowbot.util.QQMsgUtil

open class Contact(@JvmField var botId: Long, @JvmField val id: Long) : Contact {
    private var username: String? = null

    override fun getId(): Long {
        return id
    }

    override fun getName(): String {
        return username!!
    }

    protected fun setName(name: String?) {
        this.username = name
    }

    override fun sendMessage(msg: MessageChain): MessageReceipt {
        try {
            val result = sendMessageBox(msg)
            if (result == null) {
                BotManager.updateStatus(id, false)
                return OneBotMessageReceipt.create()
            } else {
                BotManager.updateStatus(id, true)
                return result
            }
        } catch (e: Exception) {
            BotManager.updateStatus(id, false)
            throw e
        }
    }

    private fun sendMessageBox(msg: MessageChain): MessageReceipt? {
        val id: Long
        val d: ActionData<MsgId?>?
        val bot: Bot
        when (this) {
            is Group -> {
                bot = BotManager.getBestBot(this.id) ?: return NoneMessageReceipt()
                d = bot.customRawRequest(
                    { "send_group_msg" }, mapOf<String, Any>(
                        "group_id" to getId(),
                        "message" to getMsgJson(msg),
                        "auto_escape" to false
                    ), MsgId::class.java
                )
                id = getId()
            }

            is GroupContact -> {
                bot = BotManager.getBestBot(this.getGroupId()) ?: return NoneMessageReceipt()
                d = bot.customRawRequest(
                    { "send_private_msg" }, mapOf<String, Any>(
                        "group_id" to this.getGroupId(),
                        "user_id" to this.getId(),
                        "message" to getMsgJson(msg),
                        "auto_escape" to false
                    ), MsgId::class.java
                )
                id = this.groupId
            }

            else -> {
                throw UnsupportedOperationException("not supported")
            }
        }
        if (d != null && d.data != null && d.data!!.messageId != null) {
            return OneBotMessageReceipt.create(botId, d.data!!.messageId, this)
        } else {
            if (msg.messageList.first() is ImageMessage) {
                Contact.log.error(
                    "发送消息：账号 {} 在 {} 发送图片时获取回执失败。发送图片状态：{}",
                    bot.selfId,
                    id,
                    bot.canSendImage()?.status ?: "无法获取"
                )
            } else {
                Contact.log.error("发送消息：账号 {} 在 {} 发送 {} 时获取回执失败。", bot.selfId, id, getMsg4Chain(msg))
            }
            return null
        }
    }

    private fun testBot(): Boolean {
        return BotManager.getBestBot(this.id) == null
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
