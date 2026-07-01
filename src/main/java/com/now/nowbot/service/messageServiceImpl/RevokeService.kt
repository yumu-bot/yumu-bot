package com.now.nowbot.service.messageServiceImpl

import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.cache.QQMessageCacheProvider
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Service("REVOKE")
class RevokeService(
    private val botContainer: BotContainer,
    private val qqMessageCacheProvider: QQMessageCacheProvider,
): MessageService<RevokeService.RevokeParam> {
    data class RevokeParam(
        val bot: Bot,
        val messageID: Long
    )

    private fun revokeLatest(event: MessageEvent): RevokeParam {
        val messages = qqMessageCacheProvider.getBotMessagesByGroup(event.subject.contactID)

        val latestMessage = messages.lastOrNull()
            ?: throw UnsupportedOperationException.BotOperation.LatestMessageUnavailable()
        val targetBotID = latestMessage.sender.userId

        val messageID = latestMessage.messageId?.toLong() ?: throw UnsupportedOperationException.BotOperation.MessageIDUnavailable()

        val targetBot = botContainer.robots[targetBotID]

        val subjectBot: Bot

        if (targetBot != null) {
            // 可以自己撤回
            subjectBot = targetBot
        } else {
            // 需要别人来撤回
            val recordedBotIDs = messages.mapNotNull { it.sender.userId }.toSet()

            val group = (event.subject as? Group)
                ?: throw UnsupportedOperationException.BotOperation.BotOffline()

            val availableBots = group.allUser.filter { it.contactID in recordedBotIDs }.ifEmpty {
                throw UnsupportedOperationException.BotOperation.BotOffline()
            }

            val roleBot = availableBots
                .filter { bot -> bot.contactID in botContainer.robots.keys && (bot.role == Role.ADMIN || bot.role == Role.OWNER) }
                .minByOrNull { bot ->
                    when (bot.role) {
                        Role.OWNER -> 0
                        Role.ADMIN -> 1
                        Role.MEMBER -> 2
                        else -> 3
                    }
                } ?: throw UnsupportedOperationException.BotOperation.NoRoledBot()

            // 如果不是成员，可以无视 2 分钟的限制

            val now = System.currentTimeMillis().milliseconds
            val time = latestMessage.time.milliseconds

            // 如果不是成员，可以无视 2 分钟的限制
            if (now - time > 120.seconds && roleBot.role == Role.MEMBER) {
                throw UnsupportedOperationException.BotOperation.Overtime()
            }

            subjectBot = botContainer.robots[roleBot.contactID]
                ?: throw UnsupportedOperationException.BotOperation.RoledBotOffline(roleBot.contactID)
        }

        return RevokeParam(subjectBot, messageID)
    }

    private fun revokeReply(event: MessageEvent, replyID: Long): RevokeParam {
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

        val response = bot.getResponseFromStringOrArray(replyID.toInt())

        val sender = response.sender ?: throw UnsupportedOperationException.BotOperation.SenderUnavailable()

        val senderID = sender.userId

        testRole(event, botID, senderID, botRole, Role.getRole(sender.role))

        val now = System.currentTimeMillis().milliseconds

        val time = response.time?.seconds ?: now

        // 如果不是成员，可以无视 2 分钟的限制
        if (now - time > 120.seconds && botRole == Role.MEMBER) {
            throw UnsupportedOperationException.BotOperation.Overtime()
        }

        return RevokeParam(bot, replyID)
    }



    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<RevokeParam>
    ): Boolean {
        val m = Instruction.REVOKE.matcher(messageText)
        if (!m.find()) {
            return false
        }

        data.value = if (event.hasReply()) {
            revokeReply(event, event.replyMessage!!.id)
        } else {
            revokeLatest(event)
        }

        return true
    }

    private fun testRole(
        event: MessageEvent, executorID: Long, executedID: Long, executorRole: Role, executedRole: Role
    ) {
        val requestID = event.sender.contactID

        if (executorID != executedID) {
            // 只有管理员及以上才有权撤回他人消息
            if (!Permission.isGroupAdmin(event) && requestID != executedID) {
                throw PermissionException.GroupException.BelowGroupOwner()
            }

            when (executorRole) {
                // 机器人是管理员：只能撤回普通成员的消息
                Role.ADMIN -> {
                    if (executedRole == Role.OWNER) {
                        throw PermissionException.GroupException.NotGroupOwner()
                    }

                    if (executedRole == Role.ADMIN) {
                        throw PermissionException.GroupException.BelowGroupOwner()
                    }
                }

                // 机器人是普通成员：无法撤回他人消息
                Role.MEMBER -> throw PermissionException.GroupException.BelowGroupAdministrator()

                // 机器人是群主：拥有最高权限，可以撤回任何人（除了自己，已在上方处理）
                else -> {}
            }
        }
    }

    override fun handleMessage(
        event: MessageEvent,
        param: RevokeParam
    ): ServiceCallStatistic? {

        val operation = runCatching {
            param.bot.deleteMsg(param.messageID.toInt())
        }.getOrElse {
            throw IllegalStateException.Revoke(param.messageID.toString())
        }

        log.info("""
            机器人 ${param.bot.selfId} 撤回消息：ID ${param.messageID}。
            状态：${operation?.status}，${operation?.retCode}，${operation?.echo}
        """.trimIndent())

        return ServiceCallStatistic.building(event)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RevokeService::class.java)
    }
}