package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.model.JsonData.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.throwable.ServiceException.MiniCardException
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithOutRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean

@Service("INFO_CARD")
class InfoCardService(
    val imageService: ImageService,
) : MessageService<OsuUser>, TencentMessageService<OsuUser> {

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<OsuUser>): Boolean {
        val matcher = Instruction.INFO_CARD.matcher(messageText)
        if (!matcher.find()) return false

        val mode = getMode(matcher)
        val user = getUserWithOutRange(event, matcher, mode)

        data.value = user
        return true
    }

    override fun HandleMessage(event: MessageEvent, osuUser: OsuUser) {
        val image: ByteArray

        try {
            image = imageService.getPanelGamma(osuUser)
        } catch (e: Exception) {
            log.error("迷你信息面板：渲染失败", e)
            throw MiniCardException(MiniCardException.Type.MINI_Render_Error)
        }

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("迷你信息面板：发送失败", e)
            throw MiniCardException(MiniCardException.Type.MINI_Send_Error)
        }
    }

    override fun accept(event: MessageEvent, messageText: String): OsuUser? {
        val matcher = OfficialInstruction.INFO_CARD.matcher(messageText)
        return if (matcher.find()) {
            val mode = getMode(matcher)
            getUserWithOutRange(event, matcher, mode)
        } else {
            null
        }
    }

    override fun reply(event: MessageEvent, param: OsuUser): MessageChain? = QQMsgUtil.getImage(imageService.getPanelGamma(param))

    companion object {
        private val log: Logger = LoggerFactory.getLogger(InfoCardService::class.java)
    }
}
