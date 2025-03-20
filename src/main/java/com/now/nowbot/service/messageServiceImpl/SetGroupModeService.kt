package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_MODE
import com.now.nowbot.util.command.FLAG_QQ_GROUP
import com.now.nowbot.util.command.FLAG_QQ_ID
import org.springframework.stereotype.Service

@Service("SET_GROUP_MODE")
class SetGroupModeService (
    private val bindDao: BindDao,
): MessageService<SetGroupModeService.SetGroupParam>{

    data class SetGroupParam(val group: Long?, val mode: OsuMode)

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<SetGroupParam>): Boolean {
        val m = Instruction.SET_GROUP_MODE.matcher(messageText)
        if (m.find()) {
            data.value = SetGroupParam(
                m.group(FLAG_QQ_ID)?.toLongOrNull() ?: m.group(FLAG_QQ_GROUP)?.toLongOrNull(),
                OsuMode.getMode(m.group(FLAG_MODE)))
            return true
        } else return false
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: SetGroupParam) {
        val mode = param.mode
        val predeterminedMode = bindDao.getGroupModeConfig(event.subject.id)

        val isNotGroupAdmin = Permission.isGroupAdmin(event).not()

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

        val text = if (OsuMode.isNotDefaultOrNull(predeterminedMode)) {
            if (isNotGroupAdmin) {
                if (OsuMode.isNotDefaultOrNull(mode)) {
                    // 无权限，想修改
                    "当前群组绑定的游戏模式为：${predeterminedMode.fullName}。\n你没有修改群组绑定游戏模式的权限。"
                } else {
                    // 无权限，不想修改
                    "当前群组绑定的游戏模式为：${predeterminedMode.fullName}。"
                }
            } else {
                // 修改已有模式状态
                if (OsuMode.isNotDefaultOrNull(mode)) {
                    bindDao.saveGroupModeConfig(group, mode)
                    "已将群组绑定的游戏模式 ${predeterminedMode.fullName} 修改为: ${mode.fullName}。"
                } else {
                    bindDao.saveGroupModeConfig(group, OsuMode.DEFAULT)
                    "已移除群组绑定的游戏模式 ${predeterminedMode.fullName}。"
                }
            }
        } else {
            if (isNotGroupAdmin) {
                if (OsuMode.isNotDefaultOrNull(mode)) {
                    // 无权限，想修改
                    "当前群组没有已绑定的游戏模式。\n你没有修改群组绑定游戏模式的权限。"
                } else {
                    // 无权限，不想修改
                    "当前群组没有已绑定的游戏模式。"
                }
            } else {
                // 赋予新模式状态
                if (OsuMode.isNotDefaultOrNull(mode)) {
                    bindDao.saveGroupModeConfig(group, mode)
                    "已将群组绑定的游戏模式修改为: ${mode.fullName}。"
                } else {
                    "当前群组没有已绑定的游戏模式。\n你可以输入 0(osu) / 1(taiko) / 2(catch) / 3(mania) 来修改群组的绑定游戏模式。"
                }
            }
        }

        event.reply(text)
    }
}
