package com.now.nowbot.service.messageServiceImpl

import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.config.Permission
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.enums.Role
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.event.getResponseFromStringOrArray
import com.now.nowbot.service.MessageService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.PermissionException
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException
import com.now.nowbot.util.Instruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service("REVOKE")
class RevokeService(private val botContainer: BotContainer): MessageService<RevokeService.RevokeParam> {
    data class RevokeParam(
        val bot: Bot,
        val messageID: Long
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<RevokeParam>
    ): Boolean {

        val m = Instruction.REVOKE.matcher(messageText)
        if (!m.find()) {
            return false
        }

        val botID = event.bot?.botID
            ?: throw UnsupportedOperationException.BotOperation.BotOffline()

        val bot = botContainer.robots[botID]
            ?: throw UnsupportedOperationException.BotOperation.BotOffline()

        val sub = event.subject

        val botRole = if (sub is Group) {
            sub.getUser(bot.selfId).role ?: Role.MEMBER
        } else {
            Role.MEMBER
        }

        val messageID = if (event.hasReply()) {
            event.replyMessage!!.id
        } else {
            throw UnsupportedOperationException.BotOperation.MustReply()
        }

        val response = bot.getResponseFromStringOrArray(messageID.toInt())

        val sender = response.sender ?: throw UnsupportedOperationException.BotOperation.SenderUnavailable()

        val senderID = sender.userId?.toLongOrNull()

        // 1. 优先排除机器人撤回自己的消息，这种情况无需任何权限检查
        if (botID != senderID) {
            // 2. 只有管理员及以上才有权撤回他人消息
            if (!Permission.isGroupAdmin(event) && event.sender.contactID != senderID) {
                return false
            }

            val targetRole = Role.getRole(sender.role)

            // 3. 核心权限校验逻辑：利用 Role 的等级比较（假设 Role 是 Enum 且有序）
            when (botRole) {
                // 机器人是管理员：只能撤回普通成员的消息
                Role.ADMIN -> {
                    if (targetRole == Role.OWNER) {
                        throw PermissionException.GroupException.NotGroupOwner()
                    }

                    if (targetRole == Role.ADMIN) {
                        throw PermissionException.GroupException.BelowGroupOwner()
                    }
                }

                // 机器人是普通成员：无法撤回他人消息
                Role.MEMBER -> throw PermissionException.GroupException.BelowGroupAdministrator()

                // 机器人是群主：拥有最高权限，可以撤回任何人（除了自己，已在上方处理）
                else -> {}
            }
        }

        val now = System.currentTimeMillis() / 1000L

        val time = response.time?.toLong() ?: now

        if (now - time > 120) {
            throw UnsupportedOperationException.BotOperation.Overtime()
        }

        data.value = RevokeParam(bot, messageID)

        return true
    }

    override fun handleMessage(
        event: MessageEvent,
        param: RevokeParam
    ): ServiceCallStatistic? {

        val operation = param.bot.deleteMsg(param.messageID.toInt())
            ?: throw IllegalStateException.Revoke(param.messageID.toString())

        log.info("""
            机器人 ${param.bot.selfId} 撤回消息：ID ${param.messageID}。
            状态：${operation.status}，${operation.retCode}，${operation.echo}
        """.trimIndent())

        return ServiceCallStatistic.building(event)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RevokeService::class.java)
    }
}