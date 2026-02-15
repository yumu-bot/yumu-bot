package com.now.nowbot.service.messageServiceImpl

import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.aop.ServiceLimit
import com.now.nowbot.config.NewbieConfig
import com.now.nowbot.config.Permission
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.contact.Contact
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.event.getMessageFromStringOrArray
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.onebot.contact.Friend
import com.now.nowbot.qq.onebot.contact.Group
import com.now.nowbot.service.MessageService
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_ANY
import com.now.nowbot.util.command.FLAG_QQ_GROUP
import com.now.nowbot.util.command.FLAG_QQ_ID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.regex.Matcher

@Service("ECHO")
class EchoService(
    private val botContainer: BotContainer,
    config: NewbieConfig,
): MessageService<EchoService.EchoParam> {
    data class EchoParam(val contact: Contact, val message: MessageChain)

    private val executorBotID = config.yumuBot

    @Throws(UnsupportedOperationException::class)
    private fun getParam(event: MessageEvent, matcher: Matcher, botContainer: BotContainer): EchoParam {
        val qq = matcher.group(FLAG_QQ_ID)?.toLongOrNull()
        val groupQQ = matcher.group(FLAG_QQ_GROUP)?.toLongOrNull()

        val any = (matcher.group(FLAG_ANY) ?: "").trim()

        val bot = botContainer.robots[executorBotID]
            ?: throw UnsupportedOperationException.BotOperation.Offline()

        val contact: Contact
        var message: MessageChain

        if (event.hasReply()) {
            val replyID = event.replyMessage!!.id

            message = bot.getMessageFromStringOrArray(replyID.toInt())
                ?: throw UnsupportedOperationException.BotOperation.MessageUnavailable(replyID)
        } else {
            message = MessageChain.MessageChainParser.parse(any)
        }

        if (qq != null) {
            val friend = bot.friendList?.data?.firstOrNull { it.userId == qq }
                ?: throw UnsupportedOperationException.BotOperation.NotFriend(qq)

            // 因为 QQ 需要 qq= 判断，所以一般来说不需要下面的 group 判定逻辑（找不到就是输错了）

            contact = Friend(bot, friend.userId, friend.nickname)
        } else if (groupQQ != null) {
            val group = bot.getGroupInfo(groupQQ, false)?.data

            if (group == null) {
                if (any.isBlank()) {
                    throw UnsupportedOperationException.BotOperation.NotInGroup(event.subject.contactID)
                }

                message = MessageChain.MessageChainParser.parse(any)
                contact = event.subject
            } else {
                contact = Group(bot, group.groupId, group.groupName)
            }
        } else {
            contact = event.subject
        }

        return EchoParam(contact, message)
    }

    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<EchoParam>): Boolean {
        val m = Instruction.ECHO.matcher(messageText)
        if (!m.find()) {
            return false
        }

        if (!Permission.isSuperAdmin(event)) {
            return false
        }

        data.value = getParam(event, m, botContainer)

        return true
    }

    @ServiceLimit(cooldownMillis = 15000)
    @CheckPermission(isSuperAdmin = true)
    override fun handleMessage(event: MessageEvent, param: EchoParam): ServiceCallStatistic? {
        param.contact.sendMessage(param.message)

        return ServiceCallStatistic.building(event)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(EchoService::class.java)
    }
}
