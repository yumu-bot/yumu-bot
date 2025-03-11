package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.CmdObject
import com.now.nowbot.util.CmdUtil.getUserWithoutRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean

@Service("BADGE") class BadgeService(
    private val imageService: ImageService,
) : MessageService<OsuUser>, TencentMessageService<OsuUser> {
    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<OsuUser>): Boolean {
        val m = Instruction.BADGE.matcher(messageText)
        if (!m.find()) {
            return false
        }

        val isMyself = AtomicBoolean(false)

        val user = getUserWithoutRange(event, m, CmdObject(OsuMode.DEFAULT), isMyself = isMyself)

        if (user.badges.isNullOrEmpty()) {
            if (isMyself.get()) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Badge, "你")
            } else {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Badge, "对方")
            }
        }

        data.value = user
        return true
    }

    override fun HandleMessage(event: MessageEvent, param: OsuUser) {
        val image = getImage(param)

        try {
            event.reply(image)
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "奖牌信息")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): OsuUser? {
        val m = OfficialInstruction.BADGE.matcher(messageText)
        if (!m.find()) {
            return null
        }

        val isMyself = AtomicBoolean(false)

        val user = getUserWithoutRange(event, m, CmdObject(OsuMode.DEFAULT), isMyself = isMyself)

        if (user.badges.isNullOrEmpty()) {
            if (isMyself.get()) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Badge, "你")
            } else {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Badge, "对方")
            }
        }

        return user
    }

    override fun reply(event: MessageEvent, param: OsuUser): MessageChain? {
        return QQMsgUtil.getImage(getImage(param))
    }

    private fun getImage(user: OsuUser): ByteArray {
        return try {
            imageService.getPanel(mapOf("user" to user), "A10")
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "奖牌信息")
        }
    }
}