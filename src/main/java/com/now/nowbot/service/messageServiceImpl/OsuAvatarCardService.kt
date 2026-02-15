package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import org.springframework.stereotype.Service

@Service("OSU_AVATAR_CARD")
class OsuAvatarCardService(
    private val userApiService: OsuUserApiService,
    private val bindDao: BindDao,
    private val imageService: ImageService,
) : MessageService<OsuUser>,
    TencentMessageService<OsuUser> {

    data class UserAvatarCardParam(
        var banner: String? = null,
        var avatar: String? = null,
        var color: String? = null,
        var name: String? = null,
    ) {
        constructor(user: OsuUser): this(
            user.coverUrl,
            user.avatarUrl,
            "hsl(${user.profileHue},60%,50%)",
            user.username,
        )
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<OsuUser>
    ): Boolean {
        val matcher = Instruction.OSU_AVATAR_PROFILE.matcher(messageText)
        if (!matcher.find()) return false

        val u = bindDao.getBindFromQQ(event.sender.contactID)
        data.value = userApiService.getOsuUser(u)
        return true
    }

    override fun handleMessage(event: MessageEvent, param: OsuUser): ServiceCallStatistic? {
        val p = UserAvatarCardParam(param)

        event.reply(imageService.getPanel(p, "Zeta"))

        return ServiceCallStatistic.build(event, userID = param.userID)
    }

    override fun accept(event: MessageEvent, messageText: String): OsuUser? {
        if (!OfficialInstruction.OSU_AVATAR_PROFILE.matcher(messageText).find()) return null
        val u = bindDao.getBindFromQQ(event.sender.contactID)
        return userApiService.getOsuUser(u)
    }

    override fun reply(event: MessageEvent, param: OsuUser): MessageChain? {
        val p = UserAvatarCardParam(param)
        return MessageChain.MessageChainBuilder().addImage(imageService.getPanel(p, "Zeta")).build()
    }
}
