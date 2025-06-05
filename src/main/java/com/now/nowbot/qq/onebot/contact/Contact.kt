package com.now.nowbot.qq.onebot.contact

import com.mikuac.shiro.common.utils.MsgUtils
import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.dto.action.common.ActionData
import com.mikuac.shiro.dto.action.common.MsgId
import com.mikuac.shiro.dto.action.response.LoginInfoResp
import com.mikuac.shiro.exception.ShiroException
import com.mikuac.shiro.exception.ShiroException.SendMessageException
import com.mikuac.shiro.exception.ShiroException.SessionCloseException
import com.now.nowbot.config.OneBotConfig
import com.now.nowbot.qq.contact.Contact
import com.now.nowbot.qq.message.*
import com.now.nowbot.qq.message.Message.JsonMessage
import com.now.nowbot.qq.onebot.OneBotMessageReceipt
import com.now.nowbot.throwable.botRuntimeException.LogException
import com.now.nowbot.util.QQMsgUtil

open class Contact(@JvmField var bot: Bot?, @JvmField val id: Long) : Contact {
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

    override fun sendMessage(msg: MessageChain): OneBotMessageReceipt {
        try {
            ifNewBot
        } catch (e: NullPointerException) {
            Contact.log.error("获取 bot 信息为空, 可能为返回数据超时, 但是仍然尝试发送")
        } catch (e: LogException) {
            Contact.log.error("无法获取 bot, 放弃发送消息：{}", msg.rawMessage, e)
        }

        val id: Long
        val d: ActionData<MsgId?>?

        when (this) {
            is Group -> {
                d = bot!!.customRawRequest(
                    { "send_group_msg" }, mapOf<String, Any>(
                        "group_id" to getId(),
                        "message" to getMsgJson(msg),
                        "auto_escape" to false
                    ), MsgId::class.java
                )
                id = getId()
            }

            is GroupContact -> {
                d = bot!!.customRawRequest(
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
                d = bot!!.customRawRequest(
                    { "send_private_msg" }, mapOf<String, Any>(
                        "user_id" to getId(),
                        "message" to getMsgJson(msg),
                        "auto_escape" to false
                    ), MsgId::class.java
                )
                id = getId()
            }
        }

        if (d != null && d.data != null && d.data!!.messageId != null) {
            return OneBotMessageReceipt.create(bot, d.data!!.messageId, this)
        } else {
            if (msg.messageList.first() is ImageMessage) {
                Contact.log.error(
                    "发送消息：账号 {} 在 {} 发送图片时获取回执失败。发送图片状态：{}",
                    bot!!.selfId,
                    id,
                    bot!!.canSendImage()?.status ?: "无法获取"
                )
            } else {
                Contact.log.error("发送消息：账号 {} 在 {} 发送 {} 时获取回执失败。", bot!!.selfId, id, getMsg4Chain(msg))
            }

            return OneBotMessageReceipt.create()
        }
    }

    private fun testBot(): Boolean {
        val info: ActionData<LoginInfoResp> = try {
            bot!!.loginInfo
        } catch (e: NullPointerException) {
            Contact.log.error("Shiro 框架：无法获取 Bot 实例的登录信息", e)
            return false
        } catch (e: ShiroException) {
            Contact.log.error("Shiro 框架异常", e)
            return false
        } catch (e: Exception) {
            Contact.log.error("未知异常", e)
            return false
        }

        try {
            info.data
        } catch (e: SendMessageException) {
            Contact.log.error("Shiro 框架：发送消息失败", e)
            return false
        } catch (e: SessionCloseException) {
            Contact.log.error("Shiro 框架：失去与 Bot 实例的连接", e)
            return false
        } catch (e: NullPointerException) {
            // com.now.nowbot.qq.contact.Contact
            // 获取bot信息为空, 可能为返回数据超时, 但是仍然尝试发送
            return false
        } catch (e: Exception) {
            Contact.log.error("test bot only", e)
            return false
        }

        return true
    }

    private val ifNewBot: Unit
        get() {
            if (testBot()) {
                return
            } else if (OneBotConfig.getBotContainer().robots.containsKey(bot!!.selfId)) {
                bot = OneBotConfig.getBotContainer().robots[bot!!.selfId]
                if (testBot()) { return }
            }
            // 移除冗余
            throw LogException("当前 bot 离线, 且未找到代替 bot")
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
