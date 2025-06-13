package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.SBBindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.SBSetModeService.SBSetModeParam
import com.now.nowbot.service.sbApiService.SBUserApiService

import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.command.FLAG_MODE
import com.now.nowbot.util.command.FLAG_NAME
import com.now.nowbot.util.command.FLAG_QQ_ID
import org.springframework.stereotype.Service

@Service("SB_SET_MODE")
class SBSetModeService (
    private val bindDao: BindDao,
    private val userApiService: SBUserApiService,
): MessageService<SBSetModeParam>, TencentMessageService<SBSetModeParam> {

    data class SBSetModeParam(val mode: OsuMode, val user: SBBindUser)

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<SBSetModeParam>): Boolean {
        val m = Instruction.SB_SET_MODE.matcher(messageText)
        if (!m.find()) return false

        val mode = OsuMode.getMode(m.group(FLAG_MODE))
        val qq = m.group(FLAG_QQ_ID)?.toLongOrNull()
        val name = m.group(FLAG_NAME)?.trim()

        val isSuper = Permission.isSuperAdmin(event.sender.id)

        data.value = if (qq != null && isSuper) {
            SBSetModeParam(mode, bindDao.getSBBindFromQQ(qq, false))
        } else if (name.isNullOrBlank().not()) {
            val id = userApiService.getUserID(name!!.trim())
                ?: throw IllegalArgumentException.WrongException.PlayerName()
            SBSetModeParam(mode, bindDao.getSBBindUserFromUserID(id))
        } else {
            SBSetModeParam(mode, bindDao.getSBBindFromQQ(event.sender.id, true))
        }
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: SBSetModeParam) {
        event.reply(getReply(param, event))
    }

    override fun accept(event: MessageEvent, messageText: String): SBSetModeParam? {
        val m = OfficialInstruction.SB_SET_MODE.matcher(messageText)

        if (!m.find()) return null

        val mode = OsuMode.getMode(m.group(FLAG_MODE))

        val user = try {
            bindDao.getSBBindFromQQ(-event.sender.id, true)
        } catch (e: BindException) {
            val sbUser = userApiService.getUser(-event.sender.id) ?: throw NoSuchElementException.Player()
            val bindUser = SBBindUser(sbUser)

            bindDao.saveBind(bindUser)!!
        }

        return SBSetModeParam(mode, user)
    }

    @Throws(Throwable::class)
    override fun reply(event: MessageEvent, param: SBSetModeParam): MessageChain? {
        return getReply(param, event)
    }

    private fun getReply(param: SBSetModeParam, event: MessageEvent): MessageChain {
        val predeterminedMode = bindDao.getGroupModeConfig(event)
        val mode = param.mode
        val user = param.user

        bindDao.updateSBMode(user.userID, mode)

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

        return MessageChain(info)
    }
}
