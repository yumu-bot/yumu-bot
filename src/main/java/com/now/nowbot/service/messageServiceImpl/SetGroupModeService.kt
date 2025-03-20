package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_MODE
import com.now.nowbot.util.command.FLAG_QQ_GROUP
import org.springframework.stereotype.Service

@Service("SET_GROUP_MODE")
class SetGroupModeService (
    private val bindDao: BindDao,
): MessageService<SetGroupModeService.SetGroupParam>{

    data class SetGroupParam(val group: Long?, val mode: OsuMode)

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<SetGroupParam>): Boolean {
        val m = Instruction.SET_GROUP_MODE.matcher(messageText)
        if (m.find()) {
            data.value = SetGroupParam(m.group(FLAG_QQ_GROUP)?.toLongOrNull(), OsuMode.getMode(m.group(FLAG_MODE)))
            return true
        } else return false
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: SetGroupParam) {
        val mode = param.mode
        val predeterminedMode = bindDao.getGroupModeConfig(event.subject.id)

        val isGroupAdmin = Permission.isGroupAdmin(event)

        if (isGroupAdmin.not()) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Permission_Group)
        }

        val group = if (param.group != null) {
            val isSuperAdmin = Permission.isSuperAdmin(event)

            if (isSuperAdmin || param.group == event.subject.id) {
                param.group
            } else {
                throw GeneralTipsException(GeneralTipsException.Type.G_Permission_Super)
            }
        } else if (event.subject is Group) {
            event.subject.id
        } else {
            // 必须群聊
            throw GeneralTipsException(GeneralTipsException.Type.G_Restricted_Group)
        }

        if (OsuMode.isNotDefaultOrNull(predeterminedMode)) {
            // 修改已有模式状态
            if (OsuMode.isNotDefaultOrNull(mode)) {
                bindDao.saveGroupModeConfig(group, mode)
                event.reply("已将群组的默认游戏模式 ${predeterminedMode.fullName} 修改为: ${mode.fullName}。")
            } else {
                bindDao.saveGroupModeConfig(group, OsuMode.DEFAULT)
                event.reply("已移除群组的默认游戏模式 ${predeterminedMode.fullName}。")
            }
        } else {
            // 赋予新模式状态
            if (OsuMode.isNotDefaultOrNull(mode)) {
                bindDao.saveGroupModeConfig(group, mode)
                event.reply("已将群组的默认游戏模式修改为: ${mode.fullName}。")
            } else {
                throw TipsException("当前群组没有已绑定的游戏模式。\n请输入 0(osu) / 1(taiko) / 2(catch) / 3(mania) 来绑定游戏模式。")
            }
        }
    }
}
