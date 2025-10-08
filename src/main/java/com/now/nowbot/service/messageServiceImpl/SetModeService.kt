package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.SetModeService.SetModeParam
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.command.FLAG_MODE
import com.now.nowbot.util.command.FLAG_NAME
import com.now.nowbot.util.command.FLAG_QQ_ID
import org.springframework.stereotype.Service

@Service("SET_MODE")
class SetModeService (
    private val bindDao: BindDao,
    private val userApiService: OsuUserApiService,
): MessageService<SetModeParam>, TencentMessageService<SetModeParam> {

    data class SetModeParam(val mode: OsuMode, val user: BindUser)

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<SetModeParam>): Boolean {
        val m = Instruction.SET_MODE.matcher(messageText)
        if (!m.find()) return false

        val mode = OsuMode.getMode(m.group(FLAG_MODE))
        val qq = m.group(FLAG_QQ_ID)?.toLongOrNull()
        val name = m.group(FLAG_NAME)?.trim()

        val isSuper = Permission.isSuperAdmin(event.sender.id)

        data.value = if (qq != null && isSuper) {
            SetModeParam(mode, bindDao.getBindFromQQ(qq, false))
        } else if (name.isNullOrBlank().not()) {
            val user = bindDao.getBindUser(name.trim())
                ?: throw IllegalArgumentException.WrongException.PlayerName()
            SetModeParam(mode, user)
        } else {
            SetModeParam(mode, bindDao.getBindFromQQ(event.sender.id, true))
        }
        return true
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: SetModeParam) {
        event.reply(getReply(param, event))
    }

    override fun accept(event: MessageEvent, messageText: String): SetModeParam? {
        val m = OfficialInstruction.SET_MODE.matcher(messageText)

        if (!m.find()) return null

        val mode = OsuMode.getMode(m.group(FLAG_MODE))

        val user = try {
            bindDao.getBindUserFromOsuID(-event.sender.id)
        } catch (e: BindException) {
            val osuUser = userApiService.getOsuUser(-event.sender.id)
            val bindUser = BindUser()
            with(bindUser) {
                this.userID = osuUser.id
                this.username = osuUser.username
                this.mode = osuUser.defaultOsuMode
            }
            bindDao.saveBind(bindUser)
        }

        return SetModeParam(mode, user)
    }

    @Throws(Throwable::class)
    override fun reply(event: MessageEvent, param: SetModeParam): MessageChain? {
        return getReply(param, event)
    }

    private fun getReply(param: SetModeParam, event: MessageEvent): MessageChain {
        val predeterminedMode = bindDao.getGroupModeConfig(event)
        val mode = param.mode
        val user = param.user

        bindDao.updateMode(user.userID, mode)

        val info = if (mode == OsuMode.DEFAULT) {
            if (user.mode.isDefault()) {
                throw TipsException("你没有已绑定的游戏模式。\n请输入 0(osu) / 1(taiko) / 2(catch) / 3(mania) 来修改绑定的游戏模式。")
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
