package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_MODE
import com.now.nowbot.util.command.FLAG_QQ_GROUP
import com.now.nowbot.util.command.FLAG_QQ_ID
import com.now.nowbot.util.command.FLAG_RANGE
import org.springframework.stereotype.Service
import kotlin.math.*

@Service("SET_GROUP_MODE")
class SetGroupModeService (
    private val bindDao: BindDao,
    private val imageService: ImageService,
): MessageService<SetGroupModeService.SetGroupParam>{

    data class SetGroupParam(val group: Long?, val mode: OsuMode)

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<SetGroupParam>): Boolean {
        val m = Instruction.SET_GROUP_MODE.matcher(messageText)
        val m2 = Instruction.GROUP_LIST.matcher(messageText)

        if (m.find()) {
            data.value = SetGroupParam(
                m.group(FLAG_QQ_ID)?.toLongOrNull() ?: m.group(FLAG_QQ_GROUP)?.toLongOrNull(),
                OsuMode.getMode(m.group(FLAG_MODE)))
        } else if (m2.find()) {
            data.value = SetGroupParam(0L - (m2.group(FLAG_RANGE)?.toLongOrNull() ?: 1L), OsuMode.DEFAULT)
        } else return false

        return true
    }

    private fun getGroupModeCharts(page: Int = 1): ByteArray {
        val list = bindDao.allGroupMode.map { it.key to it.value }

        val supplier: (Pair<Long, OsuMode>) -> List<String> = {
            pair -> listOf(pair.first.toString(), pair.second.fullName)
        }

        val str = DataUtil.getMarkDownChartFromList(list, page, supplier, """
                | 群聊 QQ | 默认游戏模式 |
                | :-- | :-: |
                """.trimIndent(), maxPerPage = 50)

        return imageService.getPanelA6(str, "group")
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: SetGroupParam) {
        val isSuperAdmin = Permission.isSuperAdmin(event)

        if (param.group != null && param.group < 0L && isSuperAdmin) {
            event.reply(getGroupModeCharts(0 - param.group.toInt()))
            return
        }

        val mode: OsuMode = param.mode

        val predeterminedMode = bindDao.getGroupModeConfig(event)

        val isNotGroupAdmin = Permission.isGroupAdmin(event).not()

        val group = if (param.group != null) {
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
                if (mode.isNotDefault()) {
                    // 无权限，想修改
                    "当前群聊绑定的游戏模式为：${predeterminedMode.fullName}。\n你没有修改群聊绑定游戏模式的权限。"
                } else {
                    // 无权限，不想修改
                    "当前群聊绑定的游戏模式为：${predeterminedMode.fullName}。"
                }
            } else {
                // 修改已有模式状态
                if (mode.isNotDefault()) {
                    bindDao.saveGroupModeConfig(group, mode)
                    "已将群聊绑定的游戏模式 ${predeterminedMode.fullName} 修改为：${mode.fullName}。"
                } else {
                    bindDao.saveGroupModeConfig(group, OsuMode.DEFAULT)
                    "已移除群聊绑定的游戏模式 ${predeterminedMode.fullName}。"
                }
            }
        } else {
            if (isNotGroupAdmin) {
                if (mode.isNotDefault()) {
                    // 无权限，想修改
                    "当前群聊没有已绑定的游戏模式。\n你没有修改群聊绑定游戏模式的权限。"
                } else {
                    // 无权限，不想修改
                    "当前群聊没有已绑定的游戏模式。"
                }
            } else {
                // 赋予新模式状态
                if (mode.isNotDefault()) {
                    bindDao.saveGroupModeConfig(group, mode)
                    "已将群聊绑定的游戏模式修改为：${mode.fullName}。"
                } else {
                    "当前群聊没有已绑定的游戏模式。\n你可以输入 0(osu) / 1(taiko) / 2(catch) / 3(mania) 来修改群聊的绑定游戏模式。"
                }
            }
        }

        event.reply(text)
    }
}
