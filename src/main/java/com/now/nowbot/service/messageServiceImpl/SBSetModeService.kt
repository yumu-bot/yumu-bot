package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.SBBindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.sbApiService.SBUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.command.FLAG_MODE
import org.springframework.stereotype.Service

@Service("SB_SET_MODE")
class SBSetModeService (
    private val bindDao: BindDao,
    private val userApiService: SBUserApiService,
): MessageService<OsuMode>, TencentMessageService<OsuMode> {

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<OsuMode>): Boolean {
        val m = Instruction.SB_SET_MODE.matcher(messageText)
        if (m.find()) {
            data.value = OsuMode.getMode(m.group(FLAG_MODE))
            return true
        } else return false
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: OsuMode) {
        val user = bindDao.getSBBindFromQQ(event.sender.id, true)
        event.reply(getReply(param, event, user))
    }

    override fun accept(event: MessageEvent, messageText: String): OsuMode? {
        val m = OfficialInstruction.SB_SET_MODE.matcher(messageText)
        if (m.find()) return OsuMode.getMode(m.group(FLAG_MODE))

        return null
    }

    @Throws(Throwable::class)
    override fun reply(event: MessageEvent, param: OsuMode): MessageChain? {
        val user = try {
            bindDao.getSBBindFromQQ(-event.sender.id, true)
        } catch (e: BindException) {
            val sbUser = userApiService.getUser(-event.sender.id) ?: throw GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerUnknown)
            val bindUser = SBBindUser(sbUser)

            bindDao.saveBind(bindUser)!!
        }

        return getReply(param, event, user)
    }

    private fun getReply(mode: OsuMode, event: MessageEvent, user: SBBindUser): MessageChain {
        val predeterminedMode = bindDao.getGroupModeConfig(event)

        val info = if (mode == OsuMode.DEFAULT) {
            if (user.mode.isDefault()) {
                throw TipsException("你没有已绑定的游戏模式。\n请输入 0(osu) / 1(taiko) / 2(catch) / 3(mania) / 4(osu relax) / 5(taiko relax) / 6(catch relax) / 8(osu autopilot) 来修改绑定的游戏模式。")
            } else {
                if (predeterminedMode.isDefault()) {
                    "已移除绑定的游戏模式 ${user.mode.fullName}。"
                } else {
                    "已移除绑定的游戏模式 ${user.mode.fullName}。\n当前群聊绑定的游戏模式为：${predeterminedMode.fullName}。"
                }
            }
            // return MessageChain("未知的游戏模式。请输入 0(osu) / 1(taiko) / 2(catch) / 3(mania)")
        } else if (mode.isEqualOrDefault(user.mode)) {
            if (predeterminedMode.isDefault()) {
                "已将绑定的游戏模式修改为：${mode.fullName}。"
            } else {
                "已将绑定的游戏模式修改为：${mode.fullName}。\n当前群聊绑定的游戏模式为：${predeterminedMode.fullName}。"
            }
        } else {
            if (predeterminedMode.isDefault()) {
                "已将绑定的游戏模式 ${user.mode.fullName} 修改为：${mode.fullName}。"
            } else {
                "已将绑定的游戏模式 ${user.mode.fullName} 修改为：${mode.fullName}。\n当前群聊绑定的游戏模式为：${predeterminedMode.fullName}。"
            }
        }

        bindDao.updateSBMode(user.userID, mode)

        return MessageChain(info)
    }
}
