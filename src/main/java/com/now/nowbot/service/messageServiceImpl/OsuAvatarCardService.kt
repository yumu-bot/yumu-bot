package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.service.UserAvatarCardParam
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import org.springframework.stereotype.Service

@Service("OSU_AVATAR_CARD")
class OsuAvatarCardService(
    private val userApiService: OsuUserApiService,
    private val bindDao: BindDao,
    private val imageService: ImageService,
) : MessageService<UserAvatarCardParam>,
    TencentMessageService<UserAvatarCardParam> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<UserAvatarCardParam>
    ): Boolean {
        val matcher = Instruction.OSU_AVATAR_CARD.matcher(messageText)
        if (!matcher.find()) return false
        val u = bindDao.getUserFromQQ(event.sender.id) ?: throw TipsException("请先绑定账号")
        val param = UserAvatarCardParam(userApiService.getPlayerInfo(u))
        data.value = param
        return true
    }

    override fun HandleMessage(event: MessageEvent, data: UserAvatarCardParam) {
        val image = imageService.getUserAvatarCard(data)
        event.subject.sendImage(image)
    }

    override fun accept(event: MessageEvent, messageText: String): UserAvatarCardParam? {
        if (!OfficialInstruction.OSU_AVATAR_CARD.matcher(messageText).find()) return null
        val u = bindDao.getUserFromQQ(event.sender.id) ?: throw TipsException("请先绑定账号")
        return UserAvatarCardParam(userApiService.getPlayerInfo(u))
    }

    override fun reply(event: MessageEvent, param: UserAvatarCardParam): MessageChain? {
        return MessageChain.MessageChainBuilder().addImage(imageService.getUserAvatarCard(param)).build()
    }
}
